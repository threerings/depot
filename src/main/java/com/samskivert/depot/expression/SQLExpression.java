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

package com.samskivert.depot.expression;

import com.samskivert.depot.SQLFragment;

/**
 * Represents an SQL expression, e.g. column name, function, or constant.
 */
public interface SQLExpression<T> extends SQLFragment
{
    /** Used internally to represent the lack of a value. */
    public static final class NoValue
    {
        public NoValue (String reason)
        {
            _reason = reason;
        }

        @Override public String toString () {
            return "[unknown value, reason=" + _reason + "]";
        }

        protected String _reason;
    }

}
