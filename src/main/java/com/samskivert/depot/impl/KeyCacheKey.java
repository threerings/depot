//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.io.Serializable;
import java.util.Arrays;

import com.samskivert.depot.CacheKey;
import com.samskivert.depot.Key;

/**
 * Converts a {@link CacheKey} to and from a {@link Key} in a way that eliminates references to
 * non-Java classes (so that we don't have to deal with RMI classloader hell when replicating cache
 * contents).
 */
public class KeyCacheKey
    implements CacheKey, Serializable
{
    public KeyCacheKey (Key<?> key)
    {
        _cacheId = key.getPersistentClass().getName();
        Comparable<?>[] values = key.getValues();
        _values = new Comparable<?>[values.length];
        for (int ii = 0; ii < _values.length; ii++) {
            _values[ii] = values[ii]; // TODO: check for non-system-class and serialize
        }
    }

    // from CacheKey
    public String getCacheId ()
    {
        return _cacheId;
    }

    // from CacheKey
    public Serializable getCacheKey ()
    {
        return this;
    }

    @Override // from Object
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(_values, ((KeyCacheKey) obj)._values);
    }

    @Override // from Object
    public int hashCode ()
    {
        return Arrays.hashCode(_values);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder(_cacheId);
        builder.append("(");
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append(_values[ii]);
        }
        builder.append(")");
        return builder.toString();
    }

    protected String _cacheId;
    protected Comparable<?>[] _values;
}
