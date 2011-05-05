//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.util.EnumSet;

import org.junit.Test;
import static org.junit.Assert.*;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.impl.DepotUtil;

/**
 * Tests some super basic {@link Key} stuff.
 */
public class KeyTest extends TestBase
{
    @Test public void testSlowConstructor ()
    {
        int species = 10, monkeyId = 15;
        Key<MonkeyRecord> key = MonkeyRecord.getKey(species, monkeyId);

        // make sure that the arguments we passed in got assigned in the right positions
        ColumnExp<?>[] kfs = DepotUtil.getKeyFields(MonkeyRecord.class);
        int kspecies = 0, kmonkeyId = 0;
        for (int ii = 0; ii < kfs.length; ii++) {
            if (MonkeyRecord.SPECIES.equals(kfs[ii])) {
                kspecies = (Integer)(key.getValues()[ii]);
            } else if (MonkeyRecord.MONKEY_ID.equals(kfs[ii])) {
                kmonkeyId = (Integer)(key.getValues()[ii]);
            }
        }
        assertEquals(species, kspecies);
        assertEquals(monkeyId, kmonkeyId);
    }

    @Test public void testFastConstructor ()
    {
        int recordId = 10;
        Key<TestRecord> key = TestRecord.getKey(recordId);

        // make sure that the arguments we passed in got assigned in the right positions
        ColumnExp<?>[] kfs = DepotUtil.getKeyFields(TestRecord.class);
        int krecordId = 0;
        for (int ii = 0; ii < kfs.length; ii++) {
            if (TestRecord.RECORD_ID.equals(kfs[ii])) {
                krecordId = (Integer)(key.getValues()[ii]);
            }
        }
        assertEquals(recordId, krecordId);
    }

    @Test public void testEnumKey ()
    {
        EnumKeyRecord a = new EnumKeyRecord(EnumKeyRecord.Type.A, "ayyy");
        EnumKeyRecord b = new EnumKeyRecord(EnumKeyRecord.Type.B, "beee");
        EnumKeyRecord c = new EnumKeyRecord(EnumKeyRecord.Type.C, "ceee");
        EnumKeyRecord d = new EnumKeyRecord(EnumKeyRecord.Type.D, "deee");
        _repo.storeEnum(a);
        _repo.storeEnum(b);
        _repo.storeEnum(c);
        _repo.storeEnum(d);
        assertEquals(4, _repo.loadEnums(EnumSet.allOf(EnumKeyRecord.Type.class)).size());

        assertEquals("beee", _repo.loadEnum(EnumKeyRecord.Type.B).name);

        _repo.from(EnumKeyRecord.class).whereTrue().delete();
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
