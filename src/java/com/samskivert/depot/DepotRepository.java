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

package com.samskivert.depot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.ArrayUtil;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;

import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.InsertClause;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;

import com.samskivert.depot.impl.DepotMarshaller;
import com.samskivert.depot.impl.DepotMigrationHistoryRecord;
import com.samskivert.depot.impl.DepotTypes;
import com.samskivert.depot.impl.FindAllKeysQuery;
import com.samskivert.depot.impl.FindAllQuery;
import com.samskivert.depot.impl.FindOneQuery;
import com.samskivert.depot.impl.Modifier.*;
import com.samskivert.depot.impl.Modifier;
import com.samskivert.depot.impl.SQLBuilder;
import com.samskivert.depot.impl.clause.DeleteClause;
import com.samskivert.depot.impl.clause.UpdateClause;

import static com.samskivert.depot.Log.log;

/**
 * Provides a base for classes that provide access to persistent objects. Also defines the
 * mechanism by which all persistent queries and updates are routed through the distributed cache.
 */
public abstract class DepotRepository
{
    public enum CacheStrategy {
        /** Completely bypass the cache for this query. */
        NONE,

        /** Use the {@link #SHORT_KEYS} strategy if possible, else revert to {@link #NONE}. */
        BEST,

        /**
         * Resolve this collection query in two steps: first we enumerate the primary keys for
         * all the records that satisfy the query, then we acquire the actual data corresponding
         * to each key -- first by consulting the cache, and then only loading from the database
         * the records for the keys that were not located in the cache.
         *
         * Note: This strategy may not be used on @Computed records, for records that do not in
         * fact have a primary key, or for queries that use @FieldOverrides.
         */
        RECORDS,

        /**
         * This strategy is identical to {@link #RECORDS}, but we also cache the keyset fetched
         * in the first pass. This makes it much more efficient, but also less reliable because
         * there is no invalidation of the keyset query: If records are inserted, deleted or
         * modified, cached keysets will not be updated.
         *
         * Keysets cached using this strategy should have a short time-to-live.
         *
         * Note: This strategy may not be used on @Computed records, for records that do not in
         * fact have a primary key, or for queries that use @FieldOverrides.
         */
        SHORT_KEYS,

        /**
         * This strategy is identical to {@link #RECORDS}, but we also cache the keyset fetched
         * in the first pass. This makes it much more efficient, but also less reliable because
         * there is no invalidation of the keyset query: If records are inserted, deleted or
         * modified, cached keysets will not be updated.
         *
         * Keysets cached using this strategy may have a long time-to-live.
         *
         * Note: This strategy may not be used on @Computed records, for records that do not in
         * fact have a primary key, or for queries that use @FieldOverrides.
         */
        LONG_KEYS,

        /**
         * This cache strategy is direct and explicit, eschewing the dual phases of the {@link
         * #RECORDS} and {@link #SHORT_KEYS} approaches. However, before the database is invoked at
         * all, we consult the cache hoping to find the entire result set already stashed away in
         * there, using the entire query as the key. If we failed to find it, we execute the query
         * and update the cache with the result.
         *
         * This strategy has none of the limitations of {@link #SHORT_KEYS} and can be used with
         * key-less and @Computed records and arbitrarily complicated queries. Note however that as
         * with {@link #SHORT_KEYS}, there is no automatic invalidation. It is also potentially
         * very memory intensive.
         */
        CONTENTS
    };

    /**
     * Creates a repository with the supplied persistence context. Any schema migrations needed by
     * this repository should be registered in its constructor. A repository should <em>not</em>
     * perform any actual database operations in its constructor, only register schema
     * migrations. Initialization related database operations should be performed in {@link #init}.
     */
    protected DepotRepository (PersistenceContext context)
    {
        _ctx = context;
        _ctx.repositoryCreated(this);
    }

    /**
     * Creates a repository with the supplied connection provider and its own private persistence
     * context. This should generally not be used for new systems, and is only included to
     * facilitate the integration of small numbers of Depot-based repositories into systems using
     * the older samskivert SimpleRepository system.
     */
    protected DepotRepository (ConnectionProvider conprov)
    {
        _ctx = new PersistenceContext();
        _ctx.init(getClass().getName(), conprov, null);
        _ctx.repositoryCreated(this);
    }

    /**
     * Resolves all persistent records registered to this repository (via {@link
     * #getManagedRecords}. This will be done before the repository is initialized via {@link
     * #init}.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected void resolveRecords ()
        throws DatabaseException
    {
        Set<Class<? extends PersistentRecord>> classes = Sets.newHashSet();
        getManagedRecords(classes);
        for (Class<? extends PersistentRecord> rclass : classes) {
            _ctx.getMarshaller(rclass);
        }
    }

    /**
     * Provides a place where a repository can perform any initialization that requires database
     * operations.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected void init ()
        throws DatabaseException
    {
        // run any registered data migrations
        for (DataMigration migration : _dataMigs) {
            runMigration(migration);
        }
        _dataMigs = null; // note that we've been initialized
    }

    /**
     * Registers a data migration for this repository. This migration will only be run once and its
     * unique identifier will be stored persistently to ensure that it is never run again on the
     * same database. Nonetheless, migrations should strive to be idempotent because someone might
     * come along and create a brand new system installation and all registered migrations will be
     * run once on the freshly created database. As with all database migrations, understand
     * clearly how the process works and think about edge cases when creating a migration.
     *
     * <p> See {@link PersistenceContext#registerMigration} for details on how schema migrations
     * operate and how they might interact with data migrations.
     */
    protected void registerMigration (DataMigration migration)
    {
        if (_dataMigs == null) {
            // we've already been initialized, so we have to run this migration immediately
            runMigration(migration);
        } else {
            _dataMigs.add(migration);
        }
    }

    /**
     * Adds the persistent classes used by this repository to the supplied set.
     */
    protected abstract void getManagedRecords (Set<Class<? extends PersistentRecord>> classes);

    /**
     * Loads the persistent object that matches the specified primary key.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> T load (Key<T> key, QueryClause... clauses)
        throws DatabaseException
    {
        return load(key, CacheStrategy.BEST, clauses);
    }

    /**
     * Loads the persistent object that matches the specified primary key.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> T load (Key<T> key, CacheStrategy strategy,
                                                   QueryClause... clauses)
        throws DatabaseException
    {
        clauses = ArrayUtil.append(clauses, key);
        return _ctx.invoke(new FindOneQuery<T>(_ctx, key.getPersistentClass(), strategy, clauses));
    }

    /**
     * Loads the first persistent object that matches the supplied query clauses.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, QueryClause... clauses)
        throws DatabaseException
    {
        return load(type, CacheStrategy.BEST, clauses);
    }

    /**
     * Loads the first persistent object that matches the supplied query clauses.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, CacheStrategy strategy,
                                                   QueryClause... clauses)
        throws DatabaseException
    {
        return _ctx.invoke(new FindOneQuery<T>(_ctx, type, strategy, clauses));
    }

    /**
     * Loads up all persistent records that match the supplied set of raw primary keys.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<T> loadAll (
        Class<T> type, Collection<? extends Comparable<?>> primaryKeys)
        throws DatabaseException
    {
        // convert the raw keys into real key records
        DepotMarshaller<T> marsh = _ctx.getMarshaller(type);
        List<Key<T>> keys = Lists.newArrayList();
        for (Comparable<?> key : primaryKeys) {
            keys.add(marsh.makePrimaryKey(key));
        }
        return loadAll(keys);
    }

    /**
     * Loads up all persistent records that match the supplied set of primary keys.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<T> loadAll (Collection<Key<T>> keys)
        throws DatabaseException
    {
        return (keys.size() == 0) ? Collections.<T>emptyList() :
            _ctx.invoke(new FindAllQuery.WithKeys<T>(_ctx, keys));
    }

    /**
     * A varargs version of {@link #findAll(Class,Collection)}.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<T> findAll (Class<T> type, QueryClause... clauses)
        throws DatabaseException
    {
        return findAll(type, Arrays.asList(clauses));
    }

    /**
     * Loads all persistent objects that match the specified clauses.
     *
     * We have two strategies for doing this: one performs the query as-is, the second executes two
     * passes: first fetching only key columns and consulting the cache for each such key; then, in
     * the second pass, fetching the full entity only for keys that were not found in the cache.
     *
     * The more complex strategy could save a lot of data shuffling. On the other hand, its
     * complexity is an inherent drawback, and it does execute two separate database queries for
     * what the simple method does in one.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<T> findAll (
        Class<T> type, Collection<? extends QueryClause> clauses)
        throws DatabaseException
    {
        return findAll(type, CacheStrategy.BEST, clauses);
    }

    /**
     * A varargs version of {@link #findAll(Class,CacheStrategy,Collection)}.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<T> findAll (
        Class<T> type, CacheStrategy strategy, QueryClause... clauses)
        throws DatabaseException
    {
        return findAll(type, strategy, Arrays.asList(clauses));
    }

    /**
     * Loads all persistent objects that match the specified clauses.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<T> findAll (
        Class<T> type, CacheStrategy cache, Collection<? extends QueryClause> clauses)
        throws DatabaseException
    {
        DepotMarshaller<T> marsh = _ctx.getMarshaller(type);

        switch (cache) {
        case LONG_KEYS: case SHORT_KEYS: case BEST: case RECORDS:
            String reason = null;
            if (marsh.getTableName() == null) {
                reason = type + " is computed";

            } else if (!marsh.hasPrimaryKey()) {
                reason = type + " has no primary key";

            } else {
                for (QueryClause clause : clauses) {
                    if (clause instanceof FieldOverride) {
                        reason = "query uses a FieldOverride clause";
                        break;
                    }
                }
            }
            if (cache == CacheStrategy.BEST) {
                cache = (reason != null) ? CacheStrategy.NONE : CacheStrategy.SHORT_KEYS;

            } else if (reason != null) {
                // if user explicitly asked for a strategy we can't do, protest
                throw new IllegalArgumentException(
                    "Cannot use " + cache + " strategy because " + reason);
            }
        }

        if (!_ctx.isUsingCache()) {
            cache = CacheStrategy.NONE;
        }

        switch(cache) {
        case SHORT_KEYS: case LONG_KEYS: case RECORDS:
            return _ctx.invoke(new FindAllQuery.WithCache<T>(_ctx, type, clauses, cache));

        default:
            return _ctx.invoke(new FindAllQuery.Explicitly<T>(
                    _ctx, type, clauses, cache == CacheStrategy.CONTENTS));
        }
    }

    /**
     * Looks up and returns {@link Key} records for all rows that match the supplied query clauses.
     *
     * @param forUpdate if true, the query will be run using a read-write connection to ensure that
     * it talks to the master database, if false, the query will be run on a read-only connection
     * and may load keys from a slave. For performance reasons, you should always pass false unless
     * you know you will be modifying the database as a result of this query and absolutely need
     * the latest data.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<Key<T>> findAllKeys (
        Class<T> type, boolean forUpdate, QueryClause... clause)
        throws DatabaseException
    {
        return findAllKeys(type, forUpdate, Arrays.asList(clause));
    }

    /**
     * Looks up and returns {@link Key} records for all rows that match the supplied query clauses.
     *
     * @param forUpdate if true, the query will be run using a read-write connection to ensure that
     * it talks to the master database, if false, the query will be run on a read-only connection
     * and may load keys from a slave. For performance reasons, you should always pass false unless
     * you know you will be modifying the database as a result of this query and absolutely need
     * the latest data.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> List<Key<T>> findAllKeys (
        Class<T> type, boolean forUpdate, Collection<? extends QueryClause> clauses)
        throws DatabaseException
    {
        return _ctx.invoke(new FindAllKeysQuery<T>(_ctx, type, forUpdate, clauses));
    }

    /**
     * Inserts the supplied persistent object into the database, assigning its primary key (if it
     * has one) in the process.
     *
     * @return the number of rows modified by this action, this should always be one.
     *
     * @throws DuplicateKeyException if the inserted record conflicts with the primary key (or any
     * other unique key) of a record already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int insert (T record)
        throws DatabaseException
    {
        @SuppressWarnings("unchecked") final Class<T> pClass = (Class<T>) record.getClass();
        final DepotMarshaller<T> marsh = _ctx.getMarshaller(pClass);
        Key<T> key = marsh.getPrimaryKey(record, false);

        DepotTypes types = DepotTypes.getDepotTypes(_ctx);
        types.addClass(_ctx, pClass);
        final SQLBuilder builder = _ctx.getSQLBuilder(types);

        // key will be null if record was supplied without a primary key
        return _ctx.invoke(new CachingModifier<T>(record, key, key) {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                // if needed, update our modifier's key so that it can cache our results
                Set<String> identityFields = Collections.emptySet();
                if (_key == null) {
                    // set any auto-generated column values
                    identityFields = marsh.generateFieldValues(conn, liaison, _result, false);
                    updateKey(marsh.getPrimaryKey(_result, false));
                }

                builder.newQuery(new InsertClause(pClass, _result, identityFields));

                PreparedStatement stmt = builder.prepare(conn);
                try {
                    int mods = stmt.executeUpdate();
                    // run any post-factum value generators and potentially generate our key
                    if (_key == null) {
                        marsh.generateFieldValues(conn, liaison, _result, true);
                        updateKey(marsh.getPrimaryKey(_result, false));
                    }
                    return mods;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Updates all fields of the supplied persistent object, using its primary key to identify the
     * row to be updated.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected int update (PersistentRecord record)
        throws DatabaseException
    {
        Class<? extends PersistentRecord> pClass = record.getClass();
        requireNotComputed(pClass, "update");
        DepotMarshaller<? extends PersistentRecord> marsh = _ctx.getMarshaller(pClass);
        Key<? extends PersistentRecord> key = marsh.getPrimaryKey(record);
        if (key == null) {
            throw new IllegalArgumentException("Can't update record with null primary key.");
        }
        return doUpdate(key, new UpdateClause(pClass, key, marsh.getColumnFieldNames(), record));
    }

    /**
     * Updates just the specified fields of the supplied persistent object, using its primary key
     * to identify the row to be updated. This method currently flushes the associated record from
     * the cache, but in the future it should be modified to update the modified fields in the
     * cached value iff the record exists in the cache.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int update (T record, final ColumnExp... modifiedFields)
        throws DatabaseException
    {
        @SuppressWarnings("unchecked") Class<T> pClass = (Class<T>) record.getClass();
        requireNotComputed(pClass, "update");
        DepotMarshaller<T> marsh = _ctx.getMarshaller(pClass);
        Key<T> key = marsh.getPrimaryKey(record);
        if (key == null) {
            throw new IllegalArgumentException("Can't update record with null primary key.");
        }
        return doUpdate(key, new UpdateClause(pClass, key, modifiedFields, record));
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied key.
     *
     * @param key the key for the persistent objects to be modified.
     * @param fieldsValues an array containing the columns (as ColumnExp) and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int updatePartial (Key<T> key, Object... fieldsValues)
        throws DatabaseException
    {
        return updatePartial(key.getPersistentClass(), key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied key. This
     * method currently flushes the associated record from the cache, but in the future it should
     * be modified to update the modified fields in the cached value iff the record exists in the
     * cache.
     *
     * @param type the type of the persistent object to be modified.
     * @param key the key to match in the update.
     * @param invalidator a cache invalidator that will be run prior to the update to flush the
     * relevant persistent objects from the cache, or null if no invalidation is needed.
     * @param fieldsValues an array containing the columns (as ColumnExp) and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int updatePartial (
        Class<T> type, final WhereClause key, CacheInvalidator invalidator, Object... fieldsValues)
        throws DatabaseException
    {
        requireNotComputed(type, "updateLiteral");
        if (invalidator instanceof ValidatingCacheInvalidator) {
            ((ValidatingCacheInvalidator)invalidator).validateFlushType(type); // sanity check
        }
        key.validateQueryType(type); // and another

        // separate the arguments into keys and values
        final ColumnExp[] fields = new ColumnExp[fieldsValues.length/2];
        final SQLExpression[] values = new SQLExpression[fields.length];
        for (int ii = 0, idx = 0; ii < fields.length; ii++) {
            fields[ii] = (ColumnExp)fieldsValues[idx++];
            if (fieldsValues[idx] instanceof SQLExpression) {
                values[ii] = (SQLExpression)fieldsValues[idx++];
            } else {
                values[ii] = new ValueExp(fieldsValues[idx++]);
            }
        }

        return doUpdate(invalidator, new UpdateClause(type, key, fields, values));
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary
     * key. The values in this case must be literal SQL to be inserted into the update statement.
     * In general this is used when you want to do something like the following:
     *
     * <pre>
     * update FOO set BAR = BAR + 1;
     * update BAZ set BIF = NOW();
     * </pre>
     *
     * @param key the key to match in the update.
     * @param fieldsValues a map containing the columns and the values to be assigned.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int updateLiteral (
        Key<T> key, Map<ColumnExp, ? extends SQLExpression> fieldsValues)
        throws DatabaseException
    {
        return updateLiteral(key.getPersistentClass(), key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary
     * key. The values in this case must be literal SQL to be inserted into the update statement.
     * In general this is used when you want to do something like the following:
     *
     * <pre>
     * update FOO set BAR = BAR + 1;
     * update BAZ set BIF = NOW();
     * </pre>
     *
     * @param type the type of the persistent object to be modified.
     * @param key the key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, literal value, key, literal value, etc. order.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int updateLiteral (
        Class<T> type, final WhereClause key, CacheInvalidator invalidator,
        Map<ColumnExp, ? extends SQLExpression> fieldsValues)
        throws DatabaseException
    {
        requireNotComputed(type, "updateLiteral");
        if (invalidator instanceof ValidatingCacheInvalidator) {
            ((ValidatingCacheInvalidator)invalidator).validateFlushType(type); // sanity check
        }
        key.validateQueryType(type); // and another

        // separate the arguments into keys and values
        final ColumnExp[] fields = new ColumnExp[fieldsValues.size()];
        final SQLExpression[] values = new SQLExpression[fields.length];
        int ii = 0;
        for (Map.Entry<ColumnExp, ? extends SQLExpression> entry : fieldsValues.entrySet()) {
            fields[ii] = entry.getKey();
            values[ii] = entry.getValue();
            ii ++;
        }

        return doUpdate(invalidator, new UpdateClause(type, key, fields, values));
    }

    /**
     * Stores the supplied persisent object in the database. If it has no primary key assigned (it
     * is null or zero), it will be inserted directly. Otherwise an update will first be attempted
     * and if that matches zero rows, the object will be inserted.
     *
     * @return true if the record was created, false if it was updated.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> boolean store (T record)
        throws DatabaseException
    {
        @SuppressWarnings("unchecked") final Class<T> pClass = (Class<T>) record.getClass();
        requireNotComputed(pClass, "store");

        final DepotMarshaller<T> marsh = _ctx.getMarshaller(pClass);
        Key<T> key = marsh.hasPrimaryKey() ? marsh.getPrimaryKey(record) : null;
        final UpdateClause update =
            new UpdateClause(pClass, key, marsh.getColumnFieldNames(), record);
        final SQLBuilder builder = _ctx.getSQLBuilder(DepotTypes.getDepotTypes(_ctx, update));

        // if our primary key isn't null, we start by trying to update rather than insert
        if (key != null) {
            builder.newQuery(update);
        }

        final boolean[] created = new boolean[1];
        _ctx.invoke(new CachingModifier<T>(record, key, key) {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    if (_key != null) {
                        // run the update
                        stmt = builder.prepare(conn);
                        int mods = stmt.executeUpdate();
                        if (mods > 0) {
                            // if it succeeded, we're done
                            return mods;
                        }
                        JDBCUtil.close(stmt);
                    }

                    // if the update modified zero rows or the primary key was unset, insert
                    Set<String> identityFields = Collections.emptySet();
                    if (_key == null) {
                        // first, set any auto-generated column values
                        identityFields = marsh.generateFieldValues(conn, liaison, _result, false);
                        // update our modifier's key so that it can cache our results
                        updateKey(marsh.getPrimaryKey(_result, false));
                    }

                    builder.newQuery(new InsertClause(pClass, _result, identityFields));

                    stmt = builder.prepare(conn);
                    int mods = stmt.executeUpdate();

                    // run any post-factum value generators and potentially generate our key
                    if (_key == null) {
                        marsh.generateFieldValues(conn, liaison, _result, true);
                        updateKey(marsh.getPrimaryKey(_result, false));
                    }
                    created[0] = true;
                    return mods;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        return created[0];
    }

    /**
     * Deletes all persistent objects from the database matching the primary key of the supplied
     * object (which should be one or zero).
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int delete (T record)
        throws DatabaseException
    {
        @SuppressWarnings("unchecked") Class<T> type = (Class<T>)record.getClass();
        Key<T> primaryKey = _ctx.getMarshaller(type).getPrimaryKey(record);
        if (primaryKey == null) {
            throw new IllegalArgumentException("Can't delete record with null primary key.");
        }
        return delete(primaryKey);
    }

    /**
     * Deletes all persistent objects from the database matching the supplied primary key (which
     * should be one or zero).
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int delete (Key<T> primaryKey)
        throws DatabaseException
    {
        return deleteAll(primaryKey.getPersistentClass(), primaryKey, primaryKey);
    }

    /**
     * Deletes all persistent objects from the database that match the supplied where clause.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int deleteAll (Class<T> type, final WhereClause where)
        throws DatabaseException
    {
        if (where instanceof CacheInvalidator) {
            // our where clause knows how to do its own deletion, yay!
            return deleteAll(type, where, (CacheInvalidator)where);
        } else if (_ctx.getMarshaller(type).hasPrimaryKey()) {
            // look up the primary keys for all matching rows matching and delete using those
            KeySet<T> pwhere = KeySet.newKeySet(type, findAllKeys(type, true, where));
            return deleteAll(type, pwhere, pwhere);
        } else {
            // otherwise just do the delete directly as we can't have cached a record that has no
            // primary key in the first place
            return deleteAll(type, where, null);
        }
    }

    /**
     * Deletes all persistent objects from the database that match the supplied key.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    protected <T extends PersistentRecord> int deleteAll (
        Class<T> type, final WhereClause where, CacheInvalidator invalidator)
        throws DatabaseException
    {
        if (invalidator instanceof ValidatingCacheInvalidator) {
            ((ValidatingCacheInvalidator)invalidator).validateFlushType(type); // sanity check
        }
        where.validateQueryType(type); // and another

        DeleteClause delete = new DeleteClause(type, where);
        final SQLBuilder builder = _ctx.getSQLBuilder(DepotTypes.getDepotTypes(_ctx, delete));
        builder.newQuery(delete);

        return _ctx.invoke(new Modifier(invalidator) {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                PreparedStatement stmt = builder.prepare(conn);
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    // make sure the given type corresponds to a concrete class
    protected void requireNotComputed (Class<? extends PersistentRecord> type, String action)
        throws DatabaseException
    {
        DepotMarshaller<?> marsh = _ctx.getMarshaller(type);
        if (marsh == null) {
            throw new DatabaseException("Unknown persistent type [class=" + type + "]");
        }
        if (marsh.getTableName() == null) {
            throw new DatabaseException(
                "Can't " + action + " computed entities [class=" + type + "]");
        }
    }

    /**
     * A helper method for the various partial update methods.
     */
    protected int doUpdate (CacheInvalidator invalidator, UpdateClause update)
    {
        final SQLBuilder builder = _ctx.getSQLBuilder(DepotTypes.getDepotTypes(_ctx, update));
        builder.newQuery(update);
        return _ctx.invoke(new Modifier(invalidator) {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                PreparedStatement stmt = builder.prepare(conn);
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * If the supplied migration has not already been run, it will be run and if it completes, we
     * will note in the DepotMigrationHistory table that it has been run.
     */
    protected void runMigration (DataMigration migration)
        throws DatabaseException
    {
        // attempt to get a lock to run this migration (or detect that it has already been run)
        DepotMigrationHistoryRecord record;
        while (true) {
            // check to see if the migration has already been completed
            record = load(DepotMigrationHistoryRecord.getKey(migration.getIdent()),
                          CacheStrategy.NONE);
            if (record != null && record.whenCompleted != null) {
                return; // great, no need to do anything
            }

            // if no record exists at all, try to insert one and thereby obtain the migration lock
            if (record == null) {
                try {
                    record = new DepotMigrationHistoryRecord();
                    record.ident = migration.getIdent();
                    insert(record);
                    break; // we got the lock, break out of this loop and run the migration
                } catch (DuplicateKeyException dke) {
                    // someone beat us to the punch, so we have to wait for them to finish
                }
            }

            // we didn't get the lock, so wait 5 seconds and then check to see if the other process
            // finished the update or failed in which case we'll try to grab the lock ourselves
            try {
                log.info("Waiting on migration lock for " + migration.getIdent() + ".");
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                throw new DatabaseException("Interrupted while waiting on migration lock.");
            }
        }

        log.info("Running data migration", "ident", migration.getIdent());
        try {
            // run the migration
            migration.invoke();

            // report to the world that we've done so
            record.whenCompleted = new Timestamp(System.currentTimeMillis());
            update(record);

        } finally {
            // clear out our migration history record if we failed to get the job done
            if (record.whenCompleted == null) {
                try {
                    delete(record);
                } catch (Throwable dt) {
                    log.warning("Oh noez! Failed to delete history record for failed migration. " +
                                "All clients will loop forever waiting for the lock.",
                                "ident", migration.getIdent(), dt);
                }
            }
        }
    }

    protected PersistenceContext _ctx;
    protected List<DataMigration> _dataMigs = Lists.newArrayList();
}
