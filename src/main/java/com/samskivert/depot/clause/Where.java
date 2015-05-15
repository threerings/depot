//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.google.common.collect.Lists;

import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.operator.Equals;
import com.samskivert.depot.impl.operator.IsNull;

/**
 * Represents a where clause: the condition can be any comparison operator or logical combination
 * thereof.
 */
public class Where extends WhereClause
{
    public <V extends Comparable<? super V>> Where (ColumnExp<V> column, V value)
    {
        this(new ColumnExp<?>[] { column }, new Comparable<?>[] { value });
    }

    public <V1 extends Comparable<? super V1>, V2 extends Comparable<? super V2>> Where (
        ColumnExp<V1> index1, V1 value1, ColumnExp<V2> index2, V2 value2)
    {
        this(new ColumnExp<?>[] { index1, index2 }, new Comparable<?>[] { value1, value2 });
    }

    public <V1 extends Comparable<? super V1>, V2 extends Comparable<? super V2>,
            V3 extends Comparable<? super V3>>
        Where (ColumnExp<V1> index1, V1 value1,
               ColumnExp<V2> index2, V2 value2,
               ColumnExp<V3> index3, V3 value3)
    {
        this(new ColumnExp<?>[] { index1, index2, index3 },
             new Comparable<?>[] { value1, value2, value3 });
    }

    public Where (ColumnExp<?>[] columns, Comparable<?>[] values)
    {
        this(toCondition(columns, values));
    }

    public Where (SQLExpression<?> condition)
    {
        _condition = condition;
    }

    public Where (SQLExpression<?> condition1, SQLExpression<?>... andConditions)
    {
        _condition = Ops.and(Lists.asList(condition1, andConditions));
    }

    @Override // from WhereClause
    public SQLExpression<?> getWhereExpression ()
    {
        return _condition;
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _condition.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        return String.valueOf(_condition);
    }

    protected static SQLExpression<?> toCondition (ColumnExp<?>[] columns, Comparable<?>[] values)
    {
        SQLExpression<?>[] comparisons = new SQLExpression<?>[columns.length];
        for (int ii = 0; ii < columns.length; ii ++) {
            comparisons[ii] = (values[ii] == null) ? new IsNull(columns[ii]) :
                new Equals(columns[ii], values[ii]);
        }
        return Ops.and(comparisons);
    }

    protected SQLExpression<?> _condition;
}
