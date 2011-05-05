//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a uniqueness constraint on a set of columns. If you want a single column to be
 * unique, simply use {@link Column#unique}.
 */
@Target(value={})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface UniqueConstraint
{
    /** The name of the index that will be created to enforce this constraint. */
    String name ();

    /** An array of the field names that make up the constraint */
    String[] fields ();
}
