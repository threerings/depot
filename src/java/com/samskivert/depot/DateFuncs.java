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
import com.samskivert.depot.impl.expression.DateFun.*;
import com.samskivert.depot.impl.expression.DateFun.DatePart.Part;
import com.samskivert.depot.impl.expression.DateFun.DateTruncate.Truncation;

/**
 * Provides static methods for date-related function construction.
 */
public class DateFuncs
{
    /**
     * Creates an expression that truncates the given timestamp expression to a date.
     */
    public static FluentExp truncToDay (SQLExpression exp)
    {
        return new DateTruncate(exp, Truncation.DAY);
    }

    /**
     * Creates an expression to extract the day-of-month from the the supplied timestamp expression.
     */
    public static FluentExp dayOfMonth (SQLExpression exp)
    {
        return new DatePart(exp, Part.DAY_OF_MONTH);
    }

    /**
     * Creates an expression to extract the day-of-week from the the supplied timestamp expression.
     */
    public static FluentExp dayOfWeek (SQLExpression exp)
    {
        return new DatePart(exp, Part.DAY_OF_WEEK);
    }

    /**
     * Creates an expression to extract the day-of-year from the the supplied timestamp expression.
     */
    public static FluentExp dayOfYear (SQLExpression exp)
    {
        return new DatePart(exp, Part.DAY_OF_YEAR);
    }

    /**
     * Creates an expression to extract the hour of the the supplied timestamp expression.
     */
    public static FluentExp dateHour (SQLExpression exp)
    {
        return new DatePart(exp, Part.HOUR);
    }

    /**
     * Creates an expression to extract the minute of the the supplied timestamp expression.
     */
    public static FluentExp dateMinute (SQLExpression exp)
    {
        return new DatePart(exp, Part.MINUTE);
    }

    /**
     * Creates an expression to extract the month of the the supplied timestamp expression.
     */
    public static FluentExp dateMonth (SQLExpression exp)
    {
        return new DatePart(exp, Part.MONTH);
    }

    /**
     * Creates an expression to extract the second of the the supplied timestamp expression.
     */
    public static FluentExp dateSecond (SQLExpression exp)
    {
        return new DatePart(exp, Part.SECOND);
    }

    /**
     * Creates an expression to extract the week of the the supplied timestamp expression.
     */
    public static FluentExp dateWeek (SQLExpression exp)
    {
        return new DatePart(exp, Part.WEEK);
    }

    /**
     * Creates an expression to extract the year of the the supplied timestamp expression.
     */
    public static FluentExp dateYear (SQLExpression exp)
    {
        return new DatePart(exp, Part.YEAR);
    }

    /**
     * Creates an expression to extract the epoch (aka unix timestamp, aka seconds passed since
     * 1970-01-01) of the the supplied timestamp expression.
     */
    public static FluentExp dateEpoch (SQLExpression exp)
    {
        return new DatePart(exp, Part.EPOCH);
    }

    /**
     * Creates an expression for the current timestamp.
     */
    public static FluentExp now ()
    {
        return new Now();
    }
}
