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

import java.lang.reflect.Type;

import com.samskivert.depot.annotation.Transform;

/**
 * Transforms a persistent record field into a format that can be read and written by the
 * underlying database. For example, one might transform an enum into a byte, short or integer. Or
 * one might transform a string array into a single string, using a separator known to be
 * appropriate for the contents.
 *
 * @see Transformers
 */
public abstract class Transformer<F,T>
{
    /**
     * Initialize this Transformer.
     */
    public void init (Type fieldType, Transform annotation)
    {
        // nada by default
    }

    /**
     * Transforms a runtime value into a value that can be persisted.
     *
     * @param value the value just read from a persistent record.
     *
     * @return the transformed value, which will be written to the database.
     */
    public abstract T toPersistent (F value);

    /**
     * Transforms a persisted value into a value that can be store in a runtime field.
     *
     * @param value the value just read from the database.
     *
     * @return the transformed value, which will be stored in a field of the persistent record.
     */
    public abstract F fromPersistent (T value);
}
