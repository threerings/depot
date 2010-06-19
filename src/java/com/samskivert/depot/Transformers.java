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

import com.google.common.base.Joiner;

import com.samskivert.depot.annotation.Column;

/**
 * Contains various generally useful {@link Transformer} implementations. To use a transformer, you
 * specify it via a {@link Column} annotation. For example:
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     @Transform(Transformers.CommaSeparatedString.class)
 *     public String[] cities;
 * }
 * </pre>
 */
public class Transformers
{
    /**
     * Combines the contents of a String[] column into a single string, separated by tabs. Any tabs
     * in the strings will be escaped.
     */
    public static class TabSeparatedString implements Transformer<String[], String> {
        public String toPersistent (String[] values) {
            StringBuffer buf = new StringBuffer();
            for (String value : values) {
                if (buf.length() > 0) {
                    buf.append("\t");
                }
                buf.append(value.replace("\t", "\\\t"));
            }
            return buf.toString();
        }
        public String[] fromPersistent (Class<?> ftype, String value) {
            String[] values = value.replace("\\\t", "%%ESCTAB%%").split("\t");
            for (int ii = 0; ii < values.length; ii++) {
                values[ii] = values[ii].replace("%%ESCTAB%%", "\t");
            }
            return values;
        }
    }
}
