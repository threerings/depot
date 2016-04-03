//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.google.common.collect.Iterables;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * The SQL 'in (...)' operator.
 */
public class In extends FluentExp<Boolean>
{
    /** The maximum number of keys allowed in an IN() clause. */
    public static final int MAX_KEYS = Short.MAX_VALUE;

    public In (SQLExpression<?> expression, Comparable<?>... values)
    {
        _expression = expression;
        _values = values;
    }

    public In (SQLExpression<?> pColumn, Iterable<? extends Comparable<?>> values)
    {
        this(pColumn, Iterables.toArray(values, Comparable.class));
    }

    public SQLExpression<?> getExpression ()
    {
        return _expression;
    }

    public Comparable<?>[] getValues ()
    {
        return _values;
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _expression.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(_expression).append(" in (");
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append((_values[ii] instanceof Number) ?
                String.valueOf(_values[ii]) : ("'" + _values[ii] + "'"));
        }
        return builder.append(")").toString();
    }

    protected SQLExpression<?> _expression;
    protected Comparable<?>[] _values;
}
