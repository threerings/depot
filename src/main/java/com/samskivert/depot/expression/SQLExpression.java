//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.expression;

import com.samskivert.depot.SQLFragment;

/**
 * Represents an SQL expression, e.g. column name, function, or constant.
 */
public interface SQLExpression<T> extends SQLFragment
{
    /** Used internally to represent the lack of a value. */
    public static final class NoValue
    {
        public NoValue (String reason)
        {
            _reason = reason;
        }

        @Override public String toString () {
            return "[unknown value, reason=" + _reason + "]";
        }

        protected String _reason;
    }

}
