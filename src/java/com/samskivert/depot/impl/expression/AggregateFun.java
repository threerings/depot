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
import com.samskivert.depot.impl.expression.Function.OneArgFun;

public abstract class AggregateFun extends OneArgFun
{
    public static class Average extends AggregateFun {
        public Average (SQLExpression argument) {
            this(argument, false);
        }
        public Average (SQLExpression argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "average";
        }
    }

    public static class Count extends AggregateFun {
        public Count (SQLExpression argument) {
            this(argument, false);
        }
        public Count (SQLExpression argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "count";
        }
    }

    public static class Every extends AggregateFun {
        public Every (SQLExpression argument) {
            this(argument, false);
        }
        public Every (SQLExpression argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "every";
        }
    }

    public static class Max extends AggregateFun {
        public Max (SQLExpression argument) {
            this(argument, false);
        }
        public Max (SQLExpression argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "max";
        }
    }

    public static class Min extends AggregateFun {
        public Min (SQLExpression argument) {
            this(argument, false);
        }
        public Min (SQLExpression argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "min";
        }
    }

    public static class Sum extends AggregateFun {
        public Sum (SQLExpression argument) {
            this(argument, false);
        }
        public Sum (SQLExpression argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "sum";
        }
    }

    public AggregateFun (SQLExpression argument, boolean distinct)
    {
        super(argument);
        _distinct = distinct;
    }

    public boolean isDistinct ()
    {
        return _distinct;
    }

    public String toString ()
    {
        return getCanonicalFunctionName() + "(" + (_distinct ? "distinct " : "") + _arg + ")";
    }

    protected boolean _distinct;

}
