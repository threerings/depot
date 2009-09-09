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

import com.samskivert.depot.Key;

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
import com.samskivert.depot.function.AggregateFun.Average;
import com.samskivert.depot.function.AggregateFun.Count;
import com.samskivert.depot.function.AggregateFun.Every;
import com.samskivert.depot.function.AggregateFun.Max;
import com.samskivert.depot.function.AggregateFun.Min;
import com.samskivert.depot.function.AggregateFun.Sum;
import com.samskivert.depot.function.ConditionalFun.Coalesce;
import com.samskivert.depot.function.ConditionalFun.Greatest;
import com.samskivert.depot.function.ConditionalFun.Least;
import com.samskivert.depot.function.DateFun.DatePart;
import com.samskivert.depot.function.DateFun.DateTruncate;
import com.samskivert.depot.function.DateFun.Now;
import com.samskivert.depot.function.NumericalFun.Abs;
import com.samskivert.depot.function.NumericalFun.Ceil;
import com.samskivert.depot.function.NumericalFun.Exp;
import com.samskivert.depot.function.NumericalFun.Floor;
import com.samskivert.depot.function.NumericalFun.Ln;
import com.samskivert.depot.function.NumericalFun.LogN;
import com.samskivert.depot.function.NumericalFun.Pi;
import com.samskivert.depot.function.NumericalFun.Power;
import com.samskivert.depot.function.NumericalFun.Random;
import com.samskivert.depot.function.NumericalFun.Round;
import com.samskivert.depot.function.NumericalFun.Sign;
import com.samskivert.depot.function.NumericalFun.Sqrt;
import com.samskivert.depot.function.NumericalFun.Trunc;
import com.samskivert.depot.function.StringFun.Length;
import com.samskivert.depot.function.StringFun.Lower;
import com.samskivert.depot.function.StringFun.Position;
import com.samskivert.depot.function.StringFun.Substring;
import com.samskivert.depot.function.StringFun.Trim;
import com.samskivert.depot.function.StringFun.Upper;
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

/**
 * Enumerates visitation methods for every possible SQL expression type.
 */
public interface ExpressionVisitor<T>
{
    public T visit (FieldDefinition fieldOverride);
    @SuppressWarnings("deprecation")
    public T visit (FunctionExp functionExp);
    @SuppressWarnings("deprecation")
    public T visit (EpochSeconds epochSeconds);
    public T visit (FromOverride fromOverride);
    public T visit (MultiOperator multiOperator);
    public T visit (BinaryOperator binaryOperator);
    public T visit (IsNull isNull);
    public T visit (In in);
    public T visit (FullText.Match match);
    public T visit (FullText.Rank match);
    public T visit (ColumnExp columnExp);
    public T visit (Not not);
    public T visit (GroupBy groupBy);
    public T visit (ForUpdate forUpdate);
    public T visit (OrderBy orderBy);
    public T visit (Join join);
    public T visit (Limit limit);
    public T visit (LiteralExp literal);
    public T visit (ValueExp value);
    public T visit (IntervalExp interval);
    public T visit (WhereClause where);
    public T visit (Key.Expression key);
    public T visit (Exists exists);
    public T visit (SelectClause selectClause);
    public T visit (UpdateClause updateClause);
    public T visit (DeleteClause deleteClause);
    public T visit (InsertClause insertClause);
    public T visit (CreateIndexClause createIndexClause);
    public T visit (DropIndexClause dropIndexClause);
    public T visit (Case caseExp);

    // Numerical
    public T visit (Abs exp);
    public T visit (Ceil exp);
    public T visit (Exp exp);
    public T visit (Floor exp);
    public T visit (Ln exp);
    public T visit (LogN exp);
    public T visit (Pi exp);
    public T visit (Power exp);
    public T visit (Random exp);
    public T visit (Round exp);
    public T visit (Sign exp);
    public T visit (Sqrt exp);
    public T visit (Trunc exp);

    // String
    public T visit (Length exp);
    public T visit (Lower exp);
    public T visit (Position exp);
    public T visit (Substring exp);
    public T visit (Trim exp);
    public T visit (Upper exp);

    // Date
    public T visit (DatePart exp);
    public T visit (DateTruncate exp);
    public T visit (Now exp);

    // Aggregation
    public T visit (Average exp);
    public T visit (Count exp);
    public T visit (Every exp);
    public T visit (Max exp);
    public T visit (Min exp);
    public T visit (Sum exp);

    // Conditional
    public T visit (Coalesce exp);
    public T visit (Greatest exp);
    public T visit (Least exp);
}
