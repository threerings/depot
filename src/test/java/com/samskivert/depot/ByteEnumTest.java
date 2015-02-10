//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.lang.reflect.Field;

import org.junit.Test;
import static org.junit.Assert.*;

import com.samskivert.depot.impl.FieldMarshaller;
import com.samskivert.depot.util.ByteEnum;

/**
 * Tests ByteEnum related bits.
 */
public class ByteEnumTest
{
    public enum TestEnum implements ByteEnum {
        ONE(1), TWO(2), THREE(3);

        // from interface ByteEnum
        public byte toByte () {
            return _code;
        }

        TestEnum (int code) {
            _code = (byte)code;
        }
        protected byte _code;
    }

    public class NotAnEnum implements ByteEnum {
        public byte toByte () {
            return 0;
        }
    }

    public static class TestRecord extends PersistentRecord
    {
        public TestEnum good;
        public NotAnEnum bad;
    }

    @Test public void testMarshaller ()
        throws NoSuchFieldException
    {
        Field good = TestRecord.class.getField("good");
        Field bad = TestRecord.class.getField("bad");
        assertTrue(FieldMarshaller.createMarshaller(good) != null);

        try {
            FieldMarshaller.createMarshaller(bad);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
