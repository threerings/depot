//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.depot;

import com.samskivert.depot.clause.ForUpdate;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

/**
 * Provides a variety of helper methods for constructing queries more concisely (and in some case
 * using a more fluent style).
 */
public abstract class FluentRepository extends DepotRepository
{
    protected FluentRepository (PersistenceContext context)
    {
        super(context);
    }

    /**
     * Returns a {@link Where} clause that ANDs together all of the supplied expressions.
     */
    protected static Where where (SQLExpression... exprs)
    {
        switch (exprs.length) {
        case 0:
            throw new IllegalArgumentException("Must supply at least one expression.");
        case 1:
            return new Where(exprs[0]);
        default:
            return new Where(Ops.and(exprs));
        }
    }

    /**
     * Returns a {@link Where} clause that selects rows where the supplied column equals the
     * supplied value.
     */
    protected static Where where (ColumnExp column, Comparable<?> value)
    {
        return new Where(column, value);
    }

    /**
     * Returns a {@link Where} clause that selects rows where both supplied columns equal both
     * supplied values.
     */
    protected static Where where (ColumnExp index1, Comparable<?> value1,
                                  ColumnExp index2, Comparable<?> value2)
    {
        return new Where(index1, value1, index2, value2);
    }

    /**
     * Returns a {@link Join} clause configured with the supplied left and right columns.
     */
    protected static Join join (ColumnExp left, ColumnExp right)
    {
        return new Join(left, right);
    }

    /**
     * Returns a {@link GroupBy} clause on the supplied group expressions.
     */
    protected static GroupBy groupBy (SQLExpression... exprs)
    {
        return new GroupBy(exprs);
    }

    /**
     * Returns an {@link OrderBy} clause configured to randomly order the results.
     */
    protected static OrderBy random ()
    {
        return OrderBy.random();
    }

    /**
     * Returns an {@link OrderBy} clause that ascends on the supplied expression.
     */
    protected static OrderBy ascending (SQLExpression value)
    {
        return OrderBy.ascending(value);
    }

    /**
     * Returns an {@link OrderBy} clause that descends on the supplied expression.
     */
    protected static OrderBy descending (SQLExpression value)
    {
        return OrderBy.descending(value);
    }

    /**
     * Returns a {@link Limit} clause configured with the supplied offset and count.
     */
    protected static Limit limit (int offset, int count)
    {
        return new Limit(offset, count);
    }

    /**
     * Returns a {@link FromOverride} clause configured with the supplied override class.
     */
    protected static FromOverride from (Class<? extends PersistentRecord> fromClass)
    {
        return new FromOverride(fromClass);
    }

    /**
     * Returns a {@link FromOverride} clause configured with the supplied override classes.
     */
    protected static FromOverride from (Class<? extends PersistentRecord> fromClass1,
                                        Class<? extends PersistentRecord> fromClass2)
    {
        return new FromOverride(fromClass1, fromClass2);
    }

    /**
     * Returns a {@link ForUpdate} clause which marks this query as selecting for update.
     */
    protected static ForUpdate forUpdate ()
    {
        return new ForUpdate();
    }
}
