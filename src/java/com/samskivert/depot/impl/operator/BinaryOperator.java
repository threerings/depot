//
// $Id$

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.depot.impl.expression.ValueExp;

/**
 * Does the real work for simple binary operators such as Equals.
 */
public abstract class BinaryOperator extends BaseOperator
{
    public BinaryOperator (SQLExpression lhs, SQLExpression rhs)
    {
        super(lhs, rhs);
    }

    public BinaryOperator (SQLExpression lhs, Comparable<?> rhs)
    {
        this(lhs, new ValueExp(rhs));
    }

    /**
     * Returns the string representation of the operator.
     */
    public abstract String operator();

    /**
     * Calculates our value.
     */
    public abstract Object evaluate (Object left, Object right);

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

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
