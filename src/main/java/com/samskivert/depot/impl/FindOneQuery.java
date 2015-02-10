//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samskivert.depot.CacheAdapter.CacheCategory;
import com.samskivert.depot.CacheKey;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Stats;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.impl.jdbc.DatabaseLiaison;

import static com.samskivert.depot.Log.log;

/**
 * The implementation of {@link DepotRepository#load} functionality.
 */
public class FindOneQuery<T extends PersistentRecord> extends Fetcher<T>
{
    public FindOneQuery (PersistenceContext ctx, Class<T> type,
                         DepotRepository.CacheStrategy strategy, QueryClause[] clauses)
        throws DatabaseException
    {
        _strategy = strategy;
        _marsh = ctx.getMarshaller(type);
        _select = new SelectClause(type, _marsh.getSelections(), clauses);
        WhereClause where = _select.getWhereClause();
        if (where != null) {
            _select.getWhereClause().validateQueryType(type); // sanity check
        }
        _builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
        _builder.newQuery(_select);
    }

    @Override // from Fetcher
    public T getCachedResult (PersistenceContext ctx)
    {
        CacheKey key = getCacheKey();
        if (key == null) {
            return null;
        }
        T value = ctx.<T>cacheLookup(key);
        if (value == null) {
            return null;
        }
        _cachedRecords = 1;
        // we do not want to return a reference to the actual cached entity so we clone it
        @SuppressWarnings("unchecked") T cvalue = (T) value.clone();
        return cvalue;
    }

    // from Fetcher
    public T invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        // load up the record in question
        T result = null;
        ResultSet rs = _builder.prepare(conn).executeQuery();
        if (rs.next()) {
            result = _marsh.createObject(rs);
        }
        // TODO: if (rs.next()) issue warning?
        rs.close();

        // potentially cache the result
        CacheKey key = getCacheKey();
        if (key == null) {
            // no row-specific cache key was given, if we can, create a key from the record
            if (result != null && _marsh.hasPrimaryKey()) {
                key = new KeyCacheKey(_marsh.getPrimaryKey(result));
            }
        }
        if (PersistenceContext.CACHE_DEBUG) {
            log.info("Loaded " + (key != null ? key : _marsh.getTableName()));
        }
        if (key != null) {
            ctx.cacheStore(CacheCategory.RECORD, key, (result != null) ? result.clone() : null);
            if (PersistenceContext.CACHE_DEBUG) {
                log.info("Cached " + key);
            }
        }

        return result;
    }

    // from Operation
    public void updateStats (Stats stats)
    {
        stats.noteQuery(_marsh.getPersistentClass(), 0, 0, 0, _cachedRecords, 1-_cachedRecords);
    }

    protected CacheKey getCacheKey ()
    {
        if (_strategy == DepotRepository.CacheStrategy.NONE) {
            return null;
        }
        WhereClause where = _select.getWhereClause();
        return (where != null && where instanceof CacheKey) ? (CacheKey)where : null;
    }

    protected DepotRepository.CacheStrategy _strategy;
    protected DepotMarshaller<T> _marsh;
    protected SelectClause _select;
    protected SQLBuilder _builder;
    protected int _cachedRecords;
}
