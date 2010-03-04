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

package com.samskivert.depot.impl.util;

import java.lang.reflect.Array;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import com.samskivert.depot.util.Sequence;

/**
 * An implementation of {@link Sequence} which secretly implements {@link Collection} so that
 * {@link Iterables#toArray} can efficiently turn it into an array. We use {@link
 * Iterables#toArray} in various places in Depot, so we prefer to enable such efficience.
 */
public class SeqImpl<F, T> extends AbstractCollection<T> implements Sequence<T>
{
    public SeqImpl (Collection<F> source, Function<? super F, ? extends T> func)
    {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(func);
        _source = source;
        _func = func;
    }

    @Override // from interface Sequence<T> and Collection<T>
    public Iterator<T> iterator ()
    {
        return Iterators.transform(_source.iterator(), _func);
    }

    @Override // from interface Sequence<T> and Collection<T>
    public int size ()
    {
        return _source.size();
    }

    @Override // from interface Sequence<T> and Collection<T>
    public boolean isEmpty ()
    {
        return _source.isEmpty();
    }

    // from interface Sequence<T>
    public ArrayList<T> toList ()
    {
        return copyInto(new ArrayList<T>(_source.size()));
    }

    // from interface Sequence<T>
    public HashSet<T> toSet ()
    {
        return copyInto(new HashSet<T>(_source.size()));
    }

    // from interface Sequence<T>
    public T[] toArray (Class<T> clazz)
    // In a perfect world this would be <S super T> S[] toArray (Class<S> clazz)
    {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(clazz, _source.size());
        int index = 0;
        for (F elem : _source) {
            array[index++] = _func.apply(elem);
        }
        return array;
    }

    protected <C extends Collection<T>> C copyInto (C coll)
    {
        for (F elem : _source) {
            coll.add(_func.apply(elem));
        }
        return coll;
    }

    protected final Collection<F> _source;
    protected final Function<? super F, ? extends T> _func;
}
