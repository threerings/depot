//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * The SQL 'is null' operator.
 */
public class IsNull
    implements SQLExpression<Boolean>
{
    public IsNull (SQLExpression<?> expression)
    {
        _expression = expression;
    }

    public SQLExpression<?> getExpression ()
    {
        return _expression;
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
    }

    @Override // from Object
    public String toString ()
    {
        return "IsNull(" + _expression + ")";
    }

    protected SQLExpression<?> _expression;
}
