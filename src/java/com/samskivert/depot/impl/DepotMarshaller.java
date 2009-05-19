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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.ColumnDefinition;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.Stats;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.annotation.Transient;
import com.samskivert.depot.annotation.UniqueConstraint;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.samskivert.depot.impl.clause.CreateIndexClause;

import static com.samskivert.depot.Log.log;

/**
 * Handles the marshalling and unmarshalling of persistent instances to JDBC primitives ({@link
 * PreparedStatement} and {@link ResultSet}).
 */
public class DepotMarshaller<T extends PersistentRecord>
{
    /** The name of a private static field that must be defined for all persistent object classes.
     * It is used to handle schema migration. If automatic schema migration is not desired, define
     * this field and set its value to -1. */
    public static final String SCHEMA_VERSION_FIELD = "SCHEMA_VERSION";

    /**
     * Creates a marshaller for the specified persistent object class.
     */
    public DepotMarshaller (Class<T> pClass, PersistenceContext context)
    {
        _pClass = pClass;

        Entity entity = pClass.getAnnotation(Entity.class);

        // see if this is a computed entity
        _computed = pClass.getAnnotation(Computed.class);
        if (_computed == null) {
            // if not, this class has a corresponding SQL table
            _tableName = DepotUtil.justClassName(_pClass);

            // see if there are Entity values specified
            if (entity != null) {
                if (entity.name().length() > 0) {
                    _tableName = entity.name();
                }
            }
        }

        // if the entity defines a new TableGenerator, map that in our static table as those are
        // shared across all entities
        TableGenerator generator = pClass.getAnnotation(TableGenerator.class);
        if (generator != null) {
            context.tableGenerators.put(generator.name(), generator);
        }

        boolean seenIdentityGenerator = false;

        // introspect on the class and create marshallers for persistent fields
        List<String> fields = Lists.newArrayList();
        for (Field field : _pClass.getFields()) {
            int mods = field.getModifiers();

            // check for a static constant schema version
            if (java.lang.reflect.Modifier.isStatic(mods) &&
                field.getName().equals(SCHEMA_VERSION_FIELD)) {
                try {
                    _schemaVersion = (Integer)field.get(null);
                } catch (Exception e) {
                    log.warning("Failed to read schema version [class=" + _pClass + "].", e);
                }
            }

            // the field must be public, non-static and non-transient
            if (!java.lang.reflect.Modifier.isPublic(mods) ||
                java.lang.reflect.Modifier.isStatic(mods) ||
                field.getAnnotation(Transient.class) != null) {
                continue;
            }

            FieldMarshaller<?> fm = FieldMarshaller.createMarshaller(field);
            _fields.put(field.getName(), fm);
            fields.add(field.getName());

            // check to see if this is our primary key
            if (field.getAnnotation(Id.class) != null) {
                if (_pkColumns == null) {
                    _pkColumns = Lists.newArrayList();
                }
                _pkColumns.add(fm);
            }

            // check if this field defines a new TableGenerator
            generator = field.getAnnotation(TableGenerator.class);
            if (generator != null) {
                context.tableGenerators.put(generator.name(), generator);
            }

            // check if this field is auto-generated
            GeneratedValue gv = fm.getGeneratedValue();
            if (gv != null) {
                // we can only do this on numeric fields
                Class<?> ftype = field.getType();
                boolean isNumeric = (
                    ftype.equals(Byte.TYPE) || ftype.equals(Byte.class) ||
                    ftype.equals(Short.TYPE) || ftype.equals(Short.class) ||
                    ftype.equals(Integer.TYPE) || ftype.equals(Integer.class) ||
                    ftype.equals(Long.TYPE) || ftype.equals(Long.class));
                if (!isNumeric) {
                    throw new IllegalArgumentException(
                        "Cannot use @GeneratedValue on non-numeric column: " + field.getName());
                }
                switch(gv.strategy()) {
                case AUTO:
                case IDENTITY:
                    if (seenIdentityGenerator) {
                        throw new IllegalArgumentException(
                            "Persistent records can have at most one AUTO/IDENTITY generator.");
                    }
                    _valueGenerators.put(field.getName(), new IdentityValueGenerator(gv, this, fm));
                    seenIdentityGenerator = true;
                    break;

                case TABLE:
                    String name = gv.generator();
                    generator = context.tableGenerators.get(name);
                    if (generator == null) {
                        throw new IllegalArgumentException(
                            "Unknown generator [generator=" + name + "]");
                    }
                    _valueGenerators.put(
                        field.getName(), new TableValueGenerator(generator, gv, this, fm));
                    break;

                case SEQUENCE: // TODO
                    throw new IllegalArgumentException(
                        "SEQUENCE key generation strategy not yet supported.");
                }
            }

            // check whether this field is indexed
            Index index = field.getAnnotation(Index.class);
            if (index != null) {
                String ixName = index.name().equals("") ? field.getName() + "Index" : index.name();
                _indexes.add(buildIndex(ixName, index.unique(),
                                        new ColumnExp(_pClass, field.getName())));
            }

            // if this column is marked as unique, that also means we create an index
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.unique()) {
                _indexes.add(buildIndex(field.getName() + "Index", true,
                                        new ColumnExp(_pClass, field.getName())));
            }
        }

        // if we did not find a schema version field, freak out (but not for computed records, for
        // whom there is no table)
        if (_tableName != null && _schemaVersion <= 0) {
            throw new IllegalStateException(
                pClass.getName() + "." + SCHEMA_VERSION_FIELD + " must be greater than zero.");
        }

        // generate our full list of fields/columns for use in queries
        _allFields = fields.toArray(new String[fields.size()]);

        // now check for @Entity annotations on the entire superclass chain
        Class<? extends PersistentRecord> iterClass = pClass.asSubclass(PersistentRecord.class);
        do {
            entity = iterClass.getAnnotation(Entity.class);
            if (entity != null) {
                // add any indices needed for uniqueness constraints
                for (UniqueConstraint constraint : entity.uniqueConstraints()) {
                    ColumnExp[] colExps = new ColumnExp[constraint.fields().length];
                    int ii = 0;
                    for (String field : constraint.fields()) {
                        FieldMarshaller<?> fm = _fields.get(field);
                        if (fm == null) {
                            throw new IllegalArgumentException(
                                "Unknown unique constraint field: " + field);
                        }
                        colExps[ii ++] = new ColumnExp(_pClass, field);
                    }
                    _indexes.add(buildIndex(constraint.name(), true, colExps));
                }

                // add any explicit multicolumn or complex indices
                for (Index index : entity.indices()) {
                    _indexes.add(buildIndex(index.name(), index.unique()));
                }

                // note any FTS indices
                for (FullTextIndex fti : entity.fullTextIndices()) {
                    if (_fullTextIndexes.containsKey(fti.name())) {
                        continue;
                    }
                    _fullTextIndexes.put(fti.name(), fti);
                }
            }

            iterClass = iterClass.getSuperclass().asSubclass(PersistentRecord.class);

        } while (PersistentRecord.class.isAssignableFrom(iterClass) &&
                 !PersistentRecord.class.equals(iterClass));
    }

    /**
     * Returns the persistent class this is object is a marshaller for.
     */
    public Class<T> getPersistentClass ()
    {
       return _pClass;
    }

    /**
     * Returns the @Computed annotation definition of this entity, or null if none.
     */

    public Computed getComputed ()
    {
        return _computed;
    }

    /**
     * Returns the name of the table in which persistent instances of our class are stored. By
     * default this is the classname of the persistent object without the package.
     */
    public String getTableName ()
    {
        return _tableName;
    }

    /**
     * Returns all the persistent fields of our class, in definition order.
     */
    public String[] getFieldNames ()
    {
        return _allFields;
    }

    /**
     * Returns all the persistent fields that correspond to concrete table columns.
     */
    public String[] getColumnFieldNames ()
    {
        return _columnFields;
    }

    /**
     * Return the {@link FullTextIndex} registered under the given name.
     *
     * @exception IllegalArgumentException thrown if the requested full text index does not exist
     * on this record.
     */
    public FullTextIndex getFullTextIndex (String name)
    {
        FullTextIndex fti = _fullTextIndexes.get(name);
        if (fti == null) {
            throw new IllegalStateException("Persistent class missing full text index " +
                                            "[class=" + _pClass + ", index=" + name + "]");
        }
        return fti;
    }

    /**
     * Returns the {@link FieldMarshaller} for a named field on our persistent class.
     */
    public FieldMarshaller<?> getFieldMarshaller (String fieldName)
    {
        return _fields.get(fieldName);
    }

    /**
     * Returns true if our persistent object defines a primary key.
     */
    public boolean hasPrimaryKey ()
    {
        return (_pkColumns != null);
    }

    /**
     * Returns the {@link ValueGenerator} objects used to automatically generate field values for
     * us when a new record is inserted.
     */
    public Iterable<ValueGenerator> getValueGenerators ()
    {
        return _valueGenerators.values();
    }

    /**
     * Return the names of the columns that constitute the primary key of our associated persistent
     * record.
     */
    public String[] getPrimaryKeyFields ()
    {
        String[] pkcols = new String[_pkColumns.size()];
        for (int ii = 0; ii < pkcols.length; ii ++) {
            pkcols[ii] = _pkColumns.get(ii).getField().getName();
        }
        return pkcols;
    }

    /**
     * Returns a key configured with the primary key of the supplied object. If all the fields are
     * null, this method returns null. An exception is thrown if some of the fields are null and
     * some are not, or if the object does not declare a primary key.
     */
    public Key<T> getPrimaryKey (Object object)
    {
        return getPrimaryKey(object, true);
    }

    /**
     * Returns a key configured with the primary key of the supplied object. If all the fields are
     * null, this method returns null. If some of the fields are null and some are not, an
     * exception is thrown. If the object does not declare a primary key and the second argument is
     * true, this method throws an exception; if it's false, the method returns null.
     */
    public Key<T> getPrimaryKey (Object object, boolean requireKey)
    {
        if (!hasPrimaryKey()) {
            if (requireKey) {
                throw new UnsupportedOperationException(
                    _pClass.getName() + " does not define a primary key");
            }
            return null;
        }

        try {
            Comparable<?>[] values = new Comparable<?>[_pkColumns.size()];
            int nulls = 0, zeros = 0;
            for (int ii = 0; ii < _pkColumns.size(); ii++) {
                FieldMarshaller<?> field = _pkColumns.get(ii);
                values[ii] = (Comparable<?>)field.getField().get(object);
                if (values[ii] == null) {
                    nulls++;
                } else if (values[ii] instanceof Number && ((Number)values[ii]).intValue() == 0) {
                    nulls++; // zeros are considered nulls; see below
                    zeros++;
                }
            }

            // make sure the keys are all null or all non-null
            if (nulls == 0) {
                return new Key<T>(_pClass, values);
            } else if (nulls == values.length) {
                return null;
            } else if (nulls == zeros) {
                // we also allow primary keys where there are zero-valued primitive primary key
                // columns as along as there is at least one non-zero valued additional key column;
                // this is a compromise that allows sensible things like (id=99, type=0) but
                // unfortunately also allows less sensible things like (id=0, type=5) while
                // continuing to disallow the dangerous (id=0)
                return new Key<T>(_pClass, values);
            }

            // throw an informative error message
            StringBuilder keys = new StringBuilder();
            for (int ii = 0; ii < _pkColumns.size(); ii++) {
                keys.append(", ").append(_pkColumns.get(ii).getField().getName());
                keys.append("=").append(values[ii]);
            }
            throw new IllegalArgumentException("Primary key fields are mixed null and non-null " +
                                               "[class=" + _pClass.getName() + keys + "].");

        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * Creates a primary key record for the type of object handled by this marshaller, using the
     * supplied primary key value. This is only allowed for records with single column keys.
     */
    public Key<T> makePrimaryKey (Comparable<?> value)
    {
        if (!hasPrimaryKey()) {
            throw new UnsupportedOperationException(
                getClass().getName() + " does not define a primary key");
        }
        return new Key<T>(_pClass, new Comparable<?>[] { value });
    }

    /**
     * Creates a primary key record for the type of object handled by this marshaller, using the
     * supplied result set.
     */
    public Key<T> makePrimaryKey (ResultSet rs)
        throws SQLException
    {
        if (!hasPrimaryKey()) {
            throw new UnsupportedOperationException(
                getClass().getName() + " does not define a primary key");
        }
        Comparable<?>[] values = new Comparable<?>[_pkColumns.size()];
        for (int ii = 0; ii < _pkColumns.size(); ii++) {
            Object keyValue = _pkColumns.get(ii).getFromSet(rs);
            if (!(keyValue instanceof Comparable<?>)) {
                throw new IllegalArgumentException("Key field must be Comparable<?> [field=" +
                                                   _pkColumns.get(ii).getColumnName() + "]");
            }
            values[ii] = (Comparable<?>) keyValue;
        }
        return new Key<T>(_pClass, values);
    }

    /**
     * Returns true if this marshaller has been initialized ({@link #init} has been called), its
     * migrations run and it is ready for operation. False otherwise.
     */
    public boolean isInitialized ()
    {
        return _meta != null;
    }

    /**
     * Initializes the table used by this marshaller. This is called automatically by the {@link
     * PersistenceContext} the first time an entity is used. If the table does not exist, it will
     * be created. If the schema version specified by the persistent object is newer than the
     * database schema, it will be migrated.
     */
    public void init (PersistenceContext ctx, DepotMetaData meta)
        throws DatabaseException
    {
        if (_meta != null) { // sanity check
            throw new IllegalStateException(
                "Cannot re-initialize marshaller [type=" + _pClass + "].");
        }
        _meta = meta;

        final SQLBuilder builder = ctx.getSQLBuilder(new DepotTypes(ctx, _pClass));

        // perform the context-sensitive initialization of the field marshallers
        for (FieldMarshaller<?> fm : _fields.values()) {
            fm.init(builder);
        }

        // if we have no table (i.e. we're a computed entity), we have nothing to create
        if (getTableName() == null) {
            return;
        }

        // figure out the list of fields that correspond to actual table columns and generate the
        // SQL used to create and migrate our table (unless we're a computed entity)
        _columnFields = new String[_allFields.length];
        ColumnDefinition[] declarations = new ColumnDefinition[_allFields.length];
        int jj = 0;
        for (String field : _allFields) {
            FieldMarshaller<?> fm = _fields.get(field);
            // include all persistent non-computed fields
            ColumnDefinition colDef = fm.getColumnDefinition();
            if (colDef != null) {
                _columnFields[jj] = field;
                declarations[jj] = colDef;
                jj ++;
            }
        }
        _columnFields = ArrayUtil.splice(_columnFields, jj);
        declarations = ArrayUtil.splice(declarations, jj);

        // determine whether or not this record has ever been seen
        int currentVersion = _meta.getVersion(getTableName(), false);
        if (currentVersion == -1) {
            log.info("Creating initial version record for " + _pClass.getName() + ".");
            // if not, create a version entry with version zero
            _meta.initializeVersion(getTableName());
        }

        // now check whether we need to migrate our database schema
        while (true) {
            if (currentVersion >= _schemaVersion) {
                // TODO: we used to check for staleness here, but that's slow; currently we do it
                // only after migration, we should reinstate a check maybe the first time a table
                // is read or something so that developers are more likely to get the warning
                return;
            }

            // try to update migratingVersion to the new version to indicate to other processes
            // that we are handling the migration and that they should wait
            if (_meta.updateMigratingVersion(getTableName(), _schemaVersion, 0)) {
                break; // we got the lock, let's go
            }

            // we didn't get the lock, so wait 5 seconds and then check to see if the other process
            // finished the update or failed in which case we'll try to grab the lock ourselves
            try {
                log.info("Waiting on migration lock for " + _pClass.getName() + ".");
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                throw new DatabaseException("Interrupted while waiting on migration lock.");
            }

            currentVersion = _meta.getVersion(getTableName(), true);
        }

        // fetch all relevant information regarding our table from the database
        TableMetaData metaData = TableMetaData.load(ctx, getTableName());

        try {
            if (!metaData.tableExists) {
                // if the table does not exist, create it
                createTable(ctx, builder, declarations);
                metaData = TableMetaData.load(ctx, getTableName());
            } else {
                // if it does exist, run our migrations
                metaData = runMigrations(ctx, metaData, builder, currentVersion);
            }

            // check for stale columns now that the table is up to date
            checkForStaleness(metaData, ctx, builder);

            // and update our version in the schema version table
            _meta.updateVersion(getTableName(), _schemaVersion);

        } finally {
            // set our migrating version back to zero
            try {
                if (!_meta.updateMigratingVersion(getTableName(), 0, _schemaVersion)) {
                    log.warning("Failed to restore migrating version to zero!", "record", _pClass);
                }
            } catch (Exception e) {
                log.warning("Failure restoring migrating version! Bad bad!", "record", _pClass, e);
            }
        }
    }

    /**
     * This is called by the persistence context to register a migration for the entity managed by
     * this marshaller.
     */
    public void registerMigration (SchemaMigration migration)
    {
        _schemaMigs.add(migration);
    }

    /**
     * Creates a persistent object from the supplied result set. The result set must have come from
     * a properly constructed query (see {@link BuildVisitor}).
     */
    public T createObject (ResultSet rs)
        throws SQLException
    {
        try {
            // first, build a set of the fields that we actually received
            Set<String> fields = Sets.newHashSet();
            ResultSetMetaData metadata = rs.getMetaData();
            for (int ii = 1; ii <= metadata.getColumnCount(); ii ++) {
               fields.add(metadata.getColumnName(ii));
            }

            // then create and populate the persistent object
            T po = _pClass.newInstance();
            for (FieldMarshaller<?> fm : _fields.values()) {
                if (!fields.contains(fm.getColumnName())) {
                    // this field was not in the result set, make sure that's OK
                    if (fm.getComputed() != null && !fm.getComputed().required()) {
                        continue;
                    }
                    throw new SQLException("ResultSet missing field: " + fm.getField().getName());
                }
                fm.getAndWriteToObject(rs, po);
            }
            return po;

        } catch (SQLException sqe) {
            // pass this on through
            throw sqe;

        } catch (Exception e) {
            String errmsg = "Failed to unmarshall persistent object [class=" +
                _pClass.getName() + "]";
            throw (SQLException)new SQLException(errmsg).initCause(e);
        }
    }

    /**
     * Go through the registered {@link ValueGenerator}s for our persistent object and run the ones
     * that match the current postFactum phase, filling in the fields on the supplied object while
     * we go.
     *
     * The return value is only non-empty for the !postFactum phase, in which case it is a set of
     * field names that are associated with {@link IdentityValueGenerator}, because these need
     * special handling in the INSERT (specifically, 'DEFAULT' must be supplied as a value in the
     * eventual SQL).
     */
    public Set<String> generateFieldValues (
        Connection conn, DatabaseLiaison liaison, Object po, boolean postFactum)
    {
        Set<String> idFields = Sets.newHashSet();

        for (ValueGenerator vg : _valueGenerators.values()) {
            if (!postFactum && vg instanceof IdentityValueGenerator) {
                idFields.add(vg.getFieldMarshaller().getField().getName());
            }
            if (vg.isPostFactum() != postFactum) {
                continue;
            }

            try {
                int nextValue = vg.nextGeneratedValue(conn, liaison);
                vg.getFieldMarshaller().getField().set(po, nextValue);

            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to assign primary key [type=" + _pClass + "]", e);
            }
        }
        return idFields;
    }

    protected void createTable (PersistenceContext ctx, final SQLBuilder builder,
                                final ColumnDefinition[] declarations)
        throws DatabaseException
    {
        log.info("Creating initial table '" + getTableName() + "'.");

        ctx.invoke(new Modifier() {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                // create the table
                String[] primaryKeyColumns = null;
                if (_pkColumns != null) {
                    primaryKeyColumns = new String[_pkColumns.size()];
                    for (int ii = 0; ii < primaryKeyColumns.length; ii ++) {
                        primaryKeyColumns[ii] = _pkColumns.get(ii).getColumnName();
                    }
                }
                liaison.createTableIfMissing(
                    conn, getTableName(), fieldsToColumns(_columnFields),
                    declarations, null, primaryKeyColumns);

                // add its indexen
                for (CreateIndexClause iclause : _indexes) {
                    execute(conn, builder, iclause);
                }

                // create our value generators
                for (ValueGenerator vg : _valueGenerators.values()) {
                    vg.create(conn, liaison);
                }

                // and its full text search indexes
                for (FullTextIndex fti : _fullTextIndexes.values()) {
                    builder.addFullTextSearch(conn, DepotMarshaller.this, fti);
                }

                return 0;
            }
        });
    }

    protected int execute (Connection conn, SQLBuilder builder, QueryClause clause)
        throws SQLException
    {
        if (builder.newQuery(clause)) {
            PreparedStatement stmt = builder.prepare(conn);
            try {
                return stmt.executeUpdate();
            } finally {
                JDBCUtil.close(stmt);
            }
        }
        return 0;
    }

    protected TableMetaData runMigrations (final PersistenceContext ctx, TableMetaData metaData,
                                           final SQLBuilder builder, int currentVersion)
        throws DatabaseException
    {
        log.info("Migrating " + getTableName() + " from " + currentVersion + " to " +
                 _schemaVersion + "...");

        if (_schemaMigs.size() > 0) {
            // run our pre-default-migrations
            for (SchemaMigration migration : _schemaMigs) {
                if (migration.runBeforeDefault() &&
                        migration.shouldRunMigration(currentVersion, _schemaVersion)) {
                    migration.init(getTableName(), _fields);
                    ctx.invoke(migration);
                }
            }

            // we don't know what the pre-migrations did so we have to re-read metadata
            metaData = TableMetaData.load(ctx, getTableName());
        }

        // figure out which columns we have in the table now, so that when all is said and done we
        // can see what new columns we have in the table and run the creation code for any value
        // generators that are defined on those columns (we can't just track the columns we add in
        // our automatic migrations because someone might register custom migrations that add
        // columns specially)
        Set<String> preMigrateColumns = Sets.newHashSet(metaData.tableColumns);

        // add any missing columns
        for (String fname : _columnFields) {
            final FieldMarshaller<?> fmarsh = _fields.get(fname);
            if (metaData.tableColumns.remove(fmarsh.getColumnName())) {
                continue;
            }

            // otherwise add the column
            final ColumnDefinition coldef = fmarsh.getColumnDefinition();
            log.info("Adding column to " + getTableName() + ": " + fmarsh.getColumnName());
            ctx.invoke(new Modifier.Simple() {
                @Override protected String createQuery (DatabaseLiaison liaison) {
                    return "alter table " + liaison.tableSQL(getTableName()) +
                        " add column " + liaison.columnSQL(fmarsh.getColumnName()) + " " +
                        liaison.expandDefinition(coldef);
                }
            });

            // if the column is a TIMESTAMP or DATETIME column, we need to run a special query to
            // update all existing rows to the current time because MySQL annoyingly assigns
            // TIMESTAMP columns a value of "0000-00-00 00:00:00" regardless of whether we
            // explicitly provide a "DEFAULT" value for the column or not, and DATETIME columns
            // cannot accept CURRENT_TIME or NOW() defaults at all.
            if (!coldef.nullable && (coldef.type.equalsIgnoreCase("timestamp") ||
                                     coldef.type.equalsIgnoreCase("datetime"))) {
                log.info("Assigning current time to " + fmarsh.getColumnName() + ".");
                ctx.invoke(new Modifier.Simple() {
                    @Override protected String createQuery (DatabaseLiaison liaison) {
                        // TODO: is NOW() standard SQL?
                        return "update " + liaison.tableSQL(getTableName()) +
                            " set " + liaison.columnSQL(fmarsh.getColumnName()) + " = NOW()";
                    }
                });
            }
        }

        // add or remove the primary key as needed
        if (hasPrimaryKey() && metaData.pkName == null) {
            log.info("Adding primary key.");
            ctx.invoke(new Modifier() {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException {
                    liaison.addPrimaryKey(
                        conn, getTableName(), fieldsToColumns(getPrimaryKeyFields()));
                    return 0;
                }
            });

        } else if (!hasPrimaryKey() && metaData.pkName != null) {
            final String pkName = metaData.pkName;
            log.info("Dropping primary key: " + pkName);
            ctx.invoke(new Modifier() {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException {
                    liaison.dropPrimaryKey(conn, getTableName(), pkName);
                    return 0;
                }
            });
        }

        // add any named indices that exist on the record but not yet on the table
        for (final CreateIndexClause iclause : _indexes) {
            if (metaData.indexColumns.containsKey(iclause.getName())) {
                metaData.indexColumns.remove(iclause.getName()); // this index already exists
                continue;
            }
            // but this is a new, named index, so we create it
            log.info("Creating new index: " + iclause.getName());
            ctx.invoke(new Modifier() {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException {
                    execute(conn, builder, iclause);
                    return 0;
                }
            });
        }

        // next we create any full text search indexes that exist on the record but not in the
        // table, first step being to do a dialect-sensitive enumeration of existing indexes
        Set<String> tableFts = Sets.newHashSet();
        builder.getFtsIndexes(metaData.tableColumns, metaData.indexColumns.keySet(), tableFts);

        // then iterate over what should be there
        for (final FullTextIndex recordFts : _fullTextIndexes.values()) {
            if (tableFts.contains(recordFts.name())) {
                // the table already contains this one
                continue;
            }

            // but not this one, so let's create it
            ctx.invoke(new Modifier() {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException {
                    builder.addFullTextSearch(conn, DepotMarshaller.this, recordFts);
                    return 0;
                }
            });
        }

        // we do not auto-remove columns but rather require that SchemaMigration.Drop records be
        // registered by hand to avoid accidentally causing the loss of data

        // we don't auto-remove indices either because we'd have to sort out the potentially
        // complex origins of an index (which might be because of a @Unique column or maybe the
        // index was hand defined in a @Column clause)

        // run our post-default-migrations
        for (SchemaMigration migration : _schemaMigs) {
            if (!migration.runBeforeDefault() &&
                migration.shouldRunMigration(currentVersion, _schemaVersion)) {
                migration.init(getTableName(), _fields);
                ctx.invoke(migration);
            }
        }

        // now reload our table metadata so that we can see what columns we have now
        metaData = TableMetaData.load(ctx, getTableName());

        // initialize value generators for any columns that have been newly added
        for (String column : metaData.tableColumns) {
            if (preMigrateColumns.contains(column)) {
                continue;
            }

            // see if we have a value generator for this new column
            final ValueGenerator valgen = _valueGenerators.get(column);
            if (valgen == null) {
                continue;
            }

            // note: if someone renames a column that has an identity value generator, things will
            // break because Postgres automatically creates a table_column_seq sequence that is
            // used to generate values for that column and god knows what happens when that is
            // renamed; plus we're potentially going to try to reinitialize it if it has a non-zero
            // initialValue which will use the new column name to obtain the sequence name which
            // ain't going to work either; we punt!
            ctx.invoke(new Modifier() {
                @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException {
                    valgen.create(conn, liaison);
                    return 0;
                }
            });
        }

        return metaData;
    }

    protected CreateIndexClause buildIndex (String name, boolean unique)
    {
        Method method;
        try {
            method = _pClass.getMethod(name);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException(
                "Index flagged as complex, but no defining method '" + name + "' found.", nsme);
        }
        try {
            return buildIndex(name, unique, method.invoke(null));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Error calling index definition method '" + name + "'", e);
        }
    }

    protected CreateIndexClause buildIndex (String name, boolean unique, Object config)
    {
        List<Tuple<SQLExpression, Order>> definition = Lists.newArrayList();
        if (config instanceof ColumnExp) {
            definition.add(new Tuple<SQLExpression, Order>((ColumnExp)config, Order.ASC));
        } else if (config instanceof ColumnExp[]) {
            for (ColumnExp column : (ColumnExp[])config) {
                definition.add(new Tuple<SQLExpression, Order>(column, Order.ASC));
            }
        } else if (config instanceof SQLExpression) {
            definition.add(new Tuple<SQLExpression, Order>((SQLExpression)config, Order.ASC));
        } else if (config instanceof Tuple) {
            @SuppressWarnings("unchecked") Tuple<SQLExpression, Order> tuple =
                (Tuple<SQLExpression, Order>)config;
            definition.add(tuple);
        } else if (config instanceof List) {
            @SuppressWarnings("unchecked") List<Tuple<SQLExpression, Order>> defs =
                (List<Tuple<SQLExpression, Order>>)config;
            definition.addAll(defs);
        } else {
            throw new IllegalArgumentException(
                "Method '" + name + "' must return ColumnExp[], SQLExpression or " +
                "List<Tuple<SQLExpression, Order>>");
        }
        return new CreateIndexClause(_pClass, getTableName() + "_" + name, unique, definition);
    }

    // translate an array of field names to an array of column names
    protected String[] fieldsToColumns (String[] fields)
    {
        String[] columns = new String[fields.length];
        for (int ii = 0; ii < columns.length; ii ++) {
            FieldMarshaller<?> fm = _fields.get(fields[ii]);
            if (fm == null) {
                throw new IllegalArgumentException(
                    "Unknown field on record [field=" + fields[ii] + ", class=" + _pClass + "]");
            }
            columns[ii] = fm.getColumnName();
        }
        return columns;
    }

    /**
     * Checks that there are no database columns for which we no longer have Java fields.
     */
    protected void checkForStaleness (
        TableMetaData meta, PersistenceContext ctx, SQLBuilder builder)
        throws DatabaseException
    {
        for (String fname : _columnFields) {
            FieldMarshaller<?> fmarsh = _fields.get(fname);
            meta.tableColumns.remove(fmarsh.getColumnName());
        }
        for (String column : meta.tableColumns) {
            if (builder.isPrivateColumn(column)) {
                continue;
            }
            log.warning(getTableName() + " contains stale column '" + column + "'.");
        }
    }

    protected static class TableMetaData
    {
        public boolean tableExists;
        public Set<String> tableColumns = Sets.newHashSet();
        public Map<String, Set<String>> indexColumns = Maps.newHashMap();
        public String pkName;
        public Set<String> pkColumns = Sets.newHashSet();

        public static TableMetaData load (PersistenceContext ctx, final String tableName)
            throws DatabaseException
        {
            return ctx.invoke(new Query.Trivial<TableMetaData>() {
                public TableMetaData invoke (PersistenceContext ctx, Connection conn,
                                             DatabaseLiaison dl) throws SQLException {
                    return new TableMetaData(conn.getMetaData(), tableName);
                }
                public void updateStats (Stats stats) {
                    // nothing doing
                }
            });
        }

        public TableMetaData (DatabaseMetaData meta, String tableName)
            throws SQLException
        {
            tableExists = meta.getTables(null, null, tableName, null).next();
            if (!tableExists) {
                return;
            }

            ResultSet rs = meta.getColumns(null, null, tableName, "%");
            while (rs.next()) {
                tableColumns.add(rs.getString("COLUMN_NAME"));
            }

            rs = meta.getIndexInfo(null, null, tableName, false, false);
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                Set<String> set = indexColumns.get(indexName);
                if (rs.getBoolean("NON_UNIQUE")) {
                    // not a unique index: just make sure there's an entry in the keyset
                    if (set == null) {
                        indexColumns.put(indexName, null);
                    }

                } else {
                    // for unique indices we collect the column names
                    if (set == null) {
                        set = Sets.newHashSet();
                        indexColumns.put(indexName, set);
                    }
                    set.add(rs.getString("COLUMN_NAME"));
                }
            }

            rs = meta.getPrimaryKeys(null, null, tableName);
            while (rs.next()) {
                pkName = rs.getString("PK_NAME");
                pkColumns.add(rs.getString("COLUMN_NAME"));
            }
        }

        @Override
        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Provides access to certain internal metadata. */
    protected DepotMetaData _meta;

    /** The persistent object class that we manage. */
    protected Class<T> _pClass;

    /** The name of our persistent object table. */
    protected String _tableName;

    /** The @Computed annotation of this entity, or null. */
    protected Computed _computed;

    /** A mapping of field names to value generators for that field. */
    protected Map<String, ValueGenerator> _valueGenerators = Maps.newHashMap();

    /** A field marshaller for each persistent field in our object. */
    protected Map<String, FieldMarshaller<?>> _fields = Maps.newHashMap();

    /** The field marshallers for our persistent object's primary key columns or null if it did not
     * define a primary key. */
    protected List<FieldMarshaller<?>> _pkColumns;

    /** The persisent fields of our object, in definition order. */
    protected String[] _allFields;

    /** The fields of our object with directly corresponding table columns. */
    protected String[] _columnFields;

    /** The indexes defined for this record. */
    protected List<CreateIndexClause> _indexes = Lists.newArrayList();

    /** Any full text indices defined on this entity. */
    protected Map<String, FullTextIndex> _fullTextIndexes = Maps.newHashMap();

    /** The version of our persistent object schema as specified in the class definition. */
    protected int _schemaVersion = -1;

    /** A list of hand registered schema migrations to run prior to doing the default migration. */
    protected List<SchemaMigration> _schemaMigs = Lists.newArrayList();
}
