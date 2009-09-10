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
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * The SQL 'in (...)' operator.
 */
public class In
    implements SQLOperator
{
    /** The maximum number of keys allowed in an IN() clause. */
    public static final int MAX_KEYS = Short.MAX_VALUE;

    public In (ColumnExp column, Comparable<?>... values)
    {
        _column = column;
        _values = values;
    }

    public In (ColumnExp pColumn, Collection<? extends Comparable<?>> values)
    {
        this(pColumn, values.toArray(new Comparable<?>[values.size()]));
    }

    public ColumnExp getColumn ()
    {
        return _column;
    }

    public Comparable<?>[] getValues ()
    {
        return _values;
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _column.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(_column).append(" in (");
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append(_values[ii]);
        }
        return builder.append(")").toString();
    }

    protected ColumnExp _column;
    protected Comparable<?>[] _values;
}
