//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

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
