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

package com.samskivert.depot.util;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Contains a four column result. This class is immutable and the objects it references are also
 * meant to be immutable. They will generally contain only Depot primitive types (Java primitives,
 * SQL primitives and some array types), which should be treated as immutable.
 */
public class Tuple4<A,B,C,D> implements Serializable
{
    /** The first column of the result. */
    public final A a;

    /** The second column of the result. */
    public final B b;

    /** The third column of the result. */
    public final C c;

    /** The fourth column of the result. */
    public final D d;

    /** Constructs an initialized tuple. */
    public static <A, B, C, D> Tuple4<A, B, C, D> create (A a, B b, C c, D d)
    {
        return new Tuple4<A, B, C, D>(a, b, c, d);
    }

    /** Constructs an initialized tuple. */
    public Tuple4 (A a, B b, C c, D d)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    @Override
    public String toString ()
    {
        return "[" + a + "," + b + "," + c + "," + d + "]";
    }

    @Override
    public int hashCode ()
    {
        return Objects.hashCode(a, b, c, d);
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof Tuple4<?,?,?,?>) {
            Tuple4<?,?,?,?> otup = (Tuple4<?,?,?,?>)other;
            return Objects.equal(a, otup.a) && Objects.equal(b, otup.b) &&
                Objects.equal(c, otup.c) && Objects.equal(d, otup.d);
        } else {
            return false;
        }
    }
}
