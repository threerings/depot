//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

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

    public static class Greatest<T> extends ManyArgFun<T> {
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

    public static class Least<T> extends ManyArgFun<T> {
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
