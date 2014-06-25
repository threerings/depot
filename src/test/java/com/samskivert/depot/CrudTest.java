//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests simple create/read/update/delete behaviors. 
 */
public class CrudTest extends TestBase
{
    @Test public void testCreateReadDelete ()
    {
        TestRecord in = createTestRecord(1);
        _repo.insert(in);

        TestRecord out = _repo.loadNoCache(in.recordId);
        assertNotNull(out != null); // we got a result
        assertTrue(in != out); // it didn't come from the cache

        // make sure all of the fields were marshalled and unmarshalled correctly
        assertTestRecordEquals(in, out);

        // finally clean up after ourselves
        _repo.delete(TestRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    @Test public void testUpdateDelete ()
    {
        TestRecord in = createTestRecord(1);
        _repo.insert(in);

        // first try updating using the whole-record update mechanism
        in.homeTown = "Funky Town";
        _repo.update(in);
        assertTestRecordEquals(in, _repo.loadNoCache(in.recordId));

        // then try update partial
        String name = "Bob";
        int age = 25;
        int[] numbers = { 1, 2, 3, 4, 5 };
        _repo.updatePartial(TestRecord.getKey(in.recordId), ImmutableMap.of(
                                TestRecord.NAME, name, TestRecord.AGE, age,
                                TestRecord.NUMBERS, numbers));
        TestRecord up = _repo.loadNoCache(in.recordId);
        assertEquals(name, up.name);
        assertEquals(age, up.age);
        assertTrue(Arrays.equals(numbers, up.numbers));

        // finally clean up after ourselves
        _repo.delete(TestRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
