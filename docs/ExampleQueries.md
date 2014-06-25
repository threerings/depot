_Describes various example queries._

Depot queries are constructed using a builder-pattern.

## Whole record queries

A basic query to select all rows from a table looks like so:

```java
from(PersonRecord.class).select();
```

Note that the above query and the others in these examples will use the `PersonRecord` from the
SimpleCodeExample page.

Various query clauses may be added to the basic query above to do filtering, ordering and the like.
Here's a query with a simple where clause:

```java
// selects all records with age <= 25
from(PersonRecord.class).where(PersonRecord.AGE.lessEq(25)).select();
```

One can also order and limit the results:

```java
// selects the first ten records in ascending alphabetic order on name
from(PersonRecord.class).ascending(PersonRecord.NAME).limit(10).select();
```

More complex orderings are also possible:

```java
OrderBy order = OrderBy.ascending(PersonRecord.NAME).thenDescending(PersonRecord.AGE);
from(PersonRecord.class).orderBy(order).select();
```

## Ad-hoc queries

Instead of selecting whole rows from the database, one can select individual columns, or the
results of aggregate and other functions. Here's a simple projection:

```java
List<Tuple2<Integer,String>> results =
    from(PersonRecord.class).select(PersonRecord.ID, PersonRecord.NAME);
```

Depot annotates the `ColumnExp` constants generated in your `PersistentRecord` classes with their
type so that queries like the above can be done in a type-safe manner. `Tuple` classes are provided
up to `Tuple5` for such ad-hoc queries.

As an alternative to a `Tuple` class, you can use a type-safe builder to receive the results of
your query like so:

```java
public class IdName {
    public static Builder2<IdName, Integer, String> IDNAME_BUILDER =
        new Builder2<IdName, Integer, String>() {
            public IdName build (Integer a, String b) {
                return new IdName(a, b);
            }
        };

    public int id;
    public String name;
    public IdName (int id, String name) {
        this.id = id;
        this.name = name;
    }
}

List<IdName> results =
    from(PersonRecord.class).select(IDNAME_BUILDER, PersonRecord.ID, PersonRecord.NAME);
```

Such queries will result in compile time error if the types of the columns do not match the types
expected by the builder. The `BuilderN` interfaces are also only available up to arity-5.

For situations where type-safety is not a major concern, and for cases where you wish to select
more than five columns, you can use `selectInto` which uses reflection to construct results:

```java
public class NameCount {
    public String name;
    public int count;
    public NameCount (String name, int count) {
        this.name = name;
        this.count = count;
    }
}

List<NameCount> results =
    from(PersonRecord.class).groupBy(PersonRecord.NAME).selectInto(
        NameCount.class, PersonRecord.NAME, Funcs.countStar());
```

Note that the class supplied to the `selectInto` method must have exactly one public constructor,
and the arguments to that constructor must match __in order__, the columns specified in the
`selectInto` call. The types of the selected columns (or expressions) must be convertible to the
type needed by the constructor (which means they will be widened or unboxed, but not converted from
`float` to `int` or other non-automatic conversions). These requirements are unfortunately not
checkable at compile time, and instead result in a runtime error when violated. Fortunately,
testing tends to catch any such errors before they make it into the wild.

## Count queries

The `selectCount` method exists for when you wish to simply select the count of rows that match
your query. For example:

```java
int youngins = from(PersonRecord.class).where(PersonRecord.AGE.lessEq(12)).selectCount();
```

You may also wish to group by certain columns and select the counts of rows that match each group.
This is done with an ad-hoc query:

```java
List<Tuple2<String,Integer>> results =
    from(PersonRecord.class).groupBy(PersonRecord.NAME).
        select(PersonRecord.NAME, Funcs.countStar());
```

## Other functions

A variety of other functions are defined in
[Funcs](http://depot.googlecode.com/svn/apidocs/com/samskivert/depot/Funcs.html),
[StringFuncs](http://depot.googlecode.com/svn/apidocs/com/samskivert/depot/StringFuncs.html),
[DateFuncs](http://depot.googlecode.com/svn/apidocs/com/samskivert/depot/DateFuncs.html), and
[MathFuncs](http://depot.googlecode.com/svn/apidocs/com/samskivert/depot/MathFuncs.html). These can
be used in queries, like so:

```java
List<PersonRecord> eldest =
   from(PersonRecord.class).where(PersonRecord.AGE.eq(Funcs.max(PersonRecord.AGE))).select();
```

And you can select the value of a function in an ad-hoc query:

```java
// note that load() can be used for selections that will only ever return one row
Number maxAge = from(PersonRecord.class).load(Funcs.max(PersonRecord.AGE));

// alternatively
List<Number> maxAge = from(PersonRecord.class).load(Funcs.max(PersonRecord.AGE));
assert(maxAge.size() == 1);

// here's a more complex (if somewhat nonsensical) query that groups people by the first
// letter of their name and selects the sum of all ages of the people in those groups
SQLExpression<String> firstLetter = StringFuncs.substring(PersonRecord.NAME, 0, 1);
List<String,Integer> results = from(PersonRecord.class).groupBy(firstLetter).
    select(firstLetter, Funcs.sum(PersonRecord.AGE));
```
