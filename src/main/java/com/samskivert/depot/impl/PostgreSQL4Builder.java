//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Exps;
import com.samskivert.depot.impl.operator.In;
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
                Exps.value(false).accept(this);
                return null;
            }
            in.getExpression().accept(this);
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
                    } else if (testValue instanceof Long) {
                        type = "bigint";
                    } else if (testValue instanceof String) {
                        type = "varchar";
                    } else if (testValue instanceof Short || testValue instanceof Byte) {
                        type = "smallint";
                    } else if (testValue instanceof ByteEnum) {
                        Byte[] bytes = new Byte[values.length];
                        for (int ii = 0; ii < bytes.length; ii ++) {
                            bytes[ii] = ((ByteEnum) values[ii]).toByte();
                        }
                        values = bytes;
                        type = "smallint"; // tinyint is in the spec, but PG doesn't recognize?
                    } else if (testValue instanceof Enum<?>) {
                        type = "varchar";
                        // we need to replace the enum values with their name() because otherwise
                        // the Postgres JDBC driver will call toString() on them which is incorrect
                        for (int ii = 0; ii < values.length; ii++) {
                            values[ii] = ((Enum<?>)values[ii]).name();
                        }
                    } else if (testValue instanceof Timestamp) {
                        type = "timestamp";
                    } else if (testValue instanceof Date) {
                        type = "date";
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
