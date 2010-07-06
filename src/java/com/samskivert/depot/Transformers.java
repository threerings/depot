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

import com.samskivert.depot.annotation.Column;

import com.google.common.base.Preconditions;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Contains various generally useful {@link Transformer} implementations. To use a transformer, you
 * specify it via a {@link Column} annotation. For example:
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     @Transform(Transformers.StringArray.class)
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
        public String toPersistent (String[] value)
        {
            return (value == null) ? null : encode(Arrays.asList(value));
        }

        public String[] fromPersistent (Type ftype, String encoded)
        {
            return (encoded == null) ? null : Iterables.toArray(decode(encoded), String.class);
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
        public String toPersistent (Iterable<String> value)
        {
            return (value == null) ? null : encode(value);
        }

        public Iterable<String> fromPersistent (Type ftype, String encoded)
        {
            if (encoded == null) {
                return null;
            }

            ArrayList<String> value = decode(encoded);
            Type fclass = (ftype instanceof ParameterizedType) ?
                ((ParameterizedType)ftype).getRawType() : ftype;
            if (fclass == ArrayList.class || fclass == List.class ||
                fclass == Collection.class || fclass == Iterable.class) {
                return value;
            }
            if (fclass == LinkedList.class) {
                return Lists.newLinkedList(value);
            }
            if (fclass == HashSet.class || fclass == Set.class) {
                return Sets.newHashSet(value);
            }
            // else: reflection? See if it's a collection, call the 0-arg constructor, add all
            // and return? Something?
            return value;
        }
    }

    protected abstract static class StringBase<F> implements Transformer<F, String>
    {
        protected static String encode (Iterable<String> value)
        {
            StringBuilder buf = new StringBuilder();
            for (String s : value) {
                if (s == null) {
                    buf.append("\\0"); // encode nulls as "\0" (with no terminator)
                } else {
                    s = s.replace("\\", "\\\\"); // turn \ into \\ 
                    s = s.replace("\n", "\\n");  // turn a newline in a String to "\n"
                    buf.append(s).append('\n');
                }
            }
            return buf.toString();
        }

        protected static ArrayList<String> decode (String encoded)
        {
            ArrayList<String> value = Lists.newArrayList();
            StringBuilder buf = new StringBuilder(encoded.length());
            for (int ii = 0, nn = encoded.length(); ii < nn; ii++) {
                char c = encoded.charAt(ii);
                switch (c) {
                case '\n':
                    value.add(buf.toString()); // TODO: intern?
                    buf.setLength(0);
                    break;

                case '\\':
                    Preconditions.checkArgument(++ii < nn, "Invalid encoded string");
                    char slashed = encoded.charAt(ii);
                    switch (slashed) {
                    case '0': // turn \0 into a null element
                        Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
                        value.add(null);
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
            return value;
        }
    }
}
