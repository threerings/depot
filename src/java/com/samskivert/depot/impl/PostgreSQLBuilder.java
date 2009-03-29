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

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Set;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.LiaisonRegistry;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.FullTextIndex.Configuration;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.operator.Conditionals.FullText;

import static com.samskivert.Log.log;

public class PostgreSQLBuilder
    extends SQLBuilder
{
    // Are we running with PostgreSQL 8.3's new FTS engine, or the old one?
    // TODO: Rip out when no longer needed.
    public final static boolean PG83 = Boolean.getBoolean("com.samskivert.depot.pg83");

    public class PGBuildVisitor extends BuildVisitor
    {
        @Override public void visit (FullText.Match match) {
            appendIdentifier("ftsCol_" + match.getDefinition().getName());
            _builder.append(" @@ to_tsquery('").
            append(translateFTConfig(getFTIndex(match.getDefinition()).configuration())).
            append("', ?)");
        }

        @Override public void visit (FullText.Rank rank) {
            _builder.append(PG83 ? "ts_rank" : "rank").append("(");
            appendIdentifier("ftsCol_" + rank.getDefinition().getName());
            _builder.append(", to_tsquery('").
            append(translateFTConfig(getFTIndex(rank.getDefinition()).configuration())).
            // TODO: The normalization parameter is really quite important, and should
            // TODO: perhaps be configurable, but for the moment we hard-code it to 1:
            // TODO: "divides the rank by the 1 + logarithm of the document length"
            append("', ?), 1)");
        }

        @Override public void visit (EpochSeconds epochSeconds) {
            _builder.append("date_part('epoch', ");
            epochSeconds.getArgument().accept(this);
            _builder.append(")");
        }

// TODO: enable when we can require 1.6 support
//         @Override public void visit (In in) {
//             in.getColumn().accept(this);
//             _builder.append(" = any (?)");
//         }

        protected FullTextIndex getFTIndex (FullText definition)
        {
            DepotMarshaller<?> marsh = _types.getMarshaller(definition.getPersistentClass());
            return marsh.getFullTextIndex(definition.getName());
        }

        @Override protected void appendIdentifier (String field) {
            _builder.append("\"").append(field).append("\"");
        }
        
        protected PGBuildVisitor (DepotTypes types) {
            super(types);
        }
    }

    public class PGBindVisitor extends BindVisitor
    {
        @Override public void visit (FullText.Rank rank) {
            String query = massageQuery(rank.getDefinition());
            try {
                _stmt.setString(_argIdx++, query);
            } catch (SQLException sqe) {
                throw new DatabaseException("Failed to configure full-text match column " +
                                            "[idx=" + (_argIdx-1) + ", query=" + query + "]", sqe);
            }
        }

        @Override public void visit (FullText.Match match) {
            String query = massageQuery(match.getDefinition());
            try {
                _stmt.setString(_argIdx++, query);
            } catch (SQLException sqe) {
                throw new DatabaseException("Failed to configure full-text match column " +
                                            "[idx=" + (_argIdx-1) + ", query=" + query + "]", sqe);
            }
        }
        
        protected String massageQuery (FullText match)
        {
            // The tsearch2 engine takes queries on the form
            //   (foo&bar)|goop
            // so in this first simple implementation, we just take the user query, chop it into
            // words by space/punctuation and 'or' those together like so:
            //   'ho! who goes there?' -> 'ho|who|goes|there'
            String[] searchTerms = match.getQuery().toLowerCase().split("\\W+");
            if (searchTerms.length > 0 && searchTerms[0].length() == 0) {
                searchTerms = ArrayUtil.splice(searchTerms, 0, 1);
            }
            return StringUtil.join(searchTerms, "|");
        }

// TODO: enable when we can require 1.6 support
//         @Override public void visit (In in) {
//             Comparable<?>[] values = in.getValues();
//             try {
//                 _stmt.setObject(
//                     _argIdx++, _conn.createArrayOf(getElementType(values), (Object[])values));
//             } catch (SQLException sqe) {
//                 throw new DatabaseException(
//                     "Failed to write value to statement [idx=" + (_argIdx-1) +
//                     ", values=" + StringUtil.safeToString(values) + "]", sqe);
//             }
//         }

//         protected String getElementType (Comparable<?>[] values) {
//             if (values instanceof Integer[]) {
//                 return "integer";
//             } else if (values instanceof String[]) {
//                 return "character varying";
//             } else {
//                 throw new DatabaseException(
//                     "Don't know how to make Postgres array for " + values.getClass());
//             }
//         }

        protected PGBindVisitor (DepotTypes types, Connection conn, PreparedStatement stmt) {
            super(types, conn, stmt);
        }
    }

    public PostgreSQLBuilder (DepotTypes types)
    {
        super(types);
    }

    @Override
    public void getFtsIndexes (
        Iterable<String> columns, Iterable<String> indexes, Set<String> target)
    {
        for (String column : columns) {
            if (column.startsWith("ftsCol_")) {
                target.add(column.substring("ftsCol_".length()));
            }
        }
    }

    @Override
    public <T extends PersistentRecord> boolean addFullTextSearch (
        Connection conn, DepotMarshaller<T> marshaller, FullTextIndex fts)
        throws SQLException
    {
        Class<T> pClass = marshaller.getPersistentClass();
        DatabaseLiaison liaison = LiaisonRegistry.getLiaison(conn);

        String[] fields = fts.fields();

        String table = marshaller.getTableName();
        String column = "ftsCol_" + fts.name();
        String index = table + "_ftsIx_" + fts.name();
        String trigger = table + "_ftsTrig_" + fts.name();

        // build the UPDATE
        StringBuilder initColumn = new StringBuilder("UPDATE ").
            append(liaison.tableSQL(table)).append(" SET ").append(liaison.columnSQL(column)).
            append(" = TO_TSVECTOR('").
            append(translateFTConfig(fts.configuration())).
            append("', ");

        for (int ii = 0; ii < fields.length; ii ++) {
            if (ii > 0) {
                initColumn.append(" || ' ' || ");
            }
            initColumn.append("COALESCE(").
                append(liaison.columnSQL(_types.getColumnName(pClass, fields[ii]))).
                append(", '')");
        }
        initColumn.append(")");

        String triggerFun = PG83 ? "tsvector_update_trigger" : "tsearch2";
        
        // build the CREATE TRIGGER
        StringBuilder createTrigger = new StringBuilder("CREATE TRIGGER ").
            append(liaison.columnSQL(trigger)).append(" BEFORE UPDATE OR INSERT ON ").
            append(liaison.tableSQL(table)).
            append(" FOR EACH ROW EXECUTE PROCEDURE ").append(triggerFun).append("(").
            append(liaison.columnSQL(column)).append(", ");
        
            if (PG83) {
                createTrigger.append("'").
                append(translateFTConfig(fts.configuration())).
                append("', ");
            }

        for (int ii = 0; ii < fields.length; ii ++) {
            if (ii > 0) {
                createTrigger.append(", ");
            }
            createTrigger.append(liaison.columnSQL(_types.getColumnName(pClass, fields[ii])));
        }
        createTrigger.append(")");

        // build the CREATE INDEX
        StringBuilder createIndex = new StringBuilder("CREATE INDEX ").
            append(liaison.columnSQL(index)).append(" ON " ).append(liaison.tableSQL(table)).
            append(" USING ").append(PG83 ? "GIN" : "GIST").append("(").
            append(liaison.columnSQL(column)).append(")");

        Statement stmt = conn.createStatement();
        try {
            log.info(
                "Adding full-text search column, index and trigger: " + column + ", " +
                index + ", " + trigger);
            liaison.addColumn(conn, table, column, "TSVECTOR", true);
            stmt.executeUpdate(initColumn.toString());
            stmt.executeUpdate(createIndex.toString());
            stmt.executeUpdate(createTrigger.toString());

        } finally {
            JDBCUtil.close(stmt);
        }
        return true;
    }

    @Override
    public boolean isPrivateColumn (String column)
    {
        // filter out any column that we created as part of FTS support
        return column.startsWith("ftsCol_");
    }

    @Override
    protected BuildVisitor getBuildVisitor ()
    {
        return new PGBuildVisitor(_types);
    }

    @Override
    protected BindVisitor getBindVisitor (Connection conn, PreparedStatement stmt)
    {
        return new PGBindVisitor(_types, conn, stmt);
    }

    @Override
    protected <T> String getColumnType (FieldMarshaller<?> fm, int length)
    {
        if (fm instanceof FieldMarshaller.ByteMarshaller) {
            return "SMALLINT";
        } else if (fm instanceof FieldMarshaller.ShortMarshaller) {
            return "SMALLINT";
        } else if (fm instanceof FieldMarshaller.IntMarshaller) {
            return "INTEGER";
        } else if (fm instanceof FieldMarshaller.LongMarshaller) {
            return "BIGINT";
        } else if (fm instanceof FieldMarshaller.FloatMarshaller) {
            return "REAL";
        } else if (fm instanceof FieldMarshaller.DoubleMarshaller) {
            return "DOUBLE PRECISION";
        } else if (fm instanceof FieldMarshaller.ObjectMarshaller) {
            Class<?> ftype = fm.getField().getType();
            if (ftype.equals(Byte.class)) {
                return "SMALLINT";
            } else if (ftype.equals(Short.class)) {
                return "SMALLINT";
            } else if (ftype.equals(Integer.class)) {
                return "INTEGER";
            } else if (ftype.equals(Long.class)) {
                return "BIGINT";
            } else if (ftype.equals(Float.class)) {
                return "FLOAT";
            } else if (ftype.equals(Double.class)) {
                return "DOUBLE";
            } else if (ftype.equals(String.class)) {
                if (length < (1 << 15)) {
                    return "VARCHAR(" + length + ")";
                }
                return "TEXT";
            } else if (ftype.equals(Date.class)) {
                return "DATE";
            } else if (ftype.equals(Time.class)) {
                return "TIME";
            } else if (ftype.equals(Timestamp.class)) {
                return "TIMESTAMP";
            } else if (ftype.equals(Blob.class)) {
                return "BYTEA";
            } else if (ftype.equals(Clob.class)) {
                return "TEXT";
            } else {
                throw new IllegalArgumentException(
                    "Don't know how to create SQL for " + ftype + ".");
            }
        } else if (fm instanceof FieldMarshaller.ByteArrayMarshaller) {
            return "BYTEA";
        } else if (fm instanceof FieldMarshaller.IntArrayMarshaller) {
            return "BYTEA";
        } else if (fm instanceof FieldMarshaller.ByteEnumMarshaller) {
            return "SMALLINT";
        } else if (fm instanceof FieldMarshaller.BooleanMarshaller) {
            return "BOOLEAN";
        } else {
            throw new IllegalArgumentException("Unknown field marshaller type: " + fm.getClass());
        }
    }
    
    // Translate the mildly abstracted full-text parser/dictionary configuration support
    // in FullText to actual PostgreSQL configuration identifiers.
    protected static String translateFTConfig (Configuration configuration)
    {
        // legacy support
        if (!PG83) {
            return "default";
        }
        switch(configuration) {
        case Simple:
            return "pg_catalog.simple";
        case English:
            return "pg_catalog.english";
        default:
            throw new IllegalArgumentException(
                "Unknown full text configuration: " + configuration);
        }
    }
}
