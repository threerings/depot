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

package com.samskivert.depot.tests;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.depot.CacheAdapter;
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
    public <T> Iterable<Tuple<Serializable, CachedValue<T>>> enumerate (String cacheId)
    {
        // in a real implementation this would be a lazily constructed iterable
        List<Tuple<Serializable, CachedValue<T>>> result = Lists.newArrayList();
        for (Map.Entry<Tuple<String, Serializable>, CachedValue<?>> entry: _cache.entrySet()) {
            if (entry.getKey().left.equals(cacheId)) {
                @SuppressWarnings("unchecked")
                CachedValue<T> value = (CachedValue<T>) entry.getValue();
                result.add(Tuple.newTuple(entry.getKey().right, value));
            }
        }
        return result;
    }

    protected Map<Tuple<String, Serializable>, CachedValue<?>> _cache =
        Collections.synchronizedMap(
            Maps.<Tuple<String, Serializable>, CachedValue<?>>newHashMap());
}
