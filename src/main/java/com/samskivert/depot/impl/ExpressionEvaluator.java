//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.lang.reflect.Field;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.clause.Distinct;
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

import com.samskivert.depot.expression.*;
import com.samskivert.depot.expression.SQLExpression.NoValue;

import com.samskivert.depot.operator.Case;
import com.samskivert.depot.operator.FullText;

import com.samskivert.depot.impl.clause.CreateIndexClause;
import com.samskivert.depot.impl.clause.DeleteClause;
import com.samskivert.depot.impl.clause.DropIndexClause;
import com.samskivert.depot.impl.clause.UpdateClause;
import com.samskivert.depot.impl.expression.IntervalExp;
import com.samskivert.depot.impl.expression.LiteralExp;
import com.samskivert.depot.impl.expression.RandomExp;
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
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Tuple;

import static com.samskivert.depot.Log.log;

/**
 * Attempts to compute the actual values different SQL constructs would yield if they were
 * actually send to the database to operate on rows, rather than on in-memory data objects.
 *
 * TODO: Many of the classes in com.samskivert.depot.functions.* have excellent implementations
 * TODO: that should be written.
 */
public class ExpressionEvaluator
    implements FragmentVisitor<Object>
{
    public <T extends PersistentRecord> ExpressionEvaluator (Class<T> pClass, T pRec)
    {
        _pClass = pClass;
        _pRec = pRec;
    }

    public Object visit (MultiOperator<?> multiOperator)
    {
        SQLExpression<?>[] operands = multiOperator.getArgs();
        Object[] values = new Object[operands.length];
        for (int ii = 0; ii < operands.length; ii ++) {
            values[ii] = operands[ii].accept(this);
            if (values[ii] instanceof NoValue) {
                return values[ii];
            }
        }

        return multiOperator.evaluate(values);
    }

    public Object visit (BinaryOperator<?> binaryOperator)
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
        Object operand = isNull.getExpression().accept(this);
        return (operand instanceof NoValue) ? operand : operand != null;
    }

    public Object visit (In in)
    {
        Object operand = in.getExpression().accept(this);
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

    public Object visit (Case<?> caseExp)
    {
        for (Tuple<SQLExpression<?>, SQLExpression<?>> exp : caseExp.getWhenExps()) {
            Object result = exp.left.accept(this);
            if (result instanceof NoValue || !(result instanceof Boolean)) {
                return new NoValue("Failed to evaluate case: " + exp.left + " -> " + result);
            }
            if (((Boolean) result).booleanValue()) {
                return exp.right.accept(this);
            }
        }
        SQLExpression<?> elseExp = caseExp.getElseExp();
        if (elseExp != null) {
            return elseExp.accept(this);
        }
        return null;
    }

    public Object visit (ColumnExp<?> columnExp)
    {
        Class<? extends PersistentRecord> pClass = columnExp.getPersistentClass();
        if (pClass != _pClass) {
            // TODO: Accept Class -> Record mapping
            return new NoValue("Column lookup on unknown persistent class: " + pClass);
        }
        try {
            Field field = pClass.getField(columnExp.name);
            if (field == null) {
                log.warning("Couldn't locate field on class", "field", columnExp.name,
                            "class", pClass);
                return new NoValue("Internal Error");
            }
            return field.get(_pRec);
        } catch (Exception e) {
            log.warning("Failed to retrieve field value", "field", columnExp.name, e);
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

    public Object visit (LiteralExp<?> literalExp)
    {
        return new NoValue("Cannot evaluate LiteralExp: " + literalExp);
    }

    public Object visit (RandomExp randomExp)
    {
        return new NoValue("Cannot evaluate RandomExp");
    }

    public Object visit (ValueExp<?> valueExp)
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

    public Object visit (Key.Expression key)
    {
        Class<? extends PersistentRecord> pClass = key.getPersistentClass();
        if (pClass != _pClass) {
            // TODO: Accept Class -> Record mapping
            return new NoValue("Column lookup on unknown persistent class: " + pClass);
        }

        ColumnExp<?>[] keyFields = DepotUtil.getKeyFields(pClass);
        Comparable<?>[] values = key.getValues();

        for (int ii = 0; ii < keyFields.length; ii ++) {
            Object value;
            try {
                value = pClass.getDeclaredField(keyFields[ii].name).get(_pRec);
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

    public Object visit (Exists exists)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exists);
    }

    public Object visit (Distinct distinct)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + distinct);
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

    public Object visit (DropIndexClause dropIndexClause)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + dropIndexClause);
    }

    //
    // NUMERICAL FUNCTIONS

    public Void visit (Abs<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Ceil<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Exp<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Floor<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Ln<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Log10<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Pi<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Power<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Random<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Round<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Sign<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Sqrt<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Trunc<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    //
    // STRING FUNCTIONS

    public Void visit (Length exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Lower exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Position exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Substring exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Trim exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Upper exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (DatePart exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (DateTruncate exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Now exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Average<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Count exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Every exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Max<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Min<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Sum<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    //
    // CONDITIONAL FUNCTIONS

    public Void visit (Coalesce<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Greatest<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
    }

    public Void visit (Least<?> exp)
    {
        throw new IllegalArgumentException("Can't evaluate expression: " + exp);
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
