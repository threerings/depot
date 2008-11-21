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
import java.sql.SQLException;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.depot.PersistenceContext.CacheListener;

/**
 * The base of all read-only queries.
 */
public abstract class Query<T>
    implements Operation<T>
{
    /** A simple base class for non-complex queries. */
    public static abstract class Trivial<T> extends Query<T>
    {
        @Override // from Query
        public T getCachedResult (PersistenceContext ctx) {
            return null;
        }
    }

    /**
     * If this query has a simple cached result, it should return the non-null result from this
     * method. If null is returned, the query will be {@link #invoke}d to obtain its result from
     * persistent storage.
     */
    public abstract T getCachedResult (PersistenceContext ctx);

    // from interface Operation
    public boolean isReadOnly ()
    {
        return true;
    }
}
