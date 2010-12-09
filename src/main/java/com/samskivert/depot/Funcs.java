//
// $Id: Exps.java 505 2009-08-07 01:58:58Z samskivert $
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

import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.AggregateFun.*;
import com.samskivert.depot.impl.expression.ConditionalFun.*;
import com.samskivert.depot.impl.expression.StringFun.*;

/**
 * Provides static methods for function construction.
 */
public class Funcs
{
    /**
     * Creates an aggregate expression that averages all values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static <T extends Number> FluentExp<T> average (SQLExpression<T> expr)
    {
        return new Average<T>(expr);
    }

    /**
     * Creates an expression that averages all distinct values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static <T extends Number> FluentExp<T> averageDistinct (SQLExpression<T> expr)
    {
        return new Average<T>(expr, true);
    }

    /**
     * Creates an aggregate expression that counts the number of rows from the supplied
     * expression. This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp<Integer> count (SQLExpression<?> expr)
    {
        return new Count(expr);
    }

    /**
     * Creates an aggregate expression that counts the number of distinct values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with a
     * ColumnExp.
     */
    public static FluentExp<Integer> countDistinct (SQLExpression<?> expr)
    {
        return new Count(expr, true);
    }

    /**
     * Creates an aggregate expression that evaluates to true iff every value from the supplied
     * expression is also true. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp<Boolean> every (SQLExpression<?> expr)
    {
        return new Every(expr);
    }

    /**
     * Creates an aggregate expression that finds the largest value in the values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static <T extends Number> FluentExp<T> max (SQLExpression<T> expr)
    {
        return new Max<T>(expr);
    }

    /**
     * Creates an aggregate expression that finds the largest value in the values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static <T extends Number> FluentExp<T> min (SQLExpression<T> expr)
    {
        return new Min<T>(expr);
    }

    /**
     * Creates an aggregate expression that sums all the values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static <T extends Number> FluentExp<T> sum (SQLExpression<T> expr)
    {
        return new Sum<T>(expr);
    }

    /**
     * Creates an expression that evaluates to the first supplied expression that is not null.
     */
    public static <T> FluentExp<T> coalesce (SQLExpression<? extends T> arg1,
                                             SQLExpression<? extends T> arg2)
    {
        return new Coalesce<T>(arg1, arg2);
    }

    /**
     * Creates an expression that evaluates to the first supplied expression that is not null.
     */
    public static <T> FluentExp<T> coalesce (SQLExpression<? extends T>... args)
    {
        return new Coalesce<T>(args);
    }

    /**
     * Creates an expression that evaluates to the largest of the given expressions.
     */
    public static <T> FluentExp<T> greatest (SQLExpression<? extends T> arg1,
                                             SQLExpression<? extends T> arg2)
    {
        return new Greatest<T>(arg1, arg2);
    }

    /**
     * Creates an expression that evaluates to the largest of the given expressions.
     */
    public static <T> FluentExp<T> greatest (SQLExpression<? extends T>... args)
    {
        return new Greatest<T>(args);
    }

    /**
     * Creates an expression that evaluates to the smallest of the given expressions.
     */
    public static <T> FluentExp<T> least (SQLExpression<? extends T> arg1,
                                          SQLExpression<? extends T> arg2)
    {
        return new Least<T>(arg1, arg2);
    }

    /**
     * Creates an expression that evaluates to the smallest of the given expressions.
     */
    public static <T> FluentExp<T> least (SQLExpression<? extends T>... args)
    {
        return new Least<T>(args);
    }

    /**
     * Creates an expression that evaluates to the length of the supplied array column.
     */
    public static FluentExp<Integer> arrayLength (SQLExpression<?> exp)
    {
        return new Length(exp);
    }
}
