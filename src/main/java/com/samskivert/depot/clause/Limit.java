//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 *  Represents a LIMIT/OFFSET clause, for pagination.
 */
public class Limit implements QueryClause
{
    public Limit (int offset, int count)
    {
        _offset = offset;
        _count = count;
    }

    public int getOffset ()
    {
        return _offset;
    }

    public int getCount ()
    {
        return _count;
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

    @Override // from Object
    public String toString ()
    {
        return _offset + "-" + (_offset+_count);
    }

    /** The first row of the result set to return. */
    protected int _offset;

    /** The number of rows, at most, to return. */
    protected int _count;
}
