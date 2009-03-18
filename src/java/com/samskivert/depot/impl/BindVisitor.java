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
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.depot.ByteEnum;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Key;
import com.samskivert.depot.MultiKey;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.clause.FieldDefinition;
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

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;

import com.samskivert.depot.operator.Conditionals.Case;
import com.samskivert.depot.operator.Conditionals.Exists;
import com.samskivert.depot.operator.Conditionals.FullText;
import com.samskivert.depot.operator.Conditionals.In;
import com.samskivert.depot.operator.Conditionals.IsNull;
import com.samskivert.depot.operator.Logic.Not;
import com.samskivert.depot.operator.SQLOperator.BinaryOperator;
import com.samskivert.depot.operator.SQLOperator.MultiOperator;

import com.samskivert.depot.impl.clause.CreateIndexClause;
import com.samskivert.depot.impl.clause.DeleteClause;
import com.samskivert.depot.impl.clause.DropIndexClause;
import com.samskivert.depot.impl.clause.UpdateClause;

/**
 * Implements the base functionality of the argument-binding pass of {@link SQLBuilder}. Dialectal
 * subclasses of this should be created and returned from {@link SQLBuilder#getBindVisitor}.
 *
 * This class is intimately paired with {#link BuildVisitor}.
 */
public class BindVisitor implements ExpressionVisitor
{
    public void visit (FromOverride override)
    {
        // nothing needed
    }

    public void visit (FieldDefinition fieldOverride)
    {
        // nothing needed
    }

    public void visit (Key.Expression<? extends PersistentRecord> key)
    {
        for (Comparable<?> value : key.getValues()) {
            if (value != null) {
                writeValueToStatement(value);
            }
        }
    }

    public void visit (MultiKey<? extends PersistentRecord> key)
    {
        for (Map.Entry<String, Comparable<?>> entry : key.getSingleFieldsMap().entrySet()) {
            if (entry.getValue() != null) {
                writeValueToStatement(entry.getValue());
            }
        }
        Comparable<?>[] values = key.getMultiValues();
        for (int ii = 0; ii < values.length; ii++) {
            writeValueToStatement(values[ii]);
        }
    }

    public void visit (FunctionExp functionExp)
    {
        visit(functionExp.getArguments());
    }

    public void visit (EpochSeconds epochSeconds)
    {
        epochSeconds.getArgument().accept(this);
    }

    public void visit (MultiOperator multiOperator)
    {
        visit(multiOperator.getConditions());
    }

    public void visit (BinaryOperator binaryOperator)
    {
        binaryOperator.getLeftHandSide().accept(this);
        binaryOperator.getRightHandSide().accept(this);
    }

    public void visit (IsNull isNull)
    {
    }

    public void visit (In in)
    {
        Comparable<?>[] values = in.getValues();
        for (int ii = 0; ii < values.length; ii++) {
            writeValueToStatement(values[ii]);
        }
    }

    public void visit (FullText.Match match)
    {
        // we never get here
    }

    public void visit (FullText.Rank rank)
    {
        // we never get here
    }

    public void visit (Case caseExp)
    {
        for (Tuple<SQLExpression, SQLExpression> tuple : caseExp.getWhenExps()) {
            tuple.left.accept(this);
            tuple.right.accept(this);
        }
        SQLExpression elseExp = caseExp.getElseExp();
        if (elseExp != null) {
            elseExp.accept(this);
        }
    }
    
    public void visit (ColumnExp columnExp)
    {
        // no arguments
    }

    public void visit (Not not)
    {
        not.getCondition().accept(this);
    }

    public void visit (GroupBy groupBy)
    {
        visit(groupBy.getValues());
    }

    public void visit (ForUpdate forUpdate)
    {
        // do nothing
    }

    public void visit (OrderBy orderBy)
    {
        visit(orderBy.getValues());
    }

    public void visit (WhereClause where)
    {
        where.getWhereExpression().accept(this);
    }

    public void visit (Join join)
    {
        join.getJoinCondition().accept(this);
    }

    public void visit (Limit limit)
    {
        try {
            _stmt.setInt(_argIdx++, limit.getCount());
            _stmt.setInt(_argIdx++, limit.getOffset());
        } catch (SQLException sqe) {
            throw new DatabaseException("Failed to configure statement with limit clause " +
                                        "[count=" + limit.getCount() +
                                        ", offset=" + limit.getOffset() + "]", sqe);
        }
    }

    public void visit (LiteralExp literalExp)
    {
        // do nothing
    }

    public void visit (ValueExp valueExp)
    {
        writeValueToStatement(valueExp.getValue());
    }

    public void visit (Exists<? extends PersistentRecord> exists)
    {
        exists.getSubClause().accept(this);
    }

    public void visit (SelectClause<? extends PersistentRecord> selectClause)
    {
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
    }

    public void visit (UpdateClause<? extends PersistentRecord> updateClause)
    {
        DepotMarshaller<?> marsh = _types.getMarshaller(updateClause.getPersistentClass());

        // bind the update arguments
        Object pojo = updateClause.getPojo();
        if (pojo != null) {
            for (String field : updateClause.getFields()) {
                try {
                    marsh.getFieldMarshaller(field).getAndWriteToStatement(_stmt, _argIdx++, pojo);
                } catch (Exception e) {
                    throw new DatabaseException(
                        "Failed to read field from persistent record and write it to prepared " +
                        "statement [field=" + field + "]", e);
                }
            }
        } else {
            visit(updateClause.getValues());
        }
        updateClause.getWhereClause().accept(this);
    }

    public void visit (InsertClause insertClause)
    {
        DepotMarshaller<?> marsh = _types.getMarshaller(insertClause.getPersistentClass());
        Object pojo = insertClause.getPojo();
        Set<String> idFields = insertClause.getIdentityFields();
        for (String field : marsh.getColumnFieldNames()) {
            if (!idFields.contains(field)) {
                try {
                    marsh.getFieldMarshaller(field).getAndWriteToStatement(_stmt, _argIdx++, pojo);
                } catch (Exception e) {
                    throw new DatabaseException(
                        "Failed to read field from persistent record and write it to prepared " +
                        "statement [field=" + field + "]", e);
                }
            }
        }
    }

    public void visit (DeleteClause deleteClause)
    {
        deleteClause.getWhereClause().accept(this);
    }

    public void visit (CreateIndexClause createIndexClause)
    {
        for (Tuple<SQLExpression, Order> field : createIndexClause.getFields()) {
            field.left.accept(this);
        }
    }

    public void visit (DropIndexClause<? extends PersistentRecord> createIndexClause)
    {
        // do nothing
    }

    protected BindVisitor (DepotTypes types, Connection conn, PreparedStatement stmt)
    {
        _types = types;
        _conn = conn;
        _stmt = stmt;
        _argIdx = 1;
    }

    protected void visit (SQLExpression[] expressions)
    {
        for (int ii = 0; ii < expressions.length; ii ++) {
            expressions[ii].accept(this);
        }
    }

    // write the value to the next argument slot in the prepared statement
    protected void writeValueToStatement (Object value)
    {
        try {
            // TODO: how can we abstract this fieldless marshalling
            if (value instanceof ByteEnum) {
                // byte enums require special conversion
                _stmt.setByte(_argIdx++, ((ByteEnum)value).toByte());
            } else if (value instanceof int[]) {
                // int arrays require conversion to byte arrays
                int[] data = (int[])value;
                ByteBuffer bbuf = ByteBuffer.allocate(data.length * 4);
                bbuf.asIntBuffer().put(data);
                _stmt.setObject(_argIdx++, bbuf.array());
            } else {
                _stmt.setObject(_argIdx++, value);
            }
        } catch (SQLException sqe) {
            throw new DatabaseException("Failed to write value to statement [idx=" + (_argIdx-1) +
                                        ", value=" + StringUtil.safeToString(value) + "]", sqe);
        }
    }

    protected DepotTypes _types;
    protected Connection _conn;
    protected PreparedStatement _stmt;
    protected int _argIdx;
}
