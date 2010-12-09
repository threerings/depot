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

import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.NumericalFun.*;

/**
 * Provides static methods for math-related function construction.
 */
public class MathFuncs
{
    /**
     * Creates an expression that computes the absolute value of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> abs (SQLExpression<T> expr)
    {
        return new Abs<T>(expr);
    }

    /**
     * Creates an expression that computes the integer ceiling of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> ceil (SQLExpression<T> exp)
    {
        return new Ceil<T>(exp);
    }

    /**
     * Creates an expression that computes the exponential of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> exp (SQLExpression<T> exp)
    {
        return new Exp<T>(exp);
    }

    /**
     * Creates an expression that computes the integer floor of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> floor (SQLExpression<T> exp)
    {
        return new Floor<T>(exp);
    }

    /**
     * Creates an expression that computes the natural logarithm of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> ln (SQLExpression<T> exp)
    {
        return new Ln<T>(exp);
    }

    /**
     * Creates an expression that computes the base-10 logarithm of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> log10 (SQLExpression<T> value)
    {
        return new Log10<T>(value);
    }

    /**
     * Creates an expression that evaluates to the constant PI.
     */
    public static <T extends Number> FluentExp<T> pi ()
    {
        return new Pi<T>();
    }

    /**
     * Creates an expression that computes the value expression to the given power.
     */
    public static <R extends Number, P extends Number> FluentExp<R> power (SQLExpression<R> value,
                                                                           SQLExpression<P> power)
    {
        return new Power<R, P>(value, power);
    }

    /**
     * Creates an expression that returns a random number between 0.0 and 1.0.
     */
    public static <T extends Number> FluentExp<T> random ()
    {
        return new Random<T>();
    }

    /**
     * Creates an expression that computes the whole number nearest the supplied expression.
     */
    public static <T extends Number> FluentExp<T> round (SQLExpression<T> exp)
    {
        return new Round<T>(exp);
    }

    /**
     * Creates an expression that computes the sign of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> sign (SQLExpression<T> exp)
    {
        return new Sign<T>(exp);
    }

    /**
     * Creates an expression that computes the square root of the supplied expression.
     */
    public static <T extends Number> FluentExp<T> sqrt (SQLExpression<T> exp)
    {
        return new Sqrt<T>(exp);
    }

    /**
     * Creates an expression that computes the truncation of the supplied expression,
     * i.e. the next closest whole number to zero.
     */
    public static <T extends Number> FluentExp<T> trunc (SQLExpression<T> exp)
    {
        return new Trunc<T>(exp);
    }
}
