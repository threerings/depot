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

import com.samskivert.depot.expression.SQLExpression;

/**
 * The SQL '<=' operator.
 */
public class LessThanEquals extends SQLOperator.BinaryOperator
{
    public LessThanEquals (SQLExpression column, Comparable<?> value)
    {
        super(column, value);
    }

    public LessThanEquals (SQLExpression column, SQLExpression value)
    {
        super(column, value);
    }

    @Override // from SQLOperator.BinaryOperator
    public String operator()
    {
        return "<=";
    }

    @Override // from SQLOperator.BinaryOperator
    public Object evaluate (Object left, Object right)
    {
        if (all(NUMERICAL, left, right)) {
            return NUMERICAL.apply(left) <= NUMERICAL.apply(right);
        }
        if (all(STRING, left, right) || all(DATE, left, right)) {
            return compare(STRING, left, right) <= 0;
        }
        return new NoValue("Non-comparable operand to '<=': (" + left + ", " + right + ")");
    }
}
