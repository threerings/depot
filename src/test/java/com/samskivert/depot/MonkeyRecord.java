//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2009 Michael Bayne and Pär Winzell
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

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Id;

/**
 * Used for testing.
 */
public class MonkeyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MonkeyRecord> _R = MonkeyRecord.class;
    public static final ColumnExp SPECIES = colexp(_R, "species");
    public static final ColumnExp MONKEY_ID = colexp(_R, "monkeyId");
    public static final ColumnExp NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /** This monkey's species. This is part of our key so that we have a composite key. */
    @Id public int species;

    /** This monkey's unique identifier. */
    @Id public int monkeyId;

    public String name;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MonkeyRecord}
     * with the supplied key values.
     */
    public static Key<MonkeyRecord> getKey (int species, int monkeyId)
    {
        return newKey(_R, species, monkeyId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SPECIES, MONKEY_ID); }
    // AUTO-GENERATED: METHODS END
}
