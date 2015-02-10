//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistenceContext;

import com.samskivert.jdbc.DatabaseLiaison;

/**
 * Migration to drop a table.
 */
public class DropTableMigration extends DataMigration
{
    /**
     * Creates a migration that will drop the given table if it exists.
     */
    public DropTableMigration (PersistenceContext ctx, String ident, String table)
    {
        super(ident);
        _ctx = ctx;
        _table = table;
    }

    @Override
    public void invoke ()
        throws DatabaseException
    {
        // delete the table
        _ctx.invoke(new Modifier() {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException
                {
                    return liaison.dropTable(conn, _table) ? 1 : 0;
                }
            });
        // delete the schema version
        _ctx.invoke(new Modifier.Simple() {
                @Override protected String createQuery (DatabaseLiaison liaison)
                {
                    return "delete from " +
                            liaison.tableSQL(DepotMetaData.SCHEMA_VERSION_TABLE) +
                            " where " +
                            liaison.columnSQL(DepotMetaData.P_COLUMN) +
                            " = '" + _table + "'";
                }
            });
    }

    /** Our persistence context. */
    protected final PersistenceContext _ctx;

    /** The table to drop, if it exists. */
    protected final String _table;
}
