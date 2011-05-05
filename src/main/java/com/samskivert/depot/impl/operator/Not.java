//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * Represents the truth negation of another conditon.
 */
public class Not
    implements SQLExpression<Boolean>
{
    public Not (SQLExpression<Boolean> condition)
    {
        _condition = condition;
    }

    public SQLExpression<Boolean> getCondition ()
    {
        return _condition;
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _condition.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        return "Not(" + _condition + ")";
    }

    protected SQLExpression<Boolean> _condition;
}
