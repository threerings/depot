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

package com.samskivert.depot;

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
import com.samskivert.jdbc.JDBCUtil;

import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.operator.Conditionals.*;

import static com.samskivert.depot.Log.log;

/**
 * This class implements the functionality required by {@link DepotRepository#findAll}: fetch
 * a collection of persistent objects using one of two included strategies.
 */
public abstract class FindAllQuery<T extends PersistentRecord>
    implements Query<List<T>>
{
    /**
     * The two-pass collection query implementation. See {@link DepotRepository#findAll} for 
     * details.
     */
    public static class WithCache<T extends PersistentRecord> extends FindAllQuery<T>
    {
        public WithCache (PersistenceContext ctx, Class<T> type,
                          Collection<? extends QueryClause> clauses)
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

            _select = new SelectClause<T>(_type, _marsh.getPrimaryKeyFields(), clauses);
            _builder = _ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
            _builder.newQuery(_select);
        }

        public List<T> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            Map<Key<T>, T> entities = Maps.newHashMap();
            List<Key<T>> allKeys = Lists.newArrayList();
            Set<Key<T>> fetchKeys = Sets.newHashSet();

            PreparedStatement stmt = _builder.prepare(conn);
            String stmtString = stmt.toString(); // for debugging
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Key<T> key = _marsh.makePrimaryKey(rs);
                    allKeys.add(key);

                    T value = _ctx.<T>cacheLookup(key);
                    if (value != null) {
                        @SuppressWarnings("unchecked") T newValue = (T) value.clone();
                        entities.put(key, newValue);
                        continue;
                    }

                    fetchKeys.add(key);
                }

            } finally {
                JDBCUtil.close(stmt);
            }

            _cachedRecords = entities.size();
            _uncachedRecords = fetchKeys.size();

            if (PersistenceContext.CACHE_DEBUG) {
                log.info("Loaded " + _marsh.getTableName(), "query", _select,
                         "keys", keysToString(allKeys));
            }

            return loadAndResolve(ctx, conn, allKeys, fetchKeys, entities, stmtString);
        }

        public void updateStats (Stats stats) {
            stats.noteQuery(0, 1, _cachedRecords, _uncachedRecords); // one uncached query
        }

        protected SelectClause<T> _select;
        protected int _cachedRecords, _uncachedRecords;
    }

    /**
     * The two-pass collection query implementation. See {@link DepotRepository#findAll} for 
     * details.
     */
    public static class WithKeys<T extends PersistentRecord> extends FindAllQuery<T>
    {
        public WithKeys (PersistenceContext ctx, Collection<Key<T>> keys)
            throws DatabaseException
        {
            super(ctx, keys.iterator().next().getPersistentClass());
            _keys = keys;
            _builder = ctx.getSQLBuilder(new DepotTypes(ctx, _type));
        }

        public List<T> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            Map<Key<T>, T> entities = Maps.newHashMap();
            Set<Key<T>> fetchKeys = Sets.newHashSet();
            for (Key<T> key : _keys) {
                T value = _ctx.<T>cacheLookup(key);
                if (value != null) {
                    @SuppressWarnings("unchecked") T newValue = (T) value.clone();
                    entities.put(key, newValue);
                    continue;
                }
                fetchKeys.add(key);
            }

            _cachedRecords = entities.size();
            _uncachedRecords = fetchKeys.size();

            return loadAndResolve(ctx, conn, _keys, fetchKeys, entities, null);
        }

        public void updateStats (Stats stats) {
            stats.noteQuery(0, 0, _cachedRecords, _uncachedRecords);
        }

        protected Collection<Key<T>> _keys;
        protected int _cachedRecords, _uncachedRecords;
    }

    /**
     * The single-pass collection query implementation. See {@link DepotRepository#findAll} for 
     * details.
     */
    public static class Explicitly<T extends PersistentRecord> extends FindAllQuery<T>
    {
        public Explicitly (PersistenceContext ctx, Class<T> type,
                           Collection<? extends QueryClause> clauses)
            throws DatabaseException
        {
            super(ctx, type);
            _select = new SelectClause<T>(type, _marsh.getFieldNames(), clauses);
            _builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
            _builder.newQuery(_select);
        }

        public List<T> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            List<T> result = Lists.newArrayList();
            PreparedStatement stmt = _builder.prepare(conn);
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    result.add(_marsh.createObject(rs));
                    _uncachedRecords++;
                }
            } finally {
                JDBCUtil.close(stmt);
            }
            if (PersistenceContext.CACHE_DEBUG) {
                log.info("Loaded " + _marsh.getTableName(), "query", _select,
                         "uncached", _uncachedRecords);
            }
            // TODO: do we want to cache these results?
            return result;
        }

        public void updateStats (Stats stats) {
            stats.noteQuery(0, 1, 0, _uncachedRecords);
        }

        protected SelectClause<T> _select;
        protected int _uncachedRecords;
    }

    public FindAllQuery (PersistenceContext ctx, Class<T> type)
        throws DatabaseException
    {
        _ctx = ctx;
        _type = type;
        _marsh = _ctx.getMarshaller(type);
    }

    // from interface Query
    public List<T> getCachedResult (PersistenceContext ctx)
    {
        return null; // TODO
    }

//     // from interface Query
//     public List<T> transformCacheHit (CacheKey key, List<T> bits)
//     {
//         if (bits == null) {
//             return bits;
//         }

//         List<T> result = Lists.newArrayList();
//         for (T bit : bits) {
//             if (bit != null) {
//                 @SuppressWarnings("unchecked") T cbit = (T) bit.clone();
//                 result.add(cbit);
//             } else {
//                 result.add(null);
//             }
//         }
//         return result;
//     }

    // from interface Operation
    public void updateStats (Stats stats)
    {
        // TODO
    }

    protected List<T> loadAndResolve (PersistenceContext ctx, Connection conn,
                                      Collection<Key<T>> allKeys, Set<Key<T>> fetchKeys,
                                      Map<Key<T>, T> entities, String origStmt)
        throws SQLException
    {
        if (PersistenceContext.CACHE_DEBUG && fetchKeys.size() > 0) {
            log.info("Loading " + _marsh.getTableName(), "keys", keysToString(fetchKeys));
        }

        // if we're fetching a huge number of records, we have to do it in multiple queries
        if (fetchKeys.size() > In.MAX_KEYS) {
            int keyCount = fetchKeys.size();
            do {
                Set<Key<T>> keys = Sets.newHashSet();
                Iterator<Key<T>> iter = fetchKeys.iterator();
                for (int ii = 0; ii < Math.min(keyCount, In.MAX_KEYS); ii++) {
                    keys.add(iter.next());
                    iter.remove();
                }
                keyCount -= keys.size();
                loadRecords(ctx, conn, keys, entities, origStmt);
            } while (keyCount > 0);

        } else if (fetchKeys.size() > 0) {
            loadRecords(ctx, conn, fetchKeys, entities, origStmt);
        }

        List<T> result = Lists.newArrayList();
        for (Key<T> key : allKeys) {
            T value = entities.get(key);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    protected void loadRecords (PersistenceContext ctx, Connection conn, Set<Key<T>> keys,
                                Map<Key<T>, T> entities, String origStmt)
        throws SQLException
    {
        boolean hasPrimaryKey = _marsh.hasPrimaryKey();
        _builder.newQuery(new SelectClause<T>(_type, _marsh.getFieldNames(),
                                              new KeySet<T>(_type, keys)));
        PreparedStatement stmt = _builder.prepare(conn);
        try {
            Set<Key<T>> got = Sets.newHashSet();
            ResultSet rs = stmt.executeQuery();
            int cnt = 0, dups = 0;
            while (rs.next()) {
                T obj = _marsh.createObject(rs);
                Key<T> key = _marsh.getPrimaryKey(obj);
                if (entities.put(key, obj) != null) {
                    dups++;
                }
                // cache our result if it has a primary key
                if (hasPrimaryKey) {
                    ctx.cacheStore(_marsh.getPrimaryKey(obj), obj.clone());
                }
                got.add(key);
                cnt++;
            }
            // if we get more results than we planned, or if we're doing a two-phase query and got
            // fewer, then complain
            if (cnt > keys.size() || (origStmt != null && cnt < keys.size())) {
                log.warning("Row count mismatch in second pass", "origQuery", origStmt,
                            "wanted", keys, "got", got, "dups", dups, new Exception());
            }

            if (PersistenceContext.CACHE_DEBUG) {
                log.info("Cached " + _marsh.getTableName(), "count", cnt);
            }

        } finally {
            JDBCUtil.close(stmt);
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

    protected PersistenceContext _ctx;
    protected SQLBuilder _builder;
    protected DepotMarshaller<T> _marsh;
    protected Class<T> _type;
}
