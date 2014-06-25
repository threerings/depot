//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.operator.Equals;

/**
 *  Represents a JOIN.
 */
public class Join implements QueryClause
{
    /** Indicates the join type to be used. The default is INNER. */
    public static enum Type { INNER, LEFT_OUTER, RIGHT_OUTER }

    public Join (ColumnExp<?> primary, ColumnExp<?> join)
    {
        _joinClass = join.getPersistentClass();
        _joinCondition = new Equals(primary, join);
    }

    public Join (Class<? extends PersistentRecord> joinClass, SQLExpression<?> joinCondition)
    {
        _joinClass = joinClass;
        _joinCondition = joinCondition;
    }

    /**
     * Configures the type of join to be performed.
     */
    public Join setType (Type type)
    {
        _type = type;
        return this;
    }

    public Type getType ()
    {
        return _type;
    }

    public Class<? extends PersistentRecord> getJoinClass ()
    {
        return _joinClass;
    }

    public SQLExpression<?> getJoinCondition ()
    {
        return _joinCondition;
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_joinClass);
        _joinCondition.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        return DepotUtil.justClassName(_joinClass) + ":" + _type + ":" + _joinCondition;
    }

    /** Indicates the type of join to be performed. */
    protected Type _type = Type.INNER;

    /** The class of the table we're to join against. */
    protected Class<? extends PersistentRecord> _joinClass;

    /** The condition used to join in the new table. */
    
    protected SQLExpression<?> _joinCondition;
}
