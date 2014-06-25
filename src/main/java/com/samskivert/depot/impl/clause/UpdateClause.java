//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.samskivert.depot.impl.FragmentVisitor;

/**
 * Builds actual SQL given a main persistent type and some {@link QueryClause} objects.
 */
public class UpdateClause
    implements QueryClause
{
    public UpdateClause (Class<? extends PersistentRecord> pClass, WhereClause where,
                         ColumnExp<?>[] fields, PersistentRecord pojo)
    {
        _pClass = pClass;
        _where = where;
        _fields = fields;
        _values = null;
        _pojo = pojo;
    }

    public UpdateClause (Class<? extends PersistentRecord> pClass, WhereClause where,
                         ColumnExp<?>[] fields, SQLExpression<?>[] values)
    {
        _pClass = pClass;
        _fields = fields;
        _where = where;
        _values = values;
        _pojo = null;
    }

    public WhereClause getWhereClause ()
    {
        return _where;
    }

    public ColumnExp<?>[] getFields ()
    {
        return _fields;
    }

    public SQLExpression<?>[] getValues ()
    {
        return _values;
    }

    public PersistentRecord getPojo ()
    {
        return _pojo;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
        if (_where != null) {
            _where.addClasses(classSet);
        }
        if (_values != null) {
            for (int ii = 0; ii < _values.length; ii ++) {
                _values[ii].addClasses(classSet);
            }
        }
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    /** The class we're updating. */
    protected Class<? extends PersistentRecord> _pClass;

    /** The where clause. */
    protected WhereClause _where;

    /** The persistent fields to update. */
    protected ColumnExp<?>[] _fields;

    /** The field values, or null. */
    protected SQLExpression<?>[] _values;

    /** The object from which to fetch values, or null. */
    protected PersistentRecord _pojo;
}
