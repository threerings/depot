//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.expression;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.expression.Function.ManyArgFun;
import com.samskivert.depot.impl.expression.Function.OneArgFun;
import com.samskivert.depot.impl.expression.Function.TwoArgFun;

public abstract class StringFun
{
    public static class Length extends OneArgFun<Number> {
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

    public static class Position extends TwoArgFun<Number> {
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
        public Substring (SQLExpression<String> string,
                          SQLExpression<Integer> from, SQLExpression<Integer> count) {
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
