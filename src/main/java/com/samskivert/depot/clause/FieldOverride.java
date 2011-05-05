//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.clause;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.expression.LiteralExp;

/**
 * Redirects one field of the persistent object we're creating from its default associated column
 * to a general {@link SQLExpression}.
 *
 * Thus the select portion of a query can include a reference to a different column in a different
 * table through a {@link ColumnExp}, or a literal expression such as COUNT(*) through a
 * {@link LiteralExp}.
 */
public class FieldOverride extends FieldDefinition
{
    public FieldOverride (String field, String str)
    {
        super(field, str);
    }

    public FieldOverride (String field, Class<? extends PersistentRecord> pClass, String pCol)
    {
        super(field, pClass, pCol);
    }

    public FieldOverride (String field, SQLExpression<?> override)
    {
        super(field, override);
    }

    
    public FieldOverride (ColumnExp<?> field, SQLExpression<?> override)
    {
        super(field, override);
    }
}
