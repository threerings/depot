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
     * Creates an expression that computes the integer ceiling of the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening or conversion under the hood, and using Number allows the application to do the
     * appropriate conversion at runtime.
     */
    public static FluentExp<Number> ceil (SQLExpression<? extends Number> exp)
    {
        return new Ceil<Number>(exp);
    }

    /**
     * Creates an expression that computes the integer floor of the supplied expression. Note that
     * this method forces the result to type {@link Number} because databases may perform widening
     * or conversion under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> floor (SQLExpression<? extends Number> exp)
    {
        return new Floor<Number>(exp);
    }

    /**
     * Creates an expression that computes the exponential of the supplied expression. Note that
     * this method forces the result to type {@link Number} because databases may perform widening
     * or conversion under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> exp (SQLExpression<? extends Number> exp)
    {
        return new Exp<Number>(exp);
    }

    /**
     * Creates an expression that computes the natural logarithm of the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening or conversion under the hood, and using Number allows the application to do the
     * appropriate conversion at runtime.
     */
    public static FluentExp<Number> ln (SQLExpression<? extends Number> exp)
    {
        return new Ln<Number>(exp);
    }

    /**
     * Creates an expression that computes the base-10 logarithm of the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening or conversion under the hood, and using Number allows the application to do the
     * appropriate conversion at runtime.
     */
    public static FluentExp<Number> log10 (SQLExpression<? extends Number> value)
    {
        return new Log10<Number>(value);
    }

    /**
     * Creates an expression that evaluates to the constant PI. You will need to manually specify
     * the type your database returns for PI if you plan on actually selecting this value.
     */
    public static <T extends Number> FluentExp<T> pi ()
    {
        return new Pi<T>();
    }

    /**
     * Creates an expression that computes the value expression to the given power. Note that this
     * method forces the result to type {@link Number} because databases may perform widening or
     * conversion under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> power (SQLExpression<? extends Number> value,
                                           SQLExpression<? extends Number> power)
    {
        return new Power<Number>(value, power);
    }

    /**
     * Creates an expression that returns a random number between 0.0 and 1.0. You will need to
     * manually specify the type your database returns for random values if you plan on actually
     * selecting this value.
     */
    public static <T extends Number> FluentExp<T> random ()
    {
        return new Random<T>();
    }

    /**
     * Creates an expression that computes the whole number nearest the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening or conversion under the hood, and using Number allows the application to do the
     * appropriate conversion at runtime.
     */
    public static FluentExp<Number> round (SQLExpression<? extends Number> exp)
    {
        return new Round<Number>(exp);
    }

    /**
     * Creates an expression that computes the sign of the supplied expression. Note that this
     * method forces the result to type {@link Number} because databases may perform widening or
     * conversion under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> sign (SQLExpression<? extends Number> exp)
    {
        return new Sign<Number>(exp);
    }

    /**
     * Creates an expression that computes the square root of the supplied expression. Note that
     * this method forces the result to type {@link Number} because databases may perform widening
     * or conversion under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> sqrt (SQLExpression<? extends Number> exp)
    {
        return new Sqrt<Number>(exp);
    }

    /**
     * Creates an expression that computes the truncation of the supplied expression, i.e. the next
     * closest whole number to zero. Note that this method forces the result to type {@link Number}
     * because databases may perform widening or conversion under the hood, and using Number allows
     * the application to do the appropriate conversion at runtime.
     */
    public static FluentExp<Number> trunc (SQLExpression<? extends Number> exp)
    {
        return new Trunc<Number>(exp);
    }
}
