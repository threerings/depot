//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

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
     * public static ColumnExp[] indexName ()
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
