//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

/**
 * Represents a failure reported by the underlying database.
 */
public class DatabaseException extends RuntimeException
{
    /**
     * Constructs a database exception with the specified error message.
     */
    public DatabaseException (String message)
    {
        super(message);
    }

    /**
     * Constructs a database exception with the specified error message and the chained causing
     * event.
     */
    public DatabaseException (String message, Throwable cause)
    {
        super(message);
        initCause(cause);
    }

    /**
     * Constructs a database exception with the specified chained causing event.
     */
    public DatabaseException (Throwable cause)
    {
        initCause(cause);
    }
}
