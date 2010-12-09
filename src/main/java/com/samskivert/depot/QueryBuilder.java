//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.clause.*;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.Projector;
import com.samskivert.depot.impl.FindAllQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * The root of a fluent mechanism for constructing queries. Obtain an instance via {@link
 * DepotRepository#from}.
 */
public class QueryBuilder<T extends PersistentRecord>
    implements Cloneable
{
    public QueryBuilder (PersistenceContext ctx, DepotRepository repo, Class<T> pclass)
    {
        _ctx = ctx;
        _repo = repo;
        _pclass = pclass;
    }

    /** Disables the use of the cache for this query. */
    public QueryBuilder<T> noCache () {
        return cache(DepotRepository.CacheStrategy.BEST);
    }

    /** Configures the use of {@link CacheStrategy#BEST} for this query. */
    public QueryBuilder<T> cacheBest () {
        return cache(DepotRepository.CacheStrategy.BEST);
    }

    /** Configures the use of {@link CacheStrategy#RECORDS} for this query. */
    public QueryBuilder<T> cacheRecords () {
        return cache(DepotRepository.CacheStrategy.RECORDS);
    }

    /** Configures the use of {@link CacheStrategy#SHORT_KEYS} for this query. */
    public QueryBuilder<T> cacheShortKeys () {
        return cache(DepotRepository.CacheStrategy.SHORT_KEYS);
    }

    /** Configures the use of {@link CacheStrategy#LONG_KEYS} for this query. */
    public QueryBuilder<T> cacheLongKeys () {
        return cache(DepotRepository.CacheStrategy.LONG_KEYS);
    }

    /** Configures the use of {@link CacheStrategy#CONTENTS} for this query. */
    public QueryBuilder<T> cacheContents () {
        return cache(DepotRepository.CacheStrategy.CONTENTS);
    }

    /** Configures the specified caching policy this query. */
    public QueryBuilder<T> cache (DepotRepository.CacheStrategy cache) {
        _cache = cache;
        return this;
    }

    /**
     * Configures a {@link Where} clause that matches all rows. For selections, a where clause can
     * simply be omitted, but for deletions, this method must be used if you intend to delete all
     * rows in the table.
     */
    public QueryBuilder<T> whereTrue ()
    {
        return where(Exps.literal("true"));
    }

    /**
     * Configures a {@link Where} clause that ANDs together all of the supplied expressions.
     */
    public QueryBuilder<T> where (SQLExpression<?>... exprs)
    {
        return where(Arrays.asList(exprs));
    }

    /**
     * Configures a {@link Where} clause that ANDs together all of the supplied expressions.
     */
    public QueryBuilder<T> where (Iterable<? extends SQLExpression<?>> exprs)
    {
        Iterator<? extends SQLExpression<?>> iter = exprs.iterator();
        checkArgument(iter.hasNext(), "Must supply at least one expression.");
        SQLExpression<?> first = iter.next();
        return where(iter.hasNext() ? new Where(Ops.and(exprs)) : new Where(first));
    }

    /**
     * Configures a {@link Where} clause that selects rows where the supplied column equals the
     * supplied value.
     */
    public <V extends Comparable<? super V>> QueryBuilder<T> where (ColumnExp<V> column, V value)
    {
        return where(new Where(column, value));
    }

    /**
     * Configures a {@link Where} clause that selects rows where both supplied columns equal both
     * supplied values.
     */
    public <V1 extends Comparable<? super V1>, V2 extends Comparable<? super V2>>
        QueryBuilder<T> where (ColumnExp<V1> index1, V1 value1, ColumnExp<V2> index2, V2 value2)
    {
        return where(new Where(index1, value1, index2, value2));
    }

    /**
     * Configures a {@link Where} clause that selects rows where both supplied columns equal both
     * supplied values.
     */
    public QueryBuilder<T> where (WhereClause where)
    {
        checkState(_where == null, "Where clause is already configured.");
        _where = where;
        return this;
    }

    /**
     * Adds a {@link Join} clause configured with the supplied left and right columns.
     */
    public QueryBuilder<T> join (ColumnExp<?> left, ColumnExp<?> right)
    {
        return join(new Join(left, right));
    }

    /**
     * Adds a {@link Join} clause configured with the join condition.
     */
    public QueryBuilder<T> join (Class<? extends PersistentRecord> joinClass,
                                 SQLExpression<?> joinCondition)
    {
        return join(new Join(joinClass, joinCondition));
    }

    /**
     * Adds a {@link Join} clause configured with the supplied left and right columns and join
     * type.
     */
    public QueryBuilder<T> join (ColumnExp<?> left, ColumnExp<?> right, Join.Type type)
    {
        return join(new Join(left, right).setType(type));
    }

    /**
     * Configures the query with the supplied {@link Join} clause. Multiple join clauses are
     * allowed.
     */
    public QueryBuilder<T> join (Join join)
    {
        if (_joins == null) {
            _joins = Lists.newArrayList();
        }
        _joins.add(join);
        return this;
    }

    /**
     * Configures a {@link GroupBy} clause on the supplied group expressions.
     */
    public QueryBuilder<T> groupBy (SQLExpression<?>... exprs)
    {
        checkState(_groupBy == null, "GroupBy clause is already configured.");
        _groupBy = new GroupBy(exprs);
        return this;
    }

    /**
     * Configures an {@link OrderBy} clause configured to randomly order the results.
     */
    public QueryBuilder<T> randomOrder ()
    {
        return orderBy(OrderBy.random());
    }

    /**
     * Configures an {@link OrderBy} clause that ascends on the supplied expression.
     */
    public QueryBuilder<T> ascending (SQLExpression<?> value)
    {
        return orderBy(OrderBy.ascending(value));
    }

    /**
     * Configures an {@link OrderBy} clause that descends on the supplied expression.
     */
    public QueryBuilder<T> descending (SQLExpression<?> value)
    {
        return orderBy(OrderBy.descending(value));
    }

    /**
     * Configures the query with the supplied {@link OrderBy} clause.
     */
    public QueryBuilder<T> orderBy (OrderBy orderBy)
    {
        checkState(_orderBy == null, "OrderBy clause is already configured.");
        _orderBy = orderBy;
        return this;
    }

    /**
     * Configures a {@link Limit} clause configured with the supplied count.
     */
    public QueryBuilder<T> limit (int count)
    {
        checkState(_limit == null, "Limit clause is already configured.");
        _limit = new Limit(0, count);
        return this;
    }

    /**
     * Configures a {@link Limit} clause configured with the supplied offset and count.
     */
    public QueryBuilder<T> limit (int offset, int count)
    {
        checkState(_limit == null, "Limit clause is already configured.");
        _limit = new Limit(offset, count);
        return this;
    }

    /**
     * Configures a {@link FromOverride} clause configured with the supplied override class.
     */
    public QueryBuilder<T> override (Class<? extends PersistentRecord> fromClass)
    {
        return override(new FromOverride(fromClass));
    }

    /**
     * Configures a {@link FromOverride} clause configured with the supplied override classes.
     */
    public QueryBuilder<T> override (Class<? extends PersistentRecord> fromClass1,
                                     Class<? extends PersistentRecord> fromClass2)
    {
        return override(new FromOverride(fromClass1, fromClass2));
    }

    /**
     * Configures the query with the supplied {@link FromOverride} clause.
     */
    public QueryBuilder<T> override (FromOverride fromOverride)
    {
        checkState(_fromOverride == null, "FromOverride clause is already configured.");
        _fromOverride = fromOverride;
        return this;
    }

    /**
     * Adds a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (String field, String value)
    {
        return fieldDef(new FieldDefinition(field, value));
    }

    /**
     * Adds a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (String field, SQLExpression<?> override)
    {
        return fieldDef(new FieldDefinition(field, override));
    }

    /**
     * Adds a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (ColumnExp<?> field, SQLExpression<?> override)
    {
        return fieldDef(new FieldDefinition(field, override));
    }

    /**
     * Adds a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (FieldDefinition fieldDef)
    {
        if (_fieldDefs == null) {
            _fieldDefs = Lists.newArrayList();
        }
        _fieldDefs.add(fieldDef);
        return this;
    }

    /**
     * Configures a {@link ForUpdate} clause which marks this query as selecting for update.
     */
    public QueryBuilder<T> forUpdate ()
    {
        checkState(_forUpdate == null, "ForUpdate clause is already configured.");
        _forUpdate = new ForUpdate();
        return this;
    }

    /**
     * Loads the first persistent object that matches the configured query clauses.
     */
    public T load ()
    {
        return _repo.load(_pclass, _cache, getClauseArray());
    }

    /**
     * Loads all persistent objects that match the configured query clauses.
     */
    public List<T> select ()
        throws DatabaseException
    {
        return _repo.findAll(_pclass, _cache, getClauses());
    }

    /**
     * Loads the keys of all persistent objects that match the configured query clauses. Note that
     * cache configuration is ignored for key-only queries.
     *
     * @param useMaster if true, the query will be run using a read-write connection to ensure that
     * it talks to the master database, if false, the query will be run on a read-only connection
     * and may load keys from a slave. For performance reasons, you should always pass false unless
     * you know you will be modifying the database as a result of this query and absolutely need
     * the latest data.
     */
    public List<Key<T>> selectKeys (boolean useMaster)
        throws DatabaseException
    {
        return _repo.findAllKeys(_pclass, useMaster, getClauses());
    }

    /**
     * Returns the count of rows that match the configured query clauses.
     */
    public int selectCount ()
    {
        _fromOverride = new FromOverride(_pclass);
        return _repo.load(CountRecord.class, _cache, getClauseArray()).count;
    }

    /**
     * Returns the value from the first row that matches the configured query clauses.
     */
    public <V> V load (SQLExpression<V> selexp)
    {
        return select(selexp).get(0); // TODO: revamp FindOneQuery and use that
    }

    /**
     * Returns the value from the first row that matches the configured query clauses.
     */
    public <V1, V2> Tuple2<V1, V2> load (SQLExpression<V1> exp1, SQLExpression<V2> exp2)
    {
        return select(exp1, exp2).get(0); // TODO: revamp FindOneQuery and use that
    }

    /**
     * Returns just the supplied expression from the rows matching the query.
     */
    public <V> List<V> select (SQLExpression<V> selexp)
    {
        return _ctx.invoke(new FindAllQuery.Projection<T,V>(
                               _ctx, Projector.create(_pclass, selexp), getClauses()));
    }

    /**
     * Returns just the supplied expressions from the rows matching the query.
     */
    public <V1, V2> List<Tuple2<V1,V2>> select (SQLExpression<V1> exp1, SQLExpression<V2> exp2)
    {
        return _ctx.invoke(new FindAllQuery.Projection<T,Tuple2<V1,V2>>(
                               _ctx, Projector.create(_pclass, exp1, exp2), getClauses()));
    }

    /**
     * Deletes the records that match the configured query clauses. Note that only the where
     * clauses are used to evaluate a deletion. Attempts to use other clauses will result in
     * failure.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public int delete ()
    {
        assertValidDelete();
        return _repo.deleteAll(_pclass, _where);
    }

    /**
     * Deletes the records that match the configured query clauses. The supplied cache invalidator
     * is used to remove deleted records from the cache.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public int delete (CacheInvalidator invalidator)
    {
        assertValidDelete();
        return _repo.deleteAll(_pclass, _where, invalidator);
    }

    /**
     * Returns a clone of this query builder, including all partially configured state. Useful for
     * constructing partially configured queries and then executing variants.
     */
    public QueryBuilder<T> clone ()
    {
        try {
            @SuppressWarnings("unchecked") QueryBuilder<T> qb = (QueryBuilder<T>)super.clone();
            // deep copy the list fields, if we have any
            if (qb._joins != null) {
                qb._joins = Lists.newArrayList(qb._joins);
            }
            if (qb._fieldDefs != null) {
                qb._fieldDefs = Lists.newArrayList(qb._fieldDefs);
            }
            return qb;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    protected List<QueryClause> getClauses ()
    {
        List<QueryClause> clauses = Lists.newArrayList();
        addIfNotNull(clauses, _where);
        if (_joins != null) {
            clauses.addAll(_joins);
        }
        addIfNotNull(clauses, _orderBy);
        addIfNotNull(clauses, _groupBy);
        addIfNotNull(clauses, _limit);
        addIfNotNull(clauses, _fromOverride);
        if (_fieldDefs != null) {
            clauses.addAll(_fieldDefs);
        }
        addIfNotNull(clauses, _forUpdate);
        return clauses;
    }

    protected QueryClause[] getClauseArray ()
    {
        List<QueryClause> clauses = getClauses();
        return clauses.toArray(new QueryClause[clauses.size()]);
    }

    protected void addIfNotNull (List<QueryClause> clauses, QueryClause clause)
    {
        if (clause != null) {
            clauses.add(clause);
        }
    }

    protected void assertValidDelete ()
    {
        checkState(_where != null, "Where clause must be specified for delete.");
        checkState(_joins == null, "Join clauses not supported by delete.");
        checkState(_orderBy == null, "OrderBy clause not applicable for delete.");
        checkState(_groupBy == null, "GroupBy clause not applicable for delete.");
        checkState(_limit == null, "Limit clause not supported by delete.");
        checkState(_fromOverride == null, "FromOverride clause not applicable for delete.");
        checkState(_fieldDefs == null, "FieldDefinition clauses not applicable for delete.");
        checkState(_forUpdate == null, "ForUpdate clause not supported by delete.");
    }

    protected final PersistenceContext _ctx;
    protected final DepotRepository _repo;
    protected final Class<T> _pclass;

    protected DepotRepository.CacheStrategy _cache = DepotRepository.CacheStrategy.BEST;

    protected WhereClause _where;
    protected OrderBy _orderBy;
    protected GroupBy _groupBy;
    protected Limit _limit;
    protected FromOverride _fromOverride;
    protected ForUpdate _forUpdate;

    protected List<Join> _joins;
    protected List<FieldDefinition> _fieldDefs;
}
