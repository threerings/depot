//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2009 Michael Bayne and Pär Winzell
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

package com.samskivert.depot;

import java.util.Collection;

import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.operator.Add;
import com.samskivert.depot.impl.operator.Exists;
import com.samskivert.depot.impl.operator.Like;
import com.samskivert.depot.impl.operator.Mul;
import com.samskivert.depot.impl.operator.MultiOperator;
import com.samskivert.depot.impl.operator.Not;

/**
 * Provides static methods for operator construction that don't fit nicely into the fluent style.
 * For example: Ops.and(), Ops.or() and Ops.not().
 */
public class Ops
{
    /**
     * Creates a NOT expression with the supplied target expression.
     */
    public static SQLExpression not (SQLExpression expr)
    {
        return new Not(expr);
    }

    /**
     * Creates an AND expression with the supplied target expressions.
     */
    public static FluentExp and (Collection<? extends SQLExpression> conditions)
    {
        return and(conditions.toArray(new SQLExpression[conditions.size()]));
    }

    /**
     * Creates an AND expression with the supplied target expressions.
     */
    public static FluentExp and (SQLExpression... conditions)
    {
        return new MultiOperator(conditions) {
            @Override public String operator() {
                return " and ";
            }

            @Override public Object evaluate (Object[] values) {
                // if all operands are true, return true, else if all operands are boolean, return
                // false, else return NO_VALUE
                boolean allTrue = true;
                for (Object value : values) {
                    if (value instanceof NoValue) {
                        return value;
                    }
                    if (Boolean.FALSE.equals(value)) {
                        allTrue = false;
                    } else if (!Boolean.TRUE.equals(value)) {
                        return new NoValue("Non-boolean operand to AND: " + value);
                    }
                }
                return allTrue;
            }
        };
    }

    /**
     * Creates an OR expression with the supplied target expressions.
     */
    public static FluentExp or (Collection<? extends SQLExpression> conditions)
    {
        return or(conditions.toArray(new SQLExpression[conditions.size()]));
    }

    /**
     * Creates an OR expression with the supplied target expressions.
     */
    public static FluentExp or (SQLExpression... conditions)
    {
        return new MultiOperator(conditions) {
            @Override public String operator() {
                return " or ";
            }

            @Override public Object evaluate (Object[] values) {
                boolean anyTrue = false;
                for (Object value : values) {
                    if (value instanceof NoValue) {
                        return value;
                    }
                    if (Boolean.TRUE.equals(value)) {
                        anyTrue = true;
                    } else if (!Boolean.FALSE.equals(value)) {
                        return new NoValue("Non-boolean operand to OR: " + value);
                    }
                }
                return anyTrue;
            }
        };
    }

    /**
     * Returns an expression that matches when the source is like the supplied value.
     */
    public static FluentExp like (SQLExpression source, Comparable<?> value)
    {
        return new Like(source, value);
    }

    /**
     * Returns an expression that matches when the source is like the supplied expression.
     */
    public static FluentExp like (SQLExpression source, SQLExpression expr)
    {
        return new Like(source, expr);
    }

    /**
     * Creates an EXISTS expression with the supplied select clause.
     */
    public static SQLExpression exists (SelectClause target)
    {
        return new Exists(target);
    }

    /**
     * Adds the supplied expressions together.
     */
    public static FluentExp add (SQLExpression... exprs)
    {
        return new Add(exprs);
    }

    /**
     * Multiplies the supplied expressions together.
     */
    public static FluentExp mul (SQLExpression... exprs)
    {
        return new Mul(exprs);
    }
}
