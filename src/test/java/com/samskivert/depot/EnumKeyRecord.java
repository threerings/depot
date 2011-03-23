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

import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tests a record that uses an enum as its key.
 */
public class EnumKeyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EnumKeyRecord> _R = EnumKeyRecord.class;
    public static final ColumnExp<EnumKeyRecord.Type> TYPE = colexp(_R, "type");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    public enum Type { A, B, C, D };

    /** The type is key. */
    @Id public Type type;

    public String name;

    public EnumKeyRecord () {}

    public EnumKeyRecord (Type type, String name)
    {
        this.type = type;
        this.name = name;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EnumKeyRecord}
     * with the supplied key values.
     */
    public static Key<EnumKeyRecord> getKey (EnumKeyRecord.Type type)
    {
        return newKey(_R, type);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TYPE); }
    // AUTO-GENERATED: METHODS END
}
