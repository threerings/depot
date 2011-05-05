//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

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
     * Defines the name of the index.<p>
     *
     * If this annotation is on a field, an index is created for all fields annotated with this
     * name in the order of the fields in the class.<p>
     *
     * If this is defined, a static method must be defined on the record that provides the index
     * configuration. The method must match one of the following two signatures:
     * <pre>
     * public static ColumnExp<?>[] indexName ()
     * public static List&lt;Tuple&lt;SQLExpression, OrderBy.Order>> indexName ()
     * </pre>
     * The first form will result in a simple multicolum index being created
     * with the supplied columns. The second will create a function index using the supplied
     * SQLExpressions.
     */
    String name () default "";

    /** Does this index enforce a uniqueness constraint? */
    boolean unique () default false;
}
