//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.expression.*;
import com.samskivert.depot.impl.expression.IntervalExp;
import com.samskivert.depot.impl.expression.LiteralExp;
import com.samskivert.depot.impl.expression.ValueExp;

/**
 * Provides static methods for expression construction. For example: {@link #literal}, {@link
 * #value} and {@link #years}.
 */
public class Exps
{
    /**
     * Wraps the supplied object in a value expression.
     */
    public static <T> FluentExp<T> value (T value)
    {
        return new ValueExp<T>(value);
    }

    /**
     * Creates a literal expression with the supplied SQL snippet. Note: you're probably breaking
     * cross platform compatibility by using this construction.
     */
    public static <T> SQLExpression<T> literal (String text)
    {
        return new LiteralExp<T>(text);
    }

    /**
     * Creates an interval for the specified number of years.
     */
    public static SQLExpression<Integer> years (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.YEAR, amount);
    }

    /**
     * Creates an interval for the specified number of months.
     */
    public static SQLExpression<Integer> months (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.MONTH, amount);
    }

    /**
     * Creates an interval for the specified number of days.
     */
    public static SQLExpression<Integer> days (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.DAY, amount);
    }

    /**
     * Creates an interval for the specified number of hours.
     */
    public static SQLExpression<Integer> hours (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.HOUR, amount);
    }

    /**
     * Creates an interval for the specified number of minutes.
     */
    public static SQLExpression<Integer> minutes (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.MINUTE, amount);
    }

    /**
     * Creates an interval for the specified number of seconds.
     */
    public static SQLExpression<Integer> seconds (int amount)
    {
        return new IntervalExp(IntervalExp.Unit.SECOND, amount);
    }
}
