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
        long now = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        EHCacheBin<T> bin = (EHCacheBin<T>) _bins.get(cacheId);
        if (bin == null) {
            return null;
        }
        CachedValue<T> result = lookup(bin.getCache(), cacheId, key);
        long dT = System.currentTimeMillis() - now;
        if (dT > 50) {
            log.warning("Aii! A simple ehcache lookup took over 50 ms!", "cacheId", cacheId,
                "key", key, "dT", dT);
        }
        return result;
    }

    // from CacheAdapter
    public <T> void store (CacheCategory category, String cacheId, Serializable key, T value)
    {
        long now = System.currentTimeMillis();
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
        long dT = System.currentTimeMillis() - now;
        if (dT > 50) {
            log.warning("Aii! A simple ehcache store took over 50 ms!", "cacheId", cacheId,
                "key", key, "dT", dT);
        }
    }

    // from CacheAdapter
    public void remove (String cacheId, Serializable key)
    {
        long now = System.currentTimeMillis();
        EHCacheBin<?> bin = _bins.get(cacheId);
        if (bin != null) {
            bin.getCache().remove(new EHCacheKey(cacheId, key));
        }
        long dT = System.currentTimeMillis() - now;
        if (dT > 50) {
            log.warning("Aii! A simple ehcache remove took over 50 ms!", "cacheId", cacheId,
                "key", key, "dT", dT);
        }
    }

    // from CacheAdapter
    public <T> Iterable<Tuple<Serializable, CachedValue<T>>> enumerate (final String cacheId)
    {
        if (_slowEnumerations > 5) {
            return Collections.emptySet();
        }
        long now = System.currentTimeMillis();

        EHCacheBin<?> bin = _bins.get(cacheId);
        if (bin == null) {
            return Collections.emptySet();
        }

        Set<Tuple<Serializable, CachedValue<T>>> result = Sets.newHashSet();
        Ehcache cache = bin.getCache();

        for (Serializable key : bin.getKeys()) {
            CachedValue<T> value = lookup(cache, cacheId, key);
            if (value != null) {
                result.add(Tuple.newTuple(key, value));
            }
        }
        long dT = System.currentTimeMillis() - now;
        if (dT > 50) {
            _slowEnumerations ++;
            log.warning("Aii! Enumerating cache took a long time!", "cacheId", cacheId, "dT", dT,
                "cacheSize", result.size(), "disabled", _slowEnumerations > 5);
        }
        return result;
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

    // TODO: To be removed when we're done investigating potential sluggishness
    protected int _slowEnumerations = 0;

    protected Map<CacheCategory, Ehcache> _categories =
        Collections.synchronizedMap(Maps.<CacheCategory, Ehcache>newHashMap());
    protected Map<String, EHCacheBin<?>> _bins =
        Collections.synchronizedMap(Maps.<String, EHCacheBin<?>> newHashMap());

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
                log.debug("Dropping element removal without cache bin", "key", key);
                return;
            }
            bin.removeKey(key.getCacheKey());
        }
        protected void addToBin (Ehcache cache, Element element)
        {
            EHCacheKey key = (EHCacheKey)element.getKey();
            EHCacheBin<?> bin = _bins.get(key.getCacheId());
            if (bin == null) {
                log.debug("Dropping element addition without cache bin", "key", key);
                return;
            }
            bin.addKey(key.getCacheKey());
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
