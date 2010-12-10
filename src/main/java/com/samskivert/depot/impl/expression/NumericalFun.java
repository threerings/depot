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
import com.samskivert.depot.impl.expression.Function.NoArgFun;
import com.samskivert.depot.impl.expression.Function.OneArgFun;
import com.samskivert.depot.impl.expression.Function.TwoArgFun;

public abstract class NumericalFun
{
    public static class Abs<T extends Number> extends OneArgFun<T> {
        public Abs (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "abs";
        }
    }

    public static class Ceil<T extends Number> extends OneArgFun<T> {
        public Ceil (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "ceil";
        }
    }

    public static class Exp<T extends Number> extends OneArgFun<T> {
        public Exp (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "exp";
        }
    }

    public static class Floor<T extends Number> extends OneArgFun<T> {
        public Floor (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "floor";
        }
    }

    public static class Ln<T extends Number> extends OneArgFun<T> {
        public Ln (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "ln";
        }
    }

    public static class Log10<T extends Number> extends OneArgFun<T> {
        public Log10 (SQLExpression<? extends T> value) {
            super(value);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "log10";
        }
    }

    public static class Pi<T extends Number> extends NoArgFun<T> {
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "pi";
        }
    }

    public static class Power<T extends Number> extends TwoArgFun<T> {
        public Power (SQLExpression<? extends T> value, SQLExpression<? extends Number> power) {
            super(value, power);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "Power";
        }
        public SQLExpression<?> getValue () {
            return _arg1;
        }
        public SQLExpression<?> getPower () {
            return _arg2;
        }
    }

    public static class Random<T extends Number> extends NoArgFun<T> {
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "random";
        }
    }

    public static class Round<T extends Number> extends OneArgFun<T> {
        public Round (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "round";
        }
    }

    public static class Sign<T extends Number> extends OneArgFun<T> {
        public Sign (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "sign";
        }
    }

    public static class Sqrt<T extends Number> extends OneArgFun<T> {
        public Sqrt (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "sqrt";
        }
    }

    public static class Trunc<T extends Number> extends OneArgFun<T> {
        public Trunc (SQLExpression<? extends T> argument) {
            super(argument);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "trunc";
        }
    }
}
