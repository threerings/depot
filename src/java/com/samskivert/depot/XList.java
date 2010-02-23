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

package com.samskivert.depot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;

import com.samskivert.depot.util.Sequence;

/**
 * Extends the {@link List} interface with a method {@link #map} that makes it easy to convert the
 * contents of the list to an ordered {@link Collection} of a different type via the application of
 * a {@link Function}.
 */
public interface XList<T> extends List<T>
{
    /**
     * @deprecated Use {@link DepotRepository#map} and {@link Sequence}.
     */
    @Deprecated
    public <R> Collection<R> map (Function<? super T, ? extends R> mapper);
}
