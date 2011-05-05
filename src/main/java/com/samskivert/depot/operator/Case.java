//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.operator;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.util.Tuple;

/**
 * The SQL 'case' operator.
 */
public class Case<T>
    implements SQLExpression<T>
{
    public Case (SQLExpression<?>... exps)
    {
        int i = 0;
        for (; i + 1 < exps.length; i += 2) {
            _whenExps.add(Tuple.<SQLExpression<?>, SQLExpression<?>>newTuple(exps[i], exps[i + 1]));
        }
        _elseExp = (i < exps.length) ? exps[i] : null;
    }

    public List<Tuple<SQLExpression<?>, SQLExpression<?>>> getWhenExps ()
    {
        return _whenExps;
    }

    public SQLExpression<?> getElseExp ()
    {
        return _elseExp;
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        for (Tuple<SQLExpression<?>, SQLExpression<?>> tuple : _whenExps) {
            tuple.left.addClasses(classSet);
            tuple.right.addClasses(classSet);
        }
        if (_elseExp != null) {
            _elseExp.addClasses(classSet);
        }
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Case(");
        for (Tuple<SQLExpression<?>, SQLExpression<?>> tuple : _whenExps) {
            builder.append(tuple.left.toString()).append("->");
            builder.append(tuple.right.toString()).append(",");
        }
        if (_elseExp != null) {
            builder.append(_elseExp.toString()).append(")");
        }
        return builder.toString();
    }

    protected List<Tuple<SQLExpression<?>, SQLExpression<?>>> _whenExps = Lists.newArrayList();
    protected SQLExpression<?> _elseExp;
}
