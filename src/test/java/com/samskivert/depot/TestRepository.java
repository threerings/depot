//
// $Id$
//
// samskivert library - useful routines for java programs
// Copyright (C) 2006-2008 Michael Bayne, Pär Winzell
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

import java.util.List;
import java.util.Set;

/**
 * A test tool for the Depot repository services.
 */
public class TestRepository extends FluentRepository
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
        return findAll(EnumKeyRecord.class, where(EnumKeyRecord.TYPE.in(types)));
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
