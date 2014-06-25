//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkArgument;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.util.ArrayUtil;

import com.samskivert.depot.clause.InsertClause;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.util.Sequence;
import static com.samskivert.depot.Log.log;

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
import com.samskivert.depot.impl.expression.ValueExp;
import com.samskivert.depot.impl.util.SeqImpl;

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
    }

    /**
     * Loads the persistent object that matches the specified primary key.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> T load (Key<T> key, QueryClause... clauses)
        throws DatabaseException
    {
        return load(key, CacheStrategy.BEST, clauses);
    }

    /**
     * Loads the persistent object that matches the specified primary key.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> T load (
        Key<T> key, CacheStrategy strategy, QueryClause... clauses)
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
    public <T extends PersistentRecord> T load (Class<T> type, QueryClause... clauses)
        throws DatabaseException
    {
        return load(type, CacheStrategy.BEST, clauses);
    }

    /**
     * Loads the first persistent object that matches the supplied query clauses.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> T load (
        Class<T> type, CacheStrategy strategy, QueryClause... clauses)
        throws DatabaseException
    {
        return _ctx.invoke(new FindOneQuery<T>(_ctx, type, strategy, clauses));
    }

    /**
     * Loads up all persistent records that match the supplied set of raw primary keys.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> List<T> loadAll (
        Class<T> type, Iterable<? extends Comparable<?>> primaryKeys)
        throws DatabaseException
    {
        // convert the raw keys into real key records
        return loadAll(
            Iterables.transform(primaryKeys, _ctx.getMarshaller(type).primaryKeyFunction()));
    }

    /**
     * Loads up all persistent records that match the supplied set of primary keys.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> List<T> loadAll (Iterable<Key<T>> keys)
        throws DatabaseException
    {
        return Iterables.isEmpty(keys) ? Collections.<T>emptyList() :
            _ctx.invoke(new FindAllQuery.WithKeys<T>(_ctx, keys));
    }

    /**
     * A varargs version of {@link #findAll(Class,Iterable)}.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> List<T> findAll (Class<T> type, QueryClause... clauses)
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
    public <T extends PersistentRecord> List<T> findAll (
        Class<T> type, Iterable<? extends QueryClause> clauses)
        throws DatabaseException
    {
        return findAll(type, CacheStrategy.BEST, clauses);
    }

    /**
     * A varargs version of {@link #findAll(Class,CacheStrategy,Iterable)}.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> List<T> findAll (
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
    public <T extends PersistentRecord> List<T> findAll (
        Class<T> type, CacheStrategy cache, Iterable<? extends QueryClause> clauses)
        throws DatabaseException
    {
        return _ctx.invoke(FindAllQuery.newCachedFullRecordQuery(_ctx, type, cache, clauses));
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
    public <T extends PersistentRecord> List<Key<T>> findAllKeys (
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
    public <T extends PersistentRecord> List<Key<T>> findAllKeys (
        Class<T> type, boolean forUpdate, Iterable<? extends QueryClause> clauses)
        throws DatabaseException
    {
        return _ctx.invoke(new FindAllKeysQuery<T>(_ctx, type, forUpdate, clauses));
    }

    /**
     * Returns a builder that can be used to construct a query in a fluent style. For example:
     * {@code from(FooRecord.class).where(ID.greaterThan(25)).ascending(SIZE).select()}
     */
    public <T extends PersistentRecord> Query<T> from (Class<T> type)
    {
        return new Query<T>(_ctx, this, type);
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
    public <T extends PersistentRecord> int insert (T record)
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
                    identityFields = marsh.generateFieldValues(conn, liaison, null, _result, false);
                    updateKey(marsh.getPrimaryKey(_result, false));
                }
                builder.newQuery(new InsertClause(pClass, _result, identityFields));

                PreparedStatement stmt = builder.prepareInsert(conn);
                int mods = stmt.executeUpdate();
                // run any post-factum value generators and potentially generate our key
                if (_key == null) {
                    marsh.generateFieldValues(conn, liaison, stmt, _result, true);
                    updateKey(marsh.getPrimaryKey(_result, false));
                }
                return mods;
            }
            @Override
            public void updateStats (Stats stats) {
                stats.noteModification(pClass);
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
    public int update (PersistentRecord record)
        throws DatabaseException
    {
        Class<? extends PersistentRecord> pClass = record.getClass();
        requireNotComputed(pClass, "update");
        DepotMarshaller<? extends PersistentRecord> marsh = _ctx.getMarshaller(pClass);
        Key<? extends PersistentRecord> key = marsh.getPrimaryKey(record);
        checkArgument(key != null, "Can't update record with null primary key.");
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
    public <T extends PersistentRecord> int update (T record, final ColumnExp<?>... modifiedFields)
        throws DatabaseException
    {
        @SuppressWarnings("unchecked") Class<T> pClass = (Class<T>) record.getClass();
        requireNotComputed(pClass, "update");
        DepotMarshaller<T> marsh = _ctx.getMarshaller(pClass);
        Key<T> key = marsh.getPrimaryKey(record);
        checkArgument(key != null, "Can't update record with null primary key.");
        return doUpdate(key, new UpdateClause(pClass, key, modifiedFields, record));
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied key.
     *
     * @param key the key for the persistent objects to be modified.
     * @param field the first field to be updated.
     * @param value the value to assign to the first field. This may be a primitive (Integer,
     * String, etc.) which will be wrapped in value expression or a SQLExpression instance.
     * @param more additional (field, value) pairs to be updated.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int updatePartial (
        Key<T> key, ColumnExp<?> field, Object value, Object... more)
        throws DatabaseException
    {
        return updatePartial(key.getPersistentClass(), key, key, field, value, more);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied key.
     *
     * @param key the key for the persistent objects to be modified.
     * @param updates a mapping from field to value for all values to be changed. The values may be
     * primitives (Integer, String, etc.) which will be wrapped in value expression instances or
     * SQLExpression instances defining the value.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int updatePartial (
        Key<T> key, Map<? extends ColumnExp<?>, ?> updates)
        throws DatabaseException
    {
        return updatePartial(key.getPersistentClass(), key, key, updates);
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
     * @param updates a mapping from field to value for all values to be changed. The values may be
     * primitives (Integer, String, etc.) which will be wrapped in value expression instances or
     * SQLExpression instances defining the value.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int updatePartial (
        Class<T> type, WhereClause key, CacheInvalidator invalidator,
        Map<? extends ColumnExp<?>, ?> updates)
        throws DatabaseException
    {
        // separate the arguments into keys and values
        final ColumnExp<?>[] fields = new ColumnExp<?>[updates.size()];
        final SQLExpression<?>[] values = new SQLExpression<?>[fields.length];
        int ii = 0;
        for (Map.Entry<? extends ColumnExp<?>, ?> entry : updates.entrySet()) {
            fields[ii] = entry.getKey();
            values[ii++] = makeValue(entry.getValue());
        }
        return updatePartial(type, key, invalidator, fields, values);
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
     * @param field the first field to be updated.
     * @param value the value to assign to the first field. This may be a primitive (Integer,
     * String, etc.) which will be wrapped in value expression or a SQLExpression instance.
     * @param more additional (field, value) pairs to be updated.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int updatePartial (
        Class<T> type, final WhereClause key, CacheInvalidator invalidator,
        ColumnExp<?> field, Object value, Object... more)
        throws DatabaseException
    {
        // separate the updates into keys and values
        final ColumnExp<?>[] fields = new ColumnExp<?>[1+more.length/2];
        final SQLExpression<?>[] values = new SQLExpression<?>[fields.length];
        fields[0] = field;
        values[0] = makeValue(value);
        for (int ii = 1, idx = 0; ii < fields.length; ii++) {
            fields[ii] = (ColumnExp<?>)more[idx++];
            values[ii] = makeValue(more[idx++]);
        }
        return updatePartial(type, key, invalidator, fields, values);
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
     * @param fields the fields in the objects to be updated.
     * @param values the values to be assigned to the fields.
     *
     * @return the number of rows modified by this action.
     *
     * @throws DuplicateKeyException if the update attempts to change the key columns of a row to
     * values that duplicate another row already in the database.
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int updatePartial (
        Class<T> type, final WhereClause key, CacheInvalidator invalidator,
        ColumnExp<?>[] fields, SQLExpression<?>[] values)
        throws DatabaseException
    {
        requireNotComputed(type, "updatePartial");
        if (invalidator instanceof ValidatingCacheInvalidator) {
            ((ValidatingCacheInvalidator)invalidator).validateFlushType(type); // sanity check
        }
        key.validateQueryType(type); // and another
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
    public <T extends PersistentRecord> boolean store (T record)
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
        try {
            _ctx.invoke(new CachingModifier<T>(record, key, key) {
                @Override
                protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException
                {
                    if (_key != null) {
                        // run the update
                        int mods = builder.prepare(conn).executeUpdate();
                        if (mods > 0) {
                            // if it succeeded, we're done
                            return mods;
                        }
                    }

                    // if the update modified zero rows or the primary key was unset, insert
                    Set<String> identityFields = Collections.emptySet();
                    if (_key == null) {
                        // first, set any auto-generated column values
                        identityFields = marsh.generateFieldValues(
                            conn, liaison, null, _result, false);
                        // update our modifier's key so that it can cache our results
                        updateKey(marsh.getPrimaryKey(_result, false));
                    }
                    builder.newQuery(new InsertClause(pClass, _result, identityFields));

                    PreparedStatement stmt = builder.prepareInsert(conn);
                    int mods = stmt.executeUpdate();

                    // run any post-factum value generators and potentially generate our key
                    if (_key == null) {
                        marsh.generateFieldValues(conn, liaison, stmt, _result, true);
                        updateKey(marsh.getPrimaryKey(_result, false));
                    }
                    created[0] = true;
                    return mods;
                }
                @Override
                public void updateStats (Stats stats) {
                    stats.noteModification(pClass);
                }
            });

        } catch (DuplicateKeyException dke) {
            // If we got this then the insert failed. Another node must have done the insert
            // already. A simple solution here would be to just ignore the DKE and return, because
            // by definition we're in a race condition and we can just pretend we got in first but
            // that the other caller did an update afterwards.

            // But: what if non-symmetrical code is being run on the nodes? What if the other node
            // specifically called insert()? In that case, the other node is expecting a possible
            // DKE, but this node isn't, and if the other node always calls insert() then this node
            // would expect its store() to always work and never be overwritten by the other node.
            // We need to attempt to complete the operation.
            if (key == null) {
                throw dke; // how would this even happen?
            }
            // Retry one more update.
            _ctx.invoke(new CachingModifier<T>(record, key, key) {
                @Override
                protected int invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException
                {
                    builder.newQuery(update);
                    return builder.prepare(conn).executeUpdate();
                }
                @Override
                public void updateStats (Stats stats) {
                    stats.noteModification(pClass);
                }
            });
        }

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
    public <T extends PersistentRecord> int delete (T record)
        throws DatabaseException
    {
        @SuppressWarnings("unchecked") Class<T> type = (Class<T>)record.getClass();
        Key<T> primaryKey = _ctx.getMarshaller(type).getPrimaryKey(record);
        checkArgument(primaryKey != null, "Can't delete record with null primary key.");
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
    public <T extends PersistentRecord> int delete (Key<T> primaryKey)
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
    public <T extends PersistentRecord> int deleteAll (Class<T> type, WhereClause where)
        throws DatabaseException
    {
        return deleteAll(type, where, null, null);
    }

    /**
     * Deletes all persistent objects from the database that match the supplied where clause, up to
     * the specified limit.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int deleteAll (Class<T> type, WhereClause where, Limit lim)
        throws DatabaseException
    {
        if (where instanceof CacheInvalidator) {
            // our where clause knows how to do its own deletion, yay!
            return deleteAll(type, where, lim, (CacheInvalidator)where);
        } else if (_ctx.getMarshaller(type).hasPrimaryKey()) {
            // look up the primary keys for all matching rows matching and delete using those
            KeySet<T> pwhere = KeySet.newKeySet(type, findAllKeys(type, true, where, lim));
            return deleteAll(type, pwhere, pwhere);
        } else {
            // otherwise just do the delete directly as we can't have cached a record that has no
            // primary key in the first place
            return deleteAll(type, where, lim, null);
        }
    }

    /**
     * Deletes all persistent objects from the database that match the supplied key.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int deleteAll (
        Class<T> type, WhereClause where, CacheInvalidator invalidator)
        throws DatabaseException
    {
        return deleteAll(type, where, null, invalidator);
    }

    /**
     * Deletes all persistent objects from the database that match the supplied key, up to the
     * supplied limit.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public <T extends PersistentRecord> int deleteAll (
        final Class<T> type, WhereClause where, Limit limit, CacheInvalidator invalidator)
        throws DatabaseException
    {
        if (invalidator instanceof ValidatingCacheInvalidator) {
            ((ValidatingCacheInvalidator)invalidator).validateFlushType(type); // sanity check
        }
        where.validateQueryType(type); // and another

        DeleteClause delete = new DeleteClause(type, where, limit);
        final SQLBuilder builder = _ctx.getSQLBuilder(DepotTypes.getDepotTypes(_ctx, delete));
        builder.newQuery(delete);

        return _ctx.invoke(new Modifier(invalidator) {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                return builder.prepare(conn).executeUpdate();
            }
            @Override
            public void updateStats (Stats stats) {
                stats.noteModification(type);
            }
        });
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
    public void registerMigration (DataMigration migration)
    {
        if (_dataMigs == null) {
            // we've already been initialized, so we have to run this migration immediately
            runMigration(migration);
        } else {
            _dataMigs.add(migration);
        }
    }

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
     * Adds the persistent classes used by this repository to the supplied set.
     */
    protected abstract void getManagedRecords (Set<Class<? extends PersistentRecord>> classes);

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
    protected int doUpdate (CacheInvalidator invalidator, final UpdateClause update)
    {
        final SQLBuilder builder = _ctx.getSQLBuilder(DepotTypes.getDepotTypes(_ctx, update));
        builder.newQuery(update);
        return _ctx.invoke(new Modifier(invalidator) {
            @Override
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                return builder.prepare(conn).executeUpdate();
            }
            @Override
            public void updateStats (Stats stats) {
                stats.noteModification(update.getPersistentClass());
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

    protected <T> SQLExpression<T> makeValue (T value)
    {
        if (value instanceof SQLExpression<?>) {
            @SuppressWarnings("unchecked") SQLExpression<T> eval = (SQLExpression<T>)value;
            return eval;
        } else {
            return new ValueExp<T>(value);
        }
    }

    /**
     * Concise way to transform query results.
     */
    protected <F, T> Sequence<T> map (Collection<F> source, Function<? super F, ? extends T> func)
    {
        return new SeqImpl<F, T>(source, func);
    }

    protected PersistenceContext _ctx;
    protected List<DataMigration> _dataMigs = Lists.newArrayList();
}
