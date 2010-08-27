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

package com.samskivert.depot.impl;

import java.io.Serializable;
import java.util.Arrays;

import com.samskivert.depot.CacheKey;
import com.samskivert.depot.Key;

/**
 * Converts a {@link CacheKey} to and from a {@link Key} in a way that eliminates references to
 * non-Java classes (so that we don't have to deal with RMI classloader hell when replicating cache
 * contents).
 */
public class KeyCacheKey
    implements CacheKey, Serializable
{
    public KeyCacheKey (Key<?> key)
    {
        _cacheId = key.getPersistentClass().getName();
        Comparable<?>[] values = key.getValues();
        _values = new Comparable<?>[values.length];
        for (int ii = 0; ii < _values.length; ii++) {
            _values[ii] = values[ii]; // TODO: check for non-system-class and serialize
        }
    }

    // from CacheKey
    public String getCacheId ()
    {
        return _cacheId;
    }

    // from CacheKey
    public Serializable getCacheKey ()
    {
        return this;
    }

    @Override // from Object
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(_values, ((KeyCacheKey) obj)._values);
    }

    @Override // from Object
    public int hashCode ()
    {
        return Arrays.hashCode(_values);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder(_cacheId);
        builder.append("(");
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append(_values[ii]);
        }
        builder.append(")");
        return builder.toString();
    }

    protected String _cacheId;
    protected Comparable<?>[] _values;
}
