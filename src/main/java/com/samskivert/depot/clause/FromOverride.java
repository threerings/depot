//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 *  Completely overrides the FROM clause, if it exists.
 */
public class FromOverride implements QueryClause
{
    public FromOverride (Class<? extends PersistentRecord> fromClass)
    {
        _fromClasses.add(fromClass);
    }

    public FromOverride (Class<? extends PersistentRecord> fromClass1,
                         Class<? extends PersistentRecord> fromClass2)
    {
        _fromClasses.add(fromClass1);
        _fromClasses.add(fromClass2);
    }

    public FromOverride (Collection<Class<? extends PersistentRecord>> fromClasses)
    {
        _fromClasses.addAll(fromClasses);
    }

    public List<Class<? extends PersistentRecord>> getFromClasses ()
    {
        return _fromClasses;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.addAll(getFromClasses());
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        for (Class<? extends PersistentRecord> clazz : _fromClasses) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(DepotUtil.justClassName(clazz));
        }
        return builder.toString();
    }

    /** The classes of the tables we're selecting from. */
    protected List<Class<? extends PersistentRecord>> _fromClasses = Lists.newArrayList();
}
