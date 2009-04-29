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

package com.samskivert.depot;

import java.io.Serializable;

import com.samskivert.depot.expression.ColumnExp;

/**
 * The base class for all persistent records used in Depot. Persistent records must be cloneable
 * and serializable; this class is used to enforce those requirements.
 */
public class PersistentRecord
    implements Cloneable, Serializable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PersistentRecord> _R = PersistentRecord.class;
    // AUTO-GENERATED: FIELDS END

    @Override // from Object
    public PersistentRecord clone ()
    {
        try {
            return (PersistentRecord) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // this should never happen
        }
    }

    /**
     * Creates a column expression for this class with the specified field name. Used by the
     * autogenerated column expression constants.
     */
    protected static ColumnExp colexp (Class<? extends PersistentRecord> clazz, String fieldName)
    {
        return new ColumnExp(clazz, fieldName);
    }
}
