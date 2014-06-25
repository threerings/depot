//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines an index on an entity table.
 */
@Target(value=ElementType.FIELD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Index
{
    /**
     * Defines the name of the index.
     *
     * <p>If this annotation is on a field, an index is created for all fields annotated with this
     * name in the order of the fields in the class.</p>
     *
     * <p>If this annotation is on a {@code PersistentRecord} class, a static method must be defined
     * in that class that provides the index configuration. The method must match one of the
     * following two signatures:</p>
     *
     * <pre>{@code
     * public static ColumnExp<?>[] indexName ()
     * public static List<Tuple<SQLExpression, OrderBy.Order>> indexName ()
     * }</pre>
     *
     * <p>The first form will result in a simple multicolum index being created with the supplied
     * columns. The second will create a function index using the supplied {@code
     * SQLExpression}s.</p>
     */
    String name () default "";

    /** Does this index enforce a uniqueness constraint? */
    boolean unique () default false;
}
