//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Maps;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.depot.CacheAdapter.CacheCategory;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository.CacheStrategy;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Stats;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.impl.operator.In;

import static com.samskivert.depot.Log.log;

/**
 * This class implements the functionality required by {@link DepotRepository#findAll}: fetch
 * a collection of persistent objects using one of two included strategies.
 */
public abstract class FindAllQuery<T extends PersistentRecord> extends Query<List<T>>
{
    /**
     * The two-pass collection query implementation. See {@link DepotRepository#findAll} for
     * details.
     */
    public static class WithCache<T extends PersistentRecord> extends FindAllQuery<T>
    {
        public WithCache (PersistenceContext ctx, Class<T> type,
            Iterable<? extends QueryClause> clauses, CacheStrategy strategy)
            throws DatabaseException
        {
            super(ctx, type);

            if (_marsh.getComputed() != null) {
                throw new IllegalArgumentException(
                    "This algorithm doesn't work on @Computed records.");
            }
            for (QueryClause clause : clauses) {
                if (clause instanceof FieldOverride) {
                    throw new IllegalArgumentException(
                        "This algorithm doesn't work with FieldOverrides.");
                }
            }

            _select = new SelectClause(_type, _marsh.getPrimaryKeyFields(), clauses);
            switch(strategy) {
            case SHORT_KEYS: case LONG_KEYS:
                _qkey = new SimpleCacheKey(_marsh.getTableName() + "Keys", _select.toString());
                _category = (strategy == CacheStrategy.SHORT_KEYS) ?
                    CacheCategory.SHORT_KEYSET : CacheCategory.LONG_KEYSET;
                break;

            case RECORDS:
                _qkey = null;
                break;

            default:
                throw new IllegalArgumentException("Unexpected cache strategy: " + strategy);
            }

        }

        @Override // from Query
        public List<T> getCachedResult (PersistenceContext ctx)
        {
            if (_qkey == null) {
                return null;
            }
            _keys = ctx.<KeySet<T>>cacheLookup(_qkey);
            if (_keys == null) {
                return null;
            }
            _cachedQueries++;
            _fetchKeys = loadFromCache(ctx, _keys, _entities);
            return (_fetchKeys.size() == 0) ? resolve(_keys, _entities) : null;
        }

        // from Query
        public List<T> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            // we want this to remain null if our key set came from the cache
            String stmtString = null;

            // if we didn't find our key set in the cache, load the keys that match
            if (_keys == null) {
                List<Key<T>> keys = Lists.newArrayList();
                SQLBuilder builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
                builder.newQuery(_select);
                PreparedStatement stmt = builder.prepare(conn);
                stmtString = stmt.toString(); // for debugging
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    keys.add(_marsh.makePrimaryKey(rs));
                }
                _keys = KeySet.newKeySet(_type, keys);
                _uncachedQueries++;
                if (PersistenceContext.CACHE_DEBUG) {
                    log.info("Loaded " + _marsh.getTableName() + " keys", "query", _select,
                             "keys", keysToString(_keys), "cached", (_qkey != null));
                }
                if (_qkey != null) {
                    // cache the resulting key set
                    ctx.cacheStore(_category, _qkey, _keys);
                }
                // and fetch any records we can from the cache
                _fetchKeys = loadFromCache(ctx, _keys, _entities);
            }

            // finally load the rest from the database
            return loadAndResolve(ctx, conn, _keys, _fetchKeys, _entities, stmtString);
        }

        protected SimpleCacheKey _qkey;
        protected CacheCategory _category;
        protected SelectClause _select;
        protected KeySet<T> _keys;
        protected Set<Key<T>> _fetchKeys;
        protected Map<Key<T>, T> _entities = Maps.newHashMap();
    }

    /**
     * The two-pass collection query implementation. See {@link DepotRepository#findAll} for
     * details.
     */
    public static class WithKeys<T extends PersistentRecord> extends FindAllQuery<T>
    {
        public WithKeys (PersistenceContext ctx, Iterable<Key<T>> keys)
            throws DatabaseException
        {
            super(ctx, keys.iterator().next().getPersistentClass());
            _keys = keys;
        }

        @Override // from Query
        public List<T> getCachedResult (PersistenceContext ctx)
        {
            // look up what we can from the cache
            _fetchKeys = loadFromCache(ctx, _keys, _entities);

            // if we found everything, we can just return our result straight away, yay!
            return _fetchKeys.isEmpty() ? resolve(_keys, _entities) : null;
        }

        // from Query
        public List<T> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            return loadAndResolve(ctx, conn, _keys, _fetchKeys, _entities, null);
        }

        protected Iterable<Key<T>> _keys;
        protected Set<Key<T>> _fetchKeys;
        protected Map<Key<T>, T> _entities = Maps.newHashMap();
    }

    /**
     * The single-pass collection query implementation. See {@link DepotRepository#findAll} for
     * details.
     */
    public static class Explicitly<T extends PersistentRecord> extends FindAllQuery<T>
    {
        public Explicitly (PersistenceContext ctx, Class<T> type,
                           Iterable<? extends QueryClause> clauses, boolean cachedContents)
            throws DatabaseException
        {
            super(ctx, type);

            _select = new SelectClause(type, _marsh.getFieldNames(), clauses);

            if (cachedContents) {
                _qkey = new SimpleCacheKey(_marsh.getTableName() + "Contents", _select.toString());
            } else {
                _qkey = null;
            }
        }

        @Override // from Query
        public List<T> getCachedResult (PersistenceContext ctx)
        {
            if (_qkey != null) {
                _cachedQueries++;
                return ctx.cacheLookup(_qkey);
            }
            return null;
        }

        // from Query
        public List<T> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            List<T> result = Lists.newArrayList();
            SQLBuilder builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
            builder.newQuery(_select);
            ResultSet rs = builder.prepare(conn).executeQuery();
            while (rs.next()) {
                result.add(_marsh.createObject(rs));
            }
            _explicitQueries++;
            if (PersistenceContext.CACHE_DEBUG) {
                log.info("Loaded " + _marsh.getTableName(), "query", _select, "rows",
                    result.size(), "cacheKey", _qkey);
            }
            if (_qkey != null) {
                ctx.cacheStore(CacheCategory.RESULT, _qkey, result); // cache the entire result set
            }
            _uncachedRecords += result.size();
            return result;
        }

        protected SelectClause _select;
        protected SimpleCacheKey _qkey;
    }

    // from Query
    public void updateStats (Stats stats)
    {
        stats.noteQuery(_cachedQueries, _uncachedQueries, _explicitQueries,
                        _cachedRecords, _uncachedRecords);
    }

    protected FindAllQuery (PersistenceContext ctx, Class<T> type)
        throws DatabaseException
    {
        _type = type;
        _marsh = ctx.getMarshaller(type);
    }

    protected Set<Key<T>> loadFromCache (PersistenceContext ctx, Iterable<Key<T>> allKeys,
                                         Map<Key<T>, T> entities)
    {
        Set<Key<T>> fetchKeys = Sets.newHashSet();
        for (Key<T> key : allKeys) {
            T value = ctx.<T>cacheLookup(new KeyCacheKey(key));
            if (value != null) {
                @SuppressWarnings("unchecked") T newValue = (T) value.clone();
                entities.put(key, newValue);
                continue;
            }
            fetchKeys.add(key);
        }
        if (PersistenceContext.CACHE_DEBUG) {
            log.info("Loaded from cache " + _marsh.getTableName(), "count", entities.size());
        }
        _cachedRecords = entities.size();
        _uncachedRecords = fetchKeys.size();
        return fetchKeys;
    }

    protected List<T> resolve (Iterable<Key<T>> allKeys, Map<Key<T>, T> entities)
    {
        List<T> result = (allKeys instanceof Collection<?>)
            ? Lists.<T>newArrayListWithCapacity(((Collection<?>)allKeys).size())
            : Lists.<T>newArrayList();
        for (Key<T> key : allKeys) {
            T value = entities.get(key);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    protected List<T> loadAndResolve (PersistenceContext ctx, Connection conn,
                                       Iterable<Key<T>> allKeys, Set<Key<T>> fetchKeys,
                                       Map<Key<T>, T> entities, String origStmt)
        throws SQLException
    {
        if (PersistenceContext.CACHE_DEBUG && fetchKeys.size() > 0) {
            log.info("Loading " + _marsh.getTableName(), "keys", keysToString(fetchKeys));
        }

        // if we're fetching a huge number of records, we have to do it in multiple queries
        if (fetchKeys.size() > In.MAX_KEYS) {
            int keyCount = fetchKeys.size();
            Iterator<Key<T>> iter = fetchKeys.iterator();
            do {
                Set<Key<T>> keys = Sets.newHashSet();
                for (int ii = 0, nn = Math.min(keyCount, In.MAX_KEYS); ii < nn; ii++) {
                    keys.add(iter.next());
                }
                keyCount -= keys.size();
                loadRecords(ctx, conn, keys, entities, origStmt);
            } while (keyCount > 0);

        } else if (fetchKeys.size() > 0) {
            loadRecords(ctx, conn, fetchKeys, entities, origStmt);
        }

        return resolve(allKeys, entities);
    }

    protected void loadRecords (PersistenceContext ctx, Connection conn, Set<Key<T>> keys,
                                Map<Key<T>, T> entities, String origStmt)
        throws SQLException
    {
        SelectClause select = new SelectClause(
            _type, _marsh.getFieldNames(), (QueryClause) KeySet.newKeySet(_type, keys));
        SQLBuilder builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, select));
        builder.newQuery(select);
        Set<Key<T>> got = Sets.newHashSet();
        ResultSet rs = builder.prepare(conn).executeQuery();
        int cnt = 0, dups = 0;
        while (rs.next()) {
            T obj = _marsh.createObject(rs);
            Key<T> key = _marsh.getPrimaryKey(obj);
            if (entities.put(key, obj) != null) {
                dups++;
            }
            ctx.cacheStore(CacheCategory.RECORD, new KeyCacheKey(key), obj.clone());
            got.add(key);
            cnt++;
        }
        // if we get more results than we planned, or if we're doing a two-phase query and got
        // fewer, then complain
        if (cnt > keys.size() || (origStmt != null && cnt < keys.size())) {
            log.warning("Row count mismatch in second pass", "origQuery", origStmt,
            // we need toString() here or StringUtil will get smart and dump our
                // KeySet using its iterator which results in verbosity
                "wanted", KeySet.newKeySet(_type, keys).toString(), "got", KeySet.newKeySet(
                    _type, got).toString(), "dups", dups, new Exception());
        }

        if (PersistenceContext.CACHE_DEBUG) {
            log.info("Cached " + _marsh.getTableName(), "count", cnt);
        }

    }

    protected String keysToString (Iterable<Key<T>> keySet)
    {
        StringBuilder builder = new StringBuilder("(");
        for (Key<T> key : keySet) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            key.toShortString(builder);
        }
        return builder.append(")").toString();
    }

    protected Class<T> _type;
    protected DepotMarshaller<T> _marsh;
    protected int _cachedQueries, _uncachedQueries, _explicitQueries;
    protected int _cachedRecords, _uncachedRecords;
}
