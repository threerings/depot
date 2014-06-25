# Depot Persistence Library

Depot is a relational persistence library for Java. It is an ORM library, but has aims that are
somewhat different from the popular "managed" persistence libraries like Hibernate and others.

## Design Goals

  * Eliminate (as much as possible) the use of raw SQL, instead providing Java classes that allow
    the expressions of queries and updates in as concise but compile-time checkable a manner as
    possible.
  * Reduce the pain of schema and data migrations as much as possible, but not so much that the
    system used to do the migrations is too complex for anyone to understand or use properly.
  * Use annotations to layer database metadata over the top of (almost) POJOs.
  * Use annotations that are syntactically and semantically equivalent to EJB3 persistence
    annotations wherever possible.
  * Support multiple database backends (currently MySQL, Postgresql and HSQLDB).
  * Provide caching support (currently integrated with EHCache).
  * Keep an eye toward eventual support for sharded databases (not yet implemented).

Depot studiously avoids ever doing anything magical. You only access the database when you make a
method call requesting that records be read from the database or records be updated in the database.
Depot attempts only to be a concise, compile-time checkable veneer over raw database access that
conveniently models database tables as Java objects.

In this way Depot is more of an evolution of DAO-like libraries of the past than a pared down
sibling of the managed persistence libraries of the present. It distinguishes itself by taking
advantage of annotations to concisely express database metadata and by striving to stay out of your
way as much and surprise you as little as possible.

## Code Examples

Here's are some example to give you a taste of what code using Depot looks like.

  * A [simple code example](docs/SimpleCodeExample)
  * Various [example queries](docs/ExampleQueries)

[API docs](http://threerings.github.io/depot/apidocs/) are also available.

## Features

Depot supports a number of very useful features. Here are a few of the main features for which time
has permitted documentation:

  * [Schema and Data migration](docs/SchemaMigration)
  * [Caching](docs/Caching)

## Getting Started

If you want to use Depot on your project, check the following page for information on getting Depot
and dependencies via Maven or manually, as well as what sort of configuration Depot requires to
start talking to your database.

  * [Configuring and integrating](docs/Configuration) Depot with your project

## Discussion

Feel free to pop over to the [OOO Libs](http://groups.google.com/group/ooo-libs) Google Group to
ask questions and get (and give) answers.

## History

Depot was started in September of 2006 by [Three Rings](http://www.threerings.net/) as a part of
their [Whirled](http://www.whirled.com/) project. Its primary authors are Michael Bayne and Pär
Winzell.

## License and Distribution

Depot is released under the New BSD License. The most recent version of the library is available at
https://github.com/threerings/depot/
