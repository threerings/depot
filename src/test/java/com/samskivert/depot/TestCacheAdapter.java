//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.Tuple;

/**
 * A simple cache adapter that stores all cached values in an in-memory map and never flushes.
 * Don't use this for anything other than testing or you'll regret it.
 */
public class TestCacheAdapter implements CacheAdapter
{
    // from interface CacheAdapter
    public void shutdown ()
    {
        // nothing doing!
    }

    protected static class TestCachedValue<T> implements CacheAdapter.CachedValue<T>
    {
        public TestCachedValue (T value) {
            _value = value;
        }
        public T getValue () {
            return _value;
        }
        protected final T _value;
    }

    public <T> CacheAdapter.CachedValue<T> lookup (String cacheId, Serializable key) {
        // System.err.println("GET " + key + ": " + _cache.containsKey(key));
        @SuppressWarnings("unchecked")
        CachedValue<T> value = (CachedValue<T>) _cache.get(
            new Tuple<String, Serializable>(cacheId, key));
        return value;
    }
    public <T> void store (CacheCategory category, String cacheId, Serializable key, T value) {
        // System.err.println("STORE " + key);
        _cache.put(new Tuple<String, Serializable>(cacheId, key), new TestCachedValue<T>(value));
    }
    public void remove (String cacheId, Serializable key) {
        // System.err.println("REMOVE " + key);
        _cache.remove(new Tuple<String, Serializable>(cacheId, key));
    }
    public <T> Iterable<Serializable> enumerate (String cacheId)
    {
        // in a real implementation this would be a lazily constructed iterable
        List<Serializable> result = Lists.newArrayList();
        for (Map.Entry<Tuple<String, Serializable>, CachedValue<?>> entry: _cache.entrySet()) {
            if (entry.getKey().left.equals(cacheId)) {
                result.add(entry.getKey().right);
            }
        }
        return result;
    }

    protected Map<Tuple<String, Serializable>, CachedValue<?>> _cache =
        Collections.synchronizedMap(
            Maps.<Tuple<String, Serializable>, CachedValue<?>>newHashMap());
}
