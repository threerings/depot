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

package com.samskivert.depot.impl.operator;

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.ValueExp;

/**
 * A convenient container for implementations of arithmetic operators.
 */
public abstract class Arithmetic extends MultiOperator
{
    public Arithmetic (SQLExpression column, Comparable<?> value)
    {
        super(column, new ValueExp(value));
    }

    public Arithmetic (SQLExpression... values)
    {
        super(values);
    }

    protected Object evaluate (
        Object[] ops, String name, Accumulator<Double> dAcc, Accumulator<Long> iAcc)
    {
        if (dAcc != null && all(NUMERICAL, ops)) {
            return accumulate(NUMERICAL, ops, 0.0, dAcc);
        }

        if (iAcc != null && all(INTEGRAL, ops)) {
            return accumulate(INTEGRAL, ops, 0L, iAcc);
        }
        return new NoValue("Non-numeric operand for '" + name + "' (" + ops + ")");
    }
}
