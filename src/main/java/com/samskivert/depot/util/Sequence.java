//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.util;

import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.base.Function;

/**
 * Provides read-only access to a sequence of elements. Note that these elements will generally be
 * computed via the application of a {@link Function} *every time* you iterate over this sequence.
 * Thus a caller is expected to either simply iterate over the elements once, or use {@link
 * #toList} to convert the sequence to concrete list to avoid repeated application of the
 * conversion function on elements of the sequence.
 *
 * Repository authors <i>may</i> want to return this interface for transformed findAll queries
 * in order to avoid creating a second large List to contain elements. However, that exposes
 * this interface to callers. The other two options are: go ahead and call toList() and return
 * that to callers, or enforce single-iteration and return the result of iterator().
 */
public interface Sequence<T> extends Iterable<T>
{
    /**
     * Returns the number of elements in this sequence.
     */
    int size ();

    /**
     * Returns true if this sequence is empty, false if it contains at least one element.
     */
    boolean isEmpty ();

    /**
     * Converts this sequence into an {@link ArrayList}.
     */
    ArrayList<T> toList ();

    /**
     * Converts this sequence into a {@link HashSet}.
     */
    HashSet<T> toSet ();

    /**
     * Converts this sequence into an array.
     */
    T[] toArray (Class<T> clazz);
    // Ray wishes this were <S super T> S[] toArray (Class<S> clazz);
}
