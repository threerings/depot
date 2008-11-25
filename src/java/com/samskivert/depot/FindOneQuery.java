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

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;

import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;

import static com.samskivert.depot.Log.log;

/**
 * The implementation of {@link DepotRepository#load} functionality.
 */
public class FindOneQuery<T extends PersistentRecord> extends Query<T>
{
    public FindOneQuery (PersistenceContext ctx, Class<T> type, QueryClause[] clauses)
        throws DatabaseException
    {
        _marsh = ctx.getMarshaller(type);
        _select = new SelectClause<T>(type, _marsh.getFieldNames(), clauses);
        WhereClause where = _select.getWhereClause();
        if (where != null) {
            _select.getWhereClause().validateQueryType(type); // sanity check
        }
        _builder = ctx.getSQLBuilder(DepotTypes.getDepotTypes(ctx, _select));
        _builder.newQuery(_select);
    }

    @Override // from Query
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

    // from Query
    public T invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        PreparedStatement stmt = _builder.prepare(conn);
        try {
            // load up the record in question
            T result = null;
            ResultSet rs = stmt.executeQuery();
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
                    key = _marsh.getPrimaryKey(result);
                }
            }
            if (PersistenceContext.CACHE_DEBUG) {
                log.info("Loaded " + (key != null ? key : _marsh.getTableName()));
            }
            if (key != null) {
                ctx.cacheStore(key, (result != null) ? result.clone() : null);
                if (PersistenceContext.CACHE_DEBUG) {
                    log.info("Cached " + key);
                }
            }

            return result;

        } finally {
            JDBCUtil.close(stmt);
        }
    }

    // from Operation
    public void updateStats (Stats stats)
    {
        stats.noteQuery(0, 0, _cachedRecords, 1-_cachedRecords);
    }

    protected CacheKey getCacheKey ()
    {
        WhereClause where = _select.getWhereClause();
        return (where != null && where instanceof CacheKey) ? (CacheKey)where : null;
    }

    protected DepotMarshaller<T> _marsh;
    protected SelectClause<T> _select;
    protected SQLBuilder _builder;
    protected int _cachedRecords;
}
