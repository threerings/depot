//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.expression.SQLExpression;

/**
 * Describes an index on a persistent class's table.
 */
public class IndexDesc {

    /** The expression that defines this index. */
    public final SQLExpression<?> expr;

    /** The order of the index. */
    public final OrderBy.Order order;

    public IndexDesc (SQLExpression<?> expr, OrderBy.Order order) {
        this.expr = expr;
        this.order = order;
    }

    @Override public int hashCode () {
        return expr.hashCode() ^ order.hashCode();
    }

    @Override public boolean equals (Object other) {
        return (other instanceof IndexDesc) && expr.equals(((IndexDesc)other).expr) &&
            order == ((IndexDesc)other).order;
    }
}
