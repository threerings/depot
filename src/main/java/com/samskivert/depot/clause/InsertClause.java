//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;
import java.util.Set;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * Builds actual SQL given a main persistent type and some {@link QueryClause} objects.
 */
public class InsertClause implements QueryClause
{
    public InsertClause (Class<? extends PersistentRecord> pClass, Object pojo,
                         Set<String> identityFields)
    {
        _pClass = pClass;
        _pojo = pojo;
        _idFields = identityFields;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public Object getPojo ()
    {
        return _pojo;
    }

    public Set<String> getIdentityFields ()
    {
        return _idFields;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
        // If we add SQLExpression[] values INSERT, remember to recurse into them here.
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    protected Class<? extends PersistentRecord> _pClass;

    /** The object from which to fetch values, or null. */
    protected Object _pojo;

    protected Set<String> _idFields;
}
