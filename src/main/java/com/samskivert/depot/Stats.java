//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.util.Arrays;

import static com.samskivert.depot.Log.log;

/**
 * Manages Depot performance statistics.
 */
public class Stats
{
    /** Used for tracking a set of values that fall into a discrete range of values. */
    public static class Histogram implements Cloneable
    {
        public Histogram (int minValue, int bucketWidth, int bucketCount) {
            _minValue = minValue;
            _maxValue = minValue + bucketWidth*bucketCount;
            _bucketWidth = bucketWidth;
            _buckets = new int[bucketCount];
        }

        /**
         * Returns the array containing the bucket values. The zeroth element contains the count of
         * all values less than <code>minValue</code>, the subsequent <code>bucketCount</code>
         * elements contain the count of values falling into those buckets and the last element
         * contains values greater than or equal to <code>maxValue</code>.
         */
        public int[] getBuckets () {
            return _buckets;
        }

        /**
         * Generates a terse summary of the count and contents of the values in this histogram.
         */
        public String summarize () {
            StringBuilder buf = new StringBuilder();
            buf.append(_count).append(":");
            for (int ii = 0; ii < _buckets.length; ii++) {
                if (ii > 0) {
                    buf.append(",");
                }
                buf.append(_buckets[ii]);
            }
            return buf.toString();
        }

        @Override public Histogram clone () {
            try {
                Histogram chisto = (Histogram)super.clone();
                chisto._buckets = _buckets.clone();
                return chisto;
            } catch (CloneNotSupportedException cnse) {
                throw new AssertionError(cnse);
            }
        }

        @Override public String toString () {
            return "[min=" + _minValue + ", max=" + _maxValue + ", bwidth=" + _bucketWidth +
                ", buckets=" + Arrays.toString(_buckets) + "]";
        }

        void addValue (int value) {
            if (value < _minValue) {
                _buckets[0]++;
            } else if (value >= _maxValue) {
                _buckets[_buckets.length-1]++;
            } else {
                _buckets[(value-_minValue)/_bucketWidth]++;
            }
            _count++;
        }
        int size () {
            return _count;
        }
        void clear () {
            Arrays.fill(_buckets, 0);
        }

        protected int _minValue, _maxValue, _bucketWidth, _count;
        protected int[] _buckets;
    }

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

    public synchronized void noteQuery (
        Class<? extends PersistentRecord> type, int cachedQueries, int uncachedQueries,
        int explicitQueries, int cachedRecords, int uncachedRecords)
    {
        _cachedQueries += cachedQueries;
        _uncachedQueries += uncachedQueries;
        _explicitQueries += explicitQueries;
        _cachedRecords += cachedRecords;
        _uncachedRecords += uncachedRecords;
    }

    public synchronized void noteModification (Class<? extends PersistentRecord> type)
    {
        // nothing by default
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
