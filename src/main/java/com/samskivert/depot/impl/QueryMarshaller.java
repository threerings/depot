//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

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
