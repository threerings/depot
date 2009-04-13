//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
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

package com.samskivert.depot.operator;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.SQLOperator.MultiOperator;
import com.samskivert.util.StringUtil;

/**
 * A convenient container for implementations of arithmetic operators. Classes that value brevity
 * that feel otherwise will use Arithmetic.Add() and Arithmetic.Sub().
 */
public abstract class Arithmetic extends MultiOperator
{
    /** The SQL '+' operator. */
    public static class Add extends Arithmetic
    {
        public Add (SQLExpression column, Comparable<?> value)
        {
            super(column, new ValueExp(value));
        }

        public Add (SQLExpression... values)
        {
            super(values);
        }

        @Override public String operator()
        {
            return "+";
        }

        @Override
        public Object evaluate (Object[] operands)
        {
            return evaluate(operands, "+", new Accumulator<Double>() {
                @Override public Double accumulate (Double left, Double right) {
                    return left + right;
                }}, new Accumulator<Long>() {
                @Override public Long accumulate (Long left, Long right) {
                    return left + right;
                }});
        }
    }

    /** The SQL '-' operator. */
    public static class Sub extends Arithmetic
    {
        public Sub (SQLExpression column, Comparable<?> value)
        {
            super(column, new ValueExp(value));
        }

        public Sub (SQLExpression... values)
        {
            super(values);
        }

        @Override public String operator()
        {
            return "-";
        }

        @Override
        public Object evaluate (Object[] operands)
        {
            return evaluate(operands, "-", new Accumulator<Double>() {
                @Override public Double accumulate (Double left, Double right) {
                    return left - right;
                }}, new Accumulator<Long>() {
                @Override public Long accumulate (Long left, Long right) {
                    return left - right;
                }});
        }
    }

    /** The SQL '*' operator. */
    public static class Mul extends Arithmetic
    {
        public Mul (SQLExpression column, Comparable<?> value)
        {
            super(column, new ValueExp(value));
        }

        public Mul (SQLExpression... values)
        {
            super(values);
        }

        @Override public String operator()
        {
            return "*";
        }

        @Override
        public Object evaluate (Object[] operands)
        {
            return evaluate(operands, "*", new Accumulator<Double>() {
                @Override public Double accumulate (Double left, Double right) {
                    return left * right;
                }}, new Accumulator<Long>() {
                @Override public Long accumulate (Long left, Long right) {
                    return left * right;
                }});
        }
    }

    /** The SQL '/' operator. */
    public static class Div extends Arithmetic
    {
        public Div (SQLExpression column, Comparable<?> value)
        {
            super(column, new ValueExp(value));
        }

        public Div (SQLExpression... values)
        {
            super(values);
        }

        @Override public String operator()
        {
            return " / "; // Pad with spaces to work-around a MySQL bug.
        }

        @Override
        public Object evaluate (Object[] operands)
        {
            for (int i = 1; i < operands.length; i ++) {
                if (NUMERICAL.apply(operands[i]) == Double.valueOf(0)) {
                    return new NoValue("Division by zero in: " + StringUtil.toString(operands));
                }
            }
            return evaluate(operands, "/", new Accumulator<Double>() {
                @Override public Double accumulate (Double left, Double right) {
                    return left / right;
                }}, new Accumulator<Long>() {
                @Override public Long accumulate (Long left, Long right) {
                    return left / right;
                }});
        }
    }

    /** The SQL '&' operator. */
    public static class BitAnd extends Arithmetic
    {
        public BitAnd (SQLExpression column, Comparable<?> value)
        {
            super(column, new ValueExp(value));
        }

        public BitAnd (SQLExpression... values)
        {
            super(values);
        }

        @Override public String operator()
        {
            return "&";
        }

        @Override
        public Object evaluate (Object[] operands)
        {
            return evaluate(operands, "&", null, new Accumulator<Long>() {
                @Override public Long accumulate (Long left, Long right) {
                    return left & right;
                }});
        }
    }

    /** The SQL '|' operator. */
    public static class BitOr extends Arithmetic
    {
        public BitOr (SQLExpression column, Comparable<?> value)
        {
            super(column, new ValueExp(value));
        }

        public BitOr (SQLExpression... values)
        {
            super(values);
        }

        @Override public String operator()
        {
            return "|";
        }

        @Override
        public Object evaluate (Object[] operands)
        {
            return evaluate(operands, "|", null, new Accumulator<Long>() {
                @Override public Long accumulate (Long left, Long right) {
                    return left | right;
                }});
        }
    }

    public Arithmetic (SQLExpression column, Comparable<?> value)
    {
        super(column, new ValueExp(value));
    }

    public Arithmetic (SQLExpression... values)
    {
        super(values);
    }

    protected Object evaluate (
        Object[] ops, String name, Accumulator<Double> dAcc, Accumulator<Long> iAcc)
    {
        if (dAcc != null && all(NUMERICAL, ops)) {
            return accumulate(NUMERICAL, ops, 0.0, dAcc);
        }

        if (iAcc != null && all(INTEGRAL, ops)) {
            return accumulate(INTEGRAL, ops, 0L, iAcc);
        }
        return new NoValue("Non-numeric operand for '" + name + "' (" + ops + ")");
    }
}
