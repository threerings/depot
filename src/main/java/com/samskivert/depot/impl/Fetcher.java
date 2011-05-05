//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.Stats;

/**
 * The base of all read-only queries.
 */
public abstract class Fetcher<T>
    implements Operation<T>
{
    /** A simple base class for non-complex queries. */
    public static abstract class Trivial<T> extends Fetcher<T>
    {
        @Override // from Fetcher
        public T getCachedResult (PersistenceContext ctx) {
            return null;
        }

        // from Operation
        public void updateStats (Stats stats) {
            // nothing doing
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
