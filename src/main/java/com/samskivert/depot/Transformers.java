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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.ByteEnumUtil;

import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Transform;

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
     * Transform a Set of ByteEnums into an Integer.
     * Does not presently support the immutable or interning hints.
     */
    public static class ByteEnumSet<E extends Enum<E> & ByteEnum>
        extends Transformer<Set<E>, Integer>
    {
        @Override @SuppressWarnings("unchecked")
        public void init (Type fieldType, Transform annotation)
        {
            _eclass = (Class<E>) ((ParameterizedType) fieldType).getActualTypeArguments()[0];
        }

        @Override
        public Integer toPersistent (Set<E> value)
        {
            return (value == null) ? null : ByteEnumUtil.setToInt(value);
        }

        @Override
        public Set<E> fromPersistent (Integer encoded)
        {
            return (encoded == null) ? null : ByteEnumUtil.intToSet(_eclass, encoded);
        }

        /** The enum class token. */
        protected Class<E> _eclass;
    }

    /**
     * Combines the contents of a String[] column into a single String, terminating each String
     * element with a newline. A backslash ('\') in Strings will be prefixed by another backslash,
     * newlines will be encoded as "\n", and null elements will be encoded as "\0" (but not
     * terminated by a newline).
     */
    public static class StringArray extends StringBase<String[]>
    {
        @Override protected Iterable<String> asIterable (String[] value)
        {
            return Arrays.asList(value);
        }

        @Override protected Builder<String[]> createBuilder (String encoded)
        {
            // jog through and count the elements so that we can populate the array directly
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
    public static class StringIterable extends StringBase<Iterable<String>>
    {
        @Override protected Iterable<String> asIterable (Iterable<String> value)
        {
            return value;
        }

        @Override protected Builder<Iterable<String>> createBuilder (String encoded)
        {
            Collection<String> adder;
            Collection<String> retval = null;
            Type clazz = (_ftype instanceof ParameterizedType) ?
                ((ParameterizedType)_ftype).getRawType() : _ftype;
            // TODO: fill out the collection types
            if (clazz == HashSet.class || clazz == Set.class) {
                Set<String> set = Sets.newHashSet();
                adder = set;
                if (_immutable && (clazz == Set.class)) {
                    retval = Collections.unmodifiableSet(set);
                }

            } else if (clazz == LinkedList.class) {
                adder = Lists.newLinkedList();

            } else {
                List<String> list = Lists.newArrayList();
                adder = list;
                if (_immutable &&
                        ((clazz == List.class) ||
                         (clazz == Iterable.class) ||
                         (clazz == Collection.class))) {
                    retval = Collections.unmodifiableList(list);
                }
            }
            return createBuilder(adder, (retval == null) ? adder : retval);
        }

        protected Builder<Iterable<String>> createBuilder (
            final Collection<String> adder, final Collection<String> retval)
        {
            Preconditions.checkNotNull(adder);
            return new Builder<Iterable<String>>() {
                public void add (String s) {
                    adder.add(s);
                }
                public Iterable<String> build () {
                    return (_immutable && _intern && (adder != retval))
                        ? INTERNER.intern(retval)
                        : retval;
                }
            };
        }
    }

    protected abstract static class StringBase<F> extends Transformer<F, String>
    {
        @Override
        public void init (Type fieldType, Transform annotation)
        {
            _ftype = fieldType;
            _immutable = annotation.immutable();
            _intern = annotation.intern();
        }

        @Override
        public String toPersistent (F value)
        {
            if (value == null) {
                return null;
            }

            StringBuilder buf = new StringBuilder();
            for (String s : asIterable(value)) {
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

        @Override
        public F fromPersistent (String encoded)
        {
            if (encoded == null) {
                return null;
            }

            Builder<F> builder = createBuilder(encoded);
            StringBuilder buf = new StringBuilder(encoded.length());
            for (int ii = 0, nn = encoded.length(); ii < nn; ii++) {
                char c = encoded.charAt(ii);
                switch (c) {
                case '\n':
                    String s = buf.toString();
                    builder.add(_intern ? s.intern() : s);
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

        protected abstract Iterable<String> asIterable (F value);

        protected abstract Builder<F> createBuilder (String encoded);

        /**
         * Utility tount the number of elements in the encoded non-null string.
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

        protected Type _ftype;

        /** Immutable hint. */
        protected boolean _immutable;

        /** Interning hint.*/
        protected boolean _intern;

        /** The interner we use for <em>immutable</em> values. */
        protected static final Interner<Iterable<String>> INTERNER = Interners.newWeakInterner();
    }
}
