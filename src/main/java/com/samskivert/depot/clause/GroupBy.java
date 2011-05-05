//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 *  Represents a GROUP BY clause.
 */
public class GroupBy implements QueryClause
{
    public GroupBy (SQLExpression<?>... values)
    {
        _values = values;
    }

    public SQLExpression<?>[] getValues ()
    {
        return _values;
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
    }

    /** The expressions that are generated for the clause. */
    protected SQLExpression<?>[] _values;

}
