//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.StringFun.*;

/**
 * Provides static methods for string-related function construction.
 */
public class StringFuncs
{
    /**
     * Creates an expression that evaluates to the string length of the supplied expression.
     */
    public static FluentExp<Number> length (SQLExpression<String> exp)
    {
        return new Length(exp);
    }

    /**
     * Creates an expression that down-cases the supplied expression.
     */
    public static FluentExp<String> lower (SQLExpression<String> exp)
    {
        return new Lower(exp);
    }

    /**
     * Creates an expression that locates the given substring expression within the given
     * string expression and returns the index.
     */
    public static FluentExp<Number> position (SQLExpression<String> substring,
                                              SQLExpression<String> string)
    {
        return new Position(substring, string);
    }

    /**
     * Creates an expression that evaluates to a substring of the given string expression,
     * starting at the given index and of the given length.
     */
    public static FluentExp<String> substring (SQLExpression<String> string, int from, int count)
    {
        return new Substring(string, Exps.value(from), Exps.value(count));
    }

    /**
     * Creates an expression that evaluates to a substring of the given string expression,
     * starting at the given index and of the given length.
     */
    public static FluentExp<String> substring (SQLExpression<String> string,
                                               SQLExpression<Integer> from,
                                               SQLExpression<Integer> count)
    {
        return new Substring(string, from, count);
    }

    /**
     * Creates an expression that removes whitespace from the beginning and end of the supplied
     * string expression.
     */
    public static FluentExp<String> trim (SQLExpression<String> exp)
    {
        return new Trim(exp);
    }

    /**
     * Creates an expression that up-cases the supplied string expression.
     */
    public static FluentExp<String> upper (SQLExpression<String> exp)
    {
        return new Upper(exp);
    }
}
