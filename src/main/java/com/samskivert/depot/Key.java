//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.DepotMarshaller;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.FragmentVisitor;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A special form of {@link WhereClause} that uniquely specifies a single database row and thus
 * also a single persistent object. It knows how to invalidate itself upon modification. This class
 * is created by many {@link DepotMarshaller} methods as a convenience, and may also be
 * instantiated explicitly.
 */
public class Key<T extends PersistentRecord> extends WhereClause
    implements ValidatingCacheInvalidator
{
    /** Handles the matching of the key columns to its bound values. This is needed so that we can
     * combine a bunch of keys into a {@link KeySet}. */
    public static class Expression implements SQLExpression<Object>
    {
        public Expression (Class<? extends PersistentRecord> pClass, Comparable<?>[] values) {
            _pClass = pClass;
            _values = values;
        }
        public Class<? extends PersistentRecord> getPersistentClass () {
            return _pClass;
        }
        public Comparable<?>[] getValues () {
            return _values;
        }
        public Object accept (FragmentVisitor<?> builder) {
            return builder.visit(this);
        }
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet) {
            classSet.add(getPersistentClass());
        }
        protected Class<? extends PersistentRecord> _pClass;
        protected Comparable<?>[] _values;
    }

    /**
     * Creates a single column key.
     */
    public static <T extends PersistentRecord, V extends Comparable<V>> Key<T> newKey (
        Class<T> pClass, ColumnExp<V> ix, V val)
    {
        return new Key<T>(pClass, new ColumnExp<?>[] { ix }, new Comparable<?>[] { val });
    }

    /**
     * Creates a two column key.
     */
    public static <T extends PersistentRecord, V1 extends Comparable<V1>, V2 extends Comparable<V2>>
        Key<T> newKey (Class<T> pClass, ColumnExp<V1> ix1, V1 val1, ColumnExp<V2> ix2, V2 val2)
    {
        return new Key<T>(pClass, new ColumnExp<?>[] { ix1, ix2 },
                          new Comparable<?>[] { val1, val2 });
    }

    /**
     * Creates a three column key.
     */
    public static <T extends PersistentRecord, V1 extends Comparable<V1>,
                   V2 extends Comparable<V2>, V3 extends Comparable<V3>>
        Key<T> newKey (Class<T> pClass, ColumnExp<V1> ix1, V1 val1, ColumnExp<V2> ix2, V2 val2,
                       ColumnExp<V3> ix3, V3 val3)
    {
        return new Key<T>(pClass, new ColumnExp<?>[] { ix1, ix2, ix3 },
                          new Comparable<?>[] { val1, val2, val3 });
    }

    /**
     * Returns a function that extracts an integer from a record's {@link Key}. This should only be
     * used on records whose primary key is a single integer.
     */
    public static <T extends PersistentRecord> Function<Key<T>, Integer> toInt ()
    {
        return extract(0);
    }

    /**
     * Returns a function that extracts an element key from a record's {@link Key}.
     *
     * @param index the index in the key of the element to be extracted.
     */
    public static <T extends PersistentRecord, E> Function<Key<T>, E> extract (final int index)
    {
        return new Function<Key<T>, E>() {
            public E apply (Key<T> key) {
                @SuppressWarnings("unchecked") E value = (E)key.getValues()[index];
                return value;
            }
        };
    }

    /**
     * Constructs a new multi-column {@code Key} with the given values.
     */
    public Key (Class<T> pClass, ColumnExp<?>[] fields, Comparable<?>[] values)
    {
        this(pClass, toCanonicalOrder(pClass, fields, values));
    }

    /**
     * Used to create a key when you know you have the canonical values array. Don't call this
     * unless you know what you're doing!
     */
    // TODO: This should perhaps be made package-private, but DepotMarshaller uses it
    public Key (Class<T> pClass, Comparable<?>[] values)
    {
        _pClass = pClass;
        _values = values;
    }

    /**
     * Returns the persistent class for which we represent a key.
     */
    public Class<T> getPersistentClass ()
    {
        return _pClass;
    }

    /**
     * Returns the values bound to this key.
     */
    public Comparable<?>[] getValues ()
    {
        return _values;
    }

    @Override // from WhereClause
    public SQLExpression<?> getWhereExpression ()
    {
        return new Expression(_pClass, _values);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    // from SQLExpression
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from ValidatingCacheInvalidator
    public void validateFlushType (Class<?> pClass)
    {
        checkArgument(pClass.equals(_pClass),
                      "Class mismatch between persistent record and cache invalidator " +
                      "[record=%s, invtype=%s].", pClass.getSimpleName(), _pClass.getSimpleName());
    }

    // from CacheInvalidator
    public void invalidate (PersistenceContext ctx)
    {
        ctx.cacheInvalidate(this);
    }

    /**
     * Appends just the key=value portion of our {@link #toString} to the supplied buffer.
     */
    public void toShortString (StringBuilder builder)
    {
        ColumnExp<?>[] keyFields = DepotUtil.getKeyFields(_pClass);
        for (int ii = 0; ii < keyFields.length; ii ++) {
            if (ii > 0) {
                builder.append(":");
            }
            builder.append(keyFields[ii].name).append("=").append(_values[ii]);
        }
    }

    @Override // from WhereClause
    public void validateQueryType (Class<?> pClass)
    {
        super.validateQueryType(pClass);
        validateTypesMatch(pClass, _pClass);
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(_values, ((Key<?>) obj)._values);
    }

    @Override
    public int hashCode ()
    {
        return Arrays.hashCode(_values);
    }

    @Override
    public String toString ()
    {
        StringBuilder builder = new StringBuilder(_pClass.getSimpleName());
        builder.append("(");
        toShortString(builder);
        builder.append(")");
        return builder.toString();
    }

    protected static Comparable<?>[] toCanonicalOrder (
        Class<? extends PersistentRecord> pClass, ColumnExp<?>[] fields, Comparable<?>[] values)
    {
        checkArgument(fields.length == values.length,
                      "Field and Value arrays must be of equal length.");

        // look up the cached primary key fields for this object
        ColumnExp<?>[] keyFields = DepotUtil.getKeyFields(pClass);

        // fast path!
        if (fields.length == 1 && keyFields.length == 1 && keyFields[0].equals(fields[0])) {
            return new Comparable<?>[] { values[0] };
        }

        // build a local map of field name -> field value
        Map<ColumnExp<?>, Comparable<?>> map = Maps.newHashMap();
        for (int ii = 0; ii < fields.length; ii++) {
            map.put(fields[ii], values[ii]);
        }

        // now extract the values in field order and ensure none are extra or missing
        Comparable<?>[] cvalues = new Comparable<?>[values.length];
        for (int ii = 0; ii < keyFields.length; ii++) {
            Comparable<?> value = map.remove(keyFields[ii]);
            // make sure we were provided with a value for this primary key field
            checkArgument(value != null, "Missing value for key field: " + keyFields[ii]);
            checkArgument(value instanceof Serializable,
                          "Non-serializable argument [key=%s, value=%s]", keyFields[ii], value);
            cvalues[ii] = value;
        }

        // finally make sure we were not given any fields that are not primary key fields
        checkArgument(map.isEmpty(), "Non-key columns given: " +
                      Joiner.on(", ").join(map.keySet()));

        return cvalues;
    }

    /** The persistent record type for which we are a key. */
    protected final Class<T> _pClass;

    /** The expression that identifies our row. */
    protected final Comparable<?>[] _values;
}
