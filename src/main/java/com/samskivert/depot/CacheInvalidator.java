//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.io.Serializable;

/**
 * Implementors of this interface performs perform cache invalidation for calls to
 * {@link DepotRepository#updatePartial} and {@link DepotRepository#deleteAll}.
 */
public interface CacheInvalidator
{
    public static abstract class TraverseWithFilter<T extends Serializable>
        implements CacheInvalidator
    {
        public TraverseWithFilter (Class<T> pClass) {
            this(pClass.getName());
        }

        public TraverseWithFilter (String cacheId) {
            _cacheId = cacheId;
        }

        public void invalidate (PersistenceContext ctx) {
            ctx.cacheTraverse(_cacheId, new PersistenceContext.CacheEvictionFilter<T>() {
                @Override protected boolean testForEviction (Serializable key, T record) {
                    return TraverseWithFilter.this.testForEviction(key, record);
                }
            });
        }

        protected abstract boolean testForEviction (Serializable key, T record);

        protected String _cacheId;
    }

    /**
     * Must invalidate all cache entries that depend on the records being modified or deleted.
     * This method is called just before the database statement is executed.
     */
    public void invalidate (PersistenceContext ctx);
}
