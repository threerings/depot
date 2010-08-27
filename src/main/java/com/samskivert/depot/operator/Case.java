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

package com.samskivert.depot.operator;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.util.Tuple;

/**
 * The SQL 'case' operator.
 */
public class Case
    implements SQLExpression
{
    public Case (SQLExpression... exps)
    {
        int i = 0;
        while (i+1 < exps.length) {
            _whenExps.add(Tuple.newTuple(exps[i], exps[i+1]));
            i += 2;
        }
        _elseExp = (i < exps.length) ? exps[i] : null;
    }

    public List<Tuple<SQLExpression, SQLExpression>> getWhenExps ()
    {
        return _whenExps;
    }

    public SQLExpression getElseExp ()
    {
        return _elseExp;
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        for (Tuple<SQLExpression, SQLExpression> tuple : _whenExps) {
            tuple.left.addClasses(classSet);
            tuple.right.addClasses(classSet);
        }
        if (_elseExp != null) {
            _elseExp.addClasses(classSet);
        }
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Case(");
        for (Tuple<SQLExpression, SQLExpression> tuple : _whenExps) {
            builder.append(tuple.left.toString()).append("->");
            builder.append(tuple.right.toString()).append(",");
        }
        if (_elseExp != null) {
            builder.append(_elseExp.toString()).append(")");
        }
        return builder.toString();
    }

    protected List<Tuple<SQLExpression, SQLExpression>> _whenExps = Lists.newArrayList();
    protected SQLExpression _elseExp;
}
