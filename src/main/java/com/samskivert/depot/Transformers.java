//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.ByteEnumUtil;

import com.samskivert.depot.annotation.Column;

import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Contains various generally useful {@link Transformer} implementations. To use a transformer, you
 * specify it via a {@link Column} annotation. For example:
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     &#064;Transform(Transformers.StringArray.class)
 *     public String[] cities;
 * }
 * </pre>
 */
public class Transformers
{
    /**
     * Combines the contents of a String[] column into a single String, terminating each String
     * element with a newline. A backslash ('\') in Strings will be prefixed by another backslash,
     * newlines will be encoded as "\n", and null elements will be encoded as "\0" (but not
     * terminated by a newline).
     */
    public static class StringArray extends StringBase<String[]>
    {
        protected Iterable<String> toIterable (String[] value)
        {
            return Arrays.asList(value);
        }

        protected Builder<String[]> createBuilder (Type ftype, String encoded)
        {
            final String[] result = new String[countElements(encoded)];
            return new Builder<String[]>() {
                public void add (String s) {
                    result[idx++] = s;
                }
                public String[] build () {
                    return result;
                }
                protected int idx = 0;
            };
        }
    }

    /**
     * Combines the contents of an Iterable<String> column into a single String, terminating each
     * String element with a newline. A backslash ('\') in Strings will be prefixed by another
     * backslash, newlines will be encoded as "\n", and null elements will be encoded as "\0" (but
     * not terminated by a newline).
     */
    public static class StringIterable extends StringIterableBase
    {
        protected Builder<Iterable<String>> createBuilder (Type ftype, String encoded)
        {
            Type fclass = (ftype instanceof ParameterizedType) ?
                ((ParameterizedType)ftype).getRawType() : ftype;
            final Collection<String> collection = createCollection(fclass, encoded);
            return new Builder<Iterable<String>>() {
                public void add (String s) {
                    collection.add(s);
                }
                public Iterable<String> build () {
                    return collection;
                }
            };
        }

        protected Collection<String> createCollection (Type fclass, String encoded)
        {
            // TODO: TreeSet, etc, etc
            if (fclass == HashSet.class || fclass == Set.class) {
                return Sets.newHashSet();

            } else if (fclass == LinkedList.class) {
                return Lists.newLinkedList();

            } else {
                return Lists.newArrayList();
            }
        }
    }

    public static class ImmutableStringIterable extends StringIterableBase
    {
        protected Builder<Iterable<String>> createBuilder (Type ftype, String encoded)
        {
            Type fclass = (ftype instanceof ParameterizedType) ?
                ((ParameterizedType)ftype).getRawType() : ftype;
            // TODO: SortedSet
            if (fclass == Set.class) {
                return new Builder<Iterable<String>>() {
                    public void add (String s) {
                        _builder.add(s);
                    }
                    public Iterable<String> build () {
                        return _builder.build();
                    }
                    protected ImmutableSet.Builder<String> _builder = ImmutableSet.builder();
                };

            } else { //if (fclass == List.class)
                return new Builder<Iterable<String>>() {
                    public void add (String s) {
                        _builder.add(s);
                    }
                    public Iterable<String> build () {
                        return _builder.build();
                    }
                    protected ImmutableList.Builder<String> _builder = ImmutableList.builder();
                };
            }
        }
    }

    public static class InternedImmutableStringIterable extends ImmutableStringIterable
    {
        @Override
        public Iterable<String> fromPersistent (Type ftype, String encoded)
        {
            Iterable<String> result = super.fromPersistent(ftype, encoded);
            return (result == null) ? result : INTERNER.intern(result);
        }
       
        @Override protected boolean doInterning ()
        {
            return true;
        }

        protected static final Interner<Iterable<String>> INTERNER = Interners.newWeakInterner();
    }

    public static class ByteEnumSet<E extends Enum<E> & ByteEnum>
        implements Transformer<Set<E>, Integer>
    {
        public Integer toPersistent (Set<E> value)
        {
            return (value == null) ? null : ByteEnumUtil.setToInt(value);
        }

        public Set<E> fromPersistent (Type ftype, Integer encoded)
        {
            if (encoded == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<E> eclass = (Class<E>) ((ParameterizedType) ftype).getActualTypeArguments()[0];
            return ByteEnumUtil.intToSet(eclass, encoded);
        }
    }

    protected abstract static class StringBase<F> implements Transformer<F, String>
    {
        public String toPersistent (F value)
        {
            if (value == null) {
                return null;
            }

            StringBuilder buf = new StringBuilder();
            for (String s : toIterable(value)) {
                if (s == null) {
                    buf.append("\\\n"); // encode nulls as slash followed by the terminator
                } else {
                    s = s.replace("\\", "\\\\"); // turn \ into \\ 
                    s = s.replace("\n", "\\n");  // turn a newline in a String to "\n"
                    buf.append(s).append('\n');
                }
            }
            return buf.toString();
        }

        public F fromPersistent (Type ftype, String encoded)
        {
            if (encoded == null) {
                return null;
            }

            Builder<F> builder = createBuilder(ftype, encoded);
            boolean intern = doInterning();
            StringBuilder buf = new StringBuilder(encoded.length());
            for (int ii = 0, nn = encoded.length(); ii < nn; ii++) {
                char c = encoded.charAt(ii);
                switch (c) {
                case '\n':
                    String s = buf.toString();
                    builder.add(intern ? s.intern() : s);
                    buf.setLength(0);
                    break;

                case '\\':
                    Preconditions.checkArgument(++ii < nn, "Invalid encoded string");
                    char slashed = encoded.charAt(ii);
                    switch (slashed) {
                    case '\n': // turn back into a null element
                        Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
                        builder.add(null);
                        break;
                    case 'n': // turn \n back into a newline
                        buf.append('\n');
                        break;
                    default: // this should only be a slash...
                        buf.append(slashed);
                        break;
                    }
                    break;

                default:
                    buf.append(c);
                    break;
                }
            }
            // make sure the last element was terminated
            Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
            return builder.build();
        }

        protected boolean doInterning ()
        {
            return false;
        }

        protected abstract Iterable<String> toIterable (F value);

        protected abstract Builder<F> createBuilder (Type ftype, String encoded);

        /**
         * Count the number of elements in the encoded non-null string.
         */
        protected static int countElements (String encoded)
        {
            int count = 0;
            for (int pos = 0; 0 != (pos = 1 + encoded.indexOf('\n', pos)); count++) {}
            return count;
        }

        protected interface Builder<F>
        {
            void add (String s);

            F build ();
        }
    }

    protected static abstract class StringIterableBase extends StringBase<Iterable<String>>
    {
        protected Iterable<String> toIterable (Iterable<String> value)
        {
            return value;
        }
    }
}
