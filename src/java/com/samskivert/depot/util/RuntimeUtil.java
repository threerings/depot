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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.samskivert.depot.PersistentRecord;

/**
 * Creates functions that map persistent records to runtime counterparts and vice versa.
 */
public class RuntimeUtil
{
    /**
     * Creates a function that creates an instance of R and initializes all accessible (ie. public)
     * fields of R from fields of P with matching name. Fields of P that do not exist in R will be
     * ignored. Note: the types of the fields must match exactly.
     */
    public static <P extends PersistentRecord, R> Function<P, R> makeToRuntime (
        Class<P> pclass, final Class<R> rclass)
    {
        final Field[] rfields = getRuntimeFields(rclass);
        final Field[] pfields = getPersistentFields(pclass, rfields);
        return new Function<P, R>() {
            public R apply (P record) {
                try {
                    R object = rclass.newInstance();
                    for (int ii = 0, ll = rfields.length; ii < ll; ii++) {
                        rfields[ii].set(object, pfields[ii].get(record));
                    }
                    return object;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Creates a function that creates an instance of P and initializes all accessible (ie. public)
     * fields of P from fields of R with matching name. Fields of P that do not exist in R will be
     * left as default. Note: the types of the fields must match exactly.
     */
    public static <R, P extends PersistentRecord> Function<R, P> makeToRecord (
        Class<R> rclass, final Class<P> pclass)
    {
        final Field[] rfields = getRuntimeFields(rclass);
        final Field[] pfields = getPersistentFields(pclass, rfields);
        return new Function<R, P>() {
            public P apply (R object) {
                try {
                    P record = pclass.newInstance();
                    for (int ii = 0, ll = rfields.length; ii < ll; ii++) {
                        pfields[ii].set(record, rfields[ii].get(object));
                    }
                    return record;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    protected static Field[] getRuntimeFields (Class<?> rclass)
    {
        List<Field> fields = Lists.newArrayList();
        for (Field field : rclass.getFields()) {
            int mods = field.getModifiers();
            if (!Modifier.isStatic(mods) && Modifier.isPublic(mods)) {
                fields.add(field);
            }
        }
        return fields.toArray(new Field[fields.size()]);
    }

    protected static Field[] getPersistentFields (
        Class<? extends PersistentRecord> pclass, Field[] rfields)
    {
        Field[] pfields = new Field[rfields.length];
        for (int ii = 0; ii < rfields.length; ii++) {
            try {
                pfields[ii] = pclass.getField(rfields[ii].getName());
            } catch (NoSuchFieldException nsfe) {
                throw new IllegalArgumentException(
                    "Cannot create mapping for " + rfields[ii] + ". " +
                    "No corresponding field exists in " + pclass + ".");
            }
            if (!pfields[ii].getType().equals(rfields[ii].getType())) {
                throw new IllegalArgumentException("Cannot create mapping from " + pfields[ii] +
                                                   " to " + rfields[ii] + ".");
            }
        }
        return pfields;
    }
}
