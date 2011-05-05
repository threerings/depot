//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests row counting.
 */
public class CountTest extends TestBase
{
    @Test
    public void testCount ()
    {
        for (int ii = 1; ii < 100; ii++) {
            _repo.insert(createTestRecord(ii));
        }

        assertEquals(99, _repo.from(TestRecord.class).selectCount());
        assertEquals(49, _repo.from(TestRecord.class).
                     where(TestRecord.RECORD_ID.lessThan(50)).selectCount());

        _repo.from(TestRecord.class).whereTrue().delete();

        assertEquals(0, _repo.from(TestRecord.class).
                     where(TestRecord.RECORD_ID.lessThan(50)).selectCount());
        assertEquals(0, _repo.from(TestRecord.class).selectCount());
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
