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

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * A test persistent object.
 */
@Entity
public class TestRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TestRecord> _R = TestRecord.class;
    public static final ColumnExp RECORD_ID = colexp(_R, "recordId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp AGE = colexp(_R, "age");
    public static final ColumnExp HOME_TOWN = colexp(_R, "homeTown");
    public static final ColumnExp CREATED = colexp(_R, "created");
    public static final ColumnExp LAST_MODIFIED = colexp(_R, "lastModified");
    public static final ColumnExp NUMBERS = colexp(_R, "numbers");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    @Id
    public int recordId;

    public String name;

    public int age;

    public String homeTown;

    @Index
    public Date created;

    public Timestamp lastModified;

    public int[] numbers;

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TestRecord}
     * with the supplied key values.
     */
    public static Key<TestRecord> getKey (int recordId)
    {
        return newKey(_R, recordId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORD_ID); }
    // AUTO-GENERATED: METHODS END
}
