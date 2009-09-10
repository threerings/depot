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

/**
 * Provides static methods for function construction. For example: {@link #round}, {@link
 * #count} and {@link #length}.
 */
public class Funcs
{
    /**
     * Creates an aggregate expression that averages all values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp average (SQLExpression expr)
    {
        return new Average(expr);
    }

    /**
     * Creates an expression that averages all distinct values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp averageDistinct (SQLExpression expr)
    {
        return new Average(expr, true);
    }

    /**
     * Creates an aggregate expression that counts the number of rows from the supplied
     * expression. This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp count (SQLExpression expr)
    {
        return new Count(expr);
    }

    /**
     * Creates an aggregate expression that counts the number of distinct values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with a
     * ColumnExp.
     */
    public static FluentExp countDistinct (SQLExpression expr)
    {
        return new Count(expr, true);
    }

    /**
     * Creates an aggregate expression that evaluates to true iff every value from the supplied
     * expression is also true. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp every (SQLExpression expr)
    {
        return new Every(expr);
    }

    /**
     * Creates an aggregate expression that finds the largest value in the values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp max (SQLExpression expr)
    {
        return new Max(expr);
    }

    /**
     * Creates an aggregate expression that finds the largest value in the values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp min (SQLExpression expr)
    {
        return new Min(expr);
    }

    /**
     * Creates an aggregate expression that sums all the values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp sum (SQLExpression expr)
    {
        return new Sum(expr);
    }

    /**
     * Creates an expression that evaluates to the first supplied expression that is not null.
     */
    public static FluentExp coalesce (SQLExpression... args)
    {
        return new Coalesce(args);
    }

    /**
     * Creates an expression that evaluates to the largest of the given expressions.
     */
    public static FluentExp greatest (SQLExpression... args)
    {
        return new Greatest(args);
    }

    /**
     * Creates an expression that evaluates to the smallest of the given expressions.
     */
    public static FluentExp least (SQLExpression... args)
    {
        return new Least(args);
    }
}
