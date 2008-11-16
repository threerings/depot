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

package com.samskivert.depot.expression;

import com.samskivert.depot.Key;
import com.samskivert.depot.MultiKey;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.WhereClause;

import com.samskivert.depot.clause.DeleteClause;
import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.ForUpdate;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.InsertClause;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.UpdateClause;

import com.samskivert.depot.operator.Conditionals.Exists;
import com.samskivert.depot.operator.Conditionals.In;
import com.samskivert.depot.operator.Conditionals.IsNull;
import com.samskivert.depot.operator.Conditionals.FullTextMatch;
import com.samskivert.depot.operator.Logic.Not;
import com.samskivert.depot.operator.SQLOperator.BinaryOperator;
import com.samskivert.depot.operator.SQLOperator.MultiOperator;

/**
 * Enumerates visitation methods for every possible SQL expression type.
 */
public interface ExpressionVisitor
{
    public void visit (FieldDefinition fieldOverride);
    public void visit (FunctionExp functionExp);
    public void visit (EpochSeconds epochSeconds);
    public void visit (FromOverride fromOverride);
    public void visit (MultiOperator multiOperator);
    public void visit (BinaryOperator binaryOperator);
    public void visit (IsNull isNull);
    public void visit (In in);
    public void visit (FullTextMatch match);
    public void visit (ColumnExp columnExp);
    public void visit (Not not);
    public void visit (GroupBy groupBy);
    public void visit (ForUpdate forUpdate);
    public void visit (OrderBy orderBy);
    public void visit (Join join);
    public void visit (Limit limit);
    public void visit (LiteralExp literalExp);
    public void visit (ValueExp valueExp);
    public void visit (WhereClause where);
    public void visit (Key.Expression<? extends PersistentRecord> key);
    public void visit (MultiKey<? extends PersistentRecord> key);
    public void visit (Exists<? extends PersistentRecord> exists);
    public void visit (SelectClause<? extends PersistentRecord> selectClause);
    public void visit (UpdateClause<? extends PersistentRecord> updateClause);
    public void visit (DeleteClause<? extends PersistentRecord> deleteClause);
    public void visit (InsertClause<? extends PersistentRecord> insertClause);
}
