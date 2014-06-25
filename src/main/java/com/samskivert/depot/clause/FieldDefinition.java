//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot.clause;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.expression.LiteralExp;

/**
 * Supplies a definition for a computed field of the persistent object we're creating.
 *
 * Thus the select portion of a query can include a reference to a different column in a different
 * table through a {@link ColumnExp}, or a literal expression such as COUNT(*) through a
 * {@link LiteralExp}.
 *
 * @see FieldOverride
 */
public class FieldDefinition implements QueryClause
{
    public FieldDefinition (String field, String str)
    {
        this(field, new LiteralExp<Object>(str));
    }

    public FieldDefinition (String field, Class<? extends PersistentRecord> pClass, String pCol)
    {
        this(field, new ColumnExp<Object>(pClass, pCol));
    }

    public FieldDefinition (String field, SQLExpression<?> override)
    {
        _field = field;
        _definition = override;
    }

    public FieldDefinition (ColumnExp<?> field, SQLExpression<?> override)
    {
        _field = field.name;
        _definition = override;
    }

    /**
     * The field we're defining. The Query object uses this for indexing.
     */
    public String getField ()
    {
        return _field;
    }

    public SQLExpression<?> getDefinition ()
    {
        return _definition;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _definition.addClasses(classSet);
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> visitor)
    {
        return visitor.visit(this);
    }

    /** The name of the field on the persistent object to override. */
    protected String _field;

    /** The defining expression. */
    protected SQLExpression<?> _definition;

}
