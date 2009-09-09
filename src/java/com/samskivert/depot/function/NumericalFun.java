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

package com.samskivert.depot.function;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;

public abstract class NumericalFun
{
    public static class Abs extends OneArgFun {
        public Abs (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Ceil extends OneArgFun {
        public Ceil (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Exp extends OneArgFun {
        public Exp (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Floor extends OneArgFun {
        public Floor (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Ln extends OneArgFun {
        public Ln (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Log10 extends OneArgFun {
        public Log10 (SQLExpression value) {
            super(value);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LogN extends TwoArgFun {
        public LogN (SQLExpression base, SQLExpression value) {
            super(base, value);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public SQLExpression getBase () {
            return _arg1;
        }
        public SQLExpression getValue () {
            return _arg2;
        }
    }

    public static class Pi extends NoArgFun {
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Power extends TwoArgFun {
        public Power (SQLExpression value, SQLExpression power) {
            super(value, power);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public SQLExpression getValue () {
            return _arg1;
        }
        public SQLExpression getPower () {
            return _arg2;
        }
    }

    public static class Random extends NoArgFun {
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Round extends OneArgFun {
        public Round (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Sign extends OneArgFun {
        public Sign (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Sqrt extends OneArgFun {
        public Sqrt (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Trunc extends OneArgFun {
        public Trunc (SQLExpression argument) {
            super(argument);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }
}
