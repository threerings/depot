//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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

package com.samskivert.depot.impl;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Tuple2;
import com.samskivert.depot.expression.SQLExpression;

/**
 * Contains a set of selection expressions which are to be projected (selected) from a set of
 * tables. Handles the necessary type conversions to turn the results into typed tuples.
 */
public abstract class Projector<T extends PersistentRecord,R>
{
    public static <T extends PersistentRecord, V> Projector<T,V> create (
        Class<T> ptype, SQLExpression<V> column)
    {
        return new Projector<T, V>(ptype, new SQLExpression[] { column }) {
            public V createObject (Object[] results) {
                @SuppressWarnings("unchecked") V result = (V)results[0];
                return result;
            }
        };
    }

    public static <T extends PersistentRecord, V1, V2> Projector<T,Tuple2<V1,V2>> create (
        Class<T> ptype, SQLExpression<V1> col1, SQLExpression<V2> col2)
    {
        return new Projector<T, Tuple2<V1,V2>>(ptype, new SQLExpression[] { col1, col2 }) {
            public Tuple2<V1,V2> createObject (Object[] results) {
                @SuppressWarnings("unchecked") V1 r1 = (V1)results[0];
                @SuppressWarnings("unchecked") V2 r2 = (V2)results[1];
                return new Tuple2<V1,V2>(r1, r2);
            }
        };
    }

    public final Class<T> ptype;
    public final SQLExpression<?>[] selexps;

    public abstract R createObject (Object[] results);

    protected Projector (Class<T> ptype, SQLExpression<?>[] selexps)
    {
        this.ptype = ptype;
        this.selexps = selexps;
    }
}
