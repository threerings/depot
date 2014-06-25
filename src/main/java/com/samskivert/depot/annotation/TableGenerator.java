//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation defines a primary key generator that may be referenced by name when a generator
 * element is specified for the GeneratedValue annotation. A table generator may be specified on
 * the entity class or on the primary key field. The scope of the generator name is global to the
 * persistence unit (across all generator types).
 */
@Target(value={ElementType.TYPE, ElementType.FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface TableGenerator
{
    /**
     * A unique generator name that can be referenced by one or more classes to be the
     * generator for id values.
     */
    String name ();

    /**
     * Name of table that stores the generated id values. Defaults to a name chosen by persistence
     * provider.
     */
    String table () default "";

    /**
     * Name of the primary key column in the table Defaults to a provider-chosen name.
     */
    String pkColumnName () default "";

    /**
     * Name of the column that stores the last value generated Defaults to a provider-chosen name.
     */
    String valueColumnName () default "";

    /**
     * The primary key value in the generator table that distinguishes this set of generated values
     * from others that may be stored in the table Defaults to a provider-chosen value to store in
     * the primary key column of the generator table
     */
    String pkColumnValue () default "";
}
