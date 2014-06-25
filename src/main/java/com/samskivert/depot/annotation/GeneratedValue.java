//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides for the specification of generation strategies for the values of primary keys.  The
 * GeneratedValue annotation may be applied to a primary field in conjunction with the {@link Id}
 * annotation.
 */
@Target(value=ElementType.FIELD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface GeneratedValue
{
    /** Identifies which generator should be used to generate this value when using a table or
     * sequence generator. */
    String generator () default "";

    /** Identifies the strategy to be used to generate this value. */
    GenerationType strategy () default GenerationType.AUTO;

    /**
     * The initial value to be used when allocating id numbers from the generator. The default
     * initial value is 1. <em>Note:</em> this default differs from the value used by the EJB3
     * persistence framework.
     */
    int initialValue () default 1;

    /**
     * If there are rows in our corresponding table, this boolean determines whether or not to
     * attempt to initialize the generator to the maximum value of our associated field over
     * those rows. This attribute does not exist in the EJB3 framework.
     */
    boolean migrateIfExists () default true;

    /**
     * The amount to increment by when allocating id numbers from the generator. The default
     * allocation size is 1. <em>Note:</em> this default differs from the value used by the EJB3
     * persistence framework.
     */
    int allocationSize () default 1;
}
