_Overview of Schema and Data Migration._

## Schema Migration

Depot makes simple schema migrations extremely simple and complex schema migrations pretty easy.

  * Automatic schema migration: adding a new column to a persistent record is as simple as adding
    the new field to the POJO and incrementing the `SCHEMA_VERSION_NUMBER` constant.
  * Assisted schema migration: dropping, renaming and retyping columns is very easy, and more
    sophisticated custom migrations can also be easily incorporated into Depot's schema migration
    system.
  * Data migration: migrations that do not change record schemas but manipulate their data can also
    be registered and Depot will ensure that they run successfully and only once.
  * Distributed migration coordination: Depot is designed so that you can bring up a dozen
    application servers on a dozen machines and during their initialization they will coordinate
    (through the database) which server will handle each migration and the other servers will block
    any database access until those migrations have successfully completed.

The addition of columns is automatic. Dropping, renaming and retyping are very simple:

```java
public DecorRepository (PersistenceContext ctx)
{
    super(ctx);

    ctx.registerMigration(DecorRecord.class,
                          new SchemaMigration.Rename(17004, "scale", DecorRecord.ACTOR_SCALE));
    ctx.registerMigration(DecorRecord.class, new SchemaMigration.Drop(17004, "offsetX"));
    ctx.registerMigration(DecorRecord.class, new SchemaMigration.Drop(17004, "offsetY"));
    ctx.registerMigration(DecorRecord.class,
                          new SchemaMigration.Retype(17004, DecorRecord.FURNI_SCALE));
}
```

More complex migrations are also possible, one has to take care if they wish to preserve database
agnosticism:

```java
ctx.registerMigration(FooRecord.class, new SchemaMigration(42) {
    @Override
    public Integer invoke (Connection conn, DatabaseLiaiason liaison) throws SQLException {
        // go crazy with your raw JDBC connection or use the DatabaseLiaison to
        // help you do things in a database agnostic way
    }
});
```

## Data Migration

TBD
