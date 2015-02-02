//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.google.common.base.Function;

import com.samskivert.util.Logger;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.FragmentVisitor;
import com.samskivert.depot.impl.expression.LiteralExp;
import com.samskivert.depot.impl.operator.In;

/**
 * Contains a set of primary keys that match a set of persistent records. This is used internally
 * in Depot when decomposing queries into two parts: first a query for the primary keys that
 * identify the records that match a free-form query and then another query that operates on the
 * previously identified keys. The keys obtained in the first query are used to create a KeySet and
 * modifications and deletions using this set will automatically flush the appropriate records from
 * the cache.
 */
public abstract class KeySet<T extends PersistentRecord> extends WhereClause
    implements Serializable, ValidatingCacheInvalidator, Iterable<Key<T>>
{
    /**
     * Creates a key set for the supplied persistent record and keys.
     */
    public static <T extends PersistentRecord> KeySet<T> newKeySet (
        Class<T> pClass, Collection<Key<T>> keys)
    {
        if (keys.size() == 0) {
            return new EmptyKeySet<T>(pClass);
        }

        ColumnExp<?>[] keyFields = DepotUtil.getKeyFields(pClass);
        if (keyFields.length == 1) {
            Comparable<?>[] keyArray = new Comparable<?>[keys.size()];
            int ii = 0;
            for (Key<T> key : keys) {
                keyArray[ii++] = key.getValues()[0];
            }
            return new SingleKeySet<T>(pClass, keyArray);

        } else {
            // TODO: is there a maximum size of an or query? 32768?
            Comparable<?>[][] keysValues = new Comparable<?>[keys.size()][];
            int ii = 0;
            for (Key<T> key : keys) {
                keysValues[ii++] = key.getValues();
            }
            return new MultiKeySet<T>(pClass, keyFields, keysValues);
        }
    }

    /**
     * Creates a key set for the supplied persistent record and collection of simple keys.
     *
     * @exception IllegalArgumentException thrown if the supplied record does not use a simple
     * (single-column) primay key.
     */
    public static <T extends PersistentRecord> KeySet<T> newSimpleKeySet (
        Class<T> pClass, Collection<? extends Comparable<?>> keys)
    {
        ColumnExp<?>[] keyFields = DepotUtil.getKeyFields(pClass);
        if (keyFields.length != 1) {
            throw new IllegalArgumentException(
                "Cannot create KeySet using simple keys for record with non-simple primary key " +
                "[record=" + pClass + "]");
        }
        if (keys.size() == 0) {
            return new EmptyKeySet<T>(pClass);
        } else {
            Comparable<?>[] keyArray = new Comparable<?>[keys.size()];
            return new SingleKeySet<T>(pClass, keys.toArray(keyArray));
        }
    }

    protected static class EmptyKeySet<T extends PersistentRecord> extends KeySet<T>
    {
        public EmptyKeySet (Class<T> pClass) {
            super(pClass);
        }

        @Override public SQLExpression<?> getWhereExpression () {
            return new LiteralExp<Boolean>("false");
        }

        // from Iterable<Key<T>>
        public Iterator<Key<T>> iterator () {
            return Collections.<Key<T>>emptyList().iterator();
        }

        @Override public int size () {
            return 0;
        }

        @Override public boolean equals (Object obj) {
            if (this == obj) {
                return true;
            }
            return (obj instanceof EmptyKeySet<?>) &&
                _pClass.equals(((EmptyKeySet<?>)obj)._pClass);
        }

        @Override public int hashCode () {
            return _pClass.hashCode();
        }

        @Override public String toString () {
            return DepotUtil.justClassName(_pClass) + "(empty)";
        }
    }

    protected static class SingleKeySet<T extends PersistentRecord> extends KeySet<T>
    {
        public SingleKeySet (Class<T> pClass, Comparable<?>[] keys) {
            super(pClass);
            _keys = keys;
        }

        @Override public SQLExpression<?> getWhereExpression () {
            // Single-column keys result in the compact IN(keyVal1, keyVal2, ...)
            return new In(DepotUtil.getKeyFields(_pClass)[0], _keys);
        }

        // from Iterable<Key<T>>
        public Iterator<Key<T>> iterator () {
            return Iterators.transform(
                Iterators.forArray(_keys), new Function<Comparable<?>, Key<T>>() {
                    public Key<T> apply (Comparable<?> key) {
                        return new Key<T>(_pClass, new Comparable<?>[] { key });
                    }
            });
        }

        @Override public int size () {
            return _keys.length;
        }

        @Override public boolean equals (Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SingleKeySet<?>)) {
                return false;
            }
            SingleKeySet<?> oset = (SingleKeySet<?>)obj;
            return _pClass.equals(oset._pClass) && Arrays.equals(_keys, oset._keys);
        }

        @Override public int hashCode () {
            return 31 * _pClass.hashCode() + Arrays.hashCode(_keys);
        }

        @Override public String toString () {
            return DepotUtil.justClassName(_pClass) + StringUtil.toString(_keys);
        }

        protected Comparable<?>[] _keys;
    }

    /**
     * Returns an unmodifiable {@link Collection} view on this KeySet.
     */
    public Collection<Key<T>> toCollection ()
    {
        return new AbstractCollection<Key<T>>() {
            @Override public Iterator<Key<T>> iterator () {
                return KeySet.this.iterator();
            }
            @Override public int size () {
                return KeySet.this.size();
            }
        };
    }

    /**
     * Returns the number of keys in this set.
     */
    public abstract int size ();

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
    public void invalidate (PersistenceContext ctx) {
        for (Key<T> key : this) {
            ctx.cacheInvalidate(key);
        }
    }

    // from ValidatingCacheInvalidator
    public void validateFlushType (Class<?> pClass)
    {
        if (!pClass.equals(_pClass)) {
            throw new IllegalArgumentException(Logger.format(
                "Class mismatch between persistent record and cache invalidator",
                "record", pClass.getSimpleName(), "invtype", _pClass.getSimpleName()));
        }
    }

    @Override // from WhereClause
    public void validateQueryType (Class<?> pClass)
    {
        super.validateQueryType(pClass);
        validateTypesMatch(pClass, _pClass);
    }

    protected KeySet (Class<T> pClass)
    {
        _pClass = pClass;
    }

    protected Class<T> _pClass;
}
