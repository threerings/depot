//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.annotation;

/**
 * Defines the types of primary key generation.
 */
public enum GenerationType
{
    /**
     * Indicates that the persistence provider must assign primary keys for the entity using an
     * underlying database table to ensure uniqueness.
     */
    TABLE,

    /**
     * Indicates that the persistence provider must assign primary keys for the entity using
     * database sequences.
     */
    SEQUENCE,

    /**
     * Indicates that the persistence provider must assign primary keys for the entity using
     * database identity column.
     */
    IDENTITY,

    /**
     * Indicates that the persistence provider should pick an appropriate strategy for the
     * particular database.
     */
    AUTO;
}
