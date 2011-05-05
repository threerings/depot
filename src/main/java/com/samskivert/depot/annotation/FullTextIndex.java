//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to specify that a full text index is to be included in the generated DDL
 * for a table.
 */
@Target(value={})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface FullTextIndex
{
    public enum Configuration {
        /** For searching generic strings; makes no linguistic/semantic assumptions. */
        Simple,
        /** For English text search; does basic stemming. */
        English
    }

    /**
     * An identifier for this index, unique with the scope of the record.
     */
    public String name ();

    /**
     * An array of the field names that should be indexed.
     */
    public String[] fields ();

    /**
     * What parser/dictionary to use for this index.
     */
    public Configuration configuration () default Configuration.English;
}
