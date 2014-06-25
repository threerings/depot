//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.expression;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * An expression that unambiguously identifies a field of a class, for example
 * <code>GameRecord.itemId</code>.
 */
public class ColumnExp<T> extends FluentExp<T>
{
    /** The name of the column we reference. */
    public final String name;

    public ColumnExp (Class<? extends PersistentRecord> pClass, String field)
    {
        super();
        _pClass = pClass;
        this.name = field;
    }

    /**
     * Returns a column expression for the supplied persistent class with the same name as this
     * expression. This is useful for "casting" a column expression from a parent class to a
     * derived class.
     */
    public ColumnExp<T> as (Class<? extends PersistentRecord> oClass)
    {
        return new ColumnExp<T>(oClass, name);
    }

    /** Returns a {@link Join} on this column and the supplied target. */
    public Join join (ColumnExp<?> join)
    {
        return new Join(this, join);
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    @Override // from Object
    public int hashCode ()
    {
        return _pClass.hashCode() ^ this.name.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof ColumnExp<?>) &&
            ((ColumnExp<?>)other)._pClass.equals(_pClass) &&
            ((ColumnExp<?>)other).name.equals(this.name);
    }

    @Override // from Object
    public String toString ()
    {
        return "\"" + name + "\""; // TODO: qualify with record name and be uber verbose?
    }

    /** The table that hosts the column we reference, or null. */
    protected final Class<? extends PersistentRecord> _pClass;
}
