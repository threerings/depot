//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.clause;

import com.samskivert.depot.expression.SQLExpression;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Currently only exists as a type without any functionality of its own.
 */
public abstract class WhereClause implements QueryClause
{
    /**
     * Returns the condition associated with this where clause.
     */
    public abstract SQLExpression<?> getWhereExpression ();

    /**
     * Validates that the supplied persistent record type is the type matched by this where clause.
     * Not all clauses will be able to perform this validation, but those that can, should do so to
     * help alleviate programmer error.
     *
     * @exception IllegalArgumentException thrown if the supplied class is known not to by the type
     * matched by this where clause.
     */
    public void validateQueryType (Class<?> pClass)
    {
        // nothing by default
    }

    /**
     * A helper function for implementing {@link #validateQueryType}.
     */
    protected void validateTypesMatch (Class<?> qClass, Class<?> kClass)
    {
        checkArgument(qClass.equals(kClass),
                      "Class mismatch between persistent record and key in query " +
                      "[qtype=%s, ktype=%s].", qClass.getSimpleName(), kClass.getSimpleName());
    }
}
