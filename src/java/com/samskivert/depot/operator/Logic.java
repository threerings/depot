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

package com.samskivert.depot.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * A convenient container for implementations of logical operators.  Classes that value brevity
 * feel otherwise will use Logic.And() and Logic.Not().
 */
public abstract class Logic
{
    /**
     * Represents a condition that is false iff all its subconditions are false.
     */
    public static class Or extends SQLOperator.MultiOperator
    {
        public Or (Collection<? extends SQLExpression> conditions)
        {
            super(conditions.toArray(new SQLExpression[conditions.size()]));
        }

        public Or (SQLExpression... conditions)
        {
            super(conditions);
        }

        @Override public String operator()
        {
            return " or ";
        }

        @Override
        public Object evaluate (Object[] values)
        {
            boolean anyTrue = false;
            for (Object value : values) {
                if (value instanceof NoValue) {
                    return value;
                }
                if (Boolean.TRUE.equals(value)) {
                    anyTrue = true;
                } else if (!Boolean.FALSE.equals(value)) {
                    return new NoValue("Non-boolean operand to OR: " + value);
                }
            }
            return anyTrue;
        }
    }

    /**
     * Represents a condition that is true iff all its subconditions are true.
     */
    public static class And extends SQLOperator.MultiOperator
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

    /**
     * Represents the truth negation of another conditon.
     */
    public static class Not
        implements SQLOperator
    {
        public Not (SQLExpression condition)
        {
            _condition = condition;
        }

        public SQLExpression getCondition ()
        {
            return _condition;
        }

        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            _condition.addClasses(classSet);
        }

        @Override // from Object
        public String toString ()
        {
            return "Not(" + _condition + ")";
        }

        protected SQLExpression _condition;
    }
}
