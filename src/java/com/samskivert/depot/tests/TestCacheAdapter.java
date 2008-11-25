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
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.depot.CacheAdapter;

/**
 * A simple cache adapter that stores all cached values in an in-memory map and never flushes.
 * Don't use this for anything other than testing or you'll regret it.
 */
public class TestCacheAdapter implements CacheAdapter
{
    // from interface CacheAdapter
    public synchronized <T> CacheAdapter.CacheBin<T> getCache (String id)
    {
        @SuppressWarnings("unchecked") CacheBin<T> bin = (CacheBin<T>)_bins.get(id);
        if (bin == null) {
            bin = new TestCacheBin<T>();
            _bins.put(id, bin);
        }
        return bin;
    }

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

    protected static class TestCacheBin<T> implements CacheAdapter.CacheBin<T>
    {
        public CacheAdapter.CachedValue<T> lookup (Serializable key) {
            // System.err.println("GET " + key + ": " + _cache.containsKey(key));
            return _cache.get(key);
        }
        public void store (Serializable key, T value) {
            // System.err.println("STORE " + key);
            _cache.put(key, new TestCachedValue<T>(value));
        }
        public void remove (Serializable key) {
            // System.err.println("REMOVE " + key);
            _cache.remove(key);
        }
        public Iterable<Serializable> enumerateKeys () {
            return _cache.keySet();
        }
        protected Map<Serializable, CacheAdapter.CachedValue<T>> _cache =
            Collections.synchronizedMap(
                Maps.<Serializable, CacheAdapter.CachedValue<T>>newHashMap());
    }

    protected Map<String, CacheBin<?>> _bins = Maps.newHashMap();
}
