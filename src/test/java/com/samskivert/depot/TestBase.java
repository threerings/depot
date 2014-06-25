//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Properties;

import com.google.common.collect.Lists;

import com.samskivert.jdbc.ConnectionProvider;
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
        return createPersistenceContext("test");
    }

    /**
     * Creates a persistence context configured to run against an (empty) in-memory HSQLDB.
     * <b>Note:</b> all in-memory HSQL databases with the same {@code dbname} are shared for the
     * duration of the VM, so tests have to clean up after themselves or use unique dbnames.
     */
    protected static PersistenceContext createPersistenceContext (String dbname)
    {
        return createPersistenceContext(dbname, null);
    }

    /**
     * Creates a persistence context configured to run against an (empty) in-memory HSQLDB.
     * <b>Note:</b> all in-memory HSQL databases with the same {@code dbname} are shared for the
     * duration of the VM, so tests have to clean up after themselves or use unique dbnames.
     *
     * @param initSQL initialization SQL used to populate the database before the persistence
     * context is initialized.
     */
    protected static PersistenceContext createPersistenceContext (String dbname, String[] initSQL)
    {
        Properties props = new Properties();
        props.put("default.driver", "org.hsqldb.jdbcDriver");
        props.put("default.url", "jdbc:hsqldb:mem:" + dbname);
        props.put("default.username", "sa");
        props.put("default.password", "");

        PersistenceContext perCtx = new PersistenceContext();
        ConnectionProvider conprov = new StaticConnectionProvider(props);
        if (initSQL != null) {
            try {
                Connection conn = conprov.getConnection(dbname, false);
                for (String sql : initSQL) {
                    conn.createStatement().executeUpdate(sql);
                }
                conprov.releaseConnection(dbname, false, conn);
            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        }
        perCtx.init(dbname, conprov, new TestCacheAdapter());
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
        rec.strList = Lists.newArrayList("foo", "bar", "Hello", "World");
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
        assertEquals(expect.strList, got.strList);
    }
}
