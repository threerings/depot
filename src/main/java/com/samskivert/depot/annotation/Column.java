//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Is used to specify a mapped column for a persistent property or field. If no Column annotation
 * is specified, the default values are applied.
 */
@Target(value=ElementType.FIELD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Column
{
    /**
     * The name of the column. Defaults to the field name.
     */
    String name () default "";

    /**
     * Whether the property is a unique key. This is a shortcut for the UniqueConstraint annotation
     * at the table level and is useful for when the unique key constraint is only a single
     * field. This constraint applies in addition to any constraint entailed by primary key mapping
     * and to constraints specified at the table level.
     */
    boolean unique () default false;

    /**
     * Whether the database column is nullable. <em>Note:</em> this default differs from the value
     * used by the EJB3 persistence framework.
     */
    boolean nullable () default false;

    /**
     * The column length. (Applies to String and byte[] columns.)
     */
    int length () default 255;

    /**
     * The SQL literal value to be used when defining this column's default value. The value must
     * be quoted and escaped if it is not a SQL primitive datatype. For example:
     * <code>'2006-01-01'</code> or <code>25</code> or <code>NULL</code>.
     */
    String defaultValue () default "";
}
