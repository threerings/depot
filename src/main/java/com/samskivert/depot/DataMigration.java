//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

/**
 * Encapsulates a migration of data between entities that should be run only once and using the
 * same safeguards applied to entity migrations. Note: this should not be used for schema
 * migrations, use {@link SchemaMigration} for that. Data migrations are registered on a specific
 * repository via {@link DepotRepository#registerMigration} and should be registered in the
 * repository's constructor as they will be invoked (if appropriate) in the repository's {@link
 * DepotRepository#init} method.
 *
 * <p> In general one will register an anonymous inner class in a repository's constructor and can
 * then access methods in the repository directly:
 *
 * <pre>
 * public class FooRepository extends DepotRepository {
 *     public FooRepository (PersistenceContext ctx) {
 *         super(ctx);
 *         registerMigration(new DataMigration("2008_09_25_referral_to_tracking_id") {
 *             public void invoke () throws DatabaseException {
 *                 // feel free to use load() findAll(), update(), etc.
 *             }
 *         });
 *     }
 * </pre>
 */
public abstract class DataMigration
{
    /**
     * Creates a data migration with the specified unique identifier. The identifier must be unique
     * across all users of the database and for all time, so be careful. Best to include the date
     * and pertintent information, e.g. "2008_09_25_referral_to_tracking_id".
     */
    public DataMigration (String ident)
    {
        _ident = ident;
    }

    /**
     * Returns the identifier of this migration.
     */
    public String getIdent ()
    {
        return _ident;
    }

    /**
     * Effects the data migration.
     */
    public abstract void invoke () throws DatabaseException;

    /** The unique identifier for this migration. */
    protected String _ident;
}
