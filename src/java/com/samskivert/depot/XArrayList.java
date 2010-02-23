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

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * An array list specialization that implements {@link XList}. Depot returns this list from all of
 * its methods to make it easy for callers to map the results to runtime records as desired.
 */
public class XArrayList<T> extends ArrayList<T>
    implements XList<T>
{
    @Deprecated // from interface XList<T>
    public <R> Collection<R> map (Function<? super T, ? extends R> mapper)
    {
        return Lists.transform(this, mapper);
    }
}
