//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.lang.reflect.Field;

import java.util.Map;
import java.util.Set;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.impl.jdbc.ColumnDefinition;
import com.samskivert.depot.util.ByteEnum;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.samskivert.depot.Log.log;

/**
 * At the heart of Depot's SQL generation, this object constructs an {@link FragmentVisitor}
 * object and executes it, constructing SQL and parameter bindings as it recurses.
 */
public abstract class SQLBuilder
{
    public SQLBuilder (DepotTypes types)
    {
        _types = types;
    }

    /**
     * Construct an entirely new SQL query relative to our configured {@link DepotTypes} data.
     * This method may be called multiple times, each time beginning a new query, and will return
     * true if SQL was generated. If so, a call to {@link #prepare(Connection)} should follow,
     * which creates, configures and returns the actual {@link PreparedStatement} to execute.
     */
    public boolean newQuery (QueryClause clause)
    {
        _clause = clause;
        _buildVisitor = getBuildVisitor();
        _clause.accept(_buildVisitor);
        return _buildVisitor.getQuery().trim().length() > 0;
    }

    /**
     * After {@link #newQuery(QueryClause)} has been executed, this method is run to recurse
     * through the {@link QueryClause} structure, setting the {@link PreparedStatement} arguments
     * that were defined in the generated SQL.
     *
     * This method throws {@link SQLException} and is thus meant to be called from within
     * {@link Fetcher#invoke} and {@link Modifier#invoke}.
     */
    public PreparedStatement prepare (Connection conn)
        throws SQLException
    {
        return prepare(conn, conn.prepareStatement(buildQuery()));
    }

    /**
     * A variant of {@link #prepare} that must be used for INSERTs. Due to a Postgres JDBC driver
     * bug, we need to restrict the use of RETURN_GENERATED_KEYS to insert queries only, which
     * means we need to know when we're doing an insert.
     */
    public PreparedStatement prepareInsert (Connection conn)
        throws SQLException
    {
        return prepare(
            conn, conn.prepareStatement(buildQuery(), PreparedStatement.RETURN_GENERATED_KEYS));
    }

    /**
     * Generates the SQL needed to construct a database column for field represented by the given
     * {@link FieldMarshaller}.
     *
     * TODO: This method should be split into several parts that are more easily overridden on a
     * case-by-case basis in the dialectal subclasses.
     */
    public ColumnDefinition buildColumnDefinition (FieldMarshaller<?> fm)
    {
        // if this field is @Computed, it has no SQL definition
        if (fm.getComputed() != null) {
            return null;
        }

        Field field = fm.getField();
        Column column = field.getAnnotation(Column.class);

        ColumnDefinition coldef = (column != null) ?
            new ColumnDefinition(null, column.nullable(),
                                 column.unique(), nullify(column.defaultValue())) :
            new ColumnDefinition();

        GeneratedValue genValue = fm.getGeneratedValue();
        if (genValue != null) {
            maybeMutateForGeneratedValue(field, genValue, coldef);

        } else if (coldef.defaultValue == null) {
            maybeMutateForPrimitive(field, coldef);
        }

        if (coldef.type == null) {
            coldef.type = getColumnType(fm, (column != null) ? column.length() : 255);
        }

        // sanity check nullability
        checkArgument(!coldef.nullable || !field.getType().isPrimitive(),
                      "Primitive Java type cannot be nullable [field=" + field.getName() + "]");

        return coldef;
    }

    /**
     * Add full-text search capabilities, as defined by the provided {@link FullTextIndex}, on
     * the table associated with the given {@link DepotMarshaller}. This is a highly database
     * specific operation and must thus be implemented by each dialect subclass.
     *
     * @see FullTextIndex
     */
    public abstract <T extends PersistentRecord> boolean addFullTextSearch (
        Connection conn, DepotMarshaller<T> marshaller, FullTextIndex fts)
        throws SQLException;

    /**
     * Return true if the supplied column is an internal consideration of this {@link SQLBuilder},
     * e.g. PostgreSQL's full text search data is stored in a table column that should otherwise
     * not be visible to Depot; this method helps mask it.
     */
    public abstract boolean isPrivateColumn (
        String column, Map<String, FullTextIndex> fullTextIndexes);

    /**
     * Return true if the supplied index is an internal consideration of this {@link SQLBuilder},
     * e.g. PostgreSQL automatically creates indexes that end in _key for unique columns, and its
     * primary keys have indices ending in _pkey.
     */
    public abstract boolean isPrivateIndex (
        String index, Map<String, FullTextIndex> fullTextIndexes);

    /**
     * Figure out what full text search indexes already exist on this table and add the names of
     * those indexes to the supplied target set.
     */
    public abstract void getFtsIndexes (
        Iterable<String> columns, Iterable<String> indexes, Set<String> target);

    protected String buildQuery () {
        checkState(_buildVisitor != null, "Cannot prepare query until it's been built.");
        return _buildVisitor.getQuery();
    }

    protected PreparedStatement prepare (Connection conn, PreparedStatement stmt)
        throws SQLException
    {
        int argIx = 1;
        for (BuildVisitor.Bindable bindable : _buildVisitor.getBindables()) {
            try {
                bindable.doBind(conn, stmt, argIx);
            } catch (Exception e) {
                log.warning("Failed to bind statement argument", "argIx", argIx, e);
            }
            argIx ++;
        }

        if (PersistenceContext.DEBUG) {
            log.info("SQL: " + stmt.toString());
        }

        return stmt;
    }

    protected String nullify (String str) {
        return (str != null && str.length() > 0) ? str : null;
    }

    protected void maybeMutateForPrimitive (Field field, ColumnDefinition coldef)
    {
        // Java primitive types cannot be null, so we provide a default value for these columns
        // that matches Java's default for primitive types; however, if the column has a
        // generated value, don't provide a default because that will anger the database Gods
        if (field.getType().equals(Byte.TYPE) ||
            field.getType().equals(Short.TYPE) ||
            field.getType().equals(Integer.TYPE) ||
            field.getType().equals(Long.TYPE) ||
            field.getType().equals(Float.TYPE) ||
            field.getType().equals(Double.TYPE) ||
            ByteEnum.class.isAssignableFrom(field.getType())) {
            coldef.defaultValue = "0";

        } else if (field.getType().equals(Boolean.TYPE)) {
            coldef.defaultValue = getBooleanDefault();
        }
    }

    protected void maybeMutateForGeneratedValue (
            Field field, GeneratedValue genValue, ColumnDefinition coldef)
    {
        switch (genValue.strategy()) {
        case AUTO:
        case IDENTITY:
            coldef.type = getSerialType(field);
            coldef.unique = true;
            break;

        case SEQUENCE: // TODO
            throw new IllegalArgumentException(
                "SEQUENCE key generation strategy not yet supported.");
        case TABLE:
            // nothing to do here, it'll be handled later
            break;
        }
    }

    /**
     * The serial type for the field.
     */
    protected String getSerialType (Field field)
    {
        return "SERIAL";
    }

    /**
     * Returns the boolean literal that corresponds to false.
     */
    protected String getBooleanDefault ()
    {
        return "false";
    }

    /**
     * Overridden by subclasses to create a dialect-specific {@link BuildVisitor}.
     */
    protected abstract BuildVisitor getBuildVisitor ();

    /**
     * Overridden by subclasses to figure the dialect-specific SQL type of the given field.
     * @param length
     */
    protected abstract <T> String getColumnType (FieldMarshaller<?> fm, int length);

    /** The class that maps persistent classes to marshallers. */
    protected DepotTypes _types;

    protected QueryClause _clause;
    protected BuildVisitor _buildVisitor;
}
