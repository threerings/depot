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

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * An attempt at a dialect-agnostic full-text search condition, such as MySQL's MATCH() and
 * PostgreSQL's @@ TO_TSQUERY(...) abilities.
 */
public class FullText
{
    public class Rank extends FluentExp
        implements SQLExpression
    {
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
            return FullText.this.toString("Rank");
        }

        public FullText getDefinition ()
        {
            return FullText.this;
        }
    }

    public class Match extends FluentExp
        implements SQLExpression
    {
        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        public FullText getDefinition ()
        {
            return FullText.this;
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
        }

        @Override // from Object
        public String toString ()
        {
            return FullText.this.toString("Match");
        }
    }

    public FullText (Class<? extends PersistentRecord> pClass, String name, String query)
    {
        _pClass = pClass;
        _name = name;
        _query = query;
    }

    public Match match ()
    {
        return new Match();
    }

    public Rank rank ()
    {
        return new Rank();
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public String getName ()
    {
        return _name;
    }

    public String getQuery ()
    {
        return _query;
    }

    protected String toString (String subType)
    {
        return "FullText." + subType + "(" + _name + "=" + _query + ")";
    }

    protected Class<? extends PersistentRecord> _pClass;
    protected String _name;
    protected String _query;
}
