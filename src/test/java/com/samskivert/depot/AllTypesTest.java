//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the handling of our various supported field types.
 */
public class AllTypesTest extends TestBase
{
    @Test public void testCreateReadDelete ()
    {
        AllTypesRecord in = AllTypesRecord.createRecord(1);
        _repo.insert(in);

        AllTypesRecord out = _repo.loadNoCache(in.recordId);
        assertNotNull(out != null); // we got a result
        assertTrue(in != out); // it didn't come from the cache

        // make sure all of the fields were marshalled and unmarshalled correctly
        assertEquals(in, out);

        // finally clean up after ourselves
        _repo.delete(AllTypesRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    protected static class TestRepository extends DepotRepository
    {
        public AllTypesRecord loadNoCache (int recordId)
        {
            return load(AllTypesRecord.getKey(recordId), CacheStrategy.NONE);
        }

        public TestRepository (PersistenceContext perCtx)
        {
            super(perCtx);
        }

        @Override // from DepotRepository
        protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
        {
            classes.add(AllTypesRecord.class);
        }
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = new TestRepository(createPersistenceContext());
}
