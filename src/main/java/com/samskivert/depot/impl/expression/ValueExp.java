//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.expression;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * A Java value that is bound as a parameter to the query, e.g. 1 or 'abc'.
 */
public class ValueExp<T> extends FluentExp<T>
{
    public ValueExp (T value)
    {
        _value = value;
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
    }

    public T getValue ()
    {
        return _value;
    }

    @Override // from Object
    public String toString ()
    {
        return (_value instanceof Number) ? String.valueOf(_value) : ("'" + _value + "'");
    }

    /** The value to be bound to the SQL parameters. */
    protected T _value;
}
