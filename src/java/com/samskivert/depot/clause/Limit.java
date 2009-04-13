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
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 *  Represents a LIMIT/OFFSET clause, for pagination.
 */
public class Limit implements QueryClause
{
    public Limit (int offset, int count)
    {
        _offset = offset;
        _count = count;
    }

    public int getOffset ()
    {
        return _offset;
    }

    public int getCount ()
    {
        return _count;
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
    }

    @Override // from Object
    public String toString ()
    {
        return _offset + "-" + (_offset+_count);
    }

    /** The first row of the result set to return. */
    protected int _offset;

    /** The number of rows, at most, to return. */
    protected int _count;
}
