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
import com.samskivert.depot.impl.operator.Not;
import com.samskivert.depot.impl.operator.NotEquals;
import com.samskivert.depot.impl.operator.Sub;

/**
 * Provides a fluent API for creating most SQL expressions like and, or, equal, not equal, etc.
 */
public abstract class FluentExp<T>
    implements SQLExpression<T>
{
    /** Returns an {@link Equals} with this expression and the supplied target. */
    public FluentExp<Boolean> eq (Comparable<?> value)
    {
        return new Equals(this, value);
    }

    /** Returns an {@link Equals} with this expression and the supplied target. */
    public FluentExp<Boolean> eq (SQLExpression<?> expr)
    {
        return new Equals(this, expr);
    }

    /** Returns a {@link NotEquals} with this expression and the supplied target. */
    public FluentExp<Boolean> notEq (Comparable<?> value)
    {
        return new NotEquals(this, value);
    }

    /** Returns a {@link NotEquals} with this expression and the supplied target. */
    public FluentExp<Boolean> notEq (SQLExpression<?> expr)
    {
        return new NotEquals(this, expr);
    }

    /** Returns a {@link Not} {@link IsNull} with this expression as its target. */
    public SQLExpression<Boolean> notNull ()
    {
        return new Not(new IsNull(this));
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
    public FluentExp<Boolean> greaterThan (Comparable<?> value)
    {
        return new GreaterThan(this, value);
    }

    /** Returns a {@link GreaterThan} with this expression and the supplied target. */
    public FluentExp<Boolean> greaterThan (SQLExpression<?> expr)
    {
        return new GreaterThan(this, expr);
    }

    /** Returns a {@link LessThan} with this expression and the supplied target. */
    public FluentExp<Boolean> lessThan (Comparable<?> value)
    {
        return new LessThan(this, value);
    }

    /** Returns a {@link LessThan} with this expression and the supplied target. */
    public FluentExp<Boolean> lessThan (SQLExpression<?> expr)
    {
        return new LessThan(this, expr);
    }

    /** Returns a {@link GreaterThanEquals} with this expression and the supplied target. */
    public FluentExp<Boolean> greaterEq (Comparable<?> value)
    {
        return new GreaterThanEquals(this, value);
    }

    /** Returns a {@link GreaterThanEquals} with this expression and the supplied target. */
    public FluentExp<Boolean> greaterEq (SQLExpression<?> expr)
    {
        return new GreaterThanEquals(this, expr);
    }

    /** Returns a {@link LessThanEquals} with this expression and the supplied target. */
    public FluentExp<Boolean> lessEq (Comparable<?> value)
    {
        return new LessThanEquals(this, value);
    }

    /** Returns a {@link LessThanEquals} with this expression and the supplied target. */
    public FluentExp<Boolean> lessEq (SQLExpression<?> expr)
    {
        return new LessThanEquals(this, expr);
    }

    /** Returns a boolean and of this expression and the supplied target. */
    public FluentExp<Boolean> and (SQLExpression<Boolean> expr)
    {
        return Ops.and(this, expr);
    }

    /** Returns a boolean or of this expression and the supplied target. */
    public FluentExp<Boolean> or (SQLExpression<Boolean> expr)
    {
        return Ops.or(this, expr);
    }

    /** Returns a bitwise and of this expression and the supplied target. */
    public <V extends Number> FluentExp<V> bitAnd (V value)
    {
        return new BitAnd<V>(this, value);
    }

    /** Returns a bitwise and of this expression and the supplied target. */
    public <V extends Number> FluentExp<V> bitAnd (SQLExpression<V> expr)
    {
        return new BitAnd<V>(this, expr);
    }

    /** Returns a bitwise or of this expression and the supplied target. */
    public <V extends Number> FluentExp<V> bitOr (V value)
    {
        return new BitOr<V>(this, value);
    }

    /** Returns a bitwise or of this expression and the supplied target. */
    public <V extends Number> FluentExp<V> bitOr (SQLExpression<V> expr)
    {
        return new BitOr<V>(this, expr);
    }

    /** Returns the sum of this expression and the supplied target. */
    public <V extends Number> FluentExp<V> plus (V value)
    {
        return new Add<V>(this, value);
    }

    /** Returns the sum of this expression and the supplied target. */
    public <V extends Number> FluentExp<V> plus (SQLExpression<V> expr)
    {
        return new Add<V>(this, expr);
    }

    /** Returns this expression minus the supplied target. */
    public <V extends Number> FluentExp<V> minus (V value)
    {
        return new Sub<V>(this, value);
    }

    /** Returns this expression minus the supplied target. */
    public <V extends Number> FluentExp<V> minus (SQLExpression<V> expr)
    {
        return new Sub<V>(this, expr);
    }

    /** Returns this expression times the supplied target. */
    public <V extends Number> FluentExp<V> times (V value)
    {
        return new Mul<V>(this, value);
    }

    /** Returns this expression times the supplied target. */
    public <V extends Number> FluentExp<V> times (SQLExpression<V> expr)
    {
        return new Mul<V>(this, expr);
    }

    /** Returns this expression divided by the supplied target. */
    public <V extends Number> FluentExp<V> div (V value)
    {
        return new Div<V>(this, value);
    }

    /** Returns this expression divided by the supplied target. */
    public <V extends Number> FluentExp<V> div (SQLExpression<V> expr)
    {
        return new Div<V>(this, expr);
    }

    /** Returns a {@link Like} on this column and the supplied target. */
    public FluentExp<Boolean> like (Comparable<?> value)
    {
        return new Like(this, value, true);
    }

    /** Returns a {@link Like} on this column and the supplied target. */
    public FluentExp<Boolean> like (SQLExpression<?> expr)
    {
        return new Like(this, expr, true);
    }

    /** Returns a negated {@link Like} on this column and the supplied target. */
    public FluentExp<Boolean> notLike (Comparable<?> value)
    {
        return new Like(this, value, false);
    }

    /** Returns a negated {@link Like} on this column and the supplied target. */
    public FluentExp<Boolean> notLike (SQLExpression<?> expr)
    {
        return new Like(this, expr, false);
    }

    /** Returns this expression minus the supplied target, assuming this and the target represent
     * Date values. Because date arithmetic is database specific, you're on your own with types. */
    public FluentExp<Number> dateSub (SQLExpression<?> expr)
    {
        return new Sub<Number>(this, expr);
    }

    /** Returns this expression plus the supplied target, assuming this and the target represent
     * Date values. Because date arithmetic is database specific, you're on your own with types. */
    public FluentExp<Number> dateAdd (SQLExpression<?> expr)
    {
        return new Add<Number>(this, expr);
    }
}
