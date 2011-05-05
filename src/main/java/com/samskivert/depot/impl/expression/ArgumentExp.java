//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.expression;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;

public abstract class ArgumentExp<T> extends FluentExp<T>
{
    protected ArgumentExp (SQLExpression<?>... args)
    {
        _args = args;
    }

    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        for (SQLExpression<?> arg : _args) {
            arg.addClasses(classSet);
        }
    }

    public SQLExpression<?>[] getArgs ()
    {
        return _args;
    }

    protected SQLExpression<?>[] _args;
}
