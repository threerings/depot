//
// $Id: $
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

package com.samskivert.depot.impl.expression;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.expression.Function.ManyArgFun;
import com.samskivert.depot.impl.expression.Function.OneArgFun;
import com.samskivert.depot.impl.expression.Function.TwoArgFun;

public abstract class StringFun
{
    public static class Length extends OneArgFun<Integer> {
        // can take both String or array types (anything that turns into byte[])
        public Length (SQLExpression<?> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "length";
        }
    }

    public static class Lower extends OneArgFun<String> {
        public Lower (SQLExpression<String> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "lower";
        }
    }

    public static class Position extends TwoArgFun<Integer> {
        public Position (SQLExpression<String> substring, SQLExpression<String> string) {
            super(substring, string);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "position";
        }
        public SQLExpression<?> getSubString () {
            return _arg1;
        }
        public SQLExpression<?> getString () {
            return _arg2;
        }
    }

    public static class Substring extends ManyArgFun<String> {
        public Substring (SQLExpression<String> string, SQLExpression<String> from,
                          SQLExpression<Integer> count) {
            super(string, from, count);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "substring";
        }
    }

    public static class Trim extends OneArgFun<String> {
        public Trim (SQLExpression<String> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "trim";
        }
    }

    public static class Upper extends OneArgFun<String> {
        public Upper (SQLExpression<String> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "upper";
        }
    }
}
