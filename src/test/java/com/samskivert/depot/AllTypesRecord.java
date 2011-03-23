//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;

/**
 * Tests all of the supported Depot field types.
 */
public class AllTypesRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AllTypesRecord> _R = AllTypesRecord.class;
    public static final ColumnExp<Integer> RECORD_ID = colexp(_R, "recordId");
    public static final ColumnExp<Boolean> BOOLEAN_VALUE = colexp(_R, "booleanValue");
    public static final ColumnExp<Byte> BYTE_VALUE = colexp(_R, "byteValue");
    public static final ColumnExp<Short> SHORT_VALUE = colexp(_R, "shortValue");
    public static final ColumnExp<Integer> INT_VALUE = colexp(_R, "intValue");
    public static final ColumnExp<Long> LONG_VALUE = colexp(_R, "longValue");
    public static final ColumnExp<Float> FLOAT_VALUE = colexp(_R, "floatValue");
    public static final ColumnExp<Double> DOUBLE_VALUE = colexp(_R, "doubleValue");
    public static final ColumnExp<Boolean> BOXED_BOOLEAN = colexp(_R, "boxedBoolean");
    public static final ColumnExp<Byte> BOXED_BYTE = colexp(_R, "boxedByte");
    public static final ColumnExp<Short> BOXED_SHORT = colexp(_R, "boxedShort");
    public static final ColumnExp<Integer> BOXED_INT = colexp(_R, "boxedInt");
    public static final ColumnExp<Long> BOXED_LONG = colexp(_R, "boxedLong");
    public static final ColumnExp<Float> BOXED_FLOAT = colexp(_R, "boxedFloat");
    public static final ColumnExp<Double> BOXED_DOUBLE = colexp(_R, "boxedDouble");
    public static final ColumnExp<byte[]> BYTE_ARRAY = colexp(_R, "byteArray");
    public static final ColumnExp<int[]> INT_ARRAY = colexp(_R, "intArray");
    public static final ColumnExp<String> STRING = colexp(_R, "string");
    public static final ColumnExp<Date> DATE = colexp(_R, "date");
    public static final ColumnExp<Time> TIME = colexp(_R, "time");
    public static final ColumnExp<Timestamp> TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp<AllTypesRecord.TestEnum> TEST_ENUM = colexp(_R, "testEnum");
    public static final ColumnExp<Boolean> NULL_BOXED_BOOLEAN = colexp(_R, "nullBoxedBoolean");
    public static final ColumnExp<Byte> NULL_BOXED_BYTE = colexp(_R, "nullBoxedByte");
    public static final ColumnExp<Short> NULL_BOXED_SHORT = colexp(_R, "nullBoxedShort");
    public static final ColumnExp<Integer> NULL_BOXED_INT = colexp(_R, "nullBoxedInt");
    public static final ColumnExp<Long> NULL_BOXED_LONG = colexp(_R, "nullBoxedLong");
    public static final ColumnExp<Float> NULL_BOXED_FLOAT = colexp(_R, "nullBoxedFloat");
    public static final ColumnExp<Double> NULL_BOXED_DOUBLE = colexp(_R, "nullBoxedDouble");
    public static final ColumnExp<byte[]> NULL_BYTE_ARRAY = colexp(_R, "nullByteArray");
    public static final ColumnExp<int[]> NULL_INT_ARRAY = colexp(_R, "nullIntArray");
    public static final ColumnExp<String> NULL_STRING = colexp(_R, "nullString");
    public static final ColumnExp<Date> NULL_DATE = colexp(_R, "nullDate");
    public static final ColumnExp<Time> NULL_TIME = colexp(_R, "nullTime");
    public static final ColumnExp<Timestamp> NULL_TIMESTAMP = colexp(_R, "nullTimestamp");
    public static final ColumnExp<AllTypesRecord.TestEnum> NULL_TEST_ENUM = colexp(_R, "nullTestEnum");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    public enum TestEnum { ONE, TWO, THREE; }

    @Id public int recordId;

    public boolean booleanValue;
    public byte byteValue;
    public short shortValue;
    public int intValue;
    public long longValue;
    public float floatValue;
    public double doubleValue;

    public Boolean boxedBoolean;
    public Byte boxedByte;
    public Short boxedShort;
    public Integer boxedInt;
    public Long boxedLong;
    public Float boxedFloat;
    public Double boxedDouble;
    public byte[] byteArray;
    public int[] intArray;

    public String string;

    public Date date;
    public Time time;
    public Timestamp timestamp;
    // public Blob blob; // tested by byte[]
    // public Clob clob; // not clear how to test this

    public TestEnum testEnum;

    @Column(nullable=true) public Boolean nullBoxedBoolean;
    @Column(nullable=true) public Byte nullBoxedByte;
    @Column(nullable=true) public Short nullBoxedShort;
    @Column(nullable=true) public Integer nullBoxedInt;
    @Column(nullable=true) public Long nullBoxedLong;
    @Column(nullable=true) public Float nullBoxedFloat;
    @Column(nullable=true) public Double nullBoxedDouble;
    @Column(nullable=true) public byte[] nullByteArray;
    @Column(nullable=true) public int[] nullIntArray;
    @Column(nullable=true) public String nullString;
    @Column(nullable=true) public Date nullDate;
    @Column(nullable=true) public Time nullTime;
    @Column(nullable=true) public Timestamp nullTimestamp;
    @Column(nullable=true) public TestEnum nullTestEnum;

    @Override public boolean equals (Object other) {
        if (!(other instanceof AllTypesRecord)) {
            return false;
        }
        AllTypesRecord orec = (AllTypesRecord)other;
        return (booleanValue == orec.booleanValue) &&
            (byteValue == orec.byteValue) &&
            (shortValue == orec.shortValue) &&
            (intValue == orec.intValue) &&
            (longValue == orec.longValue) &&
            (floatValue == orec.floatValue) &&
            (doubleValue == orec.doubleValue) &&
            boxedBoolean.equals(orec.boxedBoolean) &&
            boxedByte.equals(orec.boxedByte) &&
            boxedShort.equals(orec.boxedShort) &&
            boxedInt.equals(orec.boxedInt) &&
            boxedLong.equals(orec.boxedLong) &&
            boxedFloat.equals(orec.boxedFloat) &&
            boxedDouble.equals(orec.boxedDouble) &&
            Arrays.equals(byteArray, orec.byteArray) &&
            Arrays.equals(intArray, orec.intArray) &&
            string.equals(orec.string) &&
            date.equals(orec.date) &&
            time.equals(orec.time) &&
            timestamp.equals(orec.timestamp) &&
            testEnum.equals(orec.testEnum) &&
            (nullBoxedBoolean == orec.nullBoxedBoolean) &&
            (nullBoxedByte == orec.nullBoxedByte) &&
            (nullBoxedShort == orec.nullBoxedShort) &&
            (nullBoxedInt == orec.nullBoxedInt) &&
            (nullBoxedLong == orec.nullBoxedLong) &&
            (nullBoxedFloat == orec.nullBoxedFloat) &&
            (nullBoxedDouble == orec.nullBoxedDouble) &&
            (nullByteArray == orec.nullByteArray) &&
            (nullIntArray == orec.nullIntArray) &&
            (nullString == orec.nullString) &&
            (nullDate == orec.nullDate) &&
            (nullTime == orec.nullTime) &&
            (nullTimestamp == orec.nullTimestamp) &&
            (nullTestEnum == orec.nullTestEnum);
    }

    public static AllTypesRecord createRecord (int recordId)
    {
        AllTypesRecord rec = new AllTypesRecord();
        rec.recordId = recordId;
        rec.booleanValue = true;
        rec.byteValue = 1;
        rec.shortValue = 2;
        rec.intValue = 3;
        rec.longValue = 4;
        rec.floatValue = 5.5f;
        rec.doubleValue = 6.6;
        rec.boxedBoolean = true;
        rec.boxedByte = 7;
        rec.boxedShort = 8;
        rec.boxedInt = 9;
        rec.boxedLong = 10l;
        rec.boxedFloat = 11.11f;
        rec.boxedDouble = 12.12;
        rec.byteArray = new byte[] { 13, 14 };
        rec.intArray = new int[] { 15, 16 };
        rec.string = "seventeen";
        rec.date = Date.valueOf("2010-01-18");
        rec.time = Time.valueOf("19:19:19");
        rec.timestamp = new Timestamp(System.currentTimeMillis()+20);
        rec.testEnum = TestEnum.TWO;
        return rec;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AllTypesRecord}
     * with the supplied key values.
     */
    public static Key<AllTypesRecord> getKey (int recordId)
    {
        return newKey(_R, recordId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORD_ID); }
    // AUTO-GENERATED: METHODS END
}
