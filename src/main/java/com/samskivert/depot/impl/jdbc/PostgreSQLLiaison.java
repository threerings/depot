//
// samskivert library - useful routines for java programs
// Copyright (C) 2001-2012 Michael Bayne, et al.
// http://github.com/samskivert/samskivert/blob/master/COPYING

package com.samskivert.depot.impl.jdbc;

import java.sql.*;

/**
 * A database liaison for the MySQL database.
 */
public class PostgreSQLLiaison extends BaseLiaison
{
    // from DatabaseLiaison
    public boolean matchesURL (String url)
    {
        return url.startsWith("jdbc:postgresql");
    }

    // from DatabaseLiaison
    public boolean isDuplicateRowException (SQLException sqe)
    {
        // PostgreSQL duplicate key error code is 23505
        String state = sqe.getSQLState(), msg = sqe.getMessage();
        return ("23505".equals(state)) || (msg != null && msg.indexOf("duplicate key") != -1);
    }

    // from DatabaseLiaison
    public boolean isTransientException (SQLException sqe)
    {
        if (isTransientState(sqe.getSQLState())) {
            return true;
        }
        // also check the causal chain for connection exceptions
        Throwable cause = sqe.getCause();
        if (cause instanceof SQLException && isTransientState(((SQLException)cause).getSQLState())) {
            return true;
        }

        // fall back to message matching for older drivers or unexpected exception formats
        String msg = sqe.getMessage();
        return (msg != null &&
                (msg.contains("An I/O error") ||
                 msg.contains("Connection reset") ||
                 msg.contains("Connection refused") ||
                 msg.contains("Connection timed out") ||
                 msg.contains("Broken pipe") ||
                 msg.contains("unexpected EOF on client connection") ||
                 msg.contains("terminating connection due to administrator command") ||
                 msg.contains("This connection has been closed") ||
                 msg.contains("No more data to read from socket")));
    }

    private static boolean isTransientState (String sqlState) {
        if (sqlState == null) return false;
        // SQL state class "08" means connection exception (08000 connection_exception,
        // 08001 sqlclient_unable_to_establish_sqlconnection, 08003 connection_does_not_exist,
        // 08006 connection_failure, 08P01 protocol_violation)
        if (sqlState.startsWith("08")) return true;
        // Class "57P" covers operator intervention: 57P01 admin_shutdown,
        // 57P02 crash_shutdown, 57P03 cannot_connect_now
        if (sqlState.startsWith("57P")) return true;
        // Class "53" covers insufficient resources: 53000 insufficient_resources,
        // 53100 disk_full, 53200 out_of_memory, 53300 too_many_connections
        if (sqlState.startsWith("53")) return true;
        // 40001 serialization_failure, 40P01 deadlock_detected
        if ("40001".equals(sqlState) || "40P01".equals(sqlState)) return true;
        return false;
    }

    @Override
    protected long fetchLastInsertedId (Connection conn, String table, String column)
        throws SQLException
    {
        // PostgreSQL's support for auto-generated ID's comes in the form of appropriately named
        // sequences and DEFAULT nextval(sequence) modifiers in the ID columns. To get the next ID,
        // we use the currval() method which is set in a database sessions when any given sequence
        // is incremented.
        Statement stmt = conn.createStatement();
        try {
            ResultSet rs = stmt.executeQuery(
                "select currval('\"" + table + "_" + column + "_seq\"')");
            return rs.next() ? rs.getLong(1) : super.fetchLastInsertedId(conn, table, column);
        } finally {
            JDBCUtil.close(stmt);
        }
    }

    // from DatabaseLiaison
    public void createGenerator (Connection conn, String tableName, String columnName, long initValue)
        throws SQLException
    {
        if (initValue == 1) {
            return; // that's the default! yay, do nothing
        }

        String seqname = "\"" + tableName + "_" + columnName + "_seq\"";
        Statement stmt = conn.createStatement();
        try {
            stmt.executeQuery("select setval('" + seqname + "', " + initValue + ", false)");
        } finally {
            JDBCUtil.close(stmt);
        }
        log("Initial value of " + seqname  + " set to " + initValue + ".");
    }

    // from DatabaseLiaison
    public void deleteGenerator (Connection conn, String table, String column)
        throws SQLException
    {
        executeQuery(conn, "drop sequence if exists \"" + table + "_" + column + "_seq\"");
    }

    @Override // from DatabaseLiaison
    public boolean changeColumn (Connection conn, String table, String column, String type,
                                 Boolean nullable, Boolean unique, String defaultValue)
        throws SQLException
    {
        // We can create a column as "BIGSERIAL" but when changing we need to say "BIGINT".
        // TODO: does this handle all cases?
        if ("BIGSERIAL".equalsIgnoreCase(type)) {
            type = "BIGINT";
        }

        StringBuilder lbuf = new StringBuilder();
        if (type != null) {
            executeQuery(
                conn, "ALTER TABLE " + tableSQL(table) + " ALTER COLUMN " + columnSQL(column) +
                " TYPE " + type);
            lbuf.append("type=").append(type);
        }
        if (nullable != null) {
            executeQuery(
                conn, "ALTER TABLE " + tableSQL(table) + " ALTER COLUMN " + columnSQL(column) +
                " " + (nullable ? "DROP NOT NULL" : "SET NOT NULL"));
            if (lbuf.length() > 0) {
                lbuf.append(", ");
            }
            lbuf.append("nullable=").append(nullable);
        }
        if (unique != null) {
            // TODO: I think this requires ALTER TABLE DROP CONSTRAINT and so on
            if (lbuf.length() > 0) {
                lbuf.append(", ");
            }
            lbuf.append("unique=").append(unique);
            if (unique) {
                executeQuery(
                        conn, "ALTER TABLE " + tableSQL(table) + "ADD UNIQUE (" +
                        columnSQL(column) + ")");
            } else {
                lbuf.append(" (not implemented yet)");
            }
        }
        if (defaultValue != null) {
            executeQuery(
                conn, "ALTER TABLE " + tableSQL(table) + " ALTER COLUMN " + columnSQL(column) +
                " " + (defaultValue.length() > 0 ? "SET DEFAULT " + defaultValue : "DROP DEFAULT"));
            if (lbuf.length() > 0) {
                lbuf.append(", ");
            }
            lbuf.append("defaultValue=").append(defaultValue);
        }
        log("Database column '" + column + "' of table '" + table + "' modified to have " +
            "definition [" + lbuf + "].");
        return true;
    }

    @Override
    public String getSchemaName ()
    {
        // TODO: is this global to all postgres? Should this be discovered by asking
        // DatabaseMetaData.getSchemas()?
        return "public";
    }

    // from DatabaseLiaison
    public String columnSQL (String column)
    {
        return "\"" + column + "\"";
    }

    // from DatabaseLiaison
    public String tableSQL (String table)
    {
        return "\"" + table + "\"";
    }

    // from DatabaseLiaison
    public String indexSQL (String index)
    {
        return "\"" + index + "\"";
    }
}
