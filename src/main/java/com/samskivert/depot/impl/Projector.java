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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.util.*; // TupleN

/**
 * Contains a set of selection expressions which are to be projected (selected) from a set of
 * tables. Handles the necessary type conversions to turn the results into typed tuples.
 */
public abstract class Projector<T extends PersistentRecord,R>
{
    public static <T extends PersistentRecord, V> Projector<T,V> create (
        Class<T> ptype, SQLExpression<V> column)
    {
        return new Projector<T, V>(ptype, new SQLExpression<?>[] { column }) {
            @Override public V createObject (Object[] results) {
                @SuppressWarnings("unchecked") V result = (V)results[0];
                return result;
            }
        };
    }

    public static <T extends PersistentRecord, V1, V2> Projector<T,Tuple2<V1,V2>> create (
        Class<T> ptype, SQLExpression<V1> col1, SQLExpression<V2> col2)
    {
        return new Projector<T, Tuple2<V1,V2>>(ptype, new SQLExpression<?>[] { col1, col2 }) {
            @Override public Tuple2<V1,V2> createObject (Object[] results) {
                @SuppressWarnings("unchecked") V1 r1 = (V1)results[0];
                @SuppressWarnings("unchecked") V2 r2 = (V2)results[1];
                return new Tuple2<V1,V2>(r1, r2);
            }
        };
    }

    public static <T extends PersistentRecord, V1, V2, V3> Projector<T,Tuple3<V1,V2,V3>> create (
        Class<T> ptype, SQLExpression<V1> col1, SQLExpression<V2> col2, SQLExpression<V3> col3)
    {
        return new Projector<T, Tuple3<V1,V2,V3>>(
            ptype, new SQLExpression<?>[] { col1, col2, col3 }) {
            @Override public Tuple3<V1,V2,V3> createObject (Object[] results) {
                @SuppressWarnings("unchecked") V1 r1 = (V1)results[0];
                @SuppressWarnings("unchecked") V2 r2 = (V2)results[1];
                @SuppressWarnings("unchecked") V3 r3 = (V3)results[2];
                return new Tuple3<V1,V2,V3>(r1, r2, r3);
            }
        };
    }

    public static <T extends PersistentRecord, V1, V2, V3, V4>
        Projector<T,Tuple4<V1,V2,V3,V4>> create (
            Class<T> ptype, SQLExpression<V1> col1, SQLExpression<V2> col2, SQLExpression<V3> col3,
            SQLExpression<V4> col4)
    {
        return new Projector<T, Tuple4<V1,V2,V3,V4>>(
            ptype, new SQLExpression<?>[] { col1, col2, col3, col4 }) {
            @Override public Tuple4<V1,V2,V3,V4> createObject (Object[] results) {
                @SuppressWarnings("unchecked") V1 r1 = (V1)results[0];
                @SuppressWarnings("unchecked") V2 r2 = (V2)results[1];
                @SuppressWarnings("unchecked") V3 r3 = (V3)results[2];
                @SuppressWarnings("unchecked") V4 r4 = (V4)results[3];
                return new Tuple4<V1,V2,V3,V4>(r1, r2, r3, r4);
            }
        };
    }

    public static <T extends PersistentRecord, V1, V2, V3, V4, V5>
        Projector<T,Tuple5<V1,V2,V3,V4,V5>> create (
            Class<T> ptype, SQLExpression<V1> col1, SQLExpression<V2> col2, SQLExpression<V3> col3,
            SQLExpression<V4> col4, SQLExpression<V5> col5)
    {
        return new Projector<T, Tuple5<V1,V2,V3,V4,V5>>(
            ptype, new SQLExpression<?>[] { col1, col2, col3, col4, col5 }) {
            @Override public Tuple5<V1,V2,V3,V4,V5> createObject (Object[] results) {
                @SuppressWarnings("unchecked") V1 r1 = (V1)results[0];
                @SuppressWarnings("unchecked") V2 r2 = (V2)results[1];
                @SuppressWarnings("unchecked") V3 r3 = (V3)results[2];
                @SuppressWarnings("unchecked") V4 r4 = (V4)results[3];
                @SuppressWarnings("unchecked") V5 r5 = (V5)results[4];
                return new Tuple5<V1,V2,V3,V4,V5>(r1, r2, r3, r4, r5);
            }
        };
    }

    public static <T extends PersistentRecord, V> Projector<T,V> create (
        Class<T> ptype, final Class<V> resultType, SQLExpression<?>... selexps)
    {
        return new Projector<T, V>(ptype, selexps) {
            public V createObject (Object[] results) {
                try {
                    return _ctor.newInstance(results);
                } catch (InstantiationException e) {
                    throw new DatabaseException("Invalid constructor supplied for projection", e);
                } catch (IllegalAccessException e) {
                    throw new DatabaseException("Invalid constructor supplied for projection", e);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException)t;
                    } else {
                        throw new DatabaseException("Error constructing result object", t);
                    }
                }
            }
            @SuppressWarnings("unchecked")
            protected Constructor<V> _ctor = (Constructor<V>)resultType.getConstructors()[0];
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
