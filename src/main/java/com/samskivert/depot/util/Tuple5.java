//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.util;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Contains a five column result. This class is immutable and the objects it references are also
 * meant to be immutable. They will generally contain only Depot primitive types (Java primitives,
 * SQL primitives and some array types), which should be treated as immutable.
 */
public class Tuple5<A,B,C,D,E> implements Serializable
{
    /** The first column of the result. */
    public final A a;

    /** The second column of the result. */
    public final B b;

    /** The third column of the result. */
    public final C c;

    /** The fourth column of the result. */
    public final D d;

    /** The fifth column of the result. */
    public final E e;

    /** Constructs an initialized tuple. */
    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> create (A a, B b, C c, D d, E e)
    {
        return new Tuple5<A, B, C, D, E>(a, b, c, d, e);
    }

    /** Creates a builder for 5-tuples. */
    public static <A, B, C, D, E> Builder5<Tuple5<A, B, C, D, E>, A, B, C, D, E> builder ()
    {
        return new Builder5<Tuple5<A, B, C, D, E>, A, B, C, D, E>() {
            public Tuple5<A, B, C, D, E> build (A a, B b, C c, D d, E e) {
                return create(a, b, c, d, e);
            }
        };
    }

    /** Constructs an initialized tuple. */
    public Tuple5 (A a, B b, C c, D d, E e)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    @Override
    public String toString ()
    {
        return "[" + a + "," + b + "," + c + "," + d + "," + e + "]";
    }

    @Override
    public int hashCode ()
    {
        return Objects.hashCode(a, b, c, d, e);
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof Tuple5<?,?,?,?,?>) {
            Tuple5<?,?,?,?,?> otup = (Tuple5<?,?,?,?,?>)other;
            return Objects.equal(a, otup.a) && Objects.equal(b, otup.b) &&
                Objects.equal(c, otup.c) && Objects.equal(d, otup.d) && Objects.equal(e, otup.e);
        } else {
            return false;
        }
    }
}
