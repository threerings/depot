//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maintains a record of all successfully invoked data migrations.
 */
public class DepotMigrationHistoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DepotMigrationHistoryRecord> _R = DepotMigrationHistoryRecord.class;
    public static final ColumnExp<String> IDENT = colexp(_R, "ident");
    public static final ColumnExp<Timestamp> WHEN_COMPLETED = colexp(_R, "whenCompleted");
    // AUTO-GENERATED: FIELDS END

    /** Our schema version. Probably not likely to change. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique identifier for this migration. */
    @Id public String ident;

    /** The time at which the migration was completed. */
    @Column(nullable=true)
    public Timestamp whenCompleted;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DepotMigrationHistoryRecord}
     * with the supplied key values.
     */
    public static Key<DepotMigrationHistoryRecord> getKey (String ident)
    {
        return newKey(_R, ident);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(IDENT); }
    // AUTO-GENERATED: METHODS END
}
