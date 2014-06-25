//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.QueryClause;

import com.samskivert.depot.impl.FragmentVisitor;

/**
 * Represents an DROP INDEX instruction to the database.
 */
public class DropIndexClause
    implements QueryClause
{
    public DropIndexClause (Class<? extends PersistentRecord> pClass, String name)
    {
        _pClass = pClass;
        _name = name;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public String getName ()
    {
        return _name;
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    protected Class<? extends PersistentRecord> _pClass;

    protected String _name;
}
