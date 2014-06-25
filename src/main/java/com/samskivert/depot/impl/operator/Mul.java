//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '*' operator.
 */
public class Mul<T extends Number> extends Arithmetic<T>
{
    public Mul (SQLExpression<?> column, T value)
    {
        super(column, value);
    }

    public Mul (SQLExpression<?>... values)
    {
        super(values);
    }

    @Override // from Arithmetic
    public String operator()
    {
        return "*";
    }

    @Override // from Arithmetic
    public Object evaluate (Object[] operands)
    {
        return evaluate(operands, "*", new Accumulator<Double>() {
            public Double accumulate (Double left, Double right) {
                return left * right;
            }
        }, new Accumulator<Long>() {
            public Long accumulate (Long left, Long right) {
                return left * right;
            }
        });
    }
}
