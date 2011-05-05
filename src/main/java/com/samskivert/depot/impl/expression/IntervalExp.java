//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl.expression;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * A code for representing a date interval.
 */
public class IntervalExp
    implements SQLExpression<Integer>
{
    /** The units that can be used for an interval. */
    public enum Unit { YEAR, MONTH, DAY, HOUR, MINUTE, SECOND }

    /** The unit for this interval. */
    public final Unit unit;

    /** The number of units for this interval. */
    public final int amount;

    public IntervalExp (Unit unit, int amount)
    {
        this.unit = unit;
        this.amount = amount;
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

    @Override
    public String toString ()
    {
        return amount + " " + unit;
    }
}
