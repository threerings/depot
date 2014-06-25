//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.util;

/**
 * A type-safe builder used to construct objects from the columns selected from Depot queries.
 */
public interface Builder2<T, A, B>
{
    /**
     * Builds an instance, using the supplied data.
     */
    public T build (A a, B b);
}
