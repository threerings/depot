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
import java.util.Set;

import org.junit.Test;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Transformer;
import com.samskivert.depot.Transformers;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Transform;
import com.samskivert.depot.impl.FieldMarshaller;

import static org.junit.Assert.*;

/**
 * Tests the @Transform annotation.
 */
public class TransformTest extends TestBase
{
    @Transform(CustomTypeTransformer.class)
    public static class CustomType
    {
        public final String value;

        public CustomType (String value) {
            this.value = value;
        }

        @Override public boolean equals (Object other) {
            return !(other instanceof CustomType) ? false :
                value.equals(((CustomType)other).value);
        }

        @Override public int hashCode () {
            return value.hashCode();
        }
    }

    @Transform(CustomTypeTransformer.class)
    public static class InvalidCustomType
    {
    }

    @Transform(ShortEnumTransformer.class)
    public static interface ShortEnum
    {
        public short toShort ();
    }

    public enum Ordinal implements ShortEnum {
        ONE(1), TWO(2), THREE(3);

        public short toShort () {
            return _code;
        }

        Ordinal (int code) {
            _code = (short)code;
        }
        protected short _code;
    }

    public static class CustomTypeTransformer implements Transformer<CustomType, String>
    {
        public String toPersistent (CustomType value) {
            return value.value;
        }

        public CustomType fromPersistent (Class<?> ftype, String value) {
            return new CustomType(value);
        }
    }

    public static class ShortEnumTransformer implements Transformer<ShortEnum, Short>
    {
        public Short toPersistent (ShortEnum value) {
            return value.toShort();
        }
        public ShortEnum fromPersistent (Class<?> ftype, Short value) {
            @SuppressWarnings("unchecked") Class<Dummy> eclass = (Class<Dummy>)ftype;
            return fromShort(eclass, value);
        }
        private enum Dummy implements ShortEnum {
            DUMMY;
            public short toShort () { return 0; }
        };
    }

    public static class TransformRecord extends PersistentRecord
    {
        // AUTO-GENERATED: FIELDS START
        public static final Class<TransformRecord> _R = TransformRecord.class;
        public static final ColumnExp RECORD_ID = colexp(_R, "recordId");
        public static final ColumnExp STRINGS = colexp(_R, "strings");
        public static final ColumnExp CUSTOM = colexp(_R, "custom");
        public static final ColumnExp ORDINAL = colexp(_R, "ordinal");
        // AUTO-GENERATED: FIELDS END

        public static final int SCHEMA_VERSION = 1;

        @Id public int recordId;

        @Transform(Transformers.CommaSeparatedString.class)
        public String[] strings;

        public CustomType custom;

        public Ordinal ordinal;

        // AUTO-GENERATED: METHODS START
        /**
         * Create and return a primary {@link Key} to identify a {@link TestRecord}
         * with the supplied key values.
         */
        public static Key<TransformRecord> getKey (int recordId)
        {
            return newKey(_R, recordId);
        }

        /** Register the key fields in an order matching the getKey() factory. */
        static { registerKeyFields(RECORD_ID); }
        // AUTO-GENERATED: METHODS END
    }

    public static class BadTransformRecord extends PersistentRecord
    {
        @Transform(Transformers.CommaSeparatedString.class)
        public Thread thread;

        public InvalidCustomType invalid;
    }

    public static class TransformRepository extends DepotRepository
    {
        public TransformRepository (PersistenceContext ctx) {
            super(ctx);
        }

        public TransformRecord loadNoCache (int recordId)
        {
            return load(TransformRecord.getKey(recordId), CacheStrategy.NONE);
        }

        @Override // from DepotRepository
        protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes) {
            classes.add(TransformRecord.class);
        }
    }

    @Test public void testValidAnnotations ()
        throws NoSuchFieldException
    {
        Field field = TransformRecord.class.getField("strings");
        assertTrue(FieldMarshaller.createMarshaller(field) != null);

        field = TransformRecord.class.getField("custom");
        assertTrue(FieldMarshaller.createMarshaller(field).getClass().
                   getName().endsWith("FieldMarshaller$TransformingMarshaller"));
    }

    @Test public void testInvalidFieldAnnotation ()
        throws NoSuchFieldException
    {
        try {
            Field field = BadTransformRecord.class.getField("thread");
            FieldMarshaller.createMarshaller(field);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().indexOf("@Transform error") != -1);
        }
    }

    @Test public void testInvalidTypeAnnotation ()
        throws NoSuchFieldException
    {
        try {
            Field field = BadTransformRecord.class.getField("invalid");
            FieldMarshaller.createMarshaller(field);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().indexOf("@Transform error") != -1);
        }
    }

    @Test public void testCreateReadDelete ()
    {
        TransformRecord in = new TransformRecord();
        in.recordId = 1;
        in.strings = new String[] { "one", "two", "three" };
        in.custom = new CustomType("custom");
        in.ordinal = Ordinal.THREE;
        _repo.insert(in);

        TransformRecord out = _repo.loadNoCache(in.recordId);
        assertNotNull(out != null); // we got a result
        assertTrue(in != out); // it didn't come from the cache

        // make sure all of the fields were marshalled and unmarshalled correctly
        assertEquals(in.recordId, out.recordId);
        assertArrayEquals(in.strings, out.strings);
        assertEquals(in.custom, out.custom);
        assertEquals(in.ordinal, out.ordinal);

        // finally clean up after ourselves
        _repo.delete(TransformRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    protected static <E extends Enum<E> & ShortEnum> E fromShort (Class<E> eclass, short code)
    {
        for (E value : eclass.getEnumConstants()) {
            if (value.toShort() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException(eclass + " has no value with code " + code);
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TransformRepository _repo = new TransformRepository(createPersistenceContext());
}
