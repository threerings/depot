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

import com.samskivert.util.StringUtil;

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '/' operator.
 */
public class Div<T extends Number> extends Arithmetic<T>
{
    public Div (SQLExpression<?> column, T value)
    {
        super(column, value);
    }

    public Div (SQLExpression<?>... values)
    {
        super(values);
    }

    @Override // from Arithmetic
    public String operator()
    {
        return " / "; // Pad with spaces to work-around a MySQL bug.
    }

    @Override // from Arithmetic
    public Object evaluate (Object[] operands)
    {
        for (int ii = 1; ii < operands.length; ii ++) {
            if (Double.valueOf(0).equals(NUMERICAL.apply(operands[ii]))) {
                return new NoValue("Division by zero in: " + StringUtil.toString(operands));
            }
        }
        return evaluate(operands, "/", new Accumulator<Double>() {
            public Double accumulate (Double left, Double right) {
                return left / right;
            }
        }, new Accumulator<Long>() {
            public Long accumulate (Long left, Long right) {
                return left / right;
            }
        });
    }
}
