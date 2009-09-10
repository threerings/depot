//
// $Id$

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * Represents an operator with any number of operands.
 */
public abstract class MultiOperator extends BaseOperator
{
    public MultiOperator (SQLExpression ... operands)
    {
        super(operands);
    }

    /**
     * Returns the text infix to be used to join expressions together.
     */
    public abstract String operator ();

    /**
     * Calculates our value.
     */
    public abstract Object evaluate (Object[] values);

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    @Override // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        for (SQLExpression operand : _args) {
            operand.addClasses(classSet);
        }
    }

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
