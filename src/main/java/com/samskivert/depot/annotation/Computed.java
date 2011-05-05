//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;

/**
 * Marks a field as computed, meaning it is ignored for schema purposes and it does not directly
 * correspond to a column in a table.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
public @interface Computed
{
    /** If this value is false, the field is not populated at all. */
    boolean required () default true;

    /** A non-empty value here is taken as literal SQL and used to populate the computed field. */
    String fieldDefinition () default "";

    /**
     * A computed record can shadow a concrete record, which causes any field the former has in
     * common with the latter and which is not otherwise overriden to inherit its definition. The
     * shadowed class may also be given at the field level.
     *
     * The purpose of shadowing is largely to avoid having to supply a {@link FieldOverride} when
     * querying for objects that that contain large subsets of some other persistent object's
     * fields -- in other words, when you use a computed entity to query only some of the columns
     * from a table.
     *
     * The shadowed class must have been brought into the query using e.g. {@link FromOverride}
     * or {@link Join} clauses. The referenced fields must be simple concrete columns in a table;
     * they must themselves be computed or overridden or shadowing.
     *
     * TODO: Do in fact let the shadowed field be computed, overriden or shadowing.
     */
    Class<? extends PersistentRecord> shadowOf () default PersistentRecord.class;
}
