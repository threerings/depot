//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.clause;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a complete select clause.
 */
public class SelectClause
    implements QueryClause
{
    /**
     * Creates a new select clause, selecting the supplied expressions from the specified
     * persistent class (and potentially joined classes) for rows that match the supplied clauses.
     */
    public SelectClause (Class<? extends PersistentRecord> pClass,
                         SQLExpression<?>[] selexps, QueryClause... clauses)
    {
        this(pClass, selexps, Arrays.asList(clauses));
    }

    /**
     * Creates a new select clause, selecting the supplied expressions from the specified
     * persistent class (and potentially joined classes) for rows that match the supplied clauses.
     */
    public SelectClause (Class<? extends PersistentRecord> pClass, SQLExpression<?>[] selexps,
                         Iterable<? extends QueryClause> clauses)
    {
        _pClass = pClass;
        _selexps = selexps;

        // iterate over the clauses and sort them into the different types we understand
        for (QueryClause clause : clauses) {
            if (clause == null) {
                continue;
            }
            if (clause instanceof WhereClause) {
                checkArgument(_where == null, "Query can't contain multiple Where clauses.");
                _where = (WhereClause) clause;

            } else if (clause instanceof FromOverride) {
                checkArgument(_fromOverride == null,
                              "Query can't contain multiple FromOverride clauses.");
                _fromOverride = (FromOverride) clause;

            } else if (clause instanceof Join) {
                _joinClauses.add((Join) clause);

            } else if (clause instanceof FieldDefinition) {
                _disMap.put(((FieldDefinition) clause).getField(), ((FieldDefinition) clause));

            } else if (clause instanceof Distinct) {
                checkArgument(_distinct == null, "Query can't contain multiple Distinct clauses.");
                _distinct = (Distinct) clause;

            } else if (clause instanceof OrderBy) {
                checkArgument(_orderBy == null, "Query can't contain multiple OrderBy clauses.");
                _orderBy = (OrderBy) clause;

            } else if (clause instanceof GroupBy) {
                checkArgument(_groupBy == null, "Query can't contain multiple GroupBy clauses.");
                _groupBy = (GroupBy) clause;

            } else if (clause instanceof Limit) {
                checkArgument(_limit == null, "Query can't contain multiple Limit clauses.");
                _limit = (Limit) clause;

            } else if (clause instanceof ForUpdate) {
                checkArgument(_forUpdate == null,
                              "Query can't contain multiple For Update clauses.");
                _forUpdate = (ForUpdate) clause;

            } else {
                throw new IllegalArgumentException(
                    "Unknown clause provided in select " + clause + ".");
            }
        }
    }

    public FieldDefinition lookupDefinition (String field)
    {
        return _disMap.get(field);
    }

    public Collection<FieldDefinition> getFieldDefinitions ()
    {
        return _disMap.values();
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public SQLExpression<?>[] getSelections ()
    {
        return _selexps;
    }

    public FromOverride getFromOverride ()
    {
        return _fromOverride;
    }

    public WhereClause getWhereClause ()
    {
        return _where;
    }

    public List<Join> getJoinClauses ()
    {
        return _joinClauses;
    }

    public OrderBy getOrderBy ()
    {
        return _orderBy;
    }

    public Distinct getDistinct ()
    {
        return _distinct;
    }

    public GroupBy getGroupBy ()
    {
        return _groupBy;
    }

    public Limit getLimit ()
    {
        return _limit;
    }

    public ForUpdate getForUpdate ()
    {
        return _forUpdate;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);

        if (_fromOverride != null) {
            _fromOverride.addClasses(classSet);
        }
        if (_where != null) {
            _where.addClasses(classSet);
        }
        for (Join join : _joinClauses) {
            join.addClasses(classSet);
        }
        for (FieldDefinition override : _disMap.values()) {
            override.addClasses(classSet);
        }
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(where=").append(_where);
        if (_fromOverride != null) {
            builder.append(", from=").append(_fromOverride);
        }
        if (!_joinClauses.isEmpty()) {
            builder.append(", join=").append(_joinClauses);
        }
        if (_orderBy != null) {
            builder.append(", orderBy=").append(_orderBy);
        }
        if (_distinct != null) {
            builder.append(", distinct=").append(_distinct);
        }
        if (_groupBy != null) {
            builder.append(", groupBy=").append(_groupBy);
        }
        if (_limit != null) {
            builder.append(", limit=").append(_limit);
        }
        if (_forUpdate != null) {
            builder.append(", forUpdate=").append(_forUpdate);
        }
        return builder.append(")").toString();
    }

    /** Persistent class fields mapped to field override clauses. */
    protected Map<String, FieldDefinition> _disMap = Maps.newHashMap();

    /** The persistent class this select defines. */
    protected Class<? extends PersistentRecord> _pClass;

    /** The expressions being selected. */
    protected SQLExpression<?>[] _selexps;

    /** The from override clause, if any. */
    protected FromOverride _fromOverride;

    /** The where clause. */
    protected WhereClause _where;

    /** A list of join clauses, each potentially referencing a new class. */
    protected List<Join> _joinClauses = Lists.newArrayList();

    /** The order by clause, if any. */
    protected OrderBy _orderBy;

    /** The distinct clause, if any. */
    protected Distinct _distinct;

    /** The group by clause, if any. */
    protected GroupBy _groupBy;

    /** The limit clause, if any. */
    protected Limit _limit;

    /** The For Update clause, if any. */
    protected ForUpdate _forUpdate;
}
