//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2009 Michael Bayne and Pär Winzell
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

import java.util.Collections;

import com.google.common.collect.Sets;
import com.samskivert.util.RandomUtil;

import org.junit.Test;
import static org.junit.Assert.*;

import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.SQLExpression;

/**
 * Tests queries.
 */
public class QueryTest extends TestBase
{
    @Computed(shadowOf=TestRecord.class)
    public static class TestNameRecord extends PersistentRecord
    {
        public int recordId;
        public String name;

        @Override public String toString () {
            return recordId + ":" + name;
        }
    }

    @Test public void testQueries ()
    {
        for (int ii = 1; ii <= CREATE_RECORDS; ii++) {
            TestRecord record = createTestRecord(ii);
            record.name = "Spam! " + ii;
            record.age = RandomUtil.getInt(100);
            record.homeTown = "Over there";
            _repo.insert(record);
        }

        // test the empty key set
        KeySet<TestRecord> none = KeySet.newKeySet(
            TestRecord.class, Collections.<Key<TestRecord>>emptySet());
        assertEquals(0, _repo.deleteAll(TestRecord.class, none));

        // test collection caching (TODO: check that the records are ==)
        SQLExpression<Boolean> where = TestRecord.RECORD_ID.greaterThan(CREATE_RECORDS-50);
        assertEquals(50, _repo.from(TestRecord.class).where(where).select().size());
        assertEquals(50, _repo.from(TestRecord.class).where(where).select().size());

        // test a partial key set
        KeySet<TestRecord> some = KeySet.newSimpleKeySet(
            TestRecord.class, Sets.newHashSet(1, 3, 5, 7, 9));
        assertEquals(5, _repo.loadAll(some.toCollection()).size());

        // make sure our computed records work
        for (TestNameRecord tnr : _repo.findAll(TestNameRecord.class)) {
            assertEquals("Spam! " + tnr.recordId, tnr.name);
        }

        assertEquals(CREATE_RECORDS, _repo.findAll(TestRecord.class).size());
        _repo.from(TestRecord.class).where(TestRecord.RECORD_ID.lessEq(CREATE_RECORDS/2)).delete();
        assertEquals(CREATE_RECORDS/2, _repo.findAll(TestRecord.class).size());

        _repo.from(TestRecord.class).whereTrue().delete();
        assertEquals(0, _repo.findAll(TestRecord.class).size());

//         // TODO: try to break our In() clause
//         Set<Integer> ids = Sets.newHashSet();
//         for (int ii = 1; ii <= In.MAX_KEYS*2+3; ii++) {
//             ids.add(ii);
//         }
//         _repo.deleteAll(TestRecord.class, KeySet.newSimpleKeySet(TestRecord.class, ids));
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();

    protected static final int CREATE_RECORDS = 150;
}
