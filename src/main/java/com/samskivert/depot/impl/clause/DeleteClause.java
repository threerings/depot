//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.WhereClause;

import com.samskivert.depot.impl.FragmentVisitor;

/**
 * Builds actual SQL given a main persistent type and some {@link QueryClause} objects.
 */
public class DeleteClause
    implements QueryClause
{
    public DeleteClause (Class<? extends PersistentRecord> pClass, WhereClause where, Limit limit)
    {
        _pClass = pClass;
        _where = where;
        _limit = limit;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public WhereClause getWhereClause ()
    {
        return _where;
    }

    public Limit getLimit ()
    {
        return _limit;
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

    /** The type of persistent record on which we operate. */
    protected Class<? extends PersistentRecord> _pClass;

    /** The where clause. */
    protected WhereClause _where;

    /** An optional limit clause. */
    protected Limit _limit;
}
