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
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Set;

import com.samskivert.util.Tuple;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.FullText;

import com.samskivert.depot.impl.FieldMarshaller.BooleanMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.ByteArrayMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.ByteEnumMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.ByteMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.DoubleMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.FloatMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.IntArrayMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.IntMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.LongMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.ObjectMarshaller;
import com.samskivert.depot.impl.FieldMarshaller.ShortMarshaller;
import com.samskivert.depot.impl.clause.CreateIndexClause;
import com.samskivert.depot.impl.clause.DeleteClause;
import com.samskivert.depot.impl.clause.DropIndexClause;
import com.samskivert.depot.impl.expression.DateFun.DatePart;
import com.samskivert.depot.impl.expression.DateFun.DateTruncate;
import com.samskivert.depot.impl.expression.DateFun.DatePart.Part;
import com.samskivert.depot.impl.expression.NumericalFun.Trunc;

import static com.samskivert.Log.log;

public class MySQLBuilder
    extends SQLBuilder
{
    public class MSBuildVisitor extends BuildVisitor
    {
        @Override public Void visit (FullText.Match match)
        {
            renderMatch(match.getDefinition());
            return null;
        }

        @Override public Void visit (FullText.Rank rank)
        {
            renderMatch(rank.getDefinition());
            return null;
        }

        @Override public Void visit (DeleteClause deleteClause)
        {
            _builder.append("delete from ");
            appendTableName(deleteClause.getPersistentClass());
            _builder.append(" ");

            // MySQL can't do DELETE FROM SomeTable AS T1, so we turn off abbreviations briefly.
            boolean savedFlag = _types.getUseTableAbbreviations();
            _types.setUseTableAbbreviations(false);
            try {
                deleteClause.getWhereClause().accept(this);
            } finally {
                _types.setUseTableAbbreviations(savedFlag);
            }
            return null;
        }

        @Override public Void visit (Trunc exp)
        {
            return appendFunctionCall("truncate", exp.getArg());
        }

        @Override public Void visit (DatePart exp) {
            return appendFunctionCall(getDateFunction(exp.getPart()), exp.getArg());
        }

        @Override
        public Void visit (DateTruncate exp)
        {
            // exp.getTruncation() is currently always DAY
            return appendFunctionCall("date", exp.getArg());
        }

        protected String getDateFunction (Part part)
        {
            switch(part) {
            case DAY_OF_MONTH:
                return "dayofmonth";
            case DAY_OF_WEEK:
                return "dayofweek";
            case DAY_OF_YEAR:
                return "dayofyear";
            case HOUR:
                return "hour";
            case MINUTE:
                return "minute";
            case MONTH:
                return "month";
            case SECOND:
                return "second";
            case WEEK:
                return "week";
            case YEAR:
                return "year";
            case EPOCH:
                return "unix_timestamp";
            }
            throw new IllegalArgumentException("Unknown date part: " + part);
        }

        @Override
        public Void visit (CreateIndexClause createIndexClause)
        {
            for (Tuple<SQLExpression, Order> field : createIndexClause.getFields()) {
                if (!(field.left instanceof ColumnExp)) {
                    log.warning("This database can't handle complex indexes. Aborting creation.",
                        "ixName", createIndexClause.getName());
                    return null;
                }
            }
            super.visit(createIndexClause);
            return null;
        }

        @Override
        public Void visit (DropIndexClause dropIndexClause)
        {
            // MySQL's indexes are scoped on the table, not on the database, and the
            // SQL syntax reflects it: DROP INDEX fooIx on fooTable
            _builder.append("drop index ");
            appendIdentifier(dropIndexClause.getName());
            _builder.append(" on ");
            appendTableName(dropIndexClause.getPersistentClass());
            return null;
        }

        protected MSBuildVisitor (DepotTypes types)
        {
            super(types);
        }

        @Override protected void appendTableName (Class<? extends PersistentRecord> type)
        {
            _builder.append(_types.getTableName(type));
        }

        @Override protected void appendTableAbbreviation (Class<? extends PersistentRecord> type)
        {
            _builder.append(_types.getTableAbbreviation(type));
        }

        @Override protected void appendIdentifier (String field)
        {
            _builder.append(field);
        }

        @Override
        protected void appendIndexComponent (SQLExpression expression)
        {
            // MySQL is never given complex expressions and hates parens, so just recurse
            expression.accept(this);
        }

        protected void renderMatch (FullText fullText)
        {
            _builder.append("match(");
            Class<? extends PersistentRecord> pClass = fullText.getPersistentClass();
            String[] fields = _types.getMarshaller(pClass).getFullTextIndex(
                    fullText.getName()).fields();
            for (int ii = 0; ii < fields.length; ii ++) {
                if (ii > 0) {
                    _builder.append(", ");
                }
                new ColumnExp(pClass, fields[ii]).accept(this);
            }
            _builder.append(") against (");
            bindValue(fullText.getQuery());
            _builder.append(" in boolean mode)");
        }
    }

    public MySQLBuilder (DepotTypes types)
    {
        super(types);
    }

    @Override
    public void getFtsIndexes (
        Iterable<String> columns, Iterable<String> indexes, Set<String> target)
    {
        for (String index : indexes) {
            if (index.startsWith("ftsIx_")) {
                target.add(index.substring("ftsIx_".length()));
            }
        }
    }

    @Override
    public <T extends PersistentRecord> boolean addFullTextSearch (
        Connection conn, DepotMarshaller<T> marshaller, FullTextIndex fts)
        throws SQLException
    {
        Class<T> pClass = marshaller.getPersistentClass();
        StringBuilder update = new StringBuilder("ALTER TABLE ").
            append(marshaller.getTableName()).append(" ADD FULLTEXT INDEX ftsIx_").
            append(fts.name()).append(" (");
        String[] fields = fts.fields();
        for (int ii = 0; ii < fields.length; ii ++) {
            if (ii > 0) {
                update.append(", ");
            }
            update.append(_types.getColumnName(pClass, fields[ii]));
        }
        update.append(")");

        log.info("Adding full-text search index: ftsIx_" + fts.name());
        conn.createStatement().executeUpdate(update.toString());
        return true;
    }

    @Override
    public boolean isPrivateColumn (String column)
    {
        // The MySQL builder does not yet have any private columns.
        return false;
    }

    @Override
    protected String getBooleanDefault ()
    {
        return "0";
    }

    @Override
    protected BuildVisitor getBuildVisitor ()
    {
        return new MSBuildVisitor(_types);
    }

    @Override
    protected <T> String getColumnType (FieldMarshaller<?> fm, int length)
    {
        if (fm instanceof ByteMarshaller) {
            return "TINYINT";
        } else if (fm instanceof ShortMarshaller) {
            return "SMALLINT";
        } else if (fm instanceof IntMarshaller) {
            return "INTEGER";
        } else if (fm instanceof LongMarshaller) {
            return "BIGINT";
        } else if (fm instanceof FloatMarshaller) {
            return "FLOAT";
        } else if (fm instanceof DoubleMarshaller) {
            return "DOUBLE";
        } else if (fm instanceof ObjectMarshaller) {
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
                return "DATETIME";
            } else if (ftype.equals(Timestamp.class)) {
                return "DATETIME";
            } else if (ftype.equals(Blob.class)) {
                return "BYTEA";
            } else if (ftype.equals(Clob.class)) {
                return "TEXT";
            } else {
                throw new IllegalArgumentException(
                    "Don't know how to create SQL for " + ftype + ".");
            }
        } else if (fm instanceof ByteArrayMarshaller ||
                   fm instanceof IntArrayMarshaller) {
            if (fm instanceof IntArrayMarshaller) {
                length *= 4;
            }
            // semi-arbitrarily use VARBINARY() up to 32767
            if (length < (1 << 15)) {
                return "VARBINARY(" + length + ")";
            }
            // use BLOB to 65535
            if (length < (1 << 16)) {
                return "BLOB";
            }
            if (length < (1 << 24)) {
                return "MEDIUMBLOB";
            }
            return "LONGBLOB";
        } else if (fm instanceof ByteEnumMarshaller) {
            return "TINYINT";
        } else if (fm instanceof BooleanMarshaller) {
            return "TINYINT";
        } else {
            throw new IllegalArgumentException("Unknown field marshaller type: " + fm.getClass());
        }
    }
}
