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

package com.samskivert.depot;

import java.io.Serializable;

import com.samskivert.depot.impl.QueryResult;

/**
 * Contains a two column result.
 */
public class Tuple2<A,B> implements Serializable
{
    public final A a;
    public final B b;

    public Tuple2 (A a, B b)
    {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString ()
    {
        return "[" + a + "," + b + "]";
    }
}
