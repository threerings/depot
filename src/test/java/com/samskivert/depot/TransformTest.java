//
// $Id: TransformTest.java 760 2010-12-09 00:08:58Z samskivert $
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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.ByteEnum;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.depot.annotation.Column;
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
            return (other instanceof CustomType) && value.equals(((CustomType)other).value);
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

    public enum ExtraOrdinal implements ByteEnum {
        ONE, TWO, THREE;

        public byte toByte () {
            return (byte)ordinal(); // sufficient for testing
        }
    }

    public static class CustomTypeTransformer extends Transformer<CustomType, String>
    {
        @Override public String toPersistent (CustomType value) {
            return value.value;
        }

        @Override public CustomType fromPersistent (String value) {
            return new CustomType(value);
        }
    }

    public static class ShortEnumTransformer extends Transformer<ShortEnum, Short>
    {
        @Override public void init (Type ftype, Transform annotation) {
            @SuppressWarnings("unchecked") Class<Dummy> eclass = (Class<Dummy>)ftype;
            _eclass = eclass;
        }
        @Override public Short toPersistent (ShortEnum value) {
            return value.toShort();
        }
        @Override public ShortEnum fromPersistent (Short value) {
            return fromShort(_eclass, value);
        }
        private enum Dummy implements ShortEnum {
            DUMMY;
            public short toShort () { return 0; }
        };

        protected Class<Dummy> _eclass;
    }

    public static class TransformRecord extends PersistentRecord
    {
        // AUTO-GENERATED: FIELDS START
        public static final Class<TransformRecord> _R = TransformRecord.class;
        public static final ColumnExp<Integer> RECORD_ID = colexp(_R, "recordId");
        public static final ColumnExp<String[]> STRINGS = colexp(_R, "strings");
        public static final ColumnExp<List<String>> STRING_LIST = colexp(_R, "stringList");
        public static final ColumnExp<Set<String>> STRING_SET = colexp(_R, "stringSet");
        public static final ColumnExp<CustomType> CUSTOM = colexp(_R, "custom");
        public static final ColumnExp<Ordinal> ORDINAL = colexp(_R, "ordinal");
        public static final ColumnExp<Set<ExtraOrdinal>> BOBS = colexp(_R, "bobs");
        // AUTO-GENERATED: FIELDS END

        public static final int SCHEMA_VERSION = 1;

        @Id public int recordId;

        @Column(nullable=true) @Transform(Transformers.StringArray.class)
        public String[] strings;

        @Column(nullable=true) @Transform(Transformers.StringIterable.class)
        public List<String> stringList;

        @Column(nullable=true) @Transform(Transformers.StringIterable.class)
        public Set<String> stringSet;

        public CustomType custom;

        public Ordinal ordinal;

        @Transform(Transformers.ByteEnumSet.class)
        public Set<ExtraOrdinal> bobs;

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
        @Transform(Transformers.StringArray.class)
        public Thread thread;

        public InvalidCustomType invalid;
    }

    @Test public void testValidAnnotations ()
        throws NoSuchFieldException
    {
        Field field = TransformRecord.class.getField("strings");
        assertTrue(FieldMarshaller.createMarshaller(field) != null);

        field = TransformRecord.class.getField("custom");
        assertTrue(FieldMarshaller.createMarshaller(field) != null);
    }

//    @Test public void testInvalidFieldAnnotation ()
//        throws NoSuchFieldException
//    {
//        try {
//            Field field = BadTransformRecord.class.getField("thread");
//            FieldMarshaller.createMarshaller(field);
//            fail();
//        } catch (IllegalArgumentException iae) {
//            assertTrue(iae.getMessage().indexOf("@Transform error") != -1);
//        }
//    }

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
        testCreateReadDelete(new String[] { "one", "two", "three" });
        testCreateReadDelete(new String[] { ",", null, "" });
        testCreateReadDelete(new String[] { "" });
        testCreateReadDelete(new String[] {});
        testCreateReadDelete(null);
        testCreateReadDelete(new String[] { "", "\n", "", "\n\n" });
    }

    @Test public void testProject ()
    {
        TransformRecord in = createAndInsert(new String[] { "one", "two", "three" });
        Set<ExtraOrdinal> bobs = _repo.from(TransformRecord.class)
            .where(TransformRecord.RECORD_ID.eq(in.recordId))
            .load(TransformRecord.BOBS);
        assertEquals(in.bobs, bobs);
    }

    protected TransformRecord createAndInsert (String[] strings)
    {
        TransformRecord in = new TransformRecord();
        in.recordId = 1;
        in.strings = strings;
        in.stringList = (strings == null) ? null : Lists.newArrayList(strings);
        in.stringSet = (strings == null) ? null : Sets.newHashSet(strings);
        in.custom = new CustomType("custom");
        in.ordinal = Ordinal.THREE;
        in.bobs = EnumSet.of(ExtraOrdinal.TWO);
        _repo.insert(in);
        return in;
    }

    protected void delete (TransformRecord in)
    {
        _repo.delete(TransformRecord.getKey(in.recordId));
    }

    protected void testCreateReadDelete (String[] strings)
    {
        TransformRecord in = createAndInsert(strings);

        TransformRecord out = _repo.loadNoCache(in.recordId);
        assertNotNull(out != null); // we got a result
        assertTrue(in != out); // it didn't come from the cache

        // make sure all of the fields were marshalled and unmarshalled correctly
        assertEquals(in.recordId, out.recordId);
        assertArrayEquals(in.strings, out.strings);
        assertTrue(Objects.equal(in.stringList, out.stringList));
        assertTrue(Objects.equal(in.stringSet, out.stringSet));
        assertEquals(in.custom, out.custom);
        assertEquals(in.ordinal, out.ordinal);
        assertEquals(in.bobs, out.bobs);

        // finally clean up after ourselves
        delete(in);
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

    protected static class TransformRepository extends DepotRepository
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

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TransformRepository _repo = new TransformRepository(createPersistenceContext());
}
