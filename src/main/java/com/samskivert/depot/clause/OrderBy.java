//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.util.ArrayUtil;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.expression.LiteralExp;
import com.samskivert.depot.impl.expression.RandomExp;

/**
 *  Represents an ORDER BY clause.
 */
public class OrderBy implements QueryClause
{
    /** Indicates the order of the clause. */
    public enum Order
    {
        /** Ordering by null can be used to suppress sorting in the database when using
         * a GROUP BY in your query (postgres and mysql only). */
        NULL,

        /** Ascending (nulls last). */
        ASC,

        /** Descending (nulls first). */
        DESC,

        /** Ascending; nulls first (postgres only). */
        ASC_NULLS_FIRST,

        /** Descending; nulls last (postgres only). */
        DESC_NULLS_LAST;

        @Override
        public String toString ()
        {
            return name().replace('_', ' ');
        }
    }

    /**
     * Creates and returns a random order by clause.
     */
    public static OrderBy random ()
    {
        return ascending(RandomExp.INSTANCE);
    }

    /**
     * Creates and returns an ascending order by clause on the supplied expression.
     */
    public static OrderBy ascending (SQLExpression<?> value)
    {
        return new OrderBy(new SQLExpression<?>[] { value } , new Order[] { Order.ASC });
    }

    /**
     * Creates and returns a descending order by clause on the supplied expression.
     */
    public static OrderBy descending (SQLExpression<?> value)
    {
        return new OrderBy(new SQLExpression<?>[] { value }, new Order[] { Order.DESC });
    }

    public OrderBy (SQLExpression<?>[] values, Order[] orders)
    {
        _values = values;
        _orders = orders;
    }

    public SQLExpression<?>[] getValues ()
    {
        return _values;
    }

    public Order[] getOrders ()
    {
        return _orders;
    }

    /**
     * Concatenates the supplied order expression to this one, returns a new expression.
     */
    public OrderBy thenAscending (SQLExpression<?> value)
    {
        return new OrderBy(ArrayUtil.append(_values, value),
                           ArrayUtil.append(_orders, Order.ASC));
    }

    /**
     * Creates and returns a descending order by clause on the supplied expression.
     */
    public OrderBy thenDescending (SQLExpression<?> value)
    {
        return new OrderBy(ArrayUtil.append(_values, value),
                           ArrayUtil.append(_orders, Order.DESC));
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        for (SQLExpression<?> expression : _values) {
            expression.addClasses(classSet);
        }
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append(_values[ii]).append(" ").append(_orders[ii]);
        }
        return builder.toString();
    }

    /** The expressions that are generated for the clause. */
    protected SQLExpression<?>[] _values;

    /** Whether the ordering is to be ascending or descending. */
    protected Order[] _orders;

}
