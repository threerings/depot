//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.ValueExp;

/**
 * A convenient container for implementations of arithmetic operators.
 */
public abstract class Arithmetic<T extends Number> extends MultiOperator<T>
{
    public Arithmetic (SQLExpression<?> column, T value)
    {
        super(column, new ValueExp<T>(value));
    }

    public Arithmetic (SQLExpression<?>... values)
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
