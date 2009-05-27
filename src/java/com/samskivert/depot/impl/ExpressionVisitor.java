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
import com.samskivert.depot.expression.LiteralExp;
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

/**
 * Enumerates visitation methods for every possible SQL expression type.
 */
public interface ExpressionVisitor<T>
{
    public T visit (FieldDefinition fieldOverride);
    public T visit (FunctionExp functionExp);
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
    public T visit (LiteralExp literalExp);
    public T visit (ValueExp valueExp);
    public T visit (WhereClause where);
    public T visit (Key.Expression<? extends PersistentRecord> key);
    public T visit (Exists<? extends PersistentRecord> exists);
    public T visit (SelectClause<? extends PersistentRecord> selectClause);
    public T visit (UpdateClause<? extends PersistentRecord> updateClause);
    public T visit (DeleteClause deleteClause);
    public T visit (InsertClause insertClause);
    public T visit (CreateIndexClause createIndexClause);
    public T visit (DropIndexClause<? extends PersistentRecord> dropIndexClause);
    public T visit (Case caseExp);
}
