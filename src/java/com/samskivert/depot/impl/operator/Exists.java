//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2009 Michael Bayne and Pär Winzell
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

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * The SQL 'exists' operator.
 */
public class Exists
    implements SQLOperator
{
    public Exists (SelectClause clause)
    {
        _clause = clause;
    }

    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _clause.addClasses(classSet);
    }

    public SelectClause getSubClause ()
    {
        return _clause;
    }

    @Override // from Object
    public String toString ()
    {
        return "Exists(" + _clause + ")";
    }

    protected SelectClause _clause;
}
