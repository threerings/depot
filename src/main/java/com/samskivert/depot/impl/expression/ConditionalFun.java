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

public abstract class ConditionalFun
{
    public static class Coalesce<T> extends ManyArgFun<T> {
        public Coalesce (SQLExpression<? extends T>... args) {
            super(args);
        }
        public Coalesce (SQLExpression<? extends T> arg1, SQLExpression<? extends T> arg2) {
            super(arg1, arg2);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "coalesce";
        }
    }

    public static class Greatest<T extends Number> extends ManyArgFun<T> {
        public Greatest (SQLExpression<? extends T>... args) {
            super(args);
        }
        public Greatest (SQLExpression<? extends T> arg1, SQLExpression<? extends T> arg2) {
            super(arg1, arg2);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "greatest";
        }
    }

    public static class Least<T extends Number> extends ManyArgFun<T> {
        public Least (SQLExpression<? extends T>... args) {
            super(args);
        }
        public Least (SQLExpression<? extends T> arg1, SQLExpression<? extends T> arg2) {
            super(arg1, arg2);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "least";
        }
    }
}
