//
// $Id$
//
// samskivert library - useful routines for java programs
// Copyright (C) 2006-2008 Michael Bayne, Pär Winzell
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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.RandomUtil;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.operator.GreaterThan;
import com.samskivert.depot.operator.LessThan;

/**
 * A test tool for the Depot repository services.
 */
public class TestRepository extends DepotRepository
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

    public static void main (String[] args)
        throws Exception
    {
        PersistenceContext perCtx = new PersistenceContext();
        perCtx.init("test", new StaticConnectionProvider("depot.properties"),
                    new TestCacheAdapter());

        // tests a bogus rename migration
        // perCtx.registerMigration(TestRecord.class, new SchemaMigration.Rename(1, "foo", "bar"));

        // tests a custom add column migration
        perCtx.registerMigration(TestRecord.class,
                                 new SchemaMigration.Add(2, TestRecord.HOME_TOWN, "'Anytown USA'"));

        TestRepository repo = new TestRepository(perCtx);

        System.out.println("Deleting old record.");
        repo.delete(TestRecord.getKey(1));

        Date now = new Date(System.currentTimeMillis());
        Timestamp tnow = new Timestamp(System.currentTimeMillis());

        TestRecord record = new TestRecord();
        record.recordId = 1;
        record.name = "Elvis";
        record.age = 99;
        record.created = now;
        record.homeTown = "Right here";
        record.lastModified = tnow;
        record.numbers = new int[] { 9, 0, 2, 1, 0 };

        repo.insert(record);
        System.out.println("Record: " + repo.load(TestRecord.getKey(record.recordId)));

//         record.age = 25;
//         record.name = "Bob";
//         record.numbers = new int[] { 1, 2, 3, 4, 5 };
//         repo.update(record, TestRecord.AGE, TestRecord.NAME, TestRecord.NUMBERS);

        repo.updatePartial(TestRecord.class, record.recordId,
                           TestRecord.AGE, 25, TestRecord.NAME, "Bob",
                           TestRecord.NUMBERS, new int[] { 1, 2, 3, 4, 5 });
        System.out.println("Updated " + repo.load(TestRecord.getKey(record.recordId)));

        for (int ii = 2; ii < CREATE_RECORDS; ii++) {
            record = new TestRecord();
            record.recordId = ii;
            record.name = "Spam!";
            record.age = RandomUtil.getInt(150);
            record.homeTown = "Over there";
            record.numbers = new int[] { 5, 4, 3, 2, 1 };
            record.created = now;
            record.lastModified = tnow;
            repo.insert(record);
        }

        // test the empty ky set
        KeySet<TestRecord> none = KeySet.newKeySet(
            TestRecord.class, Collections.<Key<TestRecord>>emptySet());
        System.out.println("Load none " + repo.loadAll(none.toCollection()) + ".");
        System.out.println("Delete none " + repo.deleteAll(TestRecord.class, none) + ".");

        // test collection caching
        Where where = new Where(new GreaterThan(TestRecord.RECORD_ID, 100));
        System.out.println("100 and up: " + repo.findAll(TestRecord.class, where).size());
        System.out.println("100 and up again: " + repo.findAll(TestRecord.class, where).size());

        // test a partial key set
        KeySet<TestRecord> some = KeySet.newSimpleKeySet(
            TestRecord.class, Sets.newHashSet(1, 3, 5, 7, 9));
        System.out.println("Load some " + repo.loadAll(some.toCollection()) + ".");

        System.out.println("Names " + repo.findAll(TestNameRecord.class) + ".");
        System.out.println("Have " + repo.findAll(TestRecord.class).size() + " records.");
        repo.deleteAll(TestRecord.class, new Where(new LessThan(
                                                       TestRecord.RECORD_ID, CREATE_RECORDS/2)));
        System.out.println("Now have " + repo.findAll(TestRecord.class).size() + " records.");
        repo.deleteAll(TestRecord.class, new Where(new LiteralExp("true")));
//         // TODO: try to break our In() clause
//         Set<Integer> ids = Sets.newHashSet();
//         for (int ii = 1; ii <= In.MAX_KEYS*2+3; ii++) {
//             ids.add(ii);
//         }
//         repo.deleteAll(TestRecord.class, KeySet.newSimpleKeySet(TestRecord.class, ids));
        System.out.println("Now have " + repo.findAll(TestRecord.class).size() + " records.");
    }

    public TestRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TestRecord.class);
    }

    protected static final int CREATE_RECORDS = 150;
}
