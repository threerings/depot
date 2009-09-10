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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.impl.expression.ValueExp;
import com.samskivert.depot.operator.In;
import com.samskivert.util.ByteEnum;

/**
 * Specializes our PostgreSQL builder for JDBC4.
 */
public class PostgreSQL4Builder extends PostgreSQLBuilder
{
    public class PG4BuildVisitor extends PGBuildVisitor
    {
        @Override public Void visit (In in) {
            // if the In() expression is empty, replace it with a 'false'
            final Comparable<?>[] values = in.getValues();
            if (values.length == 0) {
                new ValueExp(false).accept(this);
                return null;
            }
            in.getColumn().accept(this);
            _builder.append(" = any (?)");
            _bindables.add(new Bindable() {
                public void doBind (Connection conn, PreparedStatement stmt, int argIdx)
                    throws Exception
                {
                    stmt.setObject(argIdx, createArray(conn, values));
                }
                protected Array createArray (Connection conn, Object[] values)
                    throws SQLException
                {
                    String type;
                    Object testValue = values[0];
                    if (testValue instanceof Integer) {
                        type = "integer";
                    } else if (testValue instanceof String) {
                        type = "varchar";
                    } else if (testValue instanceof ByteEnum) {
                        Byte[] bytes = new Byte[values.length];
                        for (int ii = 0; ii < bytes.length; ii ++) {
                            bytes[ii] = ((ByteEnum) values[ii]).toByte();
                        }
                        values = bytes;
                        type = "smallint"; // tinyint is in the spec, but PG doesn't recognize?
                    } else if (testValue instanceof Timestamp) {
                        type = "timestamp";
                    } else {
                        throw new DatabaseException(
                            "Don't know how to make Postgres array for " + testValue.getClass());
                    }
                    return conn.createArrayOf(type, values);
                }
            });
            return null;
        }

        protected PG4BuildVisitor (DepotTypes types)
        {
            super(types);
        }
    }

    public PostgreSQL4Builder (DepotTypes types)
    {
        super(types);
    }

    @Override
    protected BuildVisitor getBuildVisitor ()
    {
        return new PG4BuildVisitor(_types);
    }
}
