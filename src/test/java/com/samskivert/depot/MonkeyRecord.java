//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Id;

/**
 * Used for testing.
 */
public class MonkeyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MonkeyRecord> _R = MonkeyRecord.class;
    public static final ColumnExp<Integer> SPECIES = colexp(_R, "species");
    public static final ColumnExp<Integer> MONKEY_ID = colexp(_R, "monkeyId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /** This monkey's species. This is part of our key so that we have a composite key. */
    @Id public int species;

    /** This monkey's unique identifier. */
    @Id public int monkeyId;

    public String name;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MonkeyRecord}
     * with the supplied key values.
     */
    public static Key<MonkeyRecord> getKey (int species, int monkeyId)
    {
        return newKey(_R, species, monkeyId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SPECIES, MONKEY_ID); }
    // AUTO-GENERATED: METHODS END
}
