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

import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.depot.Tuple2;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests individual field selection.
 */
public class SelectFieldsTest extends TestBase
{
    @Test public void testCreateReadDelete ()
    {
        for (int ii = 0; ii < 10; ii++) {
            _repo.insert(createTestRecord(ii));
        }
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.A, "Elvis"));
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.B, "Moses"));
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.C, "Abraham"));
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.D, "Elvis"));

        // test some basic one and two column selects
        List<Integer> allKeys = _repo.from(TestRecord.class).select(TestRecord.RECORD_ID);
        assertEquals(allKeys, Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> someKeys = _repo.from(TestRecord.class).where(
            TestRecord.RECORD_ID.greaterThan(5)).select(TestRecord.RECORD_ID);
        assertEquals(someKeys, Lists.newArrayList(6, 7, 8, 9));

        List<Tuple2<Integer,String>> data = _repo.from(TestRecord.class).where(
            TestRecord.RECORD_ID.greaterThan(7)).select(TestRecord.RECORD_ID, TestRecord.NAME);
        assertEquals(data, Lists.newArrayList(Tuple2.newTuple(8, "Elvis"),
                                              Tuple2.newTuple(9, "Elvis")));

        // test a basic join
        List<Tuple2<Integer,EnumKeyRecord.Type>> jdata = _repo.from(TestRecord.class).join(
            TestRecord.NAME, EnumKeyRecord.NAME).select(TestRecord.RECORD_ID, EnumKeyRecord.TYPE);
        System.out.println(jdata);

        // finally clean up after ourselves
        _repo.from(TestRecord.class).where(Exps.trueLiteral()).delete();
        _repo.from(EnumKeyRecord.class).where(Exps.trueLiteral()).delete();
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
