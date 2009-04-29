//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
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

package com.samskivert.depot.impl;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maintains a record of all successfully invoked data migrations.
 */
public class DepotMigrationHistoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DepotMigrationHistoryRecord> _R = DepotMigrationHistoryRecord.class;
    public static final ColumnExp IDENT = colexp(_R, "ident");
    public static final ColumnExp WHEN_COMPLETED = colexp(_R, "whenCompleted");
    // AUTO-GENERATED: FIELDS END

    /** Our schema version. Probably not likely to change. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique identifier for this migration. */
    @Id public String ident;

    /** The time at which the migration was completed. */
    @Column(nullable=true)
    public Timestamp whenCompleted;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DepotMigrationHistoryRecord}
     * with the supplied key values.
     */
    public static Key<DepotMigrationHistoryRecord> getKey (String ident)
    {
        return new Key<DepotMigrationHistoryRecord>(
                DepotMigrationHistoryRecord.class,
                new ColumnExp[] { IDENT },
                new Comparable[] { ident });
    }
    // AUTO-GENERATED: METHODS END
}
