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
    public static FluentExp abs (SQLExpression expr)
    {
        return new Abs(expr);
    }

    /**
     * Creates an expression that computes the integer ceiling of the supplied expression.
     */
    public static FluentExp ceil (SQLExpression exp)
    {
        return new Ceil(exp);
    }

    /**
     * Creates an expression that computes the exponential of the supplied expression.
     */
    public static FluentExp exp (SQLExpression exp)
    {
        return new Exp(exp);
    }

    /**
     * Creates an expression that computes the integer floor of the supplied expression.
     */
    public static FluentExp floor (SQLExpression exp)
    {
        return new Floor(exp);
    }

    /**
     * Creates an expression that computes the natural logarithm of the supplied expression.
     */
    public static FluentExp ln (SQLExpression exp)
    {
        return new Ln(exp);
    }

    /**
     * Creates an expression that computes the base-10 logarithm of the supplied expression.
     */
    public static FluentExp log10 (SQLExpression value)
    {
        return new Log10(value);
    }

    /**
     * Creates an expression that evaluates to the constant PI.
     */
    public static FluentExp pi ()
    {
        return new Pi();
    }

    /**
     * Creates an expression that computes the value expression to the given power.
     */
    public static FluentExp power (SQLExpression value, SQLExpression power)
    {
        return new Power(value, power);
    }

    /**
     * Creates an expression that returns a random number between 0.0 and 1.0.
     */
    public static FluentExp random ()
    {
        return new Random();
    }

    /**
     * Creates an expression that computes the whole number nearest the supplied expression.
     */
    public static FluentExp round (SQLExpression exp)
    {
        return new Round(exp);
    }

    /**
     * Creates an expression that computes the sign of the supplied expression.
     */
    public static FluentExp sign (SQLExpression exp)
    {
        return new Sign(exp);
    }

    /**
     * Creates an expression that computes the square root of the supplied expression.
     */
    public static FluentExp sqrt (SQLExpression exp)
    {
        return new Sqrt(exp);
    }

    /**
     * Creates an expression that computes the truncation of the supplied expression,
     * i.e. the next closest whole number to zero.
     */
    public static FluentExp trunc (SQLExpression exp)
    {
        return new Trunc(exp);
    }
}
