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
import com.samskivert.depot.util.*; // TupleN

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests projection selections, aggregates, etc.
 */
public class ProjectionTest extends TestBase
{
    @Before public void createRecords ()
    {
        for (int ii = 0; ii < 10; ii++) {
            _repo.insert(createTestRecord(ii));
        }
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.A, "Elvis"));
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.B, "Moses"));
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.C, "Abraham"));
        _repo.insert(new EnumKeyRecord(EnumKeyRecord.Type.D, "Elvis"));
    }

    @After public void cleanup ()
    {
        _repo.from(TestRecord.class).whereTrue().delete();
        _repo.from(EnumKeyRecord.class).whereTrue().delete();
    }

    @Test public void testProjection ()
    {
        // test some basic one and two column selects
        List<Integer> allKeys = _repo.from(TestRecord.class).select(TestRecord.RECORD_ID);
        assertEquals(allKeys, Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> someKeys = _repo.from(TestRecord.class).where(
            TestRecord.RECORD_ID.greaterThan(5)).select(TestRecord.RECORD_ID);
        assertEquals(someKeys, Lists.newArrayList(6, 7, 8, 9));

        List<Tuple2<Integer,String>> data = _repo.from(TestRecord.class).where(
            TestRecord.RECORD_ID.greaterThan(7)).select(TestRecord.RECORD_ID, TestRecord.NAME);
        List<Tuple2<Integer,String>> want = Lists.newArrayList();
        want.add(Tuple2.create(8, "Elvis"));
        want.add(Tuple2.create(9, "Elvis"));
        assertEquals(data, want);
    }

    @Test public void testTupleN ()
    {
        assertEquals(Tuple2.create(1, "Elvis"),
                     _repo.from(TestRecord.class).where(TestRecord.RECORD_ID.eq(1)).
                     load(TestRecord.RECORD_ID, TestRecord.NAME));
        assertEquals(Tuple3.create(1, "Elvis", 99),
                     _repo.from(TestRecord.class).where(TestRecord.RECORD_ID.eq(1)).
                     load(TestRecord.RECORD_ID, TestRecord.NAME, TestRecord.AGE));
        assertEquals(Tuple4.create(1, "Elvis", 99, "Right here"),
                     _repo.from(TestRecord.class).where(TestRecord.RECORD_ID.eq(1)).
                     load(TestRecord.RECORD_ID, TestRecord.NAME, TestRecord.AGE,
                          TestRecord.HOME_TOWN));
        assertEquals(Tuple5.create(1, "Elvis", 99, "Right here", EnumKeyRecord.Type.A),
                     _repo.from(TestRecord.class).where(TestRecord.RECORD_ID.eq(1)).
                     load(TestRecord.RECORD_ID, TestRecord.NAME, TestRecord.AGE,
                          TestRecord.HOME_TOWN, TestRecord.TYPE));
    }

    @Test public void testProjectedJoin ()
    {
        // test a basic join
        List<Tuple2<Integer,EnumKeyRecord.Type>> jdata = _repo.from(TestRecord.class).join(
            TestRecord.NAME, EnumKeyRecord.NAME).select(TestRecord.RECORD_ID, EnumKeyRecord.TYPE);
        assertEquals(20, jdata.size());
    }

    @Test public void testNoMatches ()
    {
        QueryBuilder<TestRecord> empty =
            _repo.from(TestRecord.class).where(TestRecord.AGE.greaterThan(100));

        // test a projection from a query that returns no matches
        assertNull(empty.load(TestRecord.RECORD_ID));
        assertEquals(0, empty.select().size());

        // test an aggregate on a query that matches no rows
        assertNull(empty.load(Funcs.max(TestRecord.RECORD_ID)));
    }

    @Test public void testAggregates ()
    {
        // test computed expressions on the RHS (the casts are just to cope with JUnit's overloads)
        assertEquals(9, (int)_repo.from(TestRecord.class).load(Funcs.max(TestRecord.RECORD_ID)));
        assertEquals(10, (int)_repo.from(TestRecord.class).load(Funcs.count(TestRecord.RECORD_ID)));
        assertEquals(0, (int)_repo.from(TestRecord.class).load(Funcs.min(TestRecord.RECORD_ID)));
        assertEquals(Tuple2.create(9, 0), _repo.from(TestRecord.class).load(
                         Funcs.max(TestRecord.RECORD_ID), Funcs.min(TestRecord.RECORD_ID)));

        List<Tuple2<String, Integer>> ewant = Lists.newArrayList();
        ewant.add(Tuple2.create("Abraham", 1));
        ewant.add(Tuple2.create("Elvis", 2));
        ewant.add(Tuple2.create("Moses", 1));
        assertEquals(ewant, _repo.from(EnumKeyRecord.class).
                     groupBy(EnumKeyRecord.NAME).ascending(EnumKeyRecord.NAME).
                     select(EnumKeyRecord.NAME, Funcs.count(EnumKeyRecord.TYPE)));
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
