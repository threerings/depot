//
// $Id: EHCacheAdapter.java 325 2008-11-16 08:03:33Z samskivert $
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.RMIAsynchronousCacheReplicator;

import static com.samskivert.depot.Log.log;

/**
 * An implementation of {@link CacheAdapter} for ehcache.
 */
public class OldEHCacheAdapter
    implements CacheAdapter
{
    /**
     * Creates an adapter using the supplied cache manager. Note: this adapter does not shut down
     * the supplied manager when it is shutdown. The caller is responsible for shutting down the
     * cache manager when it knows that Depot and any other clients no longer need it.
     */
    public OldEHCacheAdapter (CacheManager cachemgr)
    {
        _cachemgr = cachemgr;

        CacheManagerPeerListener listener = _cachemgr.getCachePeerListener();
        CacheManagerPeerProvider provider = _cachemgr.getCachePeerProvider();
        if ((provider != null) != (listener != null)) {
            // we want either both listener and provider, or neither
            log.warning("EHCache misconfigured, distributed mode disabled [listener =" +
                listener + ", provider=" + provider);
            _distributed = false;

        } else {
            _distributed = (listener != null);
        }
    }

    public <T> EHCacheBin<T> getCache (String id)
    {
        return new EHCacheBin<T>(id);
    }

    /**
     * The main ehcache-bridging class, a {@link CacheBin} interface against {@link Cache}.
     */
    protected class EHCacheBin<T>
    {
        // from CacheBin
        public CachedValue<T> lookup (Serializable key)
        {
            Element hit = _cache.get(key);
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

        // from CacheBin
        public void store (Serializable key, T value)
        {
            _cache.put(new Element(key, value != null ? value : NULL));
        }

        // from CacheBin
        public void remove (Serializable key)
        {
            _cache.remove(key);
        }

        // from CacheBin
        public Iterable<Serializable> enumerateKeys ()
        {
            @SuppressWarnings("unchecked") Iterable<Serializable> keys = _cache.getKeys();
            return keys;
        }

        protected EHCacheBin (String id)
        {
            synchronized (_cachemgr) {
                _cache = _cachemgr.getCache(id);
                if (_cache == null) {
                    // create the cache programatically with reasonable settings
                    // TODO: we will eventually need this to be configurable in .properties
                    _cache = new Cache(id,
                                       3000,   // keep 3000 elements in RAM
                                       false,   // overflow the rest to disk
                                       false,  // don't keep records around eternally
                                       300,    // keep them for 5 minutes after they're created
                                       60);    // or 60 seconds after last access

                    if (_distributed) {
                        // a programatically created cache has to have its replicator event
                        // listener programatically added.
                        _cache.getCacheEventNotificationService().registerListener(
                            new RMIAsynchronousCacheReplicator(false, true, false, true, 1000));
                    }
                    _cachemgr.addCache(_cache);
                }
            }
        }

        protected Cache _cache;
    }

    // from CacheAdapter
    public void shutdown ()
    {
    }

    protected boolean _distributed;
    protected CacheManager _cachemgr;

    // this is just for convenience and memory use; we don't rely on pointer equality anywhere
    protected static Serializable NULL = new NullValue() {};

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

    public <T> Iterable<Serializable> enumerate (String cacheId)
    {
        return getCache(cacheId).enumerateKeys();
    }

    public <T> CachedValue<T> lookup (String cacheId, Serializable key)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void remove (String cacheId, Serializable key)
    {
        // TODO Auto-generated method stub

    }

    public <T> void store (CacheCategory category, String cacheId, Serializable key, T value)
    {
        // TODO Auto-generated method stub

    }
}
