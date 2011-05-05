//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '>=' operator.
 */
public class GreaterThanEquals extends BinaryOperator<Boolean>
{
    public GreaterThanEquals (SQLExpression<?> column, Comparable<?> value)
    {
        super(column, value);
    }

    public GreaterThanEquals (SQLExpression<?> column, SQLExpression<?> value)
    {
        super(column, value);
    }

    @Override // from BinaryOperator
    public String operator()
    {
        return ">=";
    }

    @Override // from BinaryOperator
    public Object evaluate (Object left, Object right)
    {
        if (all(NUMERICAL, left, right)) {
            return NUMERICAL.apply(left) >= NUMERICAL.apply(right);
        }
        if (all(STRING, left, right) || all(DATE, left, right)) {
            return compare(STRING, left, right) >= 0;
        }
        return new NoValue("Non-comparable operand to '>=': (" + left + ", " + right + ")");
    }
}
