//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.Stats;
import com.samskivert.jdbc.DatabaseLiaison;

/**
 * An abstraction that encompasses both {@link Fetcher} and {@link Modifier} operations.
 */
public interface Operation<T>
{
    /**
     * Indicates whether or not this operation is safe to invoke on a database mirror.
     */
    public boolean isReadOnly ();

    /**
     * Performs the actual JDBC interactions associated with this operation. Any {@link Statement}
     * instances created with the given connection will be closed automatically after this method
     * returns. The operation need not close them itself.
     */
    public T invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
        throws SQLException;

    /**
     * Called after the operation has been invoked so that it can update our runtime statistics.
     */
    public void updateStats (Stats stats);
}
