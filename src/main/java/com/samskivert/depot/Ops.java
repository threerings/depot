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

import com.google.common.collect.Iterables;

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
    public static SQLExpression<Boolean> not (SQLExpression<Boolean> expr)
    {
        return new Not(expr);
    }

    /**
     * Creates an AND expression with the supplied target expressions.
     */
    public static FluentExp<Boolean> and (Iterable<? extends SQLExpression<?>> conditions)
    {
        return and(Iterables.toArray(conditions, SQLExpression.class));
    }

    /**
     * Creates an AND expression with the supplied target expressions.
     */
    public static FluentExp<Boolean> and (SQLExpression<?>... conditions)
    {
        return new MultiOperator<Boolean>(conditions) {
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
    public static FluentExp<Boolean> or (Iterable<? extends SQLExpression<?>> conditions)
    {
        return or(Iterables.toArray(conditions, SQLExpression.class));
    }

    /**
     * Creates an OR expression with the supplied target expressions.
     */
    public static FluentExp<Boolean> or (SQLExpression<?>... conditions)
    {
        return new MultiOperator<Boolean>(conditions) {
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
    public static FluentExp<Boolean> like (SQLExpression<?> source, Comparable<?> value)
    {
        return new Like(source, value, true);
    }

    /**
     * Returns an expression that matches when the source is like the supplied expression.
     */
    public static FluentExp<Boolean> like (SQLExpression<?> source, SQLExpression<?> expr)
    {
        return new Like(source, expr, true);
    }

    /**
     * Returns an expression that matches when the source is NOT like the supplied value.
     */
    public static FluentExp<Boolean> notLike (SQLExpression<?> source, Comparable<?> value)
    {
        return new Like(source, value, false);
    }

    /**
     * Returns an expression that matches when the source is NOT like the supplied expression.
     */
    public static FluentExp<Boolean> notLike (SQLExpression<?> source, SQLExpression<?> expr)
    {
        return new Like(source, expr, false);
    }

    /**
     * Creates an EXISTS expression with the supplied select clause.
     */
    public static SQLExpression<Boolean> exists (SelectClause target)
    {
        return new Exists(target);
    }

    /**
     * Adds the supplied expressions together.
     */
    public static <T extends Number> FluentExp<T> add (SQLExpression<T> e1, SQLExpression<T> e2)
    {
        return new Add<T>(new SQLExpression<?>[] { e1, e2 });
    }

    /**
     * Adds the supplied expressions together.
     */
    public static <T extends Number> FluentExp<T> add (Iterable<SQLExpression<T>> exprs)
    {
        return new Add<T>(Iterables.toArray(exprs, SQLExpression.class));
    }

    /**
     * Multiplies the supplied expressions together.
     */
    public static <T extends Number> FluentExp<T> mul (SQLExpression<T> e1, SQLExpression<T> e2)
    {
        return new Mul<T>(new SQLExpression<?>[] { e1, e2 });
    }

    /**
     * Multiplies the supplied expressions together.
     */
    public static <T extends Number> FluentExp<T> mul (Iterable<SQLExpression<T>> exprs)
    {
        return new Mul<T>(Iterables.toArray(exprs, SQLExpression.class));
    }
}
