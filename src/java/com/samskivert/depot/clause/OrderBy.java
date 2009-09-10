//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.util.ArrayUtil;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.depot.impl.expression.LiteralExp;

/**
 *  Represents an ORDER BY clause.
 */
public class OrderBy implements QueryClause
{
    /** Indicates the order of the clause. */
    public enum Order { ASC, DESC };

    /**
     * Creates and returns a random order by clause.
     */
    public static OrderBy random ()
    {
        return ascending(new LiteralExp("rand()"));
    }

    /**
     * Creates and returns an ascending order by clause on the supplied expression.
     */
    public static OrderBy ascending (SQLExpression value)
    {
        return new OrderBy(new SQLExpression[] { value } , new Order[] { Order.ASC });
    }

    /**
     * Creates and returns a descending order by clause on the supplied expression.
     */
    public static OrderBy descending (SQLExpression value)
    {
        return new OrderBy(new SQLExpression[] { value }, new Order[] { Order.DESC });
    }

    public OrderBy (SQLExpression[] values, Order[] orders)
    {
        _values = values;
        _orders = orders;
    }

    public SQLExpression[] getValues ()
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
    public OrderBy thenAscending (SQLExpression value)
    {
        return new OrderBy(ArrayUtil.append(_values, value),
                           ArrayUtil.append(_orders, Order.ASC));
    }

    /**
     * Creates and returns a descending order by clause on the supplied expression.
     */
    public OrderBy thenDescending (SQLExpression value)
    {
        return new OrderBy(ArrayUtil.append(_values, value),
                           ArrayUtil.append(_orders, Order.DESC));
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        for (SQLExpression expression : _values) {
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
    protected SQLExpression[] _values;

    /** Whether the ordering is to be ascending or descending. */
    protected Order[] _orders;

}
