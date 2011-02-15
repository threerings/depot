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
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.Set;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.LiaisonRegistry;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.Exps;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.FullTextIndex.Configuration;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.clause.InsertClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.impl.expression.IntervalExp;
import com.samskivert.depot.impl.expression.DateFun.DatePart;
import com.samskivert.depot.impl.expression.DateFun.DateTruncate;
import com.samskivert.depot.impl.expression.DateFun.DatePart.Part;
import com.samskivert.depot.operator.FullText;

import static com.samskivert.Log.log;

public class PostgreSQLBuilder
    extends SQLBuilder
{
    // Are we running with PostgreSQL 8.3's new FTS engine, or the old one?
    // TODO: Rip out when no longer needed.
    public final static boolean PG83 = Boolean.getBoolean("com.samskivert.depot.pg83");

    public class PGBuildVisitor extends BuildVisitor
    {
        @Override public Void visit (IntervalExp interval) {
            _builder.append("interval '").append(interval.amount);
            _builder.append(" ").append(interval.unit).append("'");
            return null;
        }

        @Override public Void visit (FullText.Match match) {
            appendIdentifier("ftsCol_" + match.getDefinition().getName());
            _builder.append(" @@ to_tsquery('").
            append(translateFTConfig(getFTIndex(match.getDefinition()).configuration())).
            append("', ");
            bindValue(massageFTQuery(match.getDefinition()));
            _builder.append(")");
            return null;
        }

        @Override public Void visit (FullText.Rank rank) {
            _builder.append(PG83 ? "ts_rank" : "rank").append("(");
            appendIdentifier("ftsCol_" + rank.getDefinition().getName());
            _builder.append(", to_tsquery('").
            append(translateFTConfig(getFTIndex(rank.getDefinition()).configuration())).
            // TODO: The normalization parameter is really quite important, and should
            // TODO: perhaps be configurable, but for the moment we hard-code it to 1:
            // TODO: "divides the rank by the 1 + logarithm of the document length"
            append("', ");
            bindValue(massageFTQuery(rank.getDefinition()));
            _builder.append("), 1)");
            return null;
        }

        @Override public Void visit (DatePart exp)
        {
            String datePart = "'" + translateDatePart(exp.getPart()) + "'";
            return appendFunctionCall("date_part", Exps.literal(datePart), exp.getArg());
        }

        @Override
        public Void visit (DateTruncate exp)
        {
            String field = "'" + exp.getTruncation().toString().toLowerCase() + "'";
            return appendFunctionCall("date_trunc", Exps.literal(field), exp.getArg());
        }

        protected String translateDatePart (Part part)
        {
            switch(part) {
            case DAY_OF_MONTH:
                return "day";
            case DAY_OF_WEEK:
                return "dow";
            case DAY_OF_YEAR:
                return "doy";
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
                return "epoch";
            }
            throw new IllegalArgumentException("Unknown date part: " + part);
        }

        protected FullTextIndex getFTIndex (FullText definition)
        {
            DepotMarshaller<?> marsh = _types.getMarshaller(definition.getPersistentClass());
            return marsh.getFullTextIndex(definition.getName());
        }

        @Override protected void appendIdentifier (String field) {
            _builder.append("\"").append(field).append("\"");
        }

        @Override protected void appendInsertColumns (InsertClause insertClause)
        {
            // see if we will be inserting any columns whatsoever
            Class<? extends PersistentRecord> pClass = insertClause.getPersistentClass();
            Set<String> idFields = insertClause.getIdentityFields();
            for (ColumnExp<?> field : _types.getMarshaller(pClass).getColumnFieldNames()) {
                if (!idFields.contains(field.name)) {
                    // we found a field we're inserting, so call super and finish
                    super.appendInsertColumns(insertClause);
                    return;
                }
            }
            // we never found anything we'll actually be inserting
            _builder.append("default values");
        }

        protected PGBuildVisitor (DepotTypes types)
        {
            super(types, true);
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
        log.info("Adding full-text search column, index and trigger: " + column + ", " +
                 index + ", " + trigger);
        liaison.addColumn(conn, table, column, "TSVECTOR", true);
        stmt.executeUpdate(initColumn.toString());
        stmt.executeUpdate(createIndex.toString());
        stmt.executeUpdate(createTrigger.toString());
        return true;
    }

    @Override
    public boolean isPrivateColumn (String column, Map<String, FullTextIndex> fullTextIndexes)
    {
        // filter out any column that we created as part of FTS support
        for (FullTextIndex fti : fullTextIndexes.values()) {
            if (("ftsCol_" + fti.name()).equals(column)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPrivateIndex (String index, Map<String, FullTextIndex> fullTextIndexes)
    {
        // filter out any index that looks like PostgreSQL created it
        if (index.endsWith("_key") || index.endsWith("_pkey")) {
            return true;
        }

        // filter out any index that we created as part of FTS support
        for (FullTextIndex fti : fullTextIndexes.values()) {
            if (index.endsWith("_ftsIx_" + fti.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BuildVisitor getBuildVisitor ()
    {
        return new PGBuildVisitor(_types);
    }

    @Override
    protected <T> String getColumnType (FieldMarshaller<?> fm, int length)
    {
        return fm.getColumnType(TYPER, length);
    }

    protected static String massageFTQuery (FullText match)
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
            throw new IllegalArgumentException("Unknown full text configuration: " + configuration);
        }
    }

    protected static final FieldMarshaller.ColumnTyper TYPER = new FieldMarshaller.ColumnTyper() {
        public String getBooleanType (int length) {
            return "BOOLEAN";
        }
        public String getByteType (int length) {
            return "SMALLINT";
        }
        public String getShortType (int length) {
            return "SMALLINT";
        }
        public String getIntType (int length) {
            return "INTEGER";
        }
        public String getLongType (int length) {
            return "BIGINT";
        }
        public String getFloatType (int length) {
            return "REAL";
        }
        public String getDoubleType (int length) {
            return "DOUBLE PRECISION";
        }
        public String getStringType (int length) {
            return "VARCHAR(" + length + ")";
        }
        public String getDateType (int length) {
            return "DATE";
        }
        public String getTimeType (int length) {
            return "TIME";
        }
        public String getTimestampType (int length) {
            return "TIMESTAMP";
        }
        public String getBlobType (int length) {
            return "BYTEA";
        }
        public String getClobType (int length) {
            return "TEXT";
        }
    };
}
