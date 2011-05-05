//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.util;

import java.lang.reflect.Array;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
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
