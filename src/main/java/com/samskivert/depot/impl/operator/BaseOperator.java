//
// $Id$

package com.samskivert.depot.impl.operator;

import java.util.Arrays;
import java.util.Date;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.ArgumentExp;

/**
 * A base class for all operators.
 */
public abstract class BaseOperator<T> extends ArgumentExp<T>
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

    public static <S, T> T accumulate (Function<S, T> fun, S[] ops, T v,
                                       BaseOperator.Accumulator<T> acc) {
        for (S op : ops) {
            v = acc.accumulate(v, fun.apply(op));
        }
        return v;
    }

    protected BaseOperator (SQLExpression<?>... operands)
    {
        super(operands);
    }

    protected static interface Accumulator<T>
    {
        T accumulate (T left, T right);
    }
}
