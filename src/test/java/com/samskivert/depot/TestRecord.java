//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * A test persistent object.
 */
@Entity
public class TestRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TestRecord> _R = TestRecord.class;
    public static final ColumnExp<Integer> RECORD_ID = colexp(_R, "recordId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<Integer> AGE = colexp(_R, "age");
    public static final ColumnExp<Float> AWESOMENESS = colexp(_R, "awesomeness");
    public static final ColumnExp<String> HOME_TOWN = colexp(_R, "homeTown");
    public static final ColumnExp<EnumKeyRecord.Type> TYPE = colexp(_R, "type");
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Timestamp> LAST_MODIFIED = colexp(_R, "lastModified");
    public static final ColumnExp<int[]> NUMBERS = colexp(_R, "numbers");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    @Id
    public int recordId;

    public String name;

    public int age;

    public float awesomeness;

    public String homeTown;

    public EnumKeyRecord.Type type;

    @Index
    public Date created;

    public Timestamp lastModified;

    public int[] numbers;

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TestRecord}
     * with the supplied key values.
     */
    public static Key<TestRecord> getKey (int recordId)
    {
        return newKey(_R, recordId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORD_ID); }
    // AUTO-GENERATED: METHODS END
}
