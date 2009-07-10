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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.util.Tuple;

/**
 * Creates functions that map persistent records to runtime counterparts and vice versa. A few bits
 * of magic are provided to help in mapping from the persistent world to the runtime work. Namely:
 *
 * <p> {@link Timestamp} and {@link java.sql.Date} are automatically converted to and from {@link
 * Date} if necessary.
 *
 * <p> You can define custom getter and setter methods in your persistent class like so:
 * <pre>
 * public class FooRecord extends PersistentRecord
 * {
 *     public long monkeyStamp;
 *
 *     public Date getMonkeyStamp ()
 *     {
 *         return new Date(monkeyStamp);
 *     }
 *
 *     public void setMonkeyStamp (Date value)
 *     {
 *         monkeyStamp = value.getTime();
 *     }
 * }
 * </pre>
 *
 * <p> The conversion methods must be named <code>get</code> and <code>set</code> followed by the
 * name of your field with the first letter changed to a capital and their type signatures must
 * exactly match those of the persistent and runtime types.
 */
public class RuntimeUtil
{
    /**
     * Creates a function that creates an instance of R and initializes all accessible (ie. public)
     * fields of R from fields of P with matching name. If the function is applied to null, null
     * will be returned. Fields of P that do not exist in R will be ignored. See the class
     * documentation for a list of field conversions that will be made automatically and the
     * mechanism for performing other conversions.
     */
    public static <P extends PersistentRecord, R> Function<P, R> makeToRuntime (
        Class<P> pclass, final Class<R> rclass)
    {
        final Field[] rfields = getRuntimeFields(rclass);
        final Getter[] getters = getPersistentGetters(pclass, rfields);
        return new Function<P, R>() {
            public R apply (P record) {
                if (record == null) {
                    return null;
                }
                try {
                    R object = rclass.newInstance();
                    for (int ii = 0, ll = rfields.length; ii < ll; ii++) {
                        rfields[ii].set(object, getters[ii].get(record));
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
     * fields of P from fields of R with matching name. If the function is applied to null a
     * NullPointerException will be thrown. Fields of P that do not exist in R will be left as
     * default. Note: the types of the fields must match exactly.
     */
    public static <R, P extends PersistentRecord> Function<R, P> makeToRecord (
        Class<R> rclass, final Class<P> pclass)
    {
        final Field[] rfields = getRuntimeFields(rclass);
        final Setter[] setters = getPersistentSetters(pclass, rfields);
        return new Function<R, P>() {
            public P apply (R object) {
                if (object == null) {
                    throw new NullPointerException(
                        "Cannot convert null runtime record to " + pclass.getSimpleName());
                }
                try {
                    P record = pclass.newInstance();
                    for (int ii = 0, ll = rfields.length; ii < ll; ii++) {
                        setters[ii].set(record, rfields[ii].get(object));
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

    protected static Getter[] getPersistentGetters (Class<?> pclass, Field[] rfields)
    {
        Field[] pfields = getPersistentFields(pclass, rfields);
        Getter[] getters = new Getter[rfields.length];
        for (int ii = 0; ii < rfields.length; ii++) {
            getters[ii] = makeGetter(pclass, pfields[ii], rfields[ii]);
        }
        return getters;
    }

    protected static Setter[] getPersistentSetters (Class<?> pclass, Field[] rfields)
    {
        Field[] pfields = getPersistentFields(pclass, rfields);
        Setter[] setters = new Setter[rfields.length];
        for (int ii = 0; ii < rfields.length; ii++) {
            setters[ii] = makeSetter(pclass, pfields[ii], rfields[ii]);
        }
        return setters;
    }

    protected static Getter makeGetter (Class<?> pclass, final Field pfield, Field rfield)
    {
        // if there's a custom getter method for the field, use that foremost
        try {
            final Method method = pclass.getMethod(makeMethodName("get", rfield.getName()));
            if (method.getReturnType().equals(rfield.getType())) {
                return new Getter() {
                    public Object get (Object object) throws Exception {
                        return method.invoke(object);
                    }
                };
            }
        } catch (NoSuchMethodException nsme) {
            // no problem, keep on truckin'
        }

        // if we have no persistent field for the runtime field, we're now out of luck
        if (pfield == null) {
            throw new IllegalArgumentException("Cannot create mapping for " + rfield + ". " +
                                               "No corresponding field exists in " + pclass + ".");
        }

        // if the fields match exactly, return a getter that just gets the field
        if (rfield.getType().equals(pfield.getType())) {
            return new Getter() {
                public Object get (Object object) throws Exception {
                    return pfield.get(object);
                }
            };
        }

        // if we can convert from the persistent type to the runtime type, do that
        final Function<Object, Object> converter = getconv(pfield.getType(), rfield.getType());
        if (converter != null) {
            return new Getter() {
                public Object get (Object object) throws Exception {
                    return converter.apply(pfield.get(object));
                }
            };
        }

        // if we have exhausted all other approaches, we're SOL
        throw new IllegalArgumentException("Cannot map " + pfield + " to " + rfield + ".");
    }

    protected static Setter makeSetter (Class<?> pclass, final Field pfield, Field rfield)
    {
        // check for a custom setter method for the field (with the correct argument type)
        try {
            final Method method = pclass.getMethod(
                makeMethodName("set", rfield.getName()), rfield.getType());
            return new Setter() {
                public void set (Object object, Object value) throws Exception {
                    method.invoke(object, value);
                }
            };
        } catch (NoSuchMethodException nsme) {
            // no problem, keep on truckin'
        }

        // if we have no persistent field for this runtime field, we're now out of luck
        if (pfield == null) {
            throw new IllegalArgumentException("Cannot create setter for " + rfield + ". " +
                                               "No corresponding field exists in " + pclass + ".");
        }

        // if the fields match exactly, return a setter that just sets the field
        if (rfield.getType().equals(pfield.getType())) {
            return new Setter() {
                public void set (Object object, Object value) throws Exception {
                    pfield.set(object, value);
                }
            };
        }

        // if we can convert from the runtime type to the persistent type, do that
        final Function<Object, Object> converter = getconv(rfield.getType(), pfield.getType());
        if (converter != null) {
            return new Setter() {
                public void set (Object object, Object value) throws Exception {
                    pfield.set(object, converter.apply(value));
                }
            };
        }

        // if we have exhausted all other approaches, we're SOL
        throw new IllegalArgumentException("Cannot map " + rfield + " to " + pfield + ".");
    }

    protected static Field[] getPersistentFields (Class<?> pclass, Field[] rfields)
    {
        Field[] pfields = new Field[rfields.length];
        for (int ii = 0; ii < rfields.length; ii++) {
            try {
                pfields[ii] = pclass.getField(rfields[ii].getName());
            } catch (NoSuchFieldException nsfe) {
                // we may have a magical method that handles this field, so leave this null
            }
        }
        return pfields;
    }

    protected static String makeMethodName (String prefix, String fieldName)
    {
        return new StringBuilder().append(prefix).
            append(Character.toUpperCase(fieldName.charAt(0))).
            append(fieldName.substring(1)).toString();
    }

    protected static interface Getter
    {
        public Object get (Object object) throws Exception;
    }

    protected static interface Setter
    {
        public void set (Object object, Object value) throws Exception;
    }

    protected static Function<Object, Object> getconv (Class<?> fc, Class<?> tc)
    {
        return _converters.get(Tuple.newTuple(fc, tc));
    }

    protected static <F, T> void regconv (Class<F> fc, Class<T> tc, Function<F, T> conv)
    {
        @SuppressWarnings("unchecked") Function<Object, Object> value =
            (Function<Object, Object>)conv;
        _converters.put(Tuple.newTuple(fc, tc), value);
    }

    protected static Map<Object, Function<Object, Object>> _converters =
        Maps.newHashMap();
    static {
        regconv(Timestamp.class, Date.class, new Function<Timestamp, Date>() {
            public Date apply (Timestamp object) {
                return (object == null) ? null : new Date(object.getTime());
            }
        });
        regconv(Date.class, Timestamp.class, new Function<Date, Timestamp>() {
            public Timestamp apply (Date object) {
                return (object == null) ? null : new Timestamp(object.getTime());
            }
        });
        regconv(java.sql.Date.class, Date.class, new Function<java.sql.Date, Date>() {
            public Date apply (java.sql.Date object) {
                return (object == null) ? null : new Date(object.getTime());
            }
        });
        regconv(Date.class, java.sql.Date.class, new Function<Date, java.sql.Date>() {
            public java.sql.Date apply (Date object) {
                return (object == null) ? null : new java.sql.Date(object.getTime());
            }
        });
    }
}
