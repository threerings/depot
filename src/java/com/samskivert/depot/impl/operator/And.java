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

import java.util.Collection;

import com.samskivert.depot.expression.SQLExpression;

/**
 * Represents a condition that is true iff all its subconditions are true.
 */
public class And extends SQLOperator.MultiOperator
{
    public And (Collection<? extends SQLExpression> conditions)
    {
        super(conditions.toArray(new SQLExpression[conditions.size()]));
    }

    public And (SQLExpression... conditions)
    {
        super(conditions);
    }

    @Override public String operator()
    {
        return " and ";
    }

    @Override
    public Object evaluate (Object[] values)
    {
        // if all operands are true, return true, else if all operands are boolean, return
        // false, else return NO_VALUE
        boolean allTrue = true;
        for (Object value : values) {
            if (value instanceof NoValue) {
                return value;
            }
            if (Boolean.FALSE.equals(value)) {
                allTrue = false;
            } else if (!Boolean.TRUE.equals(value)) {
                return new NoValue("Non-boolean operand to AND: " + value);
            }
        }
        return allTrue;
    }
}
