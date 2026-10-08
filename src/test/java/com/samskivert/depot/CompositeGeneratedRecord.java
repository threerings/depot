//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

public class CompositeGeneratedRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CompositeGeneratedRecord> _R = CompositeGeneratedRecord.class;
    public static final ColumnExp<Long> RECORD_ID = colexp(_R, "recordId");
    public static final ColumnExp<Integer> SEQ_ID = colexp(_R, "seqId");
    public static final ColumnExp<Long> VALUE = colexp(_R, "value");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    @Id @GeneratedValue
    public long recordId;

    @Id
    public int seqId;

    public long value;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CompositeGeneratedRecord}
     * with the supplied key values.
     */
    public static Key<CompositeGeneratedRecord> getKey (long recordId, int seqId)
    {
        return newKey(_R, recordId, seqId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORD_ID, SEQ_ID); }
    // AUTO-GENERATED: METHODS END
}
