//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.util.List;
import java.util.Set;

/**
 * A test tool for the Depot repository services.
 */
public class TestRepository extends DepotRepository
{
    public TestRecord loadNoCache (int recordId)
    {
        return load(TestRecord.getKey(recordId), CacheStrategy.NONE);
    }

    public TestRecord loadWithCache (int recordId)
    {
        return load(TestRecord.getKey(recordId));
    }

    public EnumKeyRecord loadEnum (EnumKeyRecord.Type type)
    {
        return load(EnumKeyRecord.getKey(type));
    }

    public List<EnumKeyRecord> loadEnums (Set<EnumKeyRecord.Type> types)
    {
        return from(EnumKeyRecord.class).where(EnumKeyRecord.TYPE.in(types)).select();
    }

    public void storeEnum (EnumKeyRecord record)
    {
        store(record);
    }

    public TestRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TestRecord.class);
        classes.add(EnumKeyRecord.class);
    }
}
