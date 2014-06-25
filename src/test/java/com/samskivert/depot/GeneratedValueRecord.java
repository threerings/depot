//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

public class GeneratedValueRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GeneratedValueRecord> _R = GeneratedValueRecord.class;
    public static final ColumnExp<Integer> RECORD_ID = colexp(_R, "recordId");
    public static final ColumnExp<Integer> VALUE = colexp(_R, "value");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    @Id @GeneratedValue
    public int recordId;

    public int value;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GeneratedValueRecord}
     * with the supplied key values.
     */
    public static Key<GeneratedValueRecord> getKey (int recordId)
    {
        return newKey(_R, recordId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORD_ID); }
    // AUTO-GENERATED: METHODS END
}
