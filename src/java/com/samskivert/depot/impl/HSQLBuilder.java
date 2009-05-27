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

import static com.samskivert.Log.log;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import com.samskivert.jdbc.ColumnDefinition;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Tuple;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.BitAnd;
import com.samskivert.depot.operator.BitOr;
import com.samskivert.depot.operator.FullText;
import com.samskivert.depot.operator.Like;
import com.samskivert.depot.operator.Or;
import com.samskivert.depot.operator.SQLOperator.MultiOperator;

import com.samskivert.depot.impl.clause.CreateIndexClause;

public class HSQLBuilder
    extends SQLBuilder
{
    public class HBuildVisitor extends BuildVisitor
    {
        @Override public Void visit (FullText.Match match)
        {
            // HSQL doesn't have real full text search, so we fake it by creating a condition like
            // (lower(COL1) like '%foo%') OR (lower(COL1) like '%bar%') OR ...
            // (lower(COL2) like '%foo%') OR (lower(COL2) like '%bar%') OR ...
            // ... and so on. Not efficient, but basically functional.
            Class<? extends PersistentRecord> pClass = match.getDefinition().getPersistentClass();

            // find the fields involved
            String[] fields = _types.getMarshaller(pClass).
                getFullTextIndex(match.getDefinition().getName()).fields();

            // explode the query into words
            String[] ftsWords = match.getDefinition().getQuery().toLowerCase().split("\\W+");
            if (ftsWords.length > 0 && ftsWords[0].length() == 0) {
                // if the query led with whitespace, the first 'word' will be empty; strip it
                ftsWords = ArrayUtil.splice(ftsWords, 0, 1);
            }

            // now iterate over the cartesian product of the query words & the fields
            List<SQLExpression> bits = Lists.newArrayList();
            for (int ii = 0; ii < fields.length; ii ++) {
                FunctionExp colexp = new FunctionExp("lower", new ColumnExp(pClass, fields[ii]));
                for (int jj = 0; jj < ftsWords.length; jj ++) {
                    // build comparisons between each word and column
                    bits.add(new Like(colexp, "%" + ftsWords[jj] + "%"));
                }
            }
            // then just OR them all together and we have our query
            _ftsCondition = new Or(bits);
            _ftsCondition.accept(this);
            return null;
        }

        @Override public Void visit (FullText.Rank rank)
        {
            // not implemented for HSQL
            _builder.append("0");
            return null;
        }

        @Override
        public Void visit (MultiOperator operator)
        {
            String op;
            // HSQL doesn't handle & and | operators
            if (operator instanceof BitAnd) {
                op = "bitand";

            } else if (operator instanceof BitOr) {
                op = "bitor";

            } else {
                return super.visit(operator);
            }

            _builder.append(op).append("(");
            boolean virgin = true;
            for (SQLExpression bit: operator.getOperands()) {
                if (!virgin) {
                    _builder.append(", ");
                }
                bit.accept(this);
                virgin = false;
            }
            _builder.append(")");
            return null;
        }

        @Override
        public Void visit (EpochSeconds epochSeconds)
        {
            _builder.append("datediff('ss', ");
            epochSeconds.getArgument().accept(this);
            _builder.append(", '1970-01-01')");
            return null;
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

        protected HBuildVisitor (DepotTypes types)
        {
            super(types);
        }

        @Override protected void appendIdentifier (String field) {
            _builder.append("\"").append(field).append("\"");
        }
    }

    public HSQLBuilder (DepotTypes types)
    {
        super(types);
    }

    @Override
    public void getFtsIndexes (
        Iterable<String> columns, Iterable<String> indexes, Set<String> target)
    {
        // do nothing
    }

    @Override
    public <T extends PersistentRecord> boolean addFullTextSearch (
        Connection conn, DepotMarshaller<T> marshaller, FullTextIndex fts)
        throws SQLException
    {
        // nothing to do for HSQL

        return true;
    }

    @Override
    public boolean isPrivateColumn (String column)
    {
        // The HSQLDB builder does not yet have any private columns.
        return false;
    }

    @Override
    protected String getBooleanDefault ()
    {
        return "false";
    }

    @Override
    protected BuildVisitor getBuildVisitor ()
    {
        return new HBuildVisitor(_types);
    }

    @Override
    protected void maybeMutateForGeneratedValue (GeneratedValue genValue, ColumnDefinition column)
    {
        // HSQL's IDENTITY() implementation does not take the form of a type, as MySQL's
        // and PostgreSQL's conveniently shared SERIAL alias, nor as MySQL's original
        // AUTO_INCREMENT modifier -- but as a default value, which admittedly makes sense
        switch (genValue.strategy()) {
        case AUTO:
        case IDENTITY:
            column.defaultValue = "IDENTITY";
            column.unique = true;
            break;

        default:
            super.maybeMutateForGeneratedValue(genValue, column);
        }
    }

    @Override
    protected <T> String getColumnType (FieldMarshaller<?> fm, int length)
    {
        if (fm instanceof FieldMarshaller.ByteMarshaller) {
            return "TINYINT";
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
                return "TINYINT";
            } else if (ftype.equals(Short.class)) {
                return "SMALLINT";
            } else if (ftype.equals(Integer.class)) {
                return "INTEGER";
            } else if (ftype.equals(Long.class)) {
                return "BIGINT";
            } else if (ftype.equals(Float.class)) {
                return "FLOAT";
            } else if (ftype.equals(Double.class)) {
                return "DOUBLE PRECISION";
            } else if (ftype.equals(String.class)) {
                return "VARCHAR(" + length + ")";
            } else if (ftype.equals(Date.class)) {
                return "DATE";
            } else if (ftype.equals(Time.class)) {
                return "TIME";
            } else if (ftype.equals(Timestamp.class)) {
                return "TIMESTAMP";
            } else if (ftype.equals(Blob.class)) {
                return "VARBINARY";
            } else if (ftype.equals(Clob.class)) {
                return "VARCHAR";
            } else {
                throw new IllegalArgumentException(
                    "Don't know how to create SQL for " + ftype + ".");
            }
        } else if (fm instanceof FieldMarshaller.ByteArrayMarshaller) {
            return "VARBINARY";
        } else if (fm instanceof FieldMarshaller.IntArrayMarshaller) {
            return "VARBINARY";
        } else if (fm instanceof FieldMarshaller.ByteEnumMarshaller) {
            return "TINYINT";
        } else if (fm instanceof FieldMarshaller.BooleanMarshaller) {
            return "BOOLEAN";
        } else {
            throw new IllegalArgumentException("Unknown field marshaller type: " + fm.getClass());
        }
    }

    /** Holds the Full Text Seach condition between build and bind phases. */
    protected SQLExpression _ftsCondition;
}

