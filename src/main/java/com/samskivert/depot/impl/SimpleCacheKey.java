//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.io.Serializable;

import com.samskivert.depot.CacheKey;

import com.google.common.base.Objects;

/**
 * Convenience class that implements {@link CacheKey} as simply as possibly. This class is
 * typically used when the caller wants to cache a non-obvious query such as a collection,
 * and needs to specify their own cache key and file it under a hand-picked cache id.
 */
public class SimpleCacheKey
    implements CacheKey
{
    /**
     * Construct a {@link SimpleCacheKey} for the given cache id with the given cache key.
     */
    public SimpleCacheKey (String cacheId, String cacheKey)
    {
        _cacheId = cacheId;
        _cacheKey = cacheKey;
    }

    // from CacheKey
    public String getCacheId ()
    {
        return _cacheId;
    }

    // from CacheKey
    public Serializable getCacheKey ()
    {
        return _cacheKey;
    }

    @Override
    public int hashCode ()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((_cacheId == null) ? 0 : _cacheId.hashCode());
        result = PRIME * result + ((_cacheKey == null) ? 0 : _cacheKey.hashCode());
        return result;
    }

    @Override
    public boolean equals (Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        SimpleCacheKey other = (SimpleCacheKey) obj;
        return Objects.equal(_cacheId, other._cacheId) &&
            Objects.equal(_cacheKey, other._cacheKey);
    }

    @Override
    public String toString ()
    {
        return "[cacheId=" + _cacheId + ", value=" + _cacheKey + "]";
    }

    protected String _cacheId;
    protected String _cacheKey;
}
