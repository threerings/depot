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

package com.samskivert.depot;

import java.util.Collection;

import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Add;
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Exists;
import com.samskivert.depot.operator.Like;
import com.samskivert.depot.operator.Mul;
import com.samskivert.depot.operator.Not;
import com.samskivert.depot.operator.Or;

/**
 * Provides static methods for operator construction that don't fit nicely into the fluent style.
 * For example: Ops.and(), Ops.or() and Ops.not().
 */
public class Ops
{
    /**
     * Creates a NOT expression with the supplied target expression.
     */
    public static SQLExpression not (SQLExpression expr)
    {
        return new Not(expr);
    }

    /**
     * Creates an AND expression with the supplied target expressions.
     */
    public static FluentExp and (Collection<? extends SQLExpression> conditions)
    {
        return new And(conditions);
    }

    /**
     * Creates an AND expression with the supplied target expressions.
     */
    public static FluentExp and (SQLExpression... conditions)
    {
        return new And(conditions);
    }

    /**
     * Creates an OR expression with the supplied target expressions.
     */
    public static FluentExp or (Collection<? extends SQLExpression> conditions)
    {
        return new Or(conditions);
    }

    /**
     * Creates an OR expression with the supplied target expressions.
     */
    public static FluentExp or (SQLExpression... conditions)
    {
        return new Or(conditions);
    }

    /**
     * Returns an expression that matches when the source is like the supplied value.
     */
    public static FluentExp like (SQLExpression source, Comparable<?> value)
    {
        return new Like(source, value);
    }

    /**
     * Returns an expression that matches when the source is like the supplied expression.
     */
    public static FluentExp like (SQLExpression source, SQLExpression expr)
    {
        return new Like(source, expr);
    }

    /**
     * Creates an EXISTS expression with the supplied select clause.
     */
    public static SQLExpression exists (SelectClause target)
    {
        return new Exists(target);
    }

    /**
     * Creates an EXISTS expression with the supplied select clause.
     */
    public static SQLExpression exists (SelectClause target)
    {
        return new Exists(target);
    }

    /**
     * Adds the supplied expressions together.
     */
    public static FluentExp add (SQLExpression... exprs)
    {
        return new Add(exprs);
    }

    /**
     * Multiplies the supplied expressions together.
     */
    public static FluentExp mul (SQLExpression... exprs)
    {
        return new Mul(exprs);
    }
}
