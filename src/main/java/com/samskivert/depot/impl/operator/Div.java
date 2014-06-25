//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '/' operator.
 */
public class Div<T extends Number> extends Arithmetic<T>
{
    public Div (SQLExpression<?> column, T value)
    {
        super(column, value);
    }

    public Div (SQLExpression<?>... values)
    {
        super(values);
    }

    @Override // from Arithmetic
    public String operator()
    {
        return " / "; // Pad with spaces to work-around a MySQL bug.
    }

    @Override // from Arithmetic
    public Object evaluate (Object[] operands)
    {
        for (int ii = 1; ii < operands.length; ii ++) {
            if (Double.valueOf(0).equals(NUMERICAL.apply(operands[ii]))) {
                return new NoValue("Division by zero in: " + StringUtil.toString(operands));
            }
        }
        return evaluate(operands, "/", new Accumulator<Double>() {
            public Double accumulate (Double left, Double right) {
                return left / right;
            }
        }, new Accumulator<Long>() {
            public Long accumulate (Long left, Long right) {
                return left / right;
            }
        });
    }
}
