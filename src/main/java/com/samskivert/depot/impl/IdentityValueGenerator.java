//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.MySQLLiaison;
import com.samskivert.depot.annotation.GeneratedValue;

import static com.samskivert.depot.Log.log;

/**
 * Generates primary keys using an identity column.
 */
public class IdentityValueGenerator extends ValueGenerator
{
    public IdentityValueGenerator (GeneratedValue gv, DepotMarshaller<?> dm, FieldMarshaller<?> fm)
    {
        super(gv, dm, fm);
    }

    @Override // from ValueGenerator
    public boolean isPostFactum ()
    {
        return true;
    }

    @Override // from ValueGenerator
    public void create (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        liaison.createGenerator(conn, _dm.getTableName(), _fm.getColumnName(), _initialValue);
    }

    @Override // from ValueGenerator
    public int nextGeneratedValue (Connection conn, DatabaseLiaison liaison, Statement stmt)
        throws SQLException
    {
        String column = _fm.getColumnName();
        // if this JDBC driver supports getGeneratedKeys, use it!
        if (stmt != null && conn.getMetaData().supportsGetGeneratedKeys()) {
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt((liaison instanceof MySQLLiaison) ? "GENERATED_KEY" : column);
            }
        }
        return liaison.lastInsertedId(conn, _dm.getTableName(), column);
    }

    @Override // from ValueGenerator
    public void delete (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        liaison.deleteGenerator(conn, _dm.getTableName(), _fm.getColumnName());
    }
}
