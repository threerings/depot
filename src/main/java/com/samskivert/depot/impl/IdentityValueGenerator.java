//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.depot.annotation.GeneratedValue;

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
        Integer id = liaison.lastInsertedId(conn, stmt, _dm.getTableName(), _fm.getColumnName());
        if (id == null) throw new SQLException(
            "Unable to obtain last inserted id [table=" + _dm.getTableName() +
            ", column=" + _fm.getColumnName() + "]");
        return id;
    }

    @Override // from ValueGenerator
    public void delete (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        liaison.deleteGenerator(conn, _dm.getTableName(), _fm.getColumnName());
    }
}
