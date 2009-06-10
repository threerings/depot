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

package com.samskivert.depot.expression;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * A code for representing a date interval.
 */
public class IntervalExp
    implements SQLExpression
{
    /** The units that can be used for an interval. */
    public enum Unit { YEAR, MONTH, DAY, HOUR, MINUTE, SECOND };

    /** The unit for this interval. */
    public final Unit unit;

    /** The number of units for this interval. */
    public final int amount;

    /**
     * Creates an interval for the specified number of years.
     */
    public static IntervalExp years (int amount)
    {
        return new IntervalExp(Unit.YEAR, amount);
    }

    /**
     * Creates an interval for the specified number of months.
     */
    public static IntervalExp months (int amount)
    {
        return new IntervalExp(Unit.MONTH, amount);
    }

    /**
     * Creates an interval for the specified number of days.
     */
    public static IntervalExp days (int amount)
    {
        return new IntervalExp(Unit.DAY, amount);
    }

    /**
     * Creates an interval for the specified number of hours.
     */
    public static IntervalExp hours (int amount)
    {
        return new IntervalExp(Unit.HOUR, amount);
    }

    /**
     * Creates an interval for the specified number of minutes.
     */
    public static IntervalExp minutes (int amount)
    {
        return new IntervalExp(Unit.MINUTE, amount);
    }

    /**
     * Creates an interval for the specified number of seconds.
     */
    public static IntervalExp seconds (int amount)
    {
        return new IntervalExp(Unit.SECOND, amount);
    }

    public IntervalExp (Unit unit, int amount)
    {
        this.unit = unit;
        this.amount = amount;
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
    }

    @Override
    public String toString ()
    {
        return amount + " " + unit;
    }
}