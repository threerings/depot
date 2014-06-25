//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '<' operator.
 */
public class LessThan extends BinaryOperator<Boolean>
{
    public LessThan (SQLExpression<?> column, Comparable<?> value)
    {
        super(column, value);
    }

    public LessThan (SQLExpression<?> column, SQLExpression<?> value)
    {
        super(column, value);
    }

    @Override // from BinaryOperator
    public String operator()
    {
        return "<";
    }

    @Override // from BinaryOperator
    public Object evaluate (Object left, Object right)
    {
        if (all(NUMERICAL, left, right)) {
            return NUMERICAL.apply(left) < NUMERICAL.apply(right);
        }
        if (all(STRING, left, right) || all(DATE, left, right)) {
            return compare(STRING, left, right) < 0;
        }
        return new NoValue("Non-comparable operand to '<': (" + left + ", " + right + ")");
    }
}
