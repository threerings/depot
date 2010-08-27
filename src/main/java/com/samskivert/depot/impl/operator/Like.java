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

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL 'like' (and 'not like') operator.
 */
public class Like extends BinaryOperator
{
    public Like (SQLExpression column, Comparable<?> value, boolean like)
    {
        super(column, value);
        _like = like;
    }

    public Like (SQLExpression column, SQLExpression value, boolean like)
    {
        super(column, value);
        _like = like;
    }

    @Override // from BinaryOperator
    public String operator()
    {
        return _like ? " like " : " not like ";
    }

    @Override // from BinaryOperator
    public Object evaluate (Object left, Object right)
    {
        return new NoValue("Like operator not implemented");
    }

    protected boolean _like;
}
