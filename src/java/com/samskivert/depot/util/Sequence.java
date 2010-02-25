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

package com.samskivert.depot.util;

import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.base.Function;

/**
 * Provides read-only access to a sequence of elements. Note that these elements will generally be
 * computed via the application of a {@link Function} *every time* you iterate over this sequence.
 * Thus a caller is expected to either simply iterate over the elements once, or use {@link
 * #toList} to convert the sequence to concrete list to avoid repeated application of the
 * conversion function on elements of the sequence.
 */
public interface Sequence<T> extends Iterable<T>
{
    /**
     * Returns the number of elements in this sequence.
     */
    int size ();

    /**
     * Returns true if this sequence is empty, false if it contains at least one element.
     */
    boolean isEmpty ();

    /**
     * Converts this sequence into an {@link ArrayList}.
     */
    ArrayList<T> toList ();

    /**
     * Converts this sequence into a {@link HashSet}.
     */
    HashSet<T> toSet ();

    /**
     * Converts this sequence into an array.
     */
    T[] toArray (Class<T> clazz);
    // Ray wishes this were <S super T> S[] toArray (Class<S> clazz);
}
