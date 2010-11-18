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

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.Tuple;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.ForUpdate;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.InsertClause;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.*;
import com.samskivert.depot.operator.Case;
import com.samskivert.depot.operator.FullText;

import com.samskivert.depot.impl.clause.CreateIndexClause;
import com.samskivert.depot.impl.clause.DeleteClause;
import com.samskivert.depot.impl.clause.DropIndexClause;
import com.samskivert.depot.impl.clause.UpdateClause;
import com.samskivert.depot.impl.expression.AggregateFun;
import com.samskivert.depot.impl.expression.IntervalExp;
import com.samskivert.depot.impl.expression.LiteralExp;
import com.samskivert.depot.impl.expression.ValueExp;
import com.samskivert.depot.impl.expression.AggregateFun.Average;
import com.samskivert.depot.impl.expression.AggregateFun.Count;
import com.samskivert.depot.impl.expression.AggregateFun.Every;
import com.samskivert.depot.impl.expression.AggregateFun.Max;
import com.samskivert.depot.impl.expression.AggregateFun.Min;
import com.samskivert.depot.impl.expression.AggregateFun.Sum;
import com.samskivert.depot.impl.expression.ConditionalFun.Coalesce;
import com.samskivert.depot.impl.expression.ConditionalFun.Greatest;
import com.samskivert.depot.impl.expression.ConditionalFun.Least;
import com.samskivert.depot.impl.expression.DateFun.DatePart;
import com.samskivert.depot.impl.expression.DateFun.DateTruncate;
import com.samskivert.depot.impl.expression.DateFun.Now;
import com.samskivert.depot.impl.expression.NumericalFun.Abs;
import com.samskivert.depot.impl.expression.NumericalFun.Ceil;
import com.samskivert.depot.impl.expression.NumericalFun.Exp;
import com.samskivert.depot.impl.expression.NumericalFun.Floor;
import com.samskivert.depot.impl.expression.NumericalFun.Ln;
import com.samskivert.depot.impl.expression.NumericalFun.Log10;
import com.samskivert.depot.impl.expression.NumericalFun.Pi;
import com.samskivert.depot.impl.expression.NumericalFun.Power;
import com.samskivert.depot.impl.expression.NumericalFun.Random;
import com.samskivert.depot.impl.expression.NumericalFun.Round;
import com.samskivert.depot.impl.expression.NumericalFun.Sign;
import com.samskivert.depot.impl.expression.NumericalFun.Sqrt;
import com.samskivert.depot.impl.expression.NumericalFun.Trunc;
import com.samskivert.depot.impl.expression.StringFun.Length;
import com.samskivert.depot.impl.expression.StringFun.Lower;
import com.samskivert.depot.impl.expression.StringFun.Position;
import com.samskivert.depot.impl.expression.StringFun.Substring;
import com.samskivert.depot.impl.expression.StringFun.Trim;
import com.samskivert.depot.impl.expression.StringFun.Upper;
import com.samskivert.depot.impl.operator.BinaryOperator;
import com.samskivert.depot.impl.operator.Exists;
import com.samskivert.depot.impl.operator.In;
import com.samskivert.depot.impl.operator.IsNull;
import com.samskivert.depot.impl.operator.MultiOperator;
import com.samskivert.depot.impl.operator.Not;

import static com.samskivert.Log.log;

/**
 * Implements the base functionality of the SQL-building pass of {@link SQLBuilder}. Dialectal
 * subclasses of this should be created and returned from {@link SQLBuilder#getBuildVisitor()}.
 */
public abstract class BuildVisitor implements FragmentVisitor<Void>
{
    public String getQuery ()
    {
        return _builder.toString();
    }

    public Iterable<Bindable> getBindables ()
    {
        return _bindables;
    }

    public Void visit (FromOverride override)
    {
        _builder.append(" from " );
        List<Class<? extends PersistentRecord>> from = override.getFromClasses();
        for (int ii = 0; ii < from.size(); ii++) {
            if (ii > 0) {
                _builder.append(", ");
            }
            appendTableName(from.get(ii));
            _builder.append(" as ");
            appendTableAbbreviation(from.get(ii));
        }
        return null;
    }

    public Void visit (FieldDefinition definition)
    {
        definition.getDefinition().accept(this);
        return null;
    }

    public Void visit (WhereClause where)
    {
        _builder.append(" where ");
        where.getWhereExpression().accept(this);
        return null;
    }

    public Void visit (Key.Expression key)
    {
        Class<? extends PersistentRecord> pClass = key.getPersistentClass();
        ColumnExp[] keyFields = DepotUtil.getKeyFields(pClass);
        Comparable<?>[] values = key.getValues();
        for (int ii = 0; ii < keyFields.length; ii ++) {
            if (ii > 0) {
                _builder.append(" and ");
            }
            // A Key's WHERE clause must mirror what's actually retrieved for the persistent
            // object, so we turn on overrides here just as we do when expanding SELECT fields
            boolean saved = _enableOverrides;
            _enableOverrides = true;
            appendRhsColumn(pClass, keyFields[ii]);
            _enableOverrides = saved;
            if (values[ii] == null) {
                _builder.append(" is null ");
            } else {
                _builder.append(" = ");
                bindValue(values[ii]);
            }
        }
        return null;
    }

    public Void visit (MultiOperator multiOperator)
    {
        SQLExpression[] conditions = multiOperator.getArgs();
        for (int ii = 0; ii < conditions.length; ii++) {
            if (ii > 0) {
                _builder.append(" ").append(multiOperator.operator()).append(" ");
            }
            _builder.append("(");
            conditions[ii].accept(this);
            _builder.append(")");
        }
        return null;
    }

    public Void visit (BinaryOperator binaryOperator)
    {
        _builder.append('(');
        binaryOperator.getLeftHandSide().accept(this);
        _builder.append(binaryOperator.operator());
        binaryOperator.getRightHandSide().accept(this);
        _builder.append(')');
        return null;
    }

    public Void visit (IsNull isNull)
    {
        isNull.getExpression().accept(this);
        _builder.append(" is null");
        return null;
    }

    public Void visit (In in)
    {
        // if the In() expression is empty, replace it with a 'false'
        if (in.getValues().length == 0) {
            new ValueExp(false).accept(this);
            return null;
        }
        in.getExpression().accept(this);
        _builder.append(" in (");
        Comparable<?>[] values = in.getValues();
        for (int ii = 0; ii < values.length; ii ++) {
            if (ii > 0) {
                _builder.append(", ");
            }
            bindValue(values[ii]);
        }
        _builder.append(")");
        return null;
    }

    public abstract Void visit (FullText.Match match);
    public abstract Void visit (FullText.Rank rank);

    public Void visit (Case caseExp)
    {
        _builder.append("(case ");
        for (Tuple<SQLExpression, SQLExpression> tuple : caseExp.getWhenExps()) {
            _builder.append(" when ");
            tuple.left.accept(this);
            _builder.append(" then ");
            tuple.right.accept(this);
        }
        SQLExpression elseExp = caseExp.getElseExp();
        if (elseExp != null) {
            _builder.append(" else ");
            elseExp.accept(this);
        }
        _builder.append(" end)");
        return null;
    }

    public Void visit (ColumnExp columnExp)
    {
        appendRhsColumn(columnExp.getPersistentClass(), columnExp);
        return null;
    }

    public Void visit (Not not)
    {
        _builder.append(" not (");
        not.getCondition().accept(this);
        _builder.append(")");
        return null;
    }

    public Void visit (GroupBy groupBy)
    {
        _builder.append(" group by ");

        SQLExpression[] values = groupBy.getValues();
        for (int ii = 0; ii < values.length; ii++) {
            if (ii > 0) {
                _builder.append(", ");
            }
            values[ii].accept(this);
        }
        return null;
    }

    public Void visit (ForUpdate forUpdate)
    {
        _builder.append(" for update ");
        return null;
    }

    public Void visit (OrderBy orderBy)
    {
        _builder.append(" order by ");

        SQLExpression[] values = orderBy.getValues();
        OrderBy.Order[] orders = orderBy.getOrders();
        for (int ii = 0; ii < values.length; ii++) {
            if (ii > 0) {
                _builder.append(", ");
            }
            values[ii].accept(this);
            _builder.append(" ").append(orders[ii]);
        }
        return null;
    }

    public Void visit (Join join)
    {
        switch (join.getType()) {
        case INNER:
            _builder.append(" inner join " );
            break;
        case LEFT_OUTER:
            _builder.append(" left outer join " );
            break;
        case RIGHT_OUTER:
            _builder.append(" right outer join " );
            break;
        }
        appendTableName(join.getJoinClass());
        _builder.append(" as ");
        appendTableAbbreviation(join.getJoinClass());
        _builder.append(" on ");
        join.getJoinCondition().accept(this);
        return null;
    }

    public Void visit (Limit limit)
    {
        _builder.append(" limit ").append(limit.getCount()).
            append(" offset ").append(limit.getOffset());
        return null;
    }

    public Void visit (LiteralExp literalExp)
    {
        _builder.append(literalExp.getText());
        return null;
    }

    public Void visit (ValueExp valueExp)
    {
        bindValue(valueExp.getValue());
        return null;
    }

    public Void visit (IntervalExp interval)
    {
        _builder.append("interval ").append(interval.amount).append(" ").append(interval.unit);
        return null;
    }

    public Void visit (Exists exists)
    {
        _builder.append("exists ");
        exists.getSubClause().accept(this);
        return null;
    }

    public Void visit (SelectClause selectClause)
    {
        Class<? extends PersistentRecord> pClass = selectClause.getPersistentClass();
        boolean isInner = _innerClause;
        _innerClause = true;

        if (isInner) {
            _builder.append("(");
        }
        _builder.append("select ");

        if (_definitions.containsKey(pClass)) {
            throw new IllegalArgumentException(
                "Can not yet nest SELECTs on the same persistent record.");
        }

        Map<String, FieldDefinition> definitionMap = Maps.newHashMap();
        for (FieldDefinition definition : selectClause.getFieldDefinitions()) {
            definitionMap.put(definition.getField(), definition);
        }
        _definitions.put(pClass, definitionMap);

        try {
            // iterate over the fields we're filling in and figure out whence each one comes
            boolean comma = false;

            // while expanding column names in the SELECT query, do aliasing and expansion
            _enableAliasing = _enableOverrides = true;

            for (ColumnExp field : selectClause.getFields()) {
                // write column to a temporary buffer
                StringBuilder saved = _builder;
                _builder = new StringBuilder();
                appendRhsColumn(pClass, field);
                String column = _builder.toString();
                _builder = saved;

                // append if non-empty
                if (column.length() > 0) {
                    if (comma) {
                        _builder.append(", ");
                    }
                    comma = true;
                    _builder.append(column);
                }
            }

            // then stop
            _enableAliasing = _enableOverrides = false;

            if (selectClause.getFromOverride() != null) {
                selectClause.getFromOverride().accept(this);

            } else {
                Computed computed = _types.getMarshaller(pClass).getComputed();
                Class<? extends PersistentRecord> tClass;
                if (computed != null && !PersistentRecord.class.equals(computed.shadowOf())) {
                    tClass = computed.shadowOf();
                } else if (_types.getTableName(pClass) != null) {
                    tClass = pClass;
                } else {
                    throw new IllegalStateException(
                        "Query on @Computed entity with no FromOverrideClause.");
                }
                _builder.append(" from ");
                appendTableName(tClass);
                _builder.append(" as ");
                appendTableAbbreviation(tClass);
            }

            for (Join clause : selectClause.getJoinClauses()) {
                clause.accept(this);
            }
            if (selectClause.getWhereClause() != null) {
                selectClause.getWhereClause().accept(this);
            }
            if (selectClause.getGroupBy() != null) {
                selectClause.getGroupBy().accept(this);
            }
            if (selectClause.getOrderBy() != null) {
                selectClause.getOrderBy().accept(this);
            }
            if (selectClause.getLimit() != null) {
                selectClause.getLimit().accept(this);
            }
            if (selectClause.getForUpdate() != null) {
                selectClause.getForUpdate().accept(this);
            }

        } finally {
            _definitions.remove(pClass);
        }
        if (isInner) {
            _builder.append(")");
        }
        return null;
    }

    public Void visit (UpdateClause updateClause)
    {
        if (updateClause.getWhereClause() == null) {
            throw new IllegalArgumentException(
                "I dare not currently perform UPDATE without a WHERE clause.");
        }
        Class<? extends PersistentRecord> pClass = updateClause.getPersistentClass();
        _innerClause = true;

        _builder.append("update ");
        appendTableName(pClass);
        _builder.append(" as ");
        appendTableAbbreviation(pClass);
        _builder.append(" set ");

        ColumnExp[] fields = updateClause.getFields();
        Object pojo = updateClause.getPojo();
        SQLExpression[] values = updateClause.getValues();
        for (int ii = 0; ii < fields.length; ii ++) {
            if (ii > 0) {
                _builder.append(", ");
            }
            appendLhsColumn(pClass, fields[ii]);

            _builder.append(" = ");
            if (pojo != null) {
                bindField(pClass, fields[ii], pojo);

            } else {
                values[ii].accept(this);
            }
        }
        updateClause.getWhereClause().accept(this);
        return null;
    }

    public Void visit (DeleteClause deleteClause)
    {
        _builder.append("delete from ");
        appendTableName(deleteClause.getPersistentClass());
        _builder.append(" as ");
        appendTableAbbreviation(deleteClause.getPersistentClass());
        _builder.append(" ");
        deleteClause.getWhereClause().accept(this);
        return null;
    }

    public Void visit (InsertClause insertClause)
    {
        _innerClause = true;
        _builder.append("insert into ");
        appendTableName(insertClause.getPersistentClass());
        _builder.append(" ");
        appendInsertColumns(insertClause);
        return null;
    }

    public Void visit (CreateIndexClause createIndexClause)
    {
        if (!_allowComplexIndices) {
            for (Tuple<SQLExpression, Order> field : createIndexClause.getFields()) {
                if (!(field.left instanceof ColumnExp)) {
                    log.warning("This database can't handle complex indexes. Aborting creation.",
                        "ixName", createIndexClause.getName());
                    return null;
                }
            }
        }
        _builder.append("create ");
        if (createIndexClause.isUnique()) {
            _builder.append("unique ");
        }
        _builder.append("index ");
        appendIdentifier(createIndexClause.getName());
        _builder.append(" on ");
        appendTableName(createIndexClause.getPersistentClass());
        _builder.append(" (");

        // turn off table abbreviations here
        _defaultType = createIndexClause.getPersistentClass();
        boolean comma = false;
        for (Tuple<SQLExpression, Order> field : createIndexClause.getFields()) {
            if (comma) {
                _builder.append(", ");
            }
            comma = true;

            // If the index can't be complex, it doesn't need to be wrapped in parenthesis.  Some
            // databases can't handle parenthesis around their index expressions either, so if a
            // complex expression is already disallowed, don't wrap it in parenthesis.
            if (_allowComplexIndices) {
                _builder.append("(");
            }
            field.left.accept(this);
            if (_allowComplexIndices) {
                _builder.append(")");
            }
            if (field.right == Order.DESC) {
                // ascending is default, print nothing unless explicitly descending
                _builder.append(" desc");
            }
        }
        // turn them back on
        _defaultType = null;

        _builder.append(")");
        return null;
    }

    public Void visit (DropIndexClause dropIndexClause)
    {
        _builder.append("drop index ");
        appendIdentifier(dropIndexClause.getName());
        return null;
    }

    //
    // NUMERICAL FUNCTIONS

    public Void visit (Abs exp)
    {
        return appendFunctionCall("abs", exp.getArg());
    }

    public Void visit (Ceil exp)
    {
        return appendFunctionCall("ceil", exp.getArg());
    }

    public Void visit (Exp exp)
    {
        return appendFunctionCall("exp", exp.getArg());
    }

    public Void visit (Floor exp)
    {
        return appendFunctionCall("floor", exp.getArg());
    }

    public Void visit (Ln exp)
    {
        return appendFunctionCall("ln", exp.getArg());
    }

    public Void visit (Log10 exp)
    {
        return appendFunctionCall("log", exp.getArg());
    }

    public Void visit (Pi exp)
    {
        return appendFunctionCall("PI");
    }

    public Void visit (Power exp)
    {
        return appendFunctionCall("power", exp.getPower(), exp.getValue());
    }

    public Void visit (Random exp)
    {
        return appendFunctionCall("random");
    }

    public Void visit (Round exp)
    {
        return appendFunctionCall("round", exp.getArg());
    }

    public Void visit (Sign exp)
    {
        return appendFunctionCall("sign", exp.getArg());
    }

    public Void visit (Sqrt exp)
    {
        return appendFunctionCall("sqrt", exp.getArg());
    }

    public Void visit (Trunc exp)
    {
        return appendFunctionCall("trunc", exp.getArg());
    }

    //
    // STRING FUNCTIONS

    public Void visit (Length exp)
    {
        return appendFunctionCall("length", exp.getArg());
    }

    public Void visit (Lower exp)
    {
        return appendFunctionCall("lower", exp.getArg());
    }

    public Void visit (Position exp)
    {
        _builder.append(" position(").append(exp.getSubString()).append(" in ").
            append(exp.getString()).append(")");
        return null;
    }

    public Void visit (Substring exp)
    {
        return appendFunctionCall("substr", exp.getArgs());
    }

    public Void visit (Trim exp)
    {
        return appendFunctionCall("trim", exp.getArg());
    }

    public Void visit (Upper exp)
    {
        return appendFunctionCall("upper", exp.getArg());
    }

    public abstract Void visit (DatePart exp);

    public abstract Void visit (DateTruncate exp);

    public Void visit (Now exp)
    {
        appendFunctionCall("now");
        return null;
    }

    public Void visit (Average exp)
    {
        return appendAggregateFunctionCall("average", exp);
    }

    public Void visit (Count exp)
    {
        return appendAggregateFunctionCall("count", exp);
    }

    public Void visit (Every exp)
    {
        return appendAggregateFunctionCall("every", exp);
    }

    public Void visit (Max exp)
    {
        return appendAggregateFunctionCall("max", exp);
    }

    public Void visit (Min exp)
    {
        return appendAggregateFunctionCall("min", exp);
    }

    public Void visit (Sum exp)
    {
        return appendAggregateFunctionCall("sum", exp);
    }

    //
    // CONDITIONAL FUNCTIONS

    public Void visit (Coalesce exp)
    {
        return appendFunctionCall("coalesce", exp.getArgs());
    }

    public Void visit (Greatest exp)
    {
        return appendFunctionCall("greatest", exp.getArgs());
    }

    public Void visit (Least exp)
    {
        return appendFunctionCall("least", exp.getArgs());
    }

    protected Void appendAggregateFunctionCall (String function, AggregateFun exp)
    {
        _builder.append(" ").append(function).append("(");
        if (exp.isDistinct()) {
            _builder.append("DISTINCT ");
        }
        appendArguments(exp.getArg());
        _builder.append(")");
        return null;
    }

    protected Void appendFunctionCall (String function, SQLExpression... args)
    {
        _builder.append(" ").append(function).append("(");
        appendArguments(args);
        _builder.append(")");
        return null;
    }

    protected Void appendArguments (SQLExpression... args)
    {
        for (int ii = 0; ii < args.length; ii ++) {
            if (ii > 0) {
                _builder.append(", ");
            }
            (args[ii]).accept(this);
        }
        return null;
    }

    protected Void bindValue (Object object)
    {
        _bindables.add(newBindable(object));
        _builder.append("?");
        return null;
    }

    protected Void bindField (
        Class<? extends PersistentRecord> pClass, ColumnExp field, Object pojo)
    {
        final DepotMarshaller<?> marshaller = _types.getMarshaller(pClass);
        _bindables.add(newBindable(marshaller, field, pojo));
        _builder.append("?");
        return null;
    }

    protected abstract void appendIdentifier (String field);

    protected void appendTableName (Class<? extends PersistentRecord> type)
    {
        appendIdentifier(_types.getTableName(type));
    }

    protected void appendTableAbbreviation (Class<? extends PersistentRecord> type)
    {
        appendIdentifier(_types.getTableAbbreviation(type));
    }

    // Constructs a name used for assignment in e.g. INSERT/UPDATE. This is the SQL
    // equivalent of an lvalue; something that can appear to the left of an equals sign.
    // We do not prepend this identifier with a table abbreviation, nor do we expand
    // field overrides, shadowOf declarations, or the like: it is just a column name.
    protected void appendLhsColumn (Class<? extends PersistentRecord> type, ColumnExp field)
    {
        // TODO: nix type and use the class from the supplied ColumnExp
        DepotMarshaller<?> dm = _types.getMarshaller(type);
        if (dm == null) {
            throw new IllegalArgumentException(
                "Unknown field on persistent record [record=" + type + ", field=" + field + "]");
        }

        FieldMarshaller<?> fm = dm.getFieldMarshaller(field.name);
        appendIdentifier(fm.getColumnName());
    }

    // Appends an expression for the given field on the given persistent record; this can
    // appear in a SELECT list, in WHERE clauses, etc, etc.
    protected void appendRhsColumn (Class<? extends PersistentRecord> type, ColumnExp field)
    {
        DepotMarshaller<?> dm = _types.getMarshaller(type);
        if (dm == null) {
            throw new IllegalArgumentException(
                "Unknown field on persistent record [record=" + type + ", field=" + field + "]");
        }

        // first, see if there's a field definition
        FieldMarshaller<?> fm = dm.getFieldMarshaller(field.name);
        Map<String, FieldDefinition> fieldDefs = _definitions.get(type);
        if (fieldDefs != null) {
            FieldDefinition fieldDef = fieldDefs.get(field.name);
            if (fieldDef != null) {
                boolean useOverride;
                if (fieldDef instanceof FieldOverride) {
                    if (fm.getComputed() != null && dm.getComputed() != null) {
                        throw new IllegalArgumentException(
                            "FieldOverride cannot be used on @Computed field: " + field);
                    }
                    useOverride = _enableOverrides;
                } else if (fm.getComputed() == null && dm.getComputed() == null) {
                    throw new IllegalArgumentException(
                        "FieldDefinition must not be used on concrete field: " + field);
                } else {
                    useOverride = true;
                }

                if (useOverride) {
                    // If a FieldOverride's target is in turn another FieldOverride, the second one
                    // is ignored. As an example, when creating ItemRecords from CloneRecords, we
                    // make Item.itemId = Clone.itemId. We also make Item.parentId = Item.itemId
                    // and would be dismayed to find Item.parentID = Item.itemId = Clone.itemId.
                    boolean saved = _enableOverrides;
                    _enableOverrides = false;
                    fieldDef.accept(this);
                    if (_enableAliasing) {
                        _builder.append(" as ");
                        appendIdentifier(fm.getColumnName());
                    }
                    _enableOverrides = saved;
                    return;
                }
            }
        }

        Computed entityComputed = dm.getComputed();

        // figure out the class we're selecting from unless we're otherwise overriden:
        // for a concrete record, simply use the corresponding table; for a computed one,
        // default to the shadowed concrete record, or null if there isn't one
        Class<? extends PersistentRecord> tableClass;
        if (entityComputed == null) {
            tableClass = type;
        } else if (!PersistentRecord.class.equals(entityComputed.shadowOf())) {
            tableClass = entityComputed.shadowOf();
        } else {
            tableClass = null;
        }

        // handle the field-level @Computed annotation, if there is one
        Computed fieldComputed = fm.getComputed();
        if (fieldComputed != null) {
            // check if the computed field has a literal SQL definition
            if (fieldComputed.fieldDefinition().length() > 0) {
                _builder.append(fieldComputed.fieldDefinition());
                if (_enableAliasing) {
                    _builder.append(" as ");
                    appendIdentifier(fm.getColumnName());
                }
                return;
            }

            // or if we can simply ignore the field
            if (!fieldComputed.required()) {
                return;
            }

            // else see if there's an overriding shadowOf definition
            if (fieldComputed.shadowOf() != null) {
                tableClass = fieldComputed.shadowOf();
            }
        }

        // if we get this far we hopefully have a table to select from, if not we're probably doing
        // something like an order by on a synthetic field "select count(distinct foo) as bar from
        // ... order by bar", so we just skip the table qualifier
        if (tableClass != null && _defaultType != tableClass) {
            appendTableAbbreviation(tableClass);
            _builder.append(".");
        }

        // if the field is shadowed, be sure to use the shadowed column's name
        if (tableClass != type && tableClass != null) {
            appendIdentifier(_types.getColumnName(tableClass, field.name));
        } else {
            appendIdentifier(fm.getColumnName());
        }
    }

    // output the column names and values for an insert
    protected void appendInsertColumns (InsertClause insertClause)
    {
        Class<? extends PersistentRecord> pClass = insertClause.getPersistentClass();
        Object pojo = insertClause.getPojo();
        DepotMarshaller<?> marsh = _types.getMarshaller(pClass);
        Set<String> idFields = insertClause.getIdentityFields();
        ColumnExp[] fields = marsh.getColumnFieldNames();

        _builder.append("(");
        boolean comma = false;
        for (ColumnExp field : fields) {
            if (idFields.contains(field.name)) {
                continue;
            }
            if (comma) {
                _builder.append(", ");
            }
            comma = true;
            appendLhsColumn(pClass, field);
        }
        _builder.append(") values (");

        comma = false;
        for (ColumnExp field : fields) {
            if (idFields.contains(field.name)) {
                continue;
            }
            if (comma) {
                _builder.append(", ");
            }
            comma = true;
            bindField(pClass, field, pojo);
        }
        _builder.append(")");
    }

    protected BuildVisitor (DepotTypes types, boolean allowComplexIndices)
    {
        _types = types;
        _allowComplexIndices = allowComplexIndices;
    }

    protected static interface Bindable
    {
        void doBind (Connection conn, PreparedStatement stmt, int argIx) throws Exception;
    }

    protected static Bindable newBindable (
        final DepotMarshaller<?> marshaller, final ColumnExp field, final Object pojo)
    {
        return new Bindable() {
            public void doBind (Connection conn, PreparedStatement stmt, int argIx)
                throws Exception {
                marshaller.getFieldMarshaller(field.name).getAndWriteToStatement(stmt, argIx, pojo);
            }
        };
    }

    protected static Bindable newBindable (final Object value)
    {
        return new Bindable() {
            public void doBind (Connection conn, PreparedStatement stmt, int argIx)
                throws Exception {
                // TODO: how can we abstract this fieldless marshalling
                if (value instanceof ByteEnum) {
                    // byte enums require special conversion
                    stmt.setByte(argIx, ((ByteEnum)value).toByte());
                } else if (value instanceof int[]) {
                    // int arrays require conversion to byte arrays
                    int[] data = (int[])value;
                    ByteBuffer bbuf = ByteBuffer.allocate(data.length * 4);
                    bbuf.asIntBuffer().put(data);
                    stmt.setObject(argIx, bbuf.array());
                } else {
                    stmt.setObject(argIx, value);
                }
            }
        };
    }

    protected DepotTypes _types;

    /** For each SQL parameter ? we add an {@link Comparable} to bind to this list. */
    protected List<Bindable> _bindables = Lists.newLinkedList();

    /** A StringBuilder to hold the constructed SQL. */
    protected StringBuilder _builder = new StringBuilder();

    /** A mapping of field overrides per persistent record. */
    protected Map<Class<? extends PersistentRecord>, Map<String, FieldDefinition>> _definitions=
        Maps.newHashMap();

    /** Set this to non-null to suppress table abbreviations from being prepended for a class. */
    protected Class<? extends PersistentRecord> _defaultType;

    /** A flag that's set to true for inner SELECT's */
    protected boolean _innerClause = false;
    protected boolean _enableOverrides = false;
    protected boolean _enableAliasing = false;

    /** If this database allows complex expressions in its indices. */
    protected final boolean _allowComplexIndices;
}
