//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import org.junit.Test;
import static org.junit.Assert.*;

public class TransactionTest extends TestBase
{
    @Test public void testSuccessfulCommit () {
        Transaction tx = _repo.ctx().startTx();
        TestRecord in = createTestRecord(1);
        _repo.insert(in);
        tx.commit();

        TestRecord out = _repo.loadNoCache(in.recordId);
        assertNotNull(out); // we got a result
        assertTrue(in != out); // it didn't come from the cache

        // make sure all of the fields were marshalled and unmarshalled correctly
        assertTestRecordEquals(in, out);

        // finally clean up after ourselves (because other tests share this database)
        _repo.delete(TestRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    @Test public void testRollback () {
        Transaction tx = _repo.ctx().startTx();
        TestRecord in1 = createTestRecord(1);
        _repo.insert(in1);
        TestRecord in2 = createTestRecord(2);
        _repo.insert(in2);
        tx.rollback();

        assertNull(_repo.loadNoCache(in1.recordId)); // we should get no result
        assertNull(_repo.loadNoCache(in2.recordId)); // we should get no result
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
