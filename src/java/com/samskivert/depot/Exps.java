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

import com.samskivert.depot.expression.*;

/**
 * Provides static methods for expression construction. For example: {@link #literal}, {@link
 * #value} and {@link #years}.
 */
public class Exps
{
    /**
     * Wraps the supplied object in a {@link ValueExp}.
     */
    public static ValueExp value (Object value)
    {
        return new ValueExp(value);
    }

    /**
     * Creates a {@link LiteralExp} with the supplied SQL snippet. Note: you're probably breaking
     * cross platform compatibility by using this construction.
     */
    public static LiteralExp literal (String text)
    {
        return new LiteralExp(text);
    }

    /**
     * Creates an interval for the specified number of years.
     */
    public static IntervalExp years (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.YEAR, amount);
    }

    /**
     * Creates an interval for the specified number of months.
     */
    public static IntervalExp months (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.MONTH, amount);
    }

    /**
     * Creates an interval for the specified number of days.
     */
    public static IntervalExp days (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.DAY, amount);
    }

    /**
     * Creates an interval for the specified number of hours.
     */
    public static IntervalExp hours (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.HOUR, amount);
    }

    /**
     * Creates an interval for the specified number of minutes.
     */
    public static IntervalExp minutes (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.MINUTE, amount);
    }

    /**
     * Creates an interval for the specified number of seconds.
     */
    public static IntervalExp seconds (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.SECOND, amount);
    }

    /**
     * Creates an expression that converts the supplied expression into seconds since the epoch.
     */
    @SuppressWarnings("deprecation")
    public static EpochSeconds epochSeconds (SQLExpression expr)
    {
        return new EpochSeconds(expr);
    }
}
