//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 *  Represents a DISTINCT [ON <exp>] clause.
 *
 *  Note: You almost certainly only ever want to use this in a SELECT statement.
 */
public class Distinct implements QueryClause
{
    public Distinct (SQLExpression<?> on)
    {
        _on = on;
    }

    public SQLExpression<?> getDistinctOn ()
    {
        return _on;
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

    /** The expression to distinguish by, for DISTINCT ON, or null for mere DISTINCT. */
    protected SQLExpression<?> _on;

}
