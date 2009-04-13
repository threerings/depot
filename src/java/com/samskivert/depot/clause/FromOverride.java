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
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 *  Completely overrides the FROM clause, if it exists.
 */
public class FromOverride implements QueryClause
{
    public FromOverride (Class<? extends PersistentRecord> fromClass)
    {
        _fromClasses.add(fromClass);
    }

    public FromOverride (Class<? extends PersistentRecord> fromClass1,
                         Class<? extends PersistentRecord> fromClass2)
    {
        _fromClasses.add(fromClass1);
        _fromClasses.add(fromClass2);
    }

    public FromOverride (Collection<Class<? extends PersistentRecord>> fromClasses)
    {
        _fromClasses.addAll(fromClasses);
    }

    public List<Class<? extends PersistentRecord>> getFromClasses ()
    {
        return _fromClasses;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.addAll(getFromClasses());
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        for (Class<? extends PersistentRecord> clazz : _fromClasses) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(DepotUtil.justClassName(clazz));
        }
        return builder.toString();
    }

    /** The classes of the tables we're selecting from. */
    protected List<Class<? extends PersistentRecord>> _fromClasses = Lists.newArrayList();
}
