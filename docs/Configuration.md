_Configuration and Dependencies._

## JVM Version Requirement

Depot currently requires JDK 1.6 or greater.

### Integrate with Ivy or Maven

Depot is published to the Maven Central repository and can be added as a dependency using the
following configuration: `com.samskivert:depot:1.6.4`

This will automatically include the Google Guava and samskivert dependencies. You can add Ehcache
as well via `net.sf.ehcache:ehcache:1.6.0` (or a newer version, if available).

## Manually Adding Dependencies

Depot depends on a small number of external libraries:

  * Google Guava - http://code.google.com/p/guava-libraries/
  * samskivert - http://code.google.com/p/samskivert/
  * Ehcache (optional) - http://ehcache.sourceforge.net/

Depot also requires a JDBC driver for the database with which you plan to operate. Depot currently
supports three database backends:

  * Postgresql - http://jdbc.postgresql.org/
  * MySQL - http://www.mysql.com/products/connector/j/
  * HSQLDB - http://hsqldb.org/ (useful for unit testing)

## Configuration

The two main components that require configuration are the JDBC connection provider and the cache
implementation.

### StaticConnectionProvider

For testing and other simple systems that don't require connection pooling, the
`StaticConnectionProvider` is a simple way to provide JDBC connections to Depot. It is used as
follows (this example uses MySQL):

```java
Properties props = new Properties();
// you'd probably load these properties from a file, but for the purposes
// of this example, we'll set them directly in the code
props.setProperty("default.driver", "com.mysql.jdbc.Driver");
props.setProperty("default.url", "jdbc:mysql://localhost:3306/dbname");
props.setProperty("default.username", "username");
props.setProperty("default.password", "password");

PersistenceContext perCtx = new PersistenceContext(
    "default", new StaticConnectionProvider(props), null);
```

### DataSourceConnectionProvider

Production systems are more likely to use a JDBC `DataSource` to obtain their connections as those
provide connection pooling and integrate with JNDI and such. The only non-obvious aspect of
configuring Depot with a `DataSource` is that you can provide two datasources: one for read-only
connections and one for read-write connections. Depot will obtain connections from the appropriate
source depending on whether or not it is doing a query that is safe to be performed against a
read-only mirror of your data or if it's doing a query that must talk to a database master.

What follows is a simple example of manually creating and configuring a Postgresql pooling
`DataSource`:

```java
PoolingDataSource readSource = new PoolingDataSource();
readSource.setDataSourceName("MyReadSource");
readSource.setServerName("myReadOnlyServerHost");
readSource.setDatabaseName("myDatabaseName");
readSource.setPortNumber(5432);
readSource.setUser("myUsername");
readSource.setPassword("myPassword");
readSource.setMaxConnections(4); // tune to your applications needs

PoolingDataSource writeSource = new PoolingDataSource();
writeSource.setDataSourceName("MyWriteSource");
writeSource.setServerName("myReadWriteServerHost");
writeSource.setDatabaseName("myDatabaseName");
writeSource.setPortNumber(5432);
writeSource.setUser("myUsername");
writeSource.setPassword("myPassword");
writeSource.setMaxConnections(1); // tune to your applications needs

PersistenceContext perCtx = new PersistenceContext(
    "notused", new DataSourceConnectionProvider("jdbc:postgresql", readSource, writeSource), null);
```

See the note below on lifecycle management.

### EHCacheAdapter

You may have noticed the second argument to the `PersistenceContext` constructor in the above
examples was always null. That is where the `CacheAdapter` is provided. By passing null, Depot will
not use caching. Depot comes with integration for Ehcache and implementing additional cache
integrations is as simple as implementing the `CacheAdapter` interface and supplying an instance to
the `PersistenceContext` constructor.

The following example assumes that you have an `ehcache.xml` configuration file in your classpath.
There are other ways to configure Ehcache but we'll leave that explanation to their documentation.

```java
CacheManager cacheMgr = CacheManager.getInstance();
ConnectionProvider conProv = // ...
PersistenceContext perCtx = new PersistenceContext("ident", conProv, new EHCacheAdapter(cacheMgr));
```

See the note below on lifecycle management.

### PersistenceContext Lifecycle

When your application is shutting down it should shutdown its `PersistenceContext`. However, to
avoid integration headaches, Depot does not take responsibility for shutting down certain of its
dependencies as those may be used by other parts of your application and you may wish to shut Depot
down independently of these other components.

#### ConnectionProvider

Depot will shutdown its connection provider when the `PersistenceContext` is shutdown, however the
two `ConnectionProvider` implementations have different shutdown behavior as explained below.

  * `StaticConnectionProvider` will close all JDBC `Connection` instances it has created when it is
    shutdown. If you are using Depot with `StaticConnectionProvider` you can simply shutdown your
    `PersistenceContext` and you're done.
  * `DataSourceConnectionProvider` will not shutdown its underlying `DataSource` implementations
    (indeed there is no API for doing so). As long as no queries are executing at the time that
    `PersistenceContext` is shutdown, then all JDBC `Connection` instances will have been closed
    and returned to the `DataSource` connection pool, so the application can shutdown its data
    sources in whatever way is appropriate.

#### CacheAdapter

Depot will shutdown its `CacheAdapter` when the `PersistenceContext` is shutdown, however the
`CacheAdapter` implementation is free to do nothing in its `shutdown` call.

  * `EHCacheAdapter` does not shutdown its underlying `CacheManager` when it is shutdown to avoid
    conflict with other aspects of the application that may use Ehcache. Thus the application is
    responsible for shutting down the `CacheManager` itself when it is known to no longer be
    needed.

## Injection

We use Guice around these parts for dependency injection. Using injection allows you to inject the
`PersistenceContext` into your repository implementations:

```java
@Singleton
public class FooRepository extends DepotRepository {
    @Inject public FooRepository (PersistenceContext perCtx) {
        super(perCtx);
    }
}
```

and then inject your repositories wherever you need them.

We also find the following pattern to be very effective:

```java
public class FooModule extends AbstractModule {
    @Override protected void configure () {
        super.configure();
        // depot dependencies (we will initialize this persistence context later when the
        // server is ready to do database operations; not initializing it now ensures that no
        // one sneaks any database manipulations into the dependency resolution phase)
        bind(PersistenceContext.class).toInstance(new PersistenceContext());
    }
}

public class WhateverHandlesAppServerLifecycle {
    public void init () {
        // initialize our persistence context
        ConnectionProvider conProv = // ...
        _perCtx.init("ident", conProv, new EHCacheAdapter(_cacheMgr));

        // initialize our depot repositories; this runs all of our schema and data migrations
        _perCtx.initializeRepositories(true);
    }

    public void shutdown () {
        _perCtx.shutdown();
        _cacheMgr.shutdown();
    }

    @Inject protected PersistenceContext _perCtx;
    protected CacheManager _cacheMgr = CacheManager.getInstance();
}
```

One major benefit to the approach of delaying the initialization of your persistence context until
the dependency resolution phase is complete is to ensure that no code accidentally (or
intentionally) starts talking to the database during that phase. You almost certainly want to
resolve all of your injection dependencies and then before you turn your application server loose,
call `initializeRepositories` to cause all of your schema and data migrations to be run (or to fail
and abort the initialization of your application).

If you don't call `initializeRepositories` then Depot will lazily initialize each
`PersistentRecord` class when it is first accessed and run any schema migrations for that record.
Data migrations will be disabled if you choose this lazily initialized approach.

Another note on `initializeRepositories` is that this will initialize all repositories that have
been constructed with the supplied `PersistenceContext` up to that point. Any repositories
constructed after `initializeRepositories` has been called will be initialized at that time
(running schema and data migrations for their records) and a warning will be generated to alert you
to this undesirable behavior. Again, experience has shown that you generally want to get all of
your schema and data migrations out of the way immediately and before the application server starts
normal operation.
