//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Sets;

import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.util.Builder2;
import com.samskivert.util.RandomUtil;

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
            record.awesomeness = RandomUtil.getFloat(1.0F);
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

        Builder2<Float, Integer, Float> ageAwesome = new Builder2<Float, Integer, Float>() {
            public Float build (Integer a, Float b) {
                return a * b;
            }
        };
        List<Float> results =
            _repo.from(TestRecord.class).select(ageAwesome, TestRecord.AGE, TestRecord.AWESOMENESS);
        assertEquals(CREATE_RECORDS, results.size());
        for (float result : results) {
            assertTrue("Age goes from [0,99) and awesomeness goes from [0.0,1.0), so their " +
            		"product should be [0.0,100), not " + result, result >= 0 && result < 100);
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
