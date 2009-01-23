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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samskivert.util.Tuple;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import static com.samskivert.depot.Log.log;

/**
 * An implementation of {@link CacheAdapter} for ehcache where each {@link CacheCategory} results
 * in one {@link Ehcache}. All (cacheId, key) combinations within one category is stuffed into the
 * same {@link Ehcache}, and all elements are cached under {@link EHCacheKey}, which basically
 * wraps just such a tuple.
 * 
 * Thus there are currently only three Ehcaches in play, called 'depotRecord', 'depotKeyset', and
 * 'depotResult'. These must be defined in your ehcache.xml configuration. If you use distributed
 * replication/invalidation, you should replicate updates and removes but not puts nor
 * updates-via-copy.
 */
public class EHCacheAdapter
    implements CacheAdapter
{
    protected static final String EHCACHE_RECORD_CACHE = "depotRecord";
    protected static final String EHCACHE_KEYSET_CACHE = "depotKeyset";
    protected static final String EHCACHE_RESULT_CACHE = "depotResult";

    /**
     * Creates an adapter using the supplied cache manager. Note: this adapter does not shut down
     * the supplied manager when it is shutdown. The caller is responsible for shutting down the
     * cache manager when it knows that Depot and any other clients no longer need it.
     */
    public EHCacheAdapter (CacheManager cachemgr)
    {
        bindEHCache(cachemgr, CacheCategory.RECORD, EHCACHE_RECORD_CACHE);
        bindEHCache(cachemgr, CacheCategory.KEYSET, EHCACHE_KEYSET_CACHE);
        bindEHCache(cachemgr, CacheCategory.RESULT, EHCACHE_RESULT_CACHE);
    }

    // from CacheAdapter
    public <T> CachedValue<T> lookup (String cacheId, Serializable key)
    {
        @SuppressWarnings("unchecked")
        EHCacheBin<T> bin = (EHCacheBin<T>) _bins.get(cacheId);
        if (bin == null) {
            return null;
        }
        return lookup(bin.getCache(), cacheId, key);
    }

    // from CacheAdapter
    public <T> void store (CacheCategory category, String cacheId, Serializable key, T value)
    {
        Ehcache cache = _categories.get(category);
        if (cache == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        @SuppressWarnings("unchecked")
        EHCacheBin<T> bin = (EHCacheBin<T>) _bins.get(cacheId);
        if (bin == null) {
            bin = new EHCacheBin<T>(cache, cacheId);
            _bins.put(cacheId, bin);
        }
        bin.getCache().put(new Element(new EHCacheKey(cacheId, key), value != null ? value : NULL));
    }

    // from CacheAdapter
    public void remove (String cacheId, Serializable key)
    {
        EHCacheBin<?> bin = _bins.get(cacheId);
        if (bin != null) {
            bin.getCache().remove(new EHCacheKey(cacheId, key));
        }
    }

    // from CacheAdapter
    public <T> Iterable<Tuple<Serializable, CachedValue<T>>> enumerate (final String cacheId)
    {
        EHCacheBin<?> bin = _bins.get(cacheId);
        if (bin == null) {
            return Collections.emptySet();
        }

        final Ehcache cache = bin.getCache();
        Iterable<Tuple<Serializable, CachedValue<T>>> tuples = Iterables.transform(
            bin.getKeys(), new Function<Serializable, Tuple<Serializable, CachedValue<T>>> () {
                public Tuple<Serializable, CachedValue<T>> apply (Serializable key) {
                    CachedValue<T> value = lookup(cache, cacheId, key);
                    return (value != null) ? Tuple.newTuple(key, value) : null;
                }
            });
        
        return Iterables.filter(tuples, Predicates.notNull());
    }

    // from CacheAdapter
    public void shutdown ()
    {
    }

    protected <T> CachedValue<T> lookup (Ehcache cache, String cacheId, Serializable key)
    {
        Element hit = cache.get(new EHCacheKey(cacheId, key));
        if (hit == null) {
            return null;
        }

        Serializable rawValue = hit.getValue();
        @SuppressWarnings("unchecked")
            final T value = (T) (rawValue instanceof NullValue ? null : rawValue);
        return new CachedValue<T>() {
            public T getValue () {
                return value;
            }
            @Override public String toString () {
                return String.valueOf(value);
            }
        };

    }

    protected Ehcache bindEHCache (CacheManager cachemgr, CacheCategory category, String cacheName)
    {
        Ehcache cache = cachemgr.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException(
            "Could not find Ehcache '" + cacheName + "'. Please fix your ehcache configuration.");
        }
        cache.getCacheEventNotificationService().registerListener(_cacheEventListener);
        _categories.put(category, cache);
        return cache;
    }

    protected Map<CacheCategory, Ehcache> _categories = Maps.newConcurrentHashMap();
    protected Map<String, EHCacheBin<?>> _bins = Maps.newConcurrentHashMap();

    protected CacheEventListener _cacheEventListener = new CacheEventListener() {
        public Object clone () throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
        public void dispose () {}
        public void notifyElementEvicted (Ehcache cache, Element element) {
            log.debug("notifyElementEvicted(" + cache + ", " + element + ")");
            removeFromBin(cache, element);
        }
        public void notifyElementExpired (Ehcache cache, Element element) {
            log.debug("notifyElementExpired(" + cache + ", " + element + ")");
            removeFromBin(cache, element);
        }
        public void notifyElementPut (Ehcache cache, Element element) {
            log.debug("notifyElementPut(" + cache + ", " + element + ")");
            addToBin(cache, element);
        }
        public void notifyElementRemoved (Ehcache cache, Element element) {
            log.debug("notifyElementRemoved(" + cache + ", " + element + ")");
            removeFromBin(cache, element);
        }
        public void notifyElementUpdated (Ehcache cache, Element element) {
            log.debug("notifyElementUpdated(" + cache + ", " + element + ")");
            addToBin(cache, element);
        }
        public void notifyRemoveAll (Ehcache cache) {}

        protected void removeFromBin (Ehcache cache, Element element)
        {
            EHCacheKey key = (EHCacheKey)element.getKey();
            EHCacheBin<?> bin = _bins.get(key.getCacheId());
            if (bin == null) {
                log.warning("Dropping element removal without cache bin", "key", key);
                return;
            }
            bin.removeKey(key.getCacheKey());
            for (Tuple<Serializable, CachedValue<Object>> tuple :
                enumerate(((EHCacheKey) element.getKey()).getCacheId())) {
                log.debug("Enumeration: " + tuple);
            }
        }
        protected void addToBin (Ehcache cache, Element element)
        {
            EHCacheKey key = (EHCacheKey)element.getKey();
            EHCacheBin<?> bin = _bins.get(key.getCacheId());
            if (bin == null) {
                log.warning("Dropping element addition without cache bin", "key", key);
                return;
            }
            bin.addKey(key.getCacheKey());
            for (Tuple<Serializable, CachedValue<Object>> tuple :
                enumerate(((EHCacheKey) element.getKey()).getCacheId())) {
                log.debug("Enumeration: " + tuple);
            }
        }
    };
    
    protected static class EHCacheBin<T>
    {
        public EHCacheBin (Ehcache cache, String id)
        {
            _cache = cache;
            _id = id;
        }

        public Set<Serializable> getKeys ()
        {
            return _keys;
        }

        public void addKey (Serializable key)
        {
            _keys.add(key);
        }

        public void removeKey (Serializable key)
        {
            _keys.remove(key);
        }

        protected Ehcache getCache ()
        {
            return _cache;
        }
        
        protected Ehcache _cache;
        protected String _id;

        protected Set<Serializable> _keys = Sets.newConcurrentHashSet();
    }

    // this is just for convenience and memory use; we don't rely on pointer equality anywhere
    protected static Serializable NULL = new NullValue() {};

    /** A class to wrap a Depot id/key into an EHCache key. */
    protected static class EHCacheKey
        implements Serializable
    {
        public EHCacheKey (String id, Serializable key)
        {
            if (id == null || key == null) {
                throw new IllegalArgumentException("Can't handle null key or id");
            }
            _id = id;
            _key = key;
        }

        public String getCacheId () {
            return _id;
        }
        
        public Serializable getCacheKey ()
        {
            return _key;
        }
        
        @Override
        public String toString ()
        {
            return "[" + _id + ", " + _key + "]";
        }
        
        @Override
        public int hashCode ()
        {
            return 31 * _id.hashCode() + _key.hashCode();
        }

        @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return _id.equals(((EHCacheKey) obj)._id) && _key.equals(((EHCacheKey) obj)._key);
        }

        protected String _id;
        protected Serializable _key;
    }

    /** A class to represent an explicitly Serializable concept of null for EHCache. */
    protected static class NullValue implements Serializable
    {
        @Override public String toString ()
        {
            return "<EHCache Null>";
        }

        @Override public boolean equals (Object other)
        {
            return other != null && other.getClass().equals(NullValue.class);
        }

        @Override public int hashCode ()
        {
            return 1;
        }
    }
}
