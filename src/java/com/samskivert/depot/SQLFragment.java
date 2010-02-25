//
// $Id$

package com.samskivert.depot;

import java.util.Collection;

import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.SQLBuilder;

/**
 * Represents a fragment of an SQL statement, generally a clause (WHERE, FROM, ...) or an
 * expression (1+1, 'foo', "columnName", ...).
 */
public interface SQLFragment
{
    /**
     * Most uses of this class have been implemented with a visitor pattern. Create your own
     * {@link FragmentVisitor} and call this method with it.
     *
     * @see SQLBuilder
     */
    public Object accept (FragmentVisitor<?> visitor);

    /**
     * Adds all persistent classes that are brought into the SQL context by this clause: FROM
     * clauses, JOINs, UPDATEs, anything that could create a new table abbreviation. This method
     * should recurse into any subordinate state that may in turn bring in new classes so that
     * sub-queries work correctly.
     */
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet);
}
