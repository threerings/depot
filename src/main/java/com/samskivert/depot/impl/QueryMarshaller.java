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

package com.samskivert.depot.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;

/**
 * Marshalls the results for a query.
 */
public interface QueryMarshaller<T extends PersistentRecord,R>
{
    /**
     * Returns the name of the table in which persistent instances of our class are stored. By
     * default this is the classname of the persistent object without the package.
     */
    String getTableName ();

    /**
     * Returns the expressions being selected for this query.
     */
    SQLExpression<?>[] getSelections ();

    /**
     * Extracts the primary key from the supplied object.
     */
    Key<T> getPrimaryKey (Object object);

    /**
     * Creates an instance of the query result from the supplied result set.
     */
    R createObject (ResultSet rs) throws SQLException;
}
