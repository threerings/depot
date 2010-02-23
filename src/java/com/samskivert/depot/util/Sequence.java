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

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

/**
 * Provides read-only access to a sequence of elements. Note that these elements will generally be
 * computed via the application of a {@link Function} *every time* you iterate over this sequence.
 * Thus a caller is expected to either simply iterate over the elements once, or use {@link
 * #toList} to convert the sequence to concrete list to avoid repeated application of the
 * conversion function on elements of the sequence.
 */
public abstract class Sequence<T> implements Iterable<T>
{
    /**
     * Applies the supplied mapping function to the elements in the supplied collection, returning
     * a lazily created read-only view of the mapped elements.
     */
    public static <F, T> Sequence<T> map (final Collection<F> source,
                                          final Function<? super F, ? extends T> func)
    {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(func);
        return new Sequence<T>() {
            public Iterator<T> iterator () {
                return Iterators.transform(source.iterator(), func);
            }
            public int size () {
                return source.size();
            }
            public boolean isEmpty () {
                return source.isEmpty();
            }
            public ArrayList<T> toList () {
                ArrayList<T> list = new ArrayList<T>(source.size());
                for (F elem : source) {
                    list.add(func.apply(elem));
                }
                return list;
            }
            public T[] toArray (Class<T> clazz) {
                @SuppressWarnings("unchecked")
                T[] array = (T[]) Array.newInstance(clazz, source.size());
                int index = 0;
                for (F elem : source) {
                    array[index++] = func.apply(elem);
                }
                return array;
            }
        };
    }

    // from interface Iterable
    public abstract Iterator<T> iterator ();

    /**
     * Returns the number of elements in this sequence.
     */
    public abstract int size ();

    /**
     * Returns true if this sequence is empty, false if it contains at least one element.
     */
    public abstract boolean isEmpty ();

    /**
     * Converts this sequence into an array list.
     */
    public abstract ArrayList<T> toList ();

    /**
     * Converts this sequence into an array.
     * I wish this were <S super T> S[] toArray (Class<S? clazz);
     */
    public abstract T[] toArray (Class<T> clazz);
}
