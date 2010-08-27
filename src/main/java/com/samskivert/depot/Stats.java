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

import com.samskivert.util.Histogram;

import static com.samskivert.depot.Log.log;

/**
 * Manages Depot performance statistics.
 */
public class Stats
{
    /**
     * An immutable class used to report statistics on repository activity. Statistics are tracked
     * from the start of the VM and are never reset.
     */
    public static class Snapshot
    {
        /** The total number of queries and modifiers executed. */
        public final int totalOps;

        /** The total number of milliseconds spent waiting for a JDBC connection. */
        public final long connectionWaitTime;

        /** The total number of collection queries that were loaded from the cache. */
        public final int cachedQueries;

        /** The total number of collection queries that were loaded from the database. */
        public final int uncachedQueries;

        /** The total number of one-phase collection queries that executed. */
        public final int explicitQueries;

        /** The number of record loads (individual or as part of a collection query) that were
         * loaded from the cache. */
        public final long cachedRecords;

        /** The number of record loads (individual or as part of a collection query) that were
         * loaded from the database. */
        public final long uncachedRecords;

        /** A histogram of query durations (with 500ms buckets). */
        public final Histogram queryHisto;

        /** The total number of milliseconds spent executing queries. */
        public final long queryTime;

        /** A histogram of modifier durations (with 500ms buckets). */
        public final Histogram modifierHisto;

        /** The total number of milliseconds spent executing modifiers. */
        public final long modifierTime;

        /** Creates a stats instance. */
        protected Snapshot (int totalOps, long connectionWaitTime,
                            int cachedQueries, int uncachedQueries, int explicitQueries,
                            int cachedRecords, int uncachedRecords,
                            Histogram queryHisto, long queryTime,
                            Histogram modifierHisto, long modifierTime)
        {
            this.totalOps = totalOps;
            this.connectionWaitTime = connectionWaitTime;
            this.cachedQueries = cachedQueries;
            this.uncachedQueries = uncachedQueries;
            this.explicitQueries = explicitQueries;
            this.cachedRecords = cachedRecords;
            this.uncachedRecords = uncachedRecords;
            this.queryHisto = queryHisto;
            this.queryTime = queryTime;
            this.modifierHisto = modifierHisto;
            this.modifierTime = modifierTime;
        }
    }

    public synchronized Snapshot getSnapshot ()
    {
        return new Snapshot(_totalOps, _connectionWaitTime,
                            _cachedQueries, _uncachedQueries, _explicitQueries,
                            _cachedRecords, _uncachedRecords,
                            _readHisto.clone(), _readTime, _writeHisto.clone(), _writeTime);
    }

    public synchronized void noteOp (
        boolean isReadOnly, long preConnect, long preInvoke, long postInvoke)
    {
        _totalOps++;
        _connectionWaitTime += (preInvoke - preConnect) / 1000000L;

        long opTime = (postInvoke - preInvoke) / 1000000L;
        if (opTime > Integer.MAX_VALUE) {
            log.warning("ZOMG! A database operation took " + opTime + "ms to complete!");
            opTime = Integer.MAX_VALUE;
        }

        if (isReadOnly) {
            _readTime += opTime;
            _readHisto.addValue((int)opTime);
        } else {
            _writeTime += opTime;
            _writeHisto.addValue((int)opTime);
        }
    }

    public synchronized void noteQuery (int cachedQueries, int uncachedQueries, int explicitQueries,
                                        int cachedRecords, int uncachedRecords)
    {
        _cachedQueries += cachedQueries;
        _uncachedQueries += uncachedQueries;
        _explicitQueries += explicitQueries;
        _cachedRecords += cachedRecords;
        _uncachedRecords += uncachedRecords;
    }

    protected int _totalOps;
    protected long _connectionWaitTime;

    protected Histogram _readHisto = new Histogram(0, 500, 20);
    protected long _readTime;

    protected Histogram _writeHisto = new Histogram(0, 500, 20);
    protected long _writeTime;

    protected int _cachedQueries, _uncachedQueries, _explicitQueries;
    protected int _cachedRecords, _uncachedRecords;
}
