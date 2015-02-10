//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.impl.DepotMetaData;
import com.samskivert.depot.impl.Modifier;
import com.samskivert.depot.impl.jdbc.DatabaseLiaison;

/**
 * Migration to drop a table.
 *
 * Note that this is a DataMigration and must have an identifier. Do not use this migration
 * to "clear" a table and repopulate it, as this migration happens outside of the normal
 * schema tracking. If you keep this migration in your code and then re-add the table in the
 * future, things may seem good for you, but a new developer will run all the migrations
 * and this will run after the schema migrations, causing them to lose the table.
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
        _ctx.getMetaData().clearVersion(_table);
    }

    /** Our persistence context. */
    protected final PersistenceContext _ctx;

    /** The table to drop, if it exists. */
    protected final String _table;
}
