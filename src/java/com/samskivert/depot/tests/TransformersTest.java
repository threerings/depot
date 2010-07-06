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

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.Sets;

import com.samskivert.util.ByteEnum;

import com.samskivert.depot.Transformers;

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
        assertArrayEquals(data, xform.fromPersistent(null, xform.toPersistent(data)));
    }

    @Test public void testEmptyStringArrays ()
    {
        Transformers.StringArray xform = new Transformers.StringArray();
        Set<String> set = Sets.newHashSet();
        // all three of these should obviously encode to different Strings
        set.add(xform.toPersistent(null));
        set.add(xform.toPersistent(new String[] {}));
        set.add(xform.toPersistent(new String[] {""}));
        assertTrue(set.size() == 3);
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
        assertTrue(set.size() == 5);

        set.clear();
        for (Set<LilEnum> subset : Sets.powerSet(EnumSet.allOf(LilEnum.class))) {
            set.add(xform.toPersistent(subset));
        }
        assertTrue(set.size() == 8);

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
