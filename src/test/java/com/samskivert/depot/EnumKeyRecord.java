//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tests a record that uses an enum as its key.
 */
public class EnumKeyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EnumKeyRecord> _R = EnumKeyRecord.class;
    public static final ColumnExp<EnumKeyRecord.Type> TYPE = colexp(_R, "type");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    public enum Type {
        A, B, C, D;

        // override toString to be sure that we don't rely on it in our internals
        public String toString () {
            return "Type:" + name();
        }
    };

    /** The type is key. */
    @Id public Type type;

    public String name;

    public EnumKeyRecord () {}

    public EnumKeyRecord (Type type, String name)
    {
        this.type = type;
        this.name = name;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EnumKeyRecord}
     * with the supplied key values.
     */
    public static Key<EnumKeyRecord> getKey (EnumKeyRecord.Type type)
    {
        return newKey(_R, type);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TYPE); }
    // AUTO-GENERATED: METHODS END
}
