//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;

/**
 * Contains a two column result. This class is immutable and the objects it references are also
 * meant to be immutable. They will generally contain only Depot primitive types (Java primitives,
 * SQL primitives and some array types), which should be treated as immutable.
 */
public class Tuple2<A,B> implements Serializable
{
    /** The first column of the result. */
    public final A a;

    /** The second column of the result. */
    public final B b;

    /** Constructs an initialized two tuple. */
    public static <A, B> Tuple2<A, B> create (A a, B b)
    {
        return new Tuple2<A, B>(a, b);
    }

    /** Creates a builder for 2-tuples. */
    public static <A, B> Builder2<Tuple2<A, B>, A, B> builder ()
    {
        return new Builder2<Tuple2<A, B>, A, B>() {
            public Tuple2<A, B> build (A a, B b) {
                return create(a, b);
            }
        };
    }

    /**
     * Converts the supplied list of tuples {@code [(a, b), ...]} into a map {@code [a -> b]}.
     * Useful in situations like so:
     * <pre>{@code
     * Map<Integer, String> results =
     *   Tuple2.toMap(from(table).select(FooRecord.INT_KEY, FooRecord.STRING_VALUE))
     * }</pre>
     */
    public static <A, B> Map<A, B> toMap (Iterable<Tuple2<A, B>> tuples)
    {
        return toMap(tuples, new HashMap<A, B>());
    }

    /**
     * Converts the supplied list of tuples {@code [(a, b), ...]} into a map {@code [a -> b]},
     * inserting the tuples into the supplied target map. Useful in situations like so:
     * <pre>{@code
     * Map<Integer, String> results =
     *   Tuple2.toMap(from(table).select(FooRecord.INT_KEY, FooRecord.STRING_VALUE))
     * }</pre>
     */
    public static <A, B> Map<A, B> toMap (Iterable<Tuple2<A, B>> tuples, Map<A, B> target)
    {
        for (Tuple2<A, B> tuple : tuples) {
            target.put(tuple.a, tuple.b);
        }
        return target;
    }

    /** Constructs an initialized two tuple. */
    public Tuple2 (A a, B b)
    {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString ()
    {
        return "[" + a + "," + b + "]";
    }

    @Override
    public int hashCode ()
    {
        return Objects.hashCode(a, b);
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof Tuple2<?,?>) {
            Tuple2<?,?> otup = (Tuple2<?,?>)other;
            return Objects.equal(a, otup.a) && Objects.equal(b, otup.b);
        } else {
            return false;
        }
    }
}
