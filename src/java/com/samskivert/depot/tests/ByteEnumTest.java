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

package com.samskivert.depot.tests;

import java.lang.reflect.Field;

import org.junit.Test;
import static org.junit.Assert.*;

import com.samskivert.util.ByteEnum;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.FieldMarshaller;

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
