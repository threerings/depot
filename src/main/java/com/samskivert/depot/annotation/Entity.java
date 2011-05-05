//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the primary field(s) of an entity.
 */
@Target(value=ElementType.TYPE)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Entity
{
    /** The name of an entity. Defaults to the unqualified name of the entity class. */
    String name () default "";

    /** Unique constraints that are to be placed on this entity's table. These constraints apply in
     * addition to any constraints specified by the Column annotation and constraints entailed by
     * primary key mappings. Defaults to no additional constraints. */
    UniqueConstraint[] uniqueConstraints () default {};

    /** Indices to add to this entity's table. */
    Index[] indices () default {};

    /** Full-text search indexes defined on this entity, if any. Defaults to none. */
    FullTextIndex[] fullTextIndices () default {};
}
