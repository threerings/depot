//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

/**
 * An augmented cache invalidator interface for invalidators that can ensure that they are
 * operating on the proper persistent record class.
 */
public interface ValidatingCacheInvalidator extends CacheInvalidator
{
    /**
     * Validates that this invalidator operates on the supplied persistent record class. This helps
     * to catch programmer errors where one record type is used for a query clause and another is
     * used for the cache invalidator.
     *
     * @exception IllegalArgumentException thrown if the supplied persistent record class does not
     * match the class that this invalidator will flush from the cache.
     */
    public void validateFlushType (Class<?> pClass);
}
