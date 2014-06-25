//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl;

import java.io.Serializable;

/**
 * A base class for all query results (persistent records, or subsets of persistent records in the
 * form of tuples of varying arity).
 */
public abstract class QueryResult
    implements Serializable, Cloneable
{
    @Override
    public QueryResult clone ()
    {
        try {
            return (QueryResult) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse); // this should never happen since we are Cloneable
        }
    }
}
