//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.collect.Lists;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Stats;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.impl.jdbc.DatabaseLiaison;

import static com.samskivert.depot.Log.log;

/**
 * Loads all primary keys for the records matching the supplied clause.
 */
public class FindAllKeysQuery<T extends PersistentRecord> extends Fetcher<List<Key<T>>>
{
    public FindAllKeysQuery (PersistenceContext ctx, Class<T> type, boolean forUpdate,
                             Iterable<? extends QueryClause> clauses)
        throws DatabaseException
    {
        _forUpdate = forUpdate;
        _marsh = ctx.getMarshaller(type);
        _select = new SelectClause(type, _marsh.getPrimaryKeyFields(), clauses);
        _builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
        _builder.newQuery(_select);
    }

    @Override // from Query
    public boolean isReadOnly ()
    {
        return !_forUpdate;
    }

    @Override // from Fetcher
    public List<Key<T>> getCachedResult (PersistenceContext ctx)
    {
        return null; // TODO
    }

    // from Fetcher
    public List<Key<T>> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        List<Key<T>> keys = Lists.newArrayList();
        PreparedStatement stmt = _builder.prepare(conn);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            keys.add(_marsh.makePrimaryKey(rs));
        }
        // TODO: cache this result?
        if (PersistenceContext.CACHE_DEBUG) {
            log.info("Loaded " + _marsh.getTableName(), "count", keys.size());
        }
        return keys;
    }

    // from Fetcher
    public void updateStats (Stats stats)
    {
        stats.noteQuery(_marsh.getPersistentClass(), 0, 1, 0, 0, 0); // one uncached query
    }

    protected boolean _forUpdate;
    protected SQLBuilder _builder;
    protected DepotMarshaller<T> _marsh;
    protected SelectClause _select;
}
