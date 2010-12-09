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
import com.samskivert.depot.impl.expression.StringFun.*;

/**
 * Provides static methods for string-related function construction.
 */
public class StringFuncs
{
    /**
     * Creates an expression that evaluates to the string length of the supplied expression.
     */
    public static FluentExp<Integer> length (SQLExpression<String> exp)
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
    public static FluentExp<Integer> position (SQLExpression<String> substring,
                                               SQLExpression<String> string)
    {
        return new Position(substring, string);
    }

    /**
     * Creates an expression that evaluates to a substring of the given string expression,
     * starting at the given index and of the given length.
     */
    public static FluentExp<String> substring (
        SQLExpression<String> string, SQLExpression<String> from, SQLExpression<Integer> count)
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
