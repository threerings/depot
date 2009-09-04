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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.TableGenerator;

import com.samskivert.jdbc.ColumnDefinition;
import com.samskivert.jdbc.DatabaseLiaison;

/**
 * Generates primary keys using an external table .
 */
public class TableValueGenerator extends ValueGenerator
{
    public TableValueGenerator (
        TableGenerator tg, GeneratedValue gv, DepotMarshaller<?> dm, FieldMarshaller<?> fm)
    {
        super(gv, dm, fm);
        _valueTable = defStr(tg.table(), "IdSequences");
        _pkColumnName = defStr(tg.pkColumnName(), "sequence");
        _pkColumnValue = defStr(tg.pkColumnValue(), "default");
        _valueColumnName = defStr(tg.valueColumnName(), "value");
    }

    @Override // from ValueGenerator
    public boolean isPostFactum ()
    {
        return false;
    }

    @Override // from ValueGenerator
    public void create (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        // make sure our table exists
        liaison.createTableIfMissing(
            conn, _valueTable,
            new String[] { _pkColumnName, _valueColumnName },
            new ColumnDefinition[] {
                new ColumnDefinition("VARCHAR(255)", true, false, null),
                new ColumnDefinition("INTEGER")
            },
            null,
            new String[] { _pkColumnName });

        // and also that there's a row in it for us
        PreparedStatement stmt = conn.prepareStatement(
            " SELECT * FROM " + liaison.tableSQL(_valueTable) +
            "  WHERE " + liaison.columnSQL(_pkColumnName) + " = ?");
        stmt.setString(1, _pkColumnValue);
        if (stmt.executeQuery().next()) {
            return;
        }

        int initialValue = _initialValue;
        if (_migrateIfExists) {
            Integer max = getFieldMaximum(conn, liaison);
            if (max != null) {
                initialValue = 1 + max.intValue();
            }
        }

        stmt = conn.prepareStatement(
            " INSERT INTO " + liaison.tableSQL(_valueTable) + " (" +
            liaison.columnSQL(_pkColumnName) + ", " + liaison.columnSQL(_valueColumnName) +
            ") VALUES (?, ?)");
        stmt.setString(1, _pkColumnValue);
        stmt.setInt(2, initialValue);
        stmt.executeUpdate();
    }

    @Override // from ValueGenerator
    public void delete (Connection conn, DatabaseLiaison liaison) throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement(
            " DELETE FROM " + liaison.tableSQL(_valueTable) +
            "       WHERE " + liaison.columnSQL(_pkColumnName) + " = ?");
        stmt.setString(1, _pkColumnValue);
        stmt.executeUpdate();
    }

    @Override // from ValueGenerator
    public int nextGeneratedValue (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        // TODO: Make this lockless!
        PreparedStatement readStatement = conn.prepareStatement(
            " SELECT " + liaison.columnSQL(_valueColumnName) +
            "   FROM " + liaison.tableSQL(_valueTable) +
            "  WHERE " + liaison.columnSQL(_pkColumnName) + " = ? ");
        readStatement.setString(1, _pkColumnValue);

        PreparedStatement writeStatement = conn.prepareStatement(
            " UPDATE " + liaison.tableSQL(_valueTable) +
            "    SET " + liaison.columnSQL(_valueColumnName) + " = ? " +
            "  WHERE " + liaison.columnSQL(_pkColumnName) + " = ? " +
            "    AND " + liaison.columnSQL(_valueColumnName) + " = ? ");

        for (int tries = 0; tries < 10; tries ++) {
            // execute the query
            ResultSet rs = readStatement.executeQuery();
            if (!rs.next()) {
                throw new SQLException(
                    "Failed to find next primary key value [table=" + _valueTable +
                    ", column=" + _valueColumnName + "]");
            }
            // fetch the next available value
            int val = rs.getInt(1);

            // claim this value locklessly
            writeStatement.setInt(1, val + _allocationSize);
            writeStatement.setString(2, _pkColumnValue);
            writeStatement.setInt(3, val);

            // if we modified a row, we know we and nobody else got this particular value!
            if (writeStatement.executeUpdate() == 1) {
                return val;
            }
            // else try again
        }
        throw new SQLException(
            "Failed to claim next primary key value in 10 attempts [table=" + _valueTable +
            ", column=" + _valueColumnName + "]");
    }

    /**
     * Convenience function to return a value or a default fallback.
     */
    protected static String defStr (String value, String def)
    {
        if (value == null || value.trim().length() == 0) {
            return def;
        }
        return value;
    }

    protected String _valueTable;
    protected String _pkColumnName;
    protected String _pkColumnValue;
    protected String _valueColumnName;
}
