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

import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Handy record for computing the count of something. For example:
 * <pre>
 * return load(CountRecord.class,
 *     new FromOverride(ForumThreadRecord.class),
 *     new Where(ForumThreadRecord.GROUP_ID.eq(groupId)).count;
 * </pre>
 */
@Computed @Entity
public class CountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CountRecord> _R = CountRecord.class;
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The computed count. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
