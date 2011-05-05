//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.expression;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.expression.Function.OneArgFun;

public abstract class AggregateFun<T> extends OneArgFun<T>
{
    public static class Average<T extends Number> extends AggregateFun<T> {
        public Average (SQLExpression<? extends T> argument) {
            this(argument, false);
        }
        public Average (SQLExpression<? extends T> argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "average";
        }
    }

    public static class Count extends AggregateFun<Number> {
        public Count (SQLExpression<?> argument) {
            this(argument, false);
        }
        public Count (SQLExpression<?> argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "count";
        }
    }

    public static class Every extends AggregateFun<Boolean> {
        public Every (SQLExpression<?> argument) {
            this(argument, false);
        }
        public Every (SQLExpression<?> argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "every";
        }
    }

    public static class Max<T extends Number> extends AggregateFun<T> {
        public Max (SQLExpression<? extends T> argument) {
            this(argument, false);
        }
        public Max (SQLExpression<? extends T> argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "max";
        }
    }

    public static class Min<T extends Number> extends AggregateFun<T> {
        public Min (SQLExpression<? extends T> argument) {
            this(argument, false);
        }
        public Min (SQLExpression<? extends T> argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "min";
        }
    }

    public static class Sum<T extends Number> extends AggregateFun<T> {
        public Sum (SQLExpression<? extends T> argument) {
            this(argument, false);
        }
        public Sum (SQLExpression<? extends T> argument, boolean distinct) {
            super(argument, distinct);
        }
        public Object accept (FragmentVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public String getCanonicalFunctionName () {
            return "sum";
        }
    }

    public AggregateFun (SQLExpression<?> argument, boolean distinct)
    {
        super(argument);
        _distinct = distinct;
    }

    public boolean isDistinct ()
    {
        return _distinct;
    }

    @Override
    public String toString ()
    {
        return getCanonicalFunctionName() + "(" + (_distinct ? "distinct " : "") + _arg + ")";
    }

    protected boolean _distinct;

}
