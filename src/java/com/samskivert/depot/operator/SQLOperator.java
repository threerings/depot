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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ArgumentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * A common interface for operator hierarchies in SQL. The main purpose of breaking this out from
 * SQLExpression is to capture the recursive nature of e.g. the logical operators, which work on
 * other SQLOperators but not general SQLExpressions.
 */
public interface SQLOperator extends SQLExpression
{
    /**
     * Represents an operator with any number of operands.
     */
    public abstract static class MultiOperator extends BaseOperator
    {
        public MultiOperator (SQLExpression ... operands)
        {
            super(operands);
        }

        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            for (SQLExpression operand : _args) {
                operand.addClasses(classSet);
            }
        }

        /**
         * Returns the text infix to be used to join expressions together.
         */
        public abstract String operator ();

        /**
         * Calculates a value
         */
        public abstract Object evaluate (Object[] values);

        @Override // from Object
        public String toString ()
        {
            StringBuilder builder = new StringBuilder("(");
            for (SQLExpression operand : _args) {
                if (builder.length() > 1) {
                    builder.append(operator());
                }
                builder.append(operand);
            }
            return builder.append(")").toString();
        }
    }

    /**
     * Does the real work for simple binary operators such as Equals.
     */
    public abstract static class BinaryOperator extends BaseOperator
    {
        public BinaryOperator (SQLExpression lhs, SQLExpression rhs)
        {
            super(lhs, rhs);
        }

        public BinaryOperator (SQLExpression lhs, Comparable<?> rhs)
        {
            this(lhs, new ValueExp(rhs));
        }

        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        public abstract Object evaluate (Object left, Object right);

        /**
         * Returns the string representation of the operator.
         */
        public abstract String operator();

        public SQLExpression getLeftHandSide ()
        {
            return _args[0];
        }

        public SQLExpression getRightHandSide ()
        {
            return _args[1];
        }

        @Override // from Object
        public String toString ()
        {
            return "(" + _args[0] + operator() + _args[1] + ")";
        }
    }

    public static abstract class BaseOperator extends ArgumentExp
        implements SQLOperator
    {
        public static Function<Object, Long> INTEGRAL = new Function<Object, Long>() {
            public Long apply (Object o) {
                if ((o instanceof Integer) || (o instanceof Long)) {
                    return ((Number) o).longValue();
                }
                return null;
            }
        };

        public static Function<Object, Double> NUMERICAL = new Function<Object, Double>() {
            public Double apply (Object o) {
                return (o instanceof Number) ? ((Number) o).doubleValue() : null;
            }
        };

        public static Function<Object, String> STRING = new Function<Object, String>() {
            public String apply (Object o) {
                return (o instanceof String) ? (String) o : null;
            }
        };

        public static Function<Object, Date> DATE = new Function<Object, Date>() {
            public Date apply (Object o) {
                return (o instanceof Date) ? (Date) o : null;
            }
        };

        public static <S, T> boolean all (Function<S, T> fun, S... obj) {
            return Iterables.all(Arrays.asList(obj), Predicates.compose(Predicates.isNull(), fun));
        }

        public static <S, T extends Comparable<T>> int compare (Function<S, T> fun, S lhs, S rhs) {
            return fun.apply(lhs).compareTo(fun.apply(rhs));
        }

        public static <S, T> T accumulate (Function<S, T> fun, S[] ops, T v, Accumulator<T> acc) {
            for (S op : ops) {
                v = acc.accumulate(v, fun.apply(op));
            }
            return v;
        }

        protected BaseOperator (SQLExpression... operands)
        {
            super(operands);
        }

        protected static interface Accumulator<T>
        {
            T accumulate (T left, T right);
        }
}
}
