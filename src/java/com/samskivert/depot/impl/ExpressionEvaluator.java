//
// $Id: ExpressionEvaluator.java 377 2009-01-08 02:31:28Z samskivert $
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and PÃ¤r Winzell
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
import java.util.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.ForUpdate;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.InsertClause;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.WhereClause;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.IntervalExp;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.SQLExpression.NoValue;
import com.samskivert.depot.expression.ValueExp;

import com.samskivert.depot.operator.Case;
import com.samskivert.depot.operator.Exists;
import com.samskivert.depot.operator.FullText;
import com.samskivert.depot.operator.In;
import com.samskivert.depot.operator.IsNull;
import com.samskivert.depot.operator.Not;
import com.samskivert.depot.operator.SQLOperator.BinaryOperator;
import com.samskivert.depot.operator.SQLOperator.MultiOperator;

import com.samskivert.depot.impl.clause.CreateIndexClause;
import com.samskivert.depot.impl.clause.DeleteClause;
import com.samskivert.depot.impl.clause.DropIndexClause;
import com.samskivert.depot.impl.clause.UpdateClause;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Tuple;

import static com.samskivert.depot.Log.log;

/**
 * Enumerates visitation methods for every possible SQL expression type.
 */
public class ExpressionEvaluator
    implements ExpressionVisitor<Object>
{
    public <T extends PersistentRecord> ExpressionEvaluator (Class<T> pClass, T pRec)
    {
        _pClass = pClass;
        _pRec = pRec;
    }

    public Object visit (FunctionExp functionExp)
    {
        SQLExpression[] arguments = functionExp.getArguments();
        Object[] values = new Object[functionExp.getArguments().length];
        for (int ii = 0; ii < values.length; ii ++) {
            values[ii] = arguments[ii].accept(this);
            if (values[ii] instanceof NoValue) {
                return values[ii];
            }
        }
        if ("lower".equalsIgnoreCase(functionExp.getFunction())) {
            if (values.length == 1 && values[0] instanceof String) {
                return ((String) values[0]).toLowerCase();
            }
        }
        return new NoValue("Bad Function: " + functionExp);
    }

    public Object visit (EpochSeconds epochSeconds)
    {
        Object result = epochSeconds.getArgument().accept(this);
        if (result instanceof Date) {
            return ((Date) result).getTime();
        }
        return new NoValue("Bad EpochSeconds: " + epochSeconds);
    }

    public Object visit (MultiOperator multiOperator)
    {
        SQLExpression[] operands = multiOperator.getOperands();
        Object[] values = new Object[operands.length];
        for (int ii = 0; ii < operands.length; ii ++) {
            values[ii] = operands[ii].accept(this);
            if (values[ii] instanceof NoValue) {
                return values[ii];
            }
        }

        return multiOperator.evaluate(values);
    }

    public Object visit (BinaryOperator binaryOperator)
    {
        Object left = binaryOperator.getLeftHandSide().accept(this);
        Object right = binaryOperator.getRightHandSide().accept(this);
        if (left instanceof NoValue) {
            return left;
        }
        if (right instanceof NoValue) {
            return right;
        }
        return binaryOperator.evaluate(left, right);
    }

    public Object visit (IsNull isNull)
    {
        Object operand = isNull.getColumn().accept(this);
        return (operand instanceof NoValue) ? operand : operand != null;
    }

    public Object visit (In in)
    {
        Object operand = in.getColumn().accept(this);
        return (operand instanceof NoValue) ? operand :
            -1 != ArrayUtil.indexOf(in.getValues(), operand);
    }

    public Object visit (FullText.Match match)
    {
        return new NoValue("Full Text Match not implemented");
    }

    public Object visit (FullText.Rank rank)
    {
        return new NoValue("Full Text Match not implemented");
    }

    public Object visit (Case caseExp)
    {
        for (Tuple<SQLExpression, SQLExpression> exp : caseExp.getWhenExps()) {
            Object result = exp.left.accept(this);
            if (result instanceof NoValue || !(result instanceof Boolean)) {
                return new NoValue("Failed to evaluate case: " + exp.left + " -> " + result);
            }
            if (((Boolean) result).booleanValue()) {
                return exp.right.accept(this);
            }
        }
        SQLExpression elseExp = caseExp.getElseExp();
        if (elseExp != null) {
            return elseExp.accept(this);
        }
        return null;
    }

    public Object visit (ColumnExp columnExp)
    {
        Class<? extends PersistentRecord> pClass = columnExp.getPersistentClass();
        if (pClass != _pClass) {
            // TODO: Accept Class -> Record mapping
            return new NoValue("Column lookup on unknown persistent class: " + pClass);
        }
        try {
            Field field = pClass.getField(columnExp.getField());
            if (field == null) {
                log.warning("Couldn't locate field on class", "field", columnExp.getField(),
                    "class", pClass);
                return new NoValue("Internal Error");
            }
            return field.get(_pRec);
        } catch (Exception e) {
            log.warning("Failed to retrieve field value", "field", columnExp.getField(), e);
            return new NoValue("Internal Error");
        }
    }

    public Object visit (Not not)
    {
        Object result = not.getCondition().accept(this);

        if (result instanceof NoValue) {
            return result;
        }
        if (result instanceof Boolean) {
            return !((Boolean) result).booleanValue();
        }
        return new NoValue("Boolean negation of non-boolean value: " + result);
    }

    public Object visit (LiteralExp literalExp)
    {
        return new NoValue("Cannot evaluate LiteralExp: " + literalExp);
    }

    public Object visit (ValueExp valueExp)
    {
        return valueExp.getValue();
    }

    public Object visit (IntervalExp interval)
    {
        return new NoValue("Cannot evaluate IntervalExp: " + interval);
    }

    public Object visit (WhereClause where)
    {
        Object result = where.getWhereExpression().accept(this);
        if (result instanceof NoValue || result instanceof Boolean) {
            return result;
        }
        return new NoValue("Non-boolean result from Where expression: " + result);
    }

    public Object visit (Key.Expression<? extends PersistentRecord> key)
    {
        Class<? extends PersistentRecord> pClass = key.getPersistentClass();
        if (pClass != _pClass) {
            // TODO: Accept Class -> Record mapping
            return new NoValue("Column lookup on unknown persistent class: " + pClass);
        }

        String[] keyFields = DepotUtil.getKeyFields(pClass);
        Comparable<?>[] values = key.getValues();

        for (int ii = 0; ii < keyFields.length; ii ++) {
            Object value;
            try {
                value = pClass.getDeclaredField(keyFields[ii]).get(_pRec);
            } catch (Exception e) {
                log.warning("Failed to retrieve field value", "field", keyFields[ii], e);
                return new NoValue("Internal Error");
            }
            if (value == null) {
                if (values[ii] != null) {
                    return false;
                }
            } else if (!value.equals(values[ii])) {
                return false;
            }
        }
        return true;
    }

    public Object visit (Exists<? extends PersistentRecord> exists)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exists);
    }

    public Object visit (GroupBy groupBy)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + groupBy);
    }

    public Object visit (ForUpdate forUpdate)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + forUpdate);
    }

    public Object visit (OrderBy orderBy)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + orderBy);
    }

    public Object visit (Join join)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + join);
    }

    public Object visit (Limit limit)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + limit);
    }

    public Object visit (FieldDefinition fieldOverride)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + fieldOverride);
    }

    public Object visit (FromOverride fromOverride)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + fromOverride);
    }

    public Object visit (SelectClause selectClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + selectClause);
    }

    public Object visit (UpdateClause updateClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + updateClause);
    }

    public Object visit (DeleteClause deleteClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + deleteClause);
    }

    public Object visit (InsertClause insertClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + insertClause);
    }

    public Object visit (CreateIndexClause createIndexClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + createIndexClause);
    }

    public Object visit (DropIndexClause<? extends PersistentRecord> dropIndexClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + dropIndexClause);
    }

    public static Double numerical (Object o)
    {
        return (o instanceof Number) ? ((Number) o).doubleValue() : null;
    }

    public static Long integral (Object o)
    {
        return ((o instanceof Integer) || (o instanceof Long)) ? ((Number) o).longValue() : null;
    }

    protected Class<? extends PersistentRecord> _pClass;
    protected PersistentRecord _pRec;
}
