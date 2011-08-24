//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.operator;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * An attempt at a dialect-agnostic full-text search condition, such as MySQL's MATCH() and
 * PostgreSQL's @@ TO_TSQUERY(...) abilities.
 */
public class FullText
{
    public class Rank extends FluentExp<Number>
    {
        // from SQLExpression
        public Object accept (FragmentVisitor<?> builder)
        {
            return builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
        }

        @Override // from Object
        public String toString ()
        {
            return FullText.this.toString("Rank");
        }

        public FullText getDefinition ()
        {
            return FullText.this;
        }
    }

    public class Match extends FluentExp<Boolean>
    {
        // from SQLExpression
        public Object accept (FragmentVisitor<?> builder)
        {
            return builder.visit(this);
        }

        public FullText getDefinition ()
        {
            return FullText.this;
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
        }

        @Override // from Object
        public String toString ()
        {
            return FullText.this.toString("Match");
        }
    }

    /**
     * @param pClass The persistent record the full-text query is defined on.
     * @param name The name of the corresponding {@link FullTextIndex}.
     * @param query What to actually search for; this can be unprocessed natural language
     * @param matchAll If true, all words in the query must be present in matches.
     */
    public FullText (
        Class<? extends PersistentRecord> pClass, String name, String query, boolean matchAll)
    {
        _pClass = pClass;
        _name = name;
        _query = query;
        _matchAll = matchAll;
    }

    /**
     * @param pClass The persistent record the full-text query is defined on.
     * @param name The name of the corresponding {@link FullTextIndex}.
     * @param query What to actually search for; this can be unprocessed natural language
     */
    public FullText (Class<? extends PersistentRecord> pClass, String name, String query)
    {
        this(pClass, name, query, false);
    }

    public Match match ()
    {
        return new Match();
    }

    public Rank rank ()
    {
        return new Rank();
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public String getName ()
    {
        return _name;
    }

    public String getQuery ()
    {
        return _query;
    }

    public boolean isMatchAll ()
    {
        return _matchAll;
    }

    protected String toString (String subType)
    {
        return "FullText." + subType + "(" + _name + "=" + _query + ", " + _matchAll + ")";
    }

    protected Class<? extends PersistentRecord> _pClass;
    protected String _name;
    protected String _query;
    protected boolean _matchAll;
}
