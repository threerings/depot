//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.clause;

import java.util.Collection;
import java.util.List;

import com.samskivert.util.Tuple;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.expression.SQLExpression;

import com.samskivert.depot.impl.FragmentVisitor;

/**
 * Represents an CREATE INDEX instruction to the database.
 */
public class CreateIndexClause
    implements QueryClause
{
    /**
     * Create a new {@link CreateIndexClause} clause. The name must be unique within the relevant
     * database.
     */
    public CreateIndexClause (Class<? extends PersistentRecord> pClass, String name, boolean unique,
                              List<Tuple<SQLExpression<?>, Order>> fields)
    {
        _pClass = pClass;
        _name = name;
        _unique = unique;
        _fields = fields;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public String getName ()
    {
        return _name;
    }

    public boolean isUnique ()
    {
        return _unique;
    }

    public List<Tuple<SQLExpression<?>,Order>> getFields ()
    {
        return _fields;
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
    protected boolean _unique;

    /** The components of the index, e.g. columns or functions of columns. */
    protected List<Tuple<SQLExpression<?>,Order>> _fields;
}
