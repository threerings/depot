//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.io.Serializable;

/**
 * This interface uniquely identifies a single persistent entry for caching purposes.
 * Queries that are given a {@link CacheKey} consult the cache before they hit the
 * database.
 */
public interface CacheKey
{
    /**
     * Returns the id of the cache in whose scope this key makes sense.
     */
    public String getCacheId ();

    /**
     * Returns the actual opaque serializable cache key under which results are stored in the cache
     * identified by {@link #getCacheId}. The object returned by this method should <em>only</em>
     * reference system classes (not application classes). Depot takes care to ensure this and you
     * probably aren't implementing your own cache keys so this should be fine.
     */
    public Serializable getCacheKey ();
}
