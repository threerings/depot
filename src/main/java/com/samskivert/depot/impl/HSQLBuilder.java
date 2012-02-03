//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import com.samskivert.depot.Exps;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.clause.Distinct;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.AggregateFun;
import com.samskivert.depot.impl.expression.DateFun.DatePart;
import com.samskivert.depot.impl.expression.DateFun.DatePart.Part;
import com.samskivert.depot.impl.expression.DateFun.DateTruncate;
import com.samskivert.depot.impl.expression.LiteralExp;
import com.samskivert.depot.impl.expression.NumericalFun;
import com.samskivert.depot.impl.expression.StringFun.Lower;
import com.samskivert.depot.impl.expression.ValueExp;
import com.samskivert.depot.impl.operator.BitAnd;
import com.samskivert.depot.impl.operator.BitOr;
import com.samskivert.depot.impl.operator.Like;
import com.samskivert.depot.impl.operator.MultiOperator;
import com.samskivert.depot.operator.FullText;
import com.samskivert.util.ArrayUtil;

import com.samskivert.jdbc.ColumnDefinition;

public class HSQLBuilder
    extends SQLBuilder
{
    public class HBuildVisitor extends BuildVisitor
    {
        @Override public Void visit (AggregateFun.Average<?> exp)
        {
            return appendAggregateFunctionCall("avg", exp);
        }

        @Override public Void visit (NumericalFun.Round<?> exp)
        {
            // HSQLDB requires a number of digits after the decimal place argument, so we supply
            // zero to emulate round on other databases
            return appendFunctionCall("round", exp.getArg(), Exps.value(0));
        }

        @Override public Void visit (NumericalFun.Trunc<?> exp)
        {
            // TODO: this would work if HSQLDB truncate actually worked, but it doesn't; we'll
            // leave it in here in case some future bug fixed version of HSQLDB does work
            return appendFunctionCall("truncate", exp.getArg(), Exps.value(0));
        }

        @Override public Void visit (NumericalFun.Random<?> exp)
        {
            return appendFunctionCall("rand");
        }

        @Override public Void visit (NumericalFun.Power<?> exp)
        {
            // HSQLDB can't handle a value argument to power(a, b), so we turn the ValueExp into a
            // LiteralExp
            SQLExpression<?> power = exp.getPower();
            if (power instanceof ValueExp<?>) {
                power = new LiteralExp<String>(((ValueExp<?>)power).getValue().toString());
            }
            return appendFunctionCall("power", exp.getValue(), power);
        }

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
            List<SQLExpression<?>> bits = Lists.newArrayList();
            for (String field : fields) {
                for (String ftsWord : ftsWords) {
                    // build comparisons between each word and column
                    bits.add(new Like(new Lower(new ColumnExp<String>(pClass, field)),
                                      "%"+ftsWord+"%", true));
                }
            }
            // then just OR them all together and we have our query
            _ftsCondition = Ops.or(bits);
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
        public Void visit (MultiOperator<?> operator)
        {
            // HSQL doesn't handle & and | operators
            if (operator instanceof BitAnd) {
                return appendFunctionCall("bitand", operator.getArgs());
            }
            if (operator instanceof BitOr) {
                return appendFunctionCall("bitor", operator.getArgs());
            }
            return super.visit(operator);
        }

        public Void visit (Distinct distinct)
        {
            if (distinct.getDistinctOn() != null) {
                throw new IllegalArgumentException("HSQL does not support DISTINCT ON");
            }
            return super.visit(distinct);
        }

        @Override public Void visit (DatePart exp) {

            if (exp.getPart() == Part.EPOCH) {
                _builder.append("datediff('ss', ");
                exp.getArg().accept(this);
                _builder.append(", '1970-01-01')");
                return null;
            }
            return appendFunctionCall(getDateFunction(exp.getPart()), exp.getArg());
        }

        @Override
        public Void visit (DateTruncate exp)
        {
            throw new IllegalArgumentException("HSQL does not have built-in date truncation");
        }

        protected HBuildVisitor (DepotTypes types)
        {
            super(types, false);
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
                // internal error, EPOCH is handled in the calling function, fall through to error
            }
            throw new IllegalArgumentException("Unknown date part: " + part);
        }

        @Override protected void appendIdentifier (String field) {
            _builder.append("\"").append(field).append("\"");
        }

        @Override protected boolean orderSupported (OrderBy.Order order)
        {
            switch (order) {
            case ASC: case DESC:
                return true;
            }
            return false;
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
    public boolean isPrivateColumn (String column, Map<String, FullTextIndex> fullTextIndexes)
    {
        // The HSQLDB builder does not yet have any private columns.
        return false;
    }

    @Override
    public boolean isPrivateIndex (String index, Map<String, FullTextIndex> fullTextIndexes)
    {
        // HSQLDB system indices start with SYS_IDX
        return index.startsWith("SYS_IDX");
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
    protected void maybeMutateForGeneratedValue (
        Field field, GeneratedValue genValue, ColumnDefinition column)
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
            super.maybeMutateForGeneratedValue(field, genValue, column);
        }
    }

    @Override
    protected <T> String getColumnType (FieldMarshaller<?> fm, int length)
    {
        return fm.getColumnType(TYPER, length);
    }

    /** Holds the Full Text Seach condition between build and bind phases. */
    protected SQLExpression<?> _ftsCondition;

    protected static final FieldMarshaller.ColumnTyper TYPER = new FieldMarshaller.ColumnTyper() {
        public String getBooleanType (int length) {
            return "BOOLEAN";
        }
        public String getByteType (int length) {
            return "TINYINT";
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
            return "VARBINARY(" + length + ")";
        }
        public String getClobType (int length) {
            return "VARCHAR";
        }
    };
}
