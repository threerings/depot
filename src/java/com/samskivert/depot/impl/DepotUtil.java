//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Simple utility methods used by various things.
 */
public class DepotUtil
{
    /**
     * Returns an array containing the names of the primary key fields for the supplied persistent
     * class. The values are introspected and cached for the lifetime of the VM.
     */
    public static ColumnExp[] getKeyFields (Class<? extends PersistentRecord> pClass)
    {
        return _keyFields.get(pClass);
    }

    /**
     * Returns the name of the supplied class minus its package.
     */
    public static String justClassName (Class<?> clazz)
    {
        return clazz.getName().substring(clazz.getName().lastIndexOf(".")+1);
    }

    /** A (never expiring) cache of primary key field names for all persistent classes (of which
     * there are merely dozens, so we don't need to worry about expiring). */
    protected static ConcurrentMap<Class<? extends PersistentRecord>, ColumnExp[]> _keyFields =
        new MapMaker()
        .makeComputingMap(new Function<Class<? extends PersistentRecord>, ColumnExp[]>() {
            public ColumnExp[] apply (Class<? extends PersistentRecord> pClass) {
                List<ColumnExp> kflist = Lists.newArrayList();
                for (Field field : pClass.getFields()) {
                    // look for @Id fields
                    if (field.getAnnotation(Id.class) != null) {
                        kflist.add(new ColumnExp(pClass, field.getName()));
                    }
                }
                return kflist.toArray(new ColumnExp[kflist.size()]);
            }
        });
}
