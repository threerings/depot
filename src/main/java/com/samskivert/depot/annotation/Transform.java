//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Transformer;

/**
 * Declares that the target of this annotation, either a field in a {@link PersistentRecord} or a
 * type (which will eventually be stored in a persistent record), should be transformed before
 * saving to the database and after loading from the database.
 *
 * <p> For example, one may choose to transform a particular persistent record field into a type
 * supported directly by Depot:
 *
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     &#064;Transform(Transformers.StringArray.class)
 *     public String[] cities;
 * }
 * </pre>
 *
 * or one may opt to specify a transformation for all fields that contain a value of a particular
 * type:
 *
 * <pre>
 * &#064;Transform(ByteEnumTransformer.class)
 * public interface ByteEnum { ... }
 * </pre>
 *
 * Note that because Depot honors @Transform annotations on any interface implemented by a type, or
 * on a type's superclass, it is possible that conflicting transformations may be specified. In
 * this case, Depot will fail with a runtime error during initialization, to indicate the problem.
 *
 * @see Transformer
 */
@Target(value={ElementType.TYPE, ElementType.FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
public @interface Transform
{
    /**
     * Specifies a transformer to be used when persisting the target of this annotation.
     */
    Class<? extends Transformer> value ();

    /**
     * Hint to the transformer whether it should return an immutable result.
     * The transformer is free to ignore this hint.
     */
    boolean immutable () default false;

    /**
     * Hint to the transformer whether it should return an interned result, which should only
     * be honored if the result is also immutable.
     * The transformer is free to ignore this hint.
     */
    boolean intern () default false;
}
