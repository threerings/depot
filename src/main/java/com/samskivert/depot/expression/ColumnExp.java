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

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * An expression that unambiguously identifies a field of a class, for example
 * <code>GameRecord.itemId</code>.
 */
public class ColumnExp extends FluentExp
{
    /** The name of the column we reference. */
    public final String name;

    public ColumnExp (Class<? extends PersistentRecord> pClass, String field)
    {
        super();
        _pClass = pClass;
        this.name = field;
    }

    /**
     * Returns a column expression for the supplied persistent class with the same name as this
     * expression. This is useful for "casting" a column expression from a parent class to a
     * derived class.
     */
    public ColumnExp as (Class<? extends PersistentRecord> oClass)
    {
        return new ColumnExp(oClass, name);
    }

    /** Returns a {@link Join} on this column and the supplied target. */
    public Join join (ColumnExp join)
    {
        return new Join(this, join);
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    @Override // from Object
    public int hashCode ()
    {
        return _pClass.hashCode() ^ this.name.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof ColumnExp) &&
            ((ColumnExp)other)._pClass.equals(_pClass) &&
            ((ColumnExp)other).name.equals(this.name);
    }

    @Override // from Object
    public String toString ()
    {
        return "\"" + name + "\""; // TODO: qualify with record name and be uber verbose?
    }

    /** The table that hosts the column we reference, or null. */
    protected final Class<? extends PersistentRecord> _pClass;
}
