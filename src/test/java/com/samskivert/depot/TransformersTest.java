//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.Sets;

import com.samskivert.depot.util.ByteEnum;

/**
 * Tests the stock transformers.
 */
public class TransformersTest
{
    @Test public void testStringArray ()
    {
        String[] data = { "something", "", null, "\nprenewline", "postnewline\n", "in\nnewline",
            "a slash \\", "\\ two slashes \\", "\n\n\n" };
        Transformers.StringArray xform = new Transformers.StringArray();
        assertArrayEquals(data, xform.fromPersistent(xform.toPersistent(data)));
    }

    @Test public void testEmptyStringArrays ()
    {
        Transformers.StringArray xform = new Transformers.StringArray();
        Set<String> set = Sets.newHashSet();
        // all three of these should obviously encode to different Strings
        set.add(xform.toPersistent(null));
        set.add(xform.toPersistent(new String[] {}));
        set.add(xform.toPersistent(new String[] {""}));
        assertEquals(3, set.size());
    }

    @Test public void testByteEnumSets ()
    {
        Transformers.ByteEnumSet<LilEnum> xform = new Transformers.ByteEnumSet<LilEnum>();
        Set<Integer> set = Sets.newHashSet();
        set.add(xform.toPersistent(null));
        set.add(xform.toPersistent(EnumSet.noneOf(LilEnum.class)));
        set.add(xform.toPersistent(EnumSet.of(LilEnum.ONE)));
        set.add(xform.toPersistent(EnumSet.of(LilEnum.TWO, LilEnum.THREE)));
        set.add(xform.toPersistent(EnumSet.allOf(LilEnum.class)));
        assertEquals(5, set.size());

        set.clear();
        for (Set<LilEnum> subset : Sets.powerSet(EnumSet.allOf(LilEnum.class))) {
            set.add(xform.toPersistent(subset));
        }
        assertEquals(8, set.size());

    }

    private static enum LilEnum
        implements ByteEnum
    {
        ONE,
        TWO,
        THREE;

        // from ByteEnum
        public byte toByte ()
        {
            return (byte)ordinal(); // we're just testing here, chill out
        }
    }
}
