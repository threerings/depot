//
// $Id: $
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

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.util.Tuple;

/**
 * Represents an CREATE INDEX instruction to the database.
 */
public class CreateIndexClause
    implements QueryClause
{
    /**
     * Create a new {@link CreateIndexClause} clause. The name must be unique within the relevant
     * database.
     */
    public CreateIndexClause (Class<? extends PersistentRecord> pClass, String name, boolean unique,
                              List<Tuple<SQLExpression, Order>> fields)
    {
        _pClass = pClass;
        _name = name;
        _unique = unique;
        _fields = fields;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public String getName ()
    {
        return _name;
    }

    public boolean isUnique ()
    {
        return _unique;
    }

    public List<Tuple<SQLExpression,Order>> getFields ()
    {
        return _fields;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    // from SQLExpression
    public void accept (ExpressionVisitor builder)
    {
        builder.visit(this);
    }

    protected Class<? extends PersistentRecord> _pClass;
    protected String _name;
    protected boolean _unique;

    /** The components of the index, e.g. columns or functions of columns. */
    protected List<Tuple<SQLExpression,Order>> _fields;
}
