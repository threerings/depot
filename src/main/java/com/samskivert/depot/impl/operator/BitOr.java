//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '|' operator.
 */
public class BitOr<T extends Number> extends Arithmetic<T>
{
    public BitOr (SQLExpression<?> column, T value)
    {
        super(column, value);
    }

    public BitOr (SQLExpression<?>... values)
    {
        super(values);
    }

    @Override // from Arithmetic
    public String operator()
    {
        return "|";
    }

    @Override // from Arithmetic
    public Object evaluate (Object[] operands)
    {
        return evaluate(operands, "|", null, new Accumulator<Long>() {
            public Long accumulate (Long left, Long right) {
                return left | right;
            }
        });
    }
}
