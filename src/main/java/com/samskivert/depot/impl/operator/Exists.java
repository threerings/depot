//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * The SQL 'exists' operator.
 */
public class Exists
    implements SQLExpression<Boolean>
{
    public Exists (SelectClause clause)
    {
        _clause = clause;
    }

    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _clause.addClasses(classSet);
    }

    public SelectClause getSubClause ()
    {
        return _clause;
    }

    @Override // from Object
    public String toString ()
    {
        return "Exists(" + _clause + ")";
    }

    protected SelectClause _clause;
}
