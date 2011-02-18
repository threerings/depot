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

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.util.RandomUtil;

import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.impl.Modifier;

/**
 * Tests various migrations.
 */
public class MigrationTest extends TestBase
{
    public static class PKMigrationRecord extends PersistentRecord
    {
        public static final int SCHEMA_VERSION = 2;
        @Id public int id;
        /* @Id removed */ public String stringId;
    }

    @Test
    public void testPKMigration ()
    {
        PersistenceContext pctx = createPersistenceContext("pkmig", PK_DUMP);
        executeSQL(pctx, PK_INIT); // now that the schema is created, we can populate
        DepotRepository repo = createRepository(pctx, PKMigrationRecord.class);
        // trigger the execution of the migrations
        pctx.initializeRepositories(true);
    }

    // @Test
    public void generateDatabaseDump ()
    {
        PersistenceContext pctx = createPersistenceContext();
        DepotRepository repo = createRepository(pctx, PKMigrationRecord.class);

        for (int ii = 1; ii <= 5; ii++) {
            PKMigrationRecord record = new PKMigrationRecord();
            record.id = ii;
            record.stringId = ""+ii;
            repo.insert(record);
        }

        executeSQL(pctx, "script 'dump.sql'");
    }

    protected void executeSQL (PersistenceContext ctx, final String sql)
    {
        ctx.invoke(new Modifier.Simple() {
            protected String createQuery (DatabaseLiaison liaison) {
                return sql;
            }
        });
    }

    protected DepotRepository createRepository (
        PersistenceContext pctx, final Class<? extends PersistentRecord> pclass)
    {
        return new DepotRepository(pctx) {
            @Override
            protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes) {
                classes.add(pclass);
            }
        };
    }

    protected static final String[] PK_DUMP = {
        "CREATE MEMORY TABLE PUBLIC.\"DepotSchemaVersion\"(" +
        "\"persistentClass\" VARCHAR(255) NOT NULL PRIMARY KEY," +
        "\"version\" INTEGER NOT NULL," +
        "\"migratingVersion\" INTEGER NOT NULL," +
        "UNIQUE(\"persistentClass\"))",

        "CREATE MEMORY TABLE PUBLIC.\"MigrationTest$PKMigrationRecord\"(" +
        "\"id\" INTEGER DEFAULT 0 NOT NULL," +
        "\"stringId\" VARCHAR(255) NOT NULL," +
        "PRIMARY KEY(\"id\",\"stringId\"))",

        "INSERT INTO \"DepotSchemaVersion\" VALUES('MigrationTest$PKMigrationRecord',1,0)"
    };

    protected static final String PK_INIT =
        "INSERT INTO \"MigrationTest$PKMigrationRecord\" VALUES(1,'1')\n" +
        "INSERT INTO \"MigrationTest$PKMigrationRecord\" VALUES(2,'2')\n" +
        "INSERT INTO \"MigrationTest$PKMigrationRecord\" VALUES(3,'3')\n" +
        "INSERT INTO \"MigrationTest$PKMigrationRecord\" VALUES(4,'4')\n" +
        "INSERT INTO \"MigrationTest$PKMigrationRecord\" VALUES(5,'5')\n";
}
