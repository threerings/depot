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

import com.samskivert.depot.operator.Add;
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Div;
import com.samskivert.depot.operator.Equals;
import com.samskivert.depot.operator.GreaterThan;
import com.samskivert.depot.operator.GreaterThanEquals;
import com.samskivert.depot.operator.LessThan;
import com.samskivert.depot.operator.LessThanEquals;
import com.samskivert.depot.operator.Mul;
import com.samskivert.depot.operator.NotEquals;
import com.samskivert.depot.operator.Or;
import com.samskivert.depot.operator.Sub;

/**
 * A base class for {@link SQLExpression} implementations that provides a plethora of combinators
 * for composing expressions.
 */
public abstract class FluentExpression
    implements SQLExpression
{
    /** Returns an {@link Equals} with this expression and the supplied target. */
    public Equals eq (Comparable<?> value)
    {
        return new Equals(this, value);
    }

    /** Returns an {@link Equals} with this expression and the supplied target. */
    public Equals eq (SQLExpression expr)
    {
        return new Equals(this, expr);
    }

    /** Returns a {@link NotEquals} with this expression and the supplied target. */
    public NotEquals notEq (Comparable<?> value)
    {
        return new NotEquals(this, value);
    }

    /** Returns a {@link NotEquals} with this expression and the supplied target. */
    public NotEquals notEq (SQLExpression expr)
    {
        return new NotEquals(this, expr);
    }

    /** Returns a {@link GreaterThan} with this expression and the supplied target. */
    public GreaterThan greaterThan (Comparable<?> value)
    {
        return new GreaterThan(this, value);
    }

    /** Returns a {@link GreaterThan} with this expression and the supplied target. */
    public GreaterThan greaterThan (SQLExpression expr)
    {
        return new GreaterThan(this, expr);
    }

    /** Returns a {@link LessThan} with this expression and the supplied target. */
    public LessThan lessThan (Comparable<?> value)
    {
        return new LessThan(this, value);
    }

    /** Returns a {@link LessThan} with this expression and the supplied target. */
    public LessThan lessThan (SQLExpression expr)
    {
        return new LessThan(this, expr);
    }

    /** Returns a {@link GreaterThanEquals} with this expression and the supplied target. */
    public GreaterThanEquals greaterEq (Comparable<?> value)
    {
        return new GreaterThanEquals(this, value);
    }

    /** Returns a {@link GreaterThanEquals} with this expression and the supplied target. */
    public GreaterThanEquals greaterEq (SQLExpression expr)
    {
        return new GreaterThanEquals(this, expr);
    }

    /** Returns a {@link LessThanEquals} with this expression and the supplied target. */
    public LessThanEquals lessEq (Comparable<?> value)
    {
        return new LessThanEquals(this, value);
    }

    /** Returns a {@link LessThanEquals} with this expression and the supplied target. */
    public LessThanEquals lessEq (SQLExpression expr)
    {
        return new LessThanEquals(this, expr);
    }

    /** Returns an {@link And} with this expression and the supplied target. */
    public And and (SQLExpression expr)
    {
        return new And(this, expr);
    }

    /** Returns an {@link Or} with this expression and the supplied target. */
    public Or or (SQLExpression expr)
    {
        return new Or(this, expr);
    }

    /** Returns an {@link Add} with this expression and the supplied target. */
    public Add plus (Comparable<?> value)
    {
        return new Add(this, value);
    }

    /** Returns an {@link Add} with this expression and the supplied target. */
    public Add plus (SQLExpression expr)
    {
        return new Add(this, expr);
    }

    /** Returns a {@link Sub} with this expression and the supplied target. */
    public Sub minus (Comparable<?> value)
    {
        return new Sub(this, value);
    }

    /** Returns a {@link Sub} with this expression and the supplied target. */
    public Sub minus (SQLExpression expr)
    {
        return new Sub(this, expr);
    }

    /** Returns a {@link Mul} with this expression and the supplied target. */
    public Mul times (Comparable<?> value)
    {
        return new Mul(this, value);
    }

    /** Returns a {@link Mul} with this expression and the supplied target. */
    public Mul times (SQLExpression expr)
    {
        return new Mul(this, expr);
    }

    /** Returns a {@link Div} with this expression and the supplied target. */
    public Div div (Comparable<?> value)
    {
        return new Div(this, value);
    }

    /** Returns a {@link Div} with this expression and the supplied target. */
    public Div div (SQLExpression expr)
    {
        return new Div(this, expr);
    }
}
