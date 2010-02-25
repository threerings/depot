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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Stats;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;

import static com.samskivert.depot.Log.log;

/**
 * Loads all primary keys for the records matching the supplied clause.
 */
public class FindAllKeysQuery<T extends PersistentRecord> extends Query<List<Key<T>>>
{
    public FindAllKeysQuery (PersistenceContext ctx, Class<T> type, boolean forUpdate,
                             Collection<? extends QueryClause> clauses)
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

    @Override // from Query
    public List<Key<T>> getCachedResult (PersistenceContext ctx)
    {
        return null; // TODO
    }

    // from Query
    public List<Key<T>> invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        List<Key<T>> keys = new ArrayList<Key<T>>();
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

    // from Query
    public void updateStats (Stats stats)
    {
        stats.noteQuery(0, 1, 0, 0, 0); // one uncached query
    }

    protected boolean _forUpdate;
    protected SQLBuilder _builder;
    protected DepotMarshaller<T> _marsh;
    protected SelectClause _select;
}
