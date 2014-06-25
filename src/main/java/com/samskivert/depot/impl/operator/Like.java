//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL 'like' (and 'not like') operator.
 */
public class Like extends BinaryOperator<Boolean>
{
    public Like (SQLExpression<?> column, Comparable<?> value, boolean like)
    {
        super(column, value);
        _like = like;
    }

    public Like (SQLExpression<?> column, SQLExpression<?> value, boolean like)
    {
        super(column, value);
        _like = like;
    }

    @Override // from BinaryOperator
    public String operator()
    {
        return _like ? " like " : " not like ";
    }

    @Override // from BinaryOperator
    public Object evaluate (Object left, Object right)
    {
        return new NoValue("Like operator not implemented");
    }

    protected boolean _like;
}
