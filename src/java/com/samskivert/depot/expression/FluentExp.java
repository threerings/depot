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

package com.samskivert.depot.expression;

import com.samskivert.depot.Ops;
import com.samskivert.depot.impl.operator.Add;
import com.samskivert.depot.impl.operator.BitAnd;
import com.samskivert.depot.impl.operator.BitOr;
import com.samskivert.depot.impl.operator.Div;
import com.samskivert.depot.impl.operator.Equals;
import com.samskivert.depot.impl.operator.GreaterThan;
import com.samskivert.depot.impl.operator.GreaterThanEquals;
import com.samskivert.depot.impl.operator.In;
import com.samskivert.depot.impl.operator.IsNull;
import com.samskivert.depot.impl.operator.LessThan;
import com.samskivert.depot.impl.operator.LessThanEquals;
import com.samskivert.depot.impl.operator.Like;
import com.samskivert.depot.impl.operator.Mul;
import com.samskivert.depot.impl.operator.NotEquals;
import com.samskivert.depot.impl.operator.Sub;

/**
 * Provides a fluent API for creating most SQL expressions like and, or, equal, not equal, etc.
 */
public abstract class FluentExp
    implements SQLExpression
{
    /** Returns an {@link Equals} with this expression and the supplied target. */
    public FluentExp eq (Comparable<?> value)
    {
        return new Equals(this, value);
    }

    /** Returns an {@link Equals} with this expression and the supplied target. */
    public FluentExp eq (SQLExpression expr)
    {
        return new Equals(this, expr);
    }

    /** Returns a {@link NotEquals} with this expression and the supplied target. */
    public FluentExp notEq (Comparable<?> value)
    {
        return new NotEquals(this, value);
    }

    /** Returns a {@link NotEquals} with this expression and the supplied target. */
    public FluentExp notEq (SQLExpression expr)
    {
        return new NotEquals(this, expr);
    }

    /** Returns an {@link IsNull} with this expression as its target. */
    public IsNull isNull ()
    {
        return new IsNull(this);
    }

    /** Returns an {@link In} with this expression and the supplied values. */
    public In in (Comparable<?>... values)
    {
        return new In(this, values);
    }

    /** Returns an {@link In} with this column and the supplied values. */
    public In in (Iterable<? extends Comparable<?>> values)
    {
        return new In(this, values);
    }

    /** Returns a {@link GreaterThan} with this expression and the supplied target. */
    public FluentExp greaterThan (Comparable<?> value)
    {
        return new GreaterThan(this, value);
    }

    /** Returns a {@link GreaterThan} with this expression and the supplied target. */
    public FluentExp greaterThan (SQLExpression expr)
    {
        return new GreaterThan(this, expr);
    }

    /** Returns a {@link LessThan} with this expression and the supplied target. */
    public FluentExp lessThan (Comparable<?> value)
    {
        return new LessThan(this, value);
    }

    /** Returns a {@link LessThan} with this expression and the supplied target. */
    public FluentExp lessThan (SQLExpression expr)
    {
        return new LessThan(this, expr);
    }

    /** Returns a {@link GreaterThanEquals} with this expression and the supplied target. */
    public FluentExp greaterEq (Comparable<?> value)
    {
        return new GreaterThanEquals(this, value);
    }

    /** Returns a {@link GreaterThanEquals} with this expression and the supplied target. */
    public FluentExp greaterEq (SQLExpression expr)
    {
        return new GreaterThanEquals(this, expr);
    }

    /** Returns a {@link LessThanEquals} with this expression and the supplied target. */
    public FluentExp lessEq (Comparable<?> value)
    {
        return new LessThanEquals(this, value);
    }

    /** Returns a {@link LessThanEquals} with this expression and the supplied target. */
    public FluentExp lessEq (SQLExpression expr)
    {
        return new LessThanEquals(this, expr);
    }

    /** Returns a boolean and of this expression and the supplied target. */
    public FluentExp and (SQLExpression expr)
    {
        return Ops.and(this, expr);
    }

    /** Returns a boolean or of this expression and the supplied target. */
    public FluentExp or (SQLExpression expr)
    {
        return Ops.or(this, expr);
    }

    /** Returns a bitwise and of this expression and the supplied target. */
    public FluentExp bitAnd (Comparable<?> value)
    {
        return new BitAnd(this, value);
    }

    /** Returns a bitwise and of this expression and the supplied target. */
    public FluentExp bitAnd (SQLExpression expr)
    {
        return new BitAnd(this, expr);
    }

    /** Returns a bitwise or of this expression and the supplied target. */
    public FluentExp bitOr (Comparable<?> value)
    {
        return new BitOr(this, value);
    }

    /** Returns a bitwise or of this expression and the supplied target. */
    public FluentExp bitOr (SQLExpression expr)
    {
        return new BitOr(this, expr);
    }

    /** Returns the sum of this expression and the supplied target. */
    public FluentExp plus (Comparable<?> value)
    {
        return new Add(this, value);
    }

    /** Returns the sum of this expression and the supplied target. */
    public FluentExp plus (SQLExpression expr)
    {
        return new Add(this, expr);
    }

    /** Returns this expression minus the supplied target. */
    public FluentExp minus (Comparable<?> value)
    {
        return new Sub(this, value);
    }

    /** Returns this expression minus the supplied target. */
    public FluentExp minus (SQLExpression expr)
    {
        return new Sub(this, expr);
    }

    /** Returns this expression times the supplied target. */
    public FluentExp times (Comparable<?> value)
    {
        return new Mul(this, value);
    }

    /** Returns this expression times the supplied target. */
    public FluentExp times (SQLExpression expr)
    {
        return new Mul(this, expr);
    }

    /** Returns this expression divided by the supplied target. */
    public FluentExp div (Comparable<?> value)
    {
        return new Div(this, value);
    }

    /** Returns this expression divided by the supplied target. */
    public FluentExp div (SQLExpression expr)
    {
        return new Div(this, expr);
    }

    /** Returns a {@link Like} on this column and the supplied target. */
    public Like like (Comparable<?> value)
    {
        return new Like(this, value, true);
    }

    /** Returns a {@link Like} on this column and the supplied target. */
    public Like like (SQLExpression expr)
    {
        return new Like(this, expr, true);
    }

    /** Returns a negated {@link Like} on this column and the supplied target. */
    public Like notLike (Comparable<?> value)
    {
        return new Like(this, value, false);
    }

    /** Returns a negated {@link Like} on this column and the supplied target. */
    public Like notLike (SQLExpression expr)
    {
        return new Like(this, expr, false);
    }

}
