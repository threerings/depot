//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

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
 * supported directly by Depot:</p>
 *
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     &#064;Transform(Transformers.StringArray.class)
 *     public String[] cities;
 * }
 * </pre>
 *
 * <p>or one may opt to specify a transformation for all fields that contain a value of a particular
 * type:</p>
 *
 * <pre>
 * &#064;Transform(ByteEnumTransformer.class)
 * public interface ByteEnum { ... }
 * </pre>
 *
 * <p>Note that because Depot honors @Transform annotations on any interface implemented by a type,
 * or on a type's superclass, it is possible that conflicting transformations may be specified. In
 * this case, Depot will fail with a runtime error during initialization, to indicate the
 * problem.</p>
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
    @SuppressWarnings("rawtypes")
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
