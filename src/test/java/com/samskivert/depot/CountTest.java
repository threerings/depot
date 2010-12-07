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

import org.junit.Test;
import static org.junit.Assert.*;

import com.samskivert.depot.clause.Where;

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

        _repo.deleteAll(TestRecord.class, new Where(Exps.literal("true")));

        assertEquals(0, _repo.from(TestRecord.class).
                     where(TestRecord.RECORD_ID.lessThan(50)).selectCount());
        assertEquals(0, _repo.from(TestRecord.class).selectCount());
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
