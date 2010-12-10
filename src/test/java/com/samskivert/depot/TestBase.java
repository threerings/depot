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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Properties;

import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.Calendars;

import static org.junit.Assert.*;

/**
 * Contains shared code to set up in-memory databases for unit testing.
 */
public abstract class TestBase
{
    /**
     * Creates a persistence context configured to run against an (empty) in-memory HSQLDB.
     * <b>Note:</b> the in-memory HSQL database is shared for the duration of the VM, so tests have
     * to clean up after themselves to avoid conflicting with one another.
     */
    protected static PersistenceContext createPersistenceContext ()
    {
        Properties props = new Properties();
        props.put("default.driver", "org.hsqldb.jdbcDriver");
        props.put("default.url", "jdbc:hsqldb:mem:test");
        props.put("default.username", "sa");
        props.put("default.password", "");

        PersistenceContext perCtx = new PersistenceContext();
        perCtx.init("test", new StaticConnectionProvider(props), new TestCacheAdapter());
        return perCtx;
    }

    /**
     * If a test doesn't need any special repository setup, it can use this method to create a
     * fresh persistence context and test repository in one fell swoop.
     */
    protected TestRepository createTestRepository ()
    {
        return new TestRepository(createPersistenceContext());
    }

    /**
     * Creates a test record with all fields initialized to valid values.
     */
    protected TestRecord createTestRecord (int recordId)
    {
        Date now = Calendars.now().zeroTime().toSQLDate();
        Timestamp tnow = new Timestamp(System.currentTimeMillis());
        TestRecord rec = new TestRecord();
        rec.recordId = recordId;
        rec.name = "Elvis";
        rec.age = 99;
        rec.awesomeness = 0.75f;
        rec.created = now;
        rec.homeTown = "Right here";
        rec.type = EnumKeyRecord.Type.A;
        rec.lastModified = tnow;
        rec.numbers = new int[] { 9, 0, 2, 1, 0 };
        return rec;
    }

    /**
     * Confirms that the supplied test records are field-by-field equal.
     */
    protected void assertTestRecordEquals (TestRecord expect, TestRecord got)
    {
        assertEquals(expect.recordId, got.recordId);
        assertEquals(expect.name, got.name);
        assertEquals(expect.age, got.age);
        assertEquals(expect.awesomeness, got.awesomeness, 0);
        assertEquals(expect.created, got.created);
        assertEquals(expect.homeTown, got.homeTown);
        assertEquals(expect.type, got.type);
        assertEquals(expect.lastModified, got.lastModified);
        assertTrue(Arrays.equals(expect.numbers, got.numbers));
    }
}
