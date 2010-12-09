//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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
