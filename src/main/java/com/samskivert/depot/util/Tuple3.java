//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.util;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Contains a three column result. This class is immutable and the objects it references are also
 * meant to be immutable. They will generally contain only Depot primitive types (Java primitives,
 * SQL primitives and some array types), which should be treated as immutable.
 */
public class Tuple3<A,B,C> implements Serializable
{
    /** The first column of the result. */
    public final A a;

    /** The second column of the result. */
    public final B b;

    /** The third column of the result. */
    public final C c;

    /** Constructs an initialized tuple. */
    public static <A, B, C> Tuple3<A, B, C> create (A a, B b, C c)
    {
        return new Tuple3<A, B, C>(a, b, c);
    }

    /** Creates a builder for 3-tuples. */
    public static <A, B, C> Builder3<Tuple3<A, B, C>, A, B, C> builder ()
    {
        return new Builder3<Tuple3<A, B, C>, A, B, C>() {
            public Tuple3<A, B, C> build (A a, B b, C c) {
                return create(a, b, c);
            }
        };
    }

    /** Constructs an initialized tuple. */
    public Tuple3 (A a, B b, C c)
    {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public String toString ()
    {
        return "[" + a + "," + b + "," + c + "]";
    }

    @Override
    public int hashCode ()
    {
        return Objects.hashCode(a, b, c);
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof Tuple3<?,?,?>) {
            Tuple3<?,?,?> otup = (Tuple3<?,?,?>)other;
            return Objects.equal(a, otup.a) && Objects.equal(b, otup.b) && Objects.equal(c, otup.c);
        } else {
            return false;
        }
    }
}
