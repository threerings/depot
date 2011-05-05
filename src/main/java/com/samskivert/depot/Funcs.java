//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

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
     * Creates an aggregate expression that averages all values from the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> average (SQLExpression<? extends Number> expr)
    {
        return new Average<Number>(expr);
    }

    /**
     * Creates an expression that averages all distinct values from the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> averageDistinct (SQLExpression<? extends Number> expr)
    {
        return new Average<Number>(expr, true);
    }

    /**
     * Creates an aggregate expression that counts the number of rows that match the other clauses
     * in this query.
     */
    public static FluentExp<Number> countStar ()
    {
        return new Count(Exps.literal("*"));
    }

    /**
     * Creates an aggregate expression that counts the number of rows from the supplied
     * expression. This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp<Number> count (SQLExpression<?> expr)
    {
        return new Count(expr);
    }

    /**
     * Creates an aggregate expression that counts the number of distinct values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with a
     * ColumnExp.
     */
    public static FluentExp<Number> countDistinct (SQLExpression<?> expr)
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
     * Creates an aggregate expression that sums all the values from the supplied expression. Note
     * that this method forces the result to type {@link Number} because databases may perform
     * widening under the hood, and using Number allows the application to do the appropriate
     * conversion at runtime.
     */
    public static FluentExp<Number> sum (SQLExpression<? extends Number> expr)
    {
        return new Sum<Number>(expr);
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
    public static FluentExp<Number> arrayLength (SQLExpression<?> exp)
    {
        return new Length(exp);
    }
}
