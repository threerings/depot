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

public class EHCacheConfig
{
    /** The maximum number of cached records to keep in memory. */
    public int elementsInMemory;

    /** Whether or not to overflow records to disk. */
    public boolean overflowToDisk;

    /** How long to keep a record in the cache after it's been stored or updated. */
    public int timeToLiveSeconds;

    /** How long to keep a record in the cache after it's last accessed. */
    public int timeToIdleSeconds;

    /** Whether or not this is a distributed cache. Setting this to true makes various demands
     * on your ehcache.xml configuration. */
    public boolean distributed;

    /** If true, newly cached records will be serialized and sent to all peer caches. */
    public boolean sendNewRecordsToPeers;

    /** If true, modification of a cached record will remove any cached versions on peers. */
    public boolean invalidateUpdatedRecordsOnPeers;

    /** If true, modification of a cached record will send the new version to all peer caches. */
    public boolean sendUpdatedRecordsToPeers;

    /** If true, removal of a cached record will remove any cached versions on peers. */
    public boolean invalidateRemovedRecordsOnPeers;

    /** How often to perform replication, in milliseconds. */
    public int replicationInterval;

    public EHCacheConfig (int elementsInMemory, boolean overflowToDisk, int timeToLiveSeconds,
        int timeToIdleSeconds, boolean distributed, boolean sendNewRecordsToPeers,
        boolean sendUpdatedRecordsToPeers, boolean invalidateUpdatedRecordsOnPeers,
        boolean invalidateRemovedRecordsOnPeers, int replicationInterval)
    {
        this.elementsInMemory = elementsInMemory;
        this.overflowToDisk = overflowToDisk;
        this.timeToLiveSeconds = timeToLiveSeconds;
        this.timeToIdleSeconds = timeToIdleSeconds;
        this.distributed = distributed;
        this.sendNewRecordsToPeers = sendNewRecordsToPeers;
        this.sendUpdatedRecordsToPeers = sendUpdatedRecordsToPeers;
        this.invalidateUpdatedRecordsOnPeers = invalidateUpdatedRecordsOnPeers;
        this.invalidateRemovedRecordsOnPeers = invalidateRemovedRecordsOnPeers;
        this.replicationInterval = replicationInterval;
    }

    public EHCacheConfig (int elementsInMemory, boolean overflowToDisk, int timeToLiveSeconds,
        int timeToIdleSeconds)
    {
        this(elementsInMemory, overflowToDisk, timeToLiveSeconds, timeToIdleSeconds, false,
            false, false, false, false, -1);
    }
}
