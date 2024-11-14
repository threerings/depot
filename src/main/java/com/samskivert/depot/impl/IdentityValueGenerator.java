//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.impl.jdbc.DatabaseLiaison;

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
    public long nextGeneratedValue (Connection conn, DatabaseLiaison liaison, Statement stmt)
        throws SQLException
    {
        return liaison.lastInsertedId(conn, stmt, _dm.getTableName(), _fm.getColumnName());
    }

    @Override // from ValueGenerator
    public void delete (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        liaison.deleteGenerator(conn, _dm.getTableName(), _fm.getColumnName());
    }
}
