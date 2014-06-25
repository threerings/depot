//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.samskivert.jdbc.ColumnDefinition;
import com.samskivert.jdbc.DatabaseLiaison;

import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.impl.FieldMarshaller;
import com.samskivert.depot.impl.Modifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.samskivert.depot.Log.log;

/**
 * Encapsulates the migration of a persistent record's database schema. These can be registered
 * with the {@link PersistenceContext} to effect hand-coded migrations between entity versions. The
 * modifier should override {@link #invoke} to perform its migrations. See {@link
 * PersistenceContext#registerMigration} for details on the migration process.
 *
 * <p> Note: these should only be used for actual schema changes (column additions, removals,
 * renames, retypes, etc.). It should not be used for data migration, use {@link DataMigration} for
 * that.
 */
public abstract class SchemaMigration extends Modifier
{
    /**
     * A convenient migration for dropping a column from an entity.
     */
    public static class Drop extends SchemaMigration
    {
        public Drop (int targetVersion, String columnName) {
            super(targetVersion);
            _columnName = columnName;
        }

        @Override
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            if (!liaison.tableContainsColumn(conn, _tableName, _columnName)) {
                // we'll accept this inconsistency
                log.warning(_tableName + "." + _columnName + " already dropped.");
                return 0;
            }

            log.info("Dropping '" + _columnName + "' from " + _tableName);
            return liaison.dropColumn(conn, _tableName, _columnName) ? 1 : 0;
        }

        protected String _columnName;
    }

    /**
     * A convenient migration for renaming a column in an entity.
     */
    public static class Rename extends SchemaMigration
    {
        public Rename (int targetVersion, String oldColumnName, ColumnExp<?> newColumn) {
            super(targetVersion);
            _oldColumnName = oldColumnName;
            _fieldName = newColumn.name;
        }

        @Override public boolean runBeforeDefault () {
            return true;
        }

        @Override
        public void init (String tableName, Map<String, FieldMarshaller<?>> marshallers) {
            super.init(tableName, marshallers);
            FieldMarshaller<?> fm = requireMarshaller(marshallers, _fieldName);
            _newColumnName = fm.getColumnName();
            _newColumnDef = fm.getColumnDefinition();
        }

        @Override
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            if (!liaison.tableContainsColumn(conn, _tableName, _oldColumnName)) {
                // if the new column already exists, log a warning, otherwise freak out
                checkArgument(liaison.tableContainsColumn(conn, _tableName, _newColumnName),
                              _tableName + " does not contain '" + _oldColumnName + "'");
                log.warning(_tableName + "." + _oldColumnName + " already renamed to " +
                            _newColumnName + ".");
                return 0;
            }

            // nor is this
            checkArgument(!liaison.tableContainsColumn(conn, _tableName, _newColumnName),
                          _tableName + " already contains '" + _newColumnName + "'");

            log.info("Renaming '" + _oldColumnName + "' to '" + _newColumnName + "' in: " +
                     _tableName);
            return liaison.renameColumn(
                conn, _tableName, _oldColumnName, _newColumnName, _newColumnDef) ? 1 : 0;
        }

        protected String _oldColumnName, _fieldName, _newColumnName;
        protected ColumnDefinition _newColumnDef;
    }

    /**
     * A convenient migration for changing the type of an existing field.
     */
    public static class Retype extends SchemaMigration
    {
        public Retype (int targetVersion, ColumnExp<?> column) {
            super(targetVersion);
            _fieldName = column.name;
        }

        @Override public boolean runBeforeDefault () {
            return false;
        }

        @Override
        public void init (String tableName, Map<String, FieldMarshaller<?>> marshallers) {
            super.init(tableName, marshallers);
            FieldMarshaller<?> marsh = requireMarshaller(marshallers, _fieldName);
            _columnName = marsh.getColumnName();
            _newColumnDef = marsh.getColumnDefinition();
        }

        @Override
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            log.info("Updating type of '" + _columnName + "' in " + _tableName);
            return liaison.changeColumn(conn, _tableName, _columnName, _newColumnDef.type,
                _newColumnDef.nullable, _newColumnDef.unique,
                _newColumnDef.defaultValue) ? 1 : 0;
        }

        protected String _fieldName, _columnName;
        protected ColumnDefinition _newColumnDef;
    }

    /**
     * A convenient migration for adding a new column that requires a default value to be specified
     * during the addition. Normally Depot will automatically handle column addition, but if you
     * have a column that normally does not have a default value but needs one when it is added to
     * a table with existing rows, you can use this migration.
     *
     * @see Column#defaultValue
     */
    public static class Add extends SchemaMigration
    {
        public Add (int targetVersion, ColumnExp<?> column, String defaultValue) {
            super(targetVersion);
            _fieldName = column.name;
            _defaultValue = defaultValue;
        }

        @Override public boolean runBeforeDefault () {
            return true;
        }

        @Override
        public void init (String tableName, Map<String, FieldMarshaller<?>> marshallers) {
            super.init(tableName, marshallers);
            FieldMarshaller<?> marsh = requireMarshaller(marshallers, _fieldName);
            _columnName = marsh.getColumnName();
            _newColumnDef = marsh.getColumnDefinition();
        }

        @Override
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            // override the default value in the column definition with the one provided
            ColumnDefinition defColumnDef = new ColumnDefinition(
                _newColumnDef.type, _newColumnDef.nullable,
                _newColumnDef.unique, _defaultValue);
            // first add the column with the overridden default value
            if (liaison.addColumn(conn, _tableName, _fieldName, defColumnDef, true)) {
                // then change the column to the permanent default value
                liaison.changeColumn(conn, _tableName, _fieldName, _newColumnDef.type,
                                     null, null, _newColumnDef.defaultValue);
                return 1;
            }
            return 0;
        }

        protected String _fieldName, _columnName, _defaultValue;
        protected ColumnDefinition _newColumnDef;
    }

    /**
     * A convenient migration for dropping a column from an entity.
     */
    public static class DropIndex extends SchemaMigration
    {
        public DropIndex (int targetVersion, String ixName) {
            super(targetVersion);
            _ixName = ixName;
        }

        @Override
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            String fullIxName = _tableName + "_" + _ixName;
            if (!liaison.tableContainsIndex(conn, _tableName, fullIxName)) {
                // we'll accept this inconsistency
                log.warning("No index '" + fullIxName + "' found on " + _tableName);
                return 0;
            }
            log.info("Dropping index '" + fullIxName + "' from " + _tableName);
            liaison.dropIndex(conn, _tableName, fullIxName);
            return 1;
        }

        protected String _ixName;
    }

    /**
     * If this method returns true, this migration will be run <b>before</b> the default
     * migrations, if false it will be run after.
     */
    public boolean runBeforeDefault ()
    {
        return true;
    }

    /**
     * When an Entity is being migrated, this method will be called to check whether this migration
     * should be run. The default implementation runs as long as the currentVersion is less than
     * the target version supplied to the migration at construct time.
     */
    public boolean shouldRunMigration (int currentVersion, int targetVersion)
    {
        return (currentVersion < _targetVersion);
    }

    /**
     * This is called to provide the migration with the name of the entity table and access to its
     * field marshallers prior to being invoked. This will <em>only</em> be called after this
     * migration has been determined to be runnable so one cannot rely on this method having been
     * called in {@link #shouldRunMigration}.
     */
    public void init (String tableName, Map<String, FieldMarshaller<?>> marshallers)
    {
        _tableName = tableName;
    }

    protected SchemaMigration (int targetVersion)
    {
        super();
        _targetVersion = targetVersion;
    }

    protected FieldMarshaller<?> requireMarshaller (
        Map<String, FieldMarshaller<?>> marshallers, String fieldName)
    {
        FieldMarshaller<?> marsh = marshallers.get(fieldName);
        checkArgument(marsh != null,
                      "'" + _tableName + "' does not contain field '" + fieldName + "'");
        return marsh;
    }

    protected int _targetVersion;
    protected String _tableName;
}
