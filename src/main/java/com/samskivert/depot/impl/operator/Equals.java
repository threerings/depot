//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '=' operator.
 */
public class Equals extends BinaryOperator<Boolean>
{
    public Equals (SQLExpression<?> column, Comparable<?> value)
    {
        super(column, value);
    }

    public Equals (SQLExpression<?> column, SQLExpression<?> value)
    {
        super(column, value);
    }

    @Override // from BinaryOperator
    public String operator()
    {
        return "=";
    }

    @Override // from BinaryOperator
    public Object evaluate (Object left, Object right)
    {
        if (left == null || right == null) {
            return new NoValue("Null operand to '=': (" + left + ", " + right + ")");
        }
        return left.equals(right);
    }
}
