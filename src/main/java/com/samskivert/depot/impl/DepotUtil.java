//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Simple utility methods used by various things.
 */
public class DepotUtil
{
    /**
     * Returns an array containing the names of the primary key fields for the supplied persistent
     * class. The values are introspected and cached for the lifetime of the VM.
     */
    public static ColumnExp<?>[] getKeyFields (Class<? extends PersistentRecord> pClass)
    {
        return _keyFields.get(pClass);
    }

    /**
     * Register the key fields to their PersistentRecord class. It should never be necessary
     * to do this manually, as it is done via a static initializer in generated PersistentRecord
     * subclasses. Calling this method after the key fields have already been registered will
     * have no effect.
     */
    public static void registerKeyFields (ColumnExp<?>... fields)
    {
        // TODO: Checks? For example: Validate all exps from same class?
        // Make a defensive copy of the array? Hide this method from public consumption?
        _keyFields.putIfAbsent(fields[0].getPersistentClass(), fields);
    }

    /**
     * Returns the name of the supplied class minus its package.
     */
    public static String justClassName (Class<?> clazz)
    {
        return clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
    }

    /** A (never expiring) cache of primary key field names for all persistent classes (of which
     * there are merely dozens, so we don't need to worry about expiring). */
    protected static ConcurrentMap<Class<? extends PersistentRecord>, ColumnExp<?>[]> _keyFields =
        new MapMaker()
        // newly generated PersistentRecord classes will register their key fields via
        // registerKeyFields, which will return an ordering determined at genrecord time.
        // We fall back to computing the fields at runtime for older PersistentRecord classes.
        .makeComputingMap(new Function<Class<? extends PersistentRecord>, ColumnExp<?>[]>() {
            public ColumnExp<?>[] apply (Class<? extends PersistentRecord> pClass) {
                List<ColumnExp<?>> kflist = Lists.newArrayList();
                for (Field field : pClass.getFields()) {
                    // look for @Id fields
                    if (field.getAnnotation(Id.class) != null) {
                        kflist.add(new ColumnExp<Object>(pClass, field.getName()));
                    }
                }
                return kflist.toArray(new ColumnExp<?>[kflist.size()]);
            }
        });
}
