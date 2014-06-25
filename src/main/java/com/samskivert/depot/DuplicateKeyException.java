//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

/**
 * Thrown when an insert or update results in a duplicate key on a column that has a uniqueness
 * constraint.
 */
public class DuplicateKeyException extends DatabaseException
{
    public DuplicateKeyException (String message)
    {
        super(message);
    }
}
