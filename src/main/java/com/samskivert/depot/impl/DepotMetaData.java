//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.jdbc.ColumnDefinition;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.HsqldbLiaison;
import com.samskivert.jdbc.MySQLLiaison;
import com.samskivert.jdbc.PostgreSQLLiaison;

import com.samskivert.depot.PersistenceContext;

/**
 * Does something extraordinary.
 */
public class DepotMetaData
{
    public void init (PersistenceContext ctx)
    {
        _ctx = ctx;
        _ctx.invoke(new SimpleModifier() {
            @Override
            protected int invoke (DatabaseLiaison liaison, Statement stmt) throws SQLException {
                // determine our JDBC major version
                _jdbcMajorVersion = stmt.getConnection().getMetaData().getJDBCMajorVersion();

                // create our schema version table if needed
                liaison.createTableIfMissing(
                    stmt.getConnection(), SCHEMA_VERSION_TABLE,
                    new String[] { P_COLUMN, V_COLUMN, MV_COLUMN },
                    new ColumnDefinition[] {
                        new ColumnDefinition("VARCHAR(255)", false, true, null),
                        new ColumnDefinition("INTEGER", false, false, null),
                        new ColumnDefinition("INTEGER", false, false, null)
                    },
                    null,
                    new String[] { P_COLUMN });

                // slurp in the current versions of all records
                readVersions(liaison, stmt);
                return 0;
            }
        });
    }

    /**
     * Returns the current version of the specified persistent class.
     *
     * @param forceUpdate if true the latest version will be read from the database rather than
     * returned from memory. Only force an update when you know that the value may have changed in
     * the database since Depot was initialized.
     *
     * @return the current version of the specified persistent class or -1 if the class has no
     * recorded version.
     */
    public int getVersion (String pClass, boolean forceUpdate)
    {
        if (forceUpdate) {
            _ctx.invoke(new SimpleModifier() {
                @Override
                protected int invoke (DatabaseLiaison liaison, Statement stmt) throws SQLException {
                    readVersions(liaison, stmt);
                    return 0;
                }
            });
        }
        Integer curvers = _curvers.get(pClass);
        return (curvers == null) ? -1 : curvers;
    }

    /**
     * Initializes the version of the specified persistent class to zero.
     */
    public void initializeVersion (final String pClass)
    {
        _ctx.invoke(new SimpleModifier() {
            @Override
            protected int invoke (DatabaseLiaison liaison, Statement stmt) throws SQLException {
                try {
                    return stmt.executeUpdate(
                        "insert into " + liaison.tableSQL(SCHEMA_VERSION_TABLE) +
                        " values('" + pClass + "', 0 , 0)");
                } catch (SQLException e) {
                    // someone else might be doing this at the exact same time which is OK,
                    // we'll coordinate with that other process in the next phase
                    if (liaison.isDuplicateRowException(e)) {
                        return 0;
                    } else {
                        throw e;
                    }
                }
            }
        });
    }

    /**
     * Updates the version of the specified persistent class to the specified version.
     */
    public void updateVersion (final String pClass, final int newVersion)
    {
        _ctx.invoke(new SimpleModifier() {
            @Override
            protected int invoke (DatabaseLiaison liaison, Statement stmt) throws SQLException {
                return stmt.executeUpdate(
                    "update " + liaison.tableSQL(SCHEMA_VERSION_TABLE) +
                    "   set " + liaison.columnSQL(V_COLUMN) + " = " + newVersion +
                    " where " + liaison.columnSQL(P_COLUMN) + " = '" + pClass + "'");
            }
        });
    }

    /**
     * Updates the current migrating version of the supplied persistent class.
     *
     * @return true if the migration lock was obtained and the migrating version was updated, false
     * if some other process acquired the lock.
     */
    public boolean updateMigratingVersion (
        final String pClass, final int newMigratingVersion, final int guardVersion)
    {
        return _ctx.invoke(new SimpleModifier() {
            @Override
            protected int invoke (DatabaseLiaison liaison, Statement stmt) throws SQLException {
                return stmt.executeUpdate(
                    "update " + liaison.tableSQL(SCHEMA_VERSION_TABLE) +
                    "   set " + liaison.columnSQL(MV_COLUMN) + " = " + newMigratingVersion +
                    " where " + liaison.columnSQL(P_COLUMN) + " = '" + pClass + "'" +
                    " and " + liaison.columnSQL(MV_COLUMN) + " = " + guardVersion);
            }
        }) > 0;
    }

    /**
     * Creates and return a new {@link SQLBuilder} for the appropriate dialect.
     *
     * TODO: At some point perhaps use a more elegant way of discerning our dialect.
     */
    public SQLBuilder getSQLBuilder (DepotTypes types, DatabaseLiaison liaison)
    {
        if (liaison instanceof PostgreSQLLiaison) {
            if (_jdbcMajorVersion >= 4) {
                return new PostgreSQL4Builder(types);
            } else {
                return new PostgreSQLBuilder(types);
            }
        }
        if (liaison instanceof MySQLLiaison) {
            return new MySQLBuilder(types);
        }
        if (liaison instanceof HsqldbLiaison) {
            return new HSQLBuilder(types);
        }
        throw new IllegalArgumentException("Unknown liaison type: " + liaison.getClass());
    }

    protected void readVersions (DatabaseLiaison liaison, Statement stmt)
        throws SQLException
    {
        ResultSet rs = stmt.executeQuery(
            " select " + liaison.columnSQL(P_COLUMN) + ", " + liaison.columnSQL(V_COLUMN) +
            "   from " + liaison.tableSQL(SCHEMA_VERSION_TABLE));
        while (rs.next()) {
            _curvers.put(rs.getString(1), rs.getInt(2));
        }
    }

    protected abstract class SimpleModifier extends Modifier {
        @Override
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            Statement stmt = conn.createStatement();
            try {
                return invoke(liaison, stmt);
            } finally {
                stmt.close();
            }
        }

        protected abstract int invoke (DatabaseLiaison liaison, Statement stmt) throws SQLException;
    }

    protected PersistenceContext _ctx;
    protected int _jdbcMajorVersion;
    protected Map<String, Integer> _curvers = Maps.newHashMap();

    /** The name of the table we use to track schema versions. */
    protected static final String SCHEMA_VERSION_TABLE = "DepotSchemaVersion";

    /** The name of the 'persistentClass' column in the {@link #SCHEMA_VERSION_TABLE}. */
    protected static final String P_COLUMN = "persistentClass";

    /** The name of the 'version' column in the {@link #SCHEMA_VERSION_TABLE}. */
    protected static final String V_COLUMN = "version";

    /** The name of the 'migratingVersion' column in the {@link #SCHEMA_VERSION_TABLE}. */
    protected static final String MV_COLUMN = "migratingVersion";
}
