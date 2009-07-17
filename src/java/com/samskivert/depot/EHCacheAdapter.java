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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samskivert.util.Histogram;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.RMICacheReplicatorFactory;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import static com.samskivert.depot.Log.log;

/**
 * An implementation of {@link CacheAdapter} for ehcache where each
 * {@link CacheAdapter.CacheCategory} results in one {@link Ehcache}. All (cacheId, key)
 * combinations within one category is stuffed into the same {@link Ehcache}, and all elements are
 * cached under {@link EHCacheKey}, which basically wraps just such a tuple.
 *
 * Thus there are currently only four Ehcaches in play, called 'depotRecord', 'depotLongKeyset',
 * 'depotShortKeyset' and 'depotResult'. These must be defined in your ehcache.xml configuration.
 * If you use distributed replication/invalidation, you should replicate updates and removes but
 * not puts nor updates-via-copy.
 */
public class EHCacheAdapter
    implements CacheAdapter
{
    public static class EHCachePerformance
    {
        public Histogram lookups;
        public Histogram stores;
        public Histogram removes;
        public Histogram enumerations;
    }

    public static class EHCacheConfig
    {
        public String name;
        public int maxElementsInMemory;
        public int timeToIdleSeconds;
        public int timeToLiveSeconds;

        public boolean overflowToDisk = false;
        public int maxElementsOnDisk = 0;
        public boolean eternal = false;
        public MemoryStoreEvictionPolicy msep = MemoryStoreEvictionPolicy.LRU;

        public EHCacheConfig (String name, int maxElementsInMemory, int timeToIdleSeconds,
                              int timeToLiveSeconds) {
            this.name = name;
            this.maxElementsInMemory = maxElementsInMemory;
            this.timeToIdleSeconds = timeToIdleSeconds;
            this.timeToLiveSeconds = timeToLiveSeconds;
        }

        public Ehcache createCache (String uid) {
            return new Cache(name + "." + uid, maxElementsInMemory, msep, overflowToDisk, null,
                             eternal, timeToLiveSeconds, timeToIdleSeconds, false, 0, null);
        }
    }

    public static final EHCacheConfig EHCACHE_RECORD_CONFIG =
        new EHCacheConfig("depotRecord", 200000, 300, 600);
    public static final EHCacheConfig EHCACHE_SHORT_KEYSET_CONFIG =
        new EHCacheConfig("depotShortKeyset", 25000, 10, 10);
    public static final EHCacheConfig EHCACHE_LONG_KEYSET_CONFIG =
        new EHCacheConfig("depotLongKeyset", 25000, 300, 300);
    public static final EHCacheConfig EHCACHE_RESULT_CONFIG =
        new EHCacheConfig("depotResult", 5000, 300, 300);

    /**
     * Creates an adapter using the supplied cache manager. Note: this adapter does not shut down
     * the supplied manager when it is shutdown. The caller is responsible for shutting down the
     * cache manager when it knows that Depot and any other clients no longer need it.
     */
    public EHCacheAdapter (CacheManager cachemgr)
    {
        _cachemgr = cachemgr;
        createEHCache(CacheCategory.RECORD, EHCACHE_RECORD_CONFIG);
        createEHCache(CacheCategory.SHORT_KEYSET, EHCACHE_SHORT_KEYSET_CONFIG);
        createEHCache(CacheCategory.LONG_KEYSET, EHCACHE_LONG_KEYSET_CONFIG);
        createEHCache(CacheCategory.RESULT, EHCACHE_RESULT_CONFIG);
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
        _lookups.addValue((int) (System.currentTimeMillis() - now));
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
        _stores.addValue((int) (System.currentTimeMillis() - now));
    }

    // from CacheAdapter
    public void remove (String cacheId, Serializable key)
    {
        long now = System.currentTimeMillis();
        EHCacheBin<?> bin = _bins.get(cacheId);
        if (bin != null) {
            bin.getCache().remove(new EHCacheKey(cacheId, key));
        }
        _removes.addValue((int) (System.currentTimeMillis() - now));
    }

    // from CacheAdapter
    public <T> Iterable<Serializable> enumerate (final String cacheId)
    {
        long now = System.currentTimeMillis();
        EHCacheBin<?> bin = _bins.get(cacheId);
        if (bin == null) {
            return Collections.emptySet();
        }

        // let's return a simple copy of the bin's fancy concurrent hashset
        Set<Serializable> result = Sets.newHashSet(bin.getKeys());
        _enumerations.addValue((int) (System.currentTimeMillis() - now));
        return result;
    }

    // from CacheAdapter
    public void shutdown ()
    {
        log.debug("EHCacheAdapter shutting down", "lookups", _lookups,
                  "stores", _stores, "removes", _removes, "enumerations", _enumerations);

        // go through and remove all of the caches we resolved
        for (Ehcache cache : _categories.values()) {
            log.debug("Removing ehcache " + cache.getName());
            _cachemgr.removeCache(cache.getName());
        }
        _categories.clear();
        _bins.clear();
    }

    /**
     * Return a snapshot of the current histograms detailing how much time the different
     * operations lookup, store, remove and enumerate take.
     */
    public EHCachePerformance getPerformanceSnapshot ()
    {
        EHCachePerformance result = new EHCachePerformance();
        result.lookups = _lookups.clone();
        result.stores = _stores.clone();
        result.removes = _removes.clone();
        result.enumerations = _enumerations.clone();
        return result;
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

    protected Ehcache createEHCache (CacheCategory category, EHCacheConfig config)
    {
        Ehcache cache = config.createCache(String.valueOf(hashCode()));
        addCacheListeners(cache);
        _cachemgr.addCache(cache);
        log.debug("Added new ehcache " + cache.getName());
        _categories.put(category, cache);
        return cache;
    }

    protected void addCacheListeners (Ehcache cache)
    {
        // add a listener that updates our local cache
        cache.getCacheEventNotificationService().registerListener(_cacheEventListener);

        // add an RMI replicator (TODO: this should be optional) (TODO: do we want only one of
        // these and to register the same one with all caches?)
        Properties props = new Properties();
        props.setProperty("replicateAsynchronously", "true");
        props.setProperty("replicatePuts", "false");
        props.setProperty("replicateUpdates", "true");
        props.setProperty("replicateUpdatesViaCopy", "false");
        props.setProperty("replicateRemovals", "true");
        cache.getCacheEventNotificationService().registerListener(
            new RMICacheReplicatorFactory().createCacheEventListener(props));
    }

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

        protected Set<Serializable> _keys =
            Sets.newSetFromMap(new ConcurrentHashMap<Serializable, Boolean>());
    }

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
            EHCacheKey okey = (EHCacheKey) obj;
            return _id.equals(okey._id) && _key.equals(okey._key);
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

    protected CacheManager _cachemgr;
    protected Histogram _lookups = new Histogram(0, 50, 20);
    protected Histogram _stores = new Histogram(0, 50, 20);
    protected Histogram _removes = new Histogram(0, 50, 20);
    protected Histogram _enumerations = new Histogram(0, 50, 20);

    protected Map<CacheCategory, Ehcache> _categories =
        Collections.synchronizedMap(Maps.<CacheCategory, Ehcache>newHashMap());
    protected Map<String, EHCacheBin<?>> _bins =
        Collections.synchronizedMap(Maps.<String, EHCacheBin<?>> newHashMap());

    // this is just for convenience and memory use; we don't rely on pointer equality anywhere
    protected static Serializable NULL = new NullValue() {};
}
