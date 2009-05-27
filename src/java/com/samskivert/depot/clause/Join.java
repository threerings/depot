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

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.depot.operator.Equals;

/**
 *  Represents a JOIN.
 */
public class Join implements QueryClause
{
    /** Indicates the join type to be used. The default is INNER. */
    public static enum Type { INNER, LEFT_OUTER, RIGHT_OUTER };

    public Join (ColumnExp primary, ColumnExp join)
    {
        _joinClass = join.getPersistentClass();
        _joinCondition = new Equals(primary, join);
    }

    public Join (Class<? extends PersistentRecord> joinClass, SQLExpression joinCondition)
    {
        _joinClass = joinClass;
        _joinCondition = joinCondition;
    }

    /**
     * Configures the type of join to be performed.
     */
    public Join setType (Type type)
    {
        _type = type;
        return this;
    }

    public Type getType ()
    {
        return _type;
    }

    public Class<? extends PersistentRecord> getJoinClass ()
    {
        return _joinClass;
    }

    public SQLExpression getJoinCondition ()
    {
        return _joinCondition;
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_joinClass);
        _joinCondition.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        return DepotUtil.justClassName(_joinClass) + ":" + _type + ":" + _joinCondition;
    }

    /** Indicates the type of join to be performed. */
    protected Type _type = Type.INNER;

    /** The class of the table we're to join against. */
    protected Class<? extends PersistentRecord> _joinClass;

    /** The condition used to join in the new table. */
    protected SQLExpression _joinCondition;
}
