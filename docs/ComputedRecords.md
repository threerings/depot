_Overview of Computed Records._

Note: computed records are largely deprecated in favor of ad-hoc queries or the use of
`selectInto`. See [the example queries page](ExampleQueries) for examples of such use.

## Computed Records

You can easily define record with computed fields or records that represent a join across multiple
tables (see below). It is very easy to select a subset of a record's fields:

```java
@Entity @Computed(shadowOf=PersonRecord.class)
public PersonNameRecord extends PersistentRecord
{
    public int personId;
    public String name;
}

List<PersonNameRecord> allNames = findAll(PersonNameRecord.class);
List<PersonNameRecord> youngNames = findAll(
    PersonNameRecord.class, new Where(PersistentRecord.AGE.lessEq(18)));
```

You can also define computed records that calculate information:

```java
@Entity @Computed
public CountRecord extends PersistentRecord
{
    @Computed(fieldDefinition="count(*)")
    public int count;
}

int personCount = load(CountRecord.class, new FromOverride(PersonRecord.class)).count;

// or if you want to be less general purpose

@Entity @Computed(shadowOf=PersonRecord.class)
public PersonCountRecord extends PersistentRecord
{
    @Computed(fieldDefinition="count(*)")
    public int count;
}

int personCount = load(PersonCountRecord.class).count;

// or something more specific

@Entity @Computed(shadowOf=PersonRecord.class)
public PersonAvgAgeRecord extends PersistentRecord
{
    @Computed(fieldDefinition="avg(*)")
    public float averageAge;
}

float averageAge = load(PersonAvgAgeRecord.class).averageAge;
```

The reader may be alarmed to notice some hard-coded SQL in those classes. One is advised to stick
to very standard SQL in cases like this to avoid introducing portability problems, but we didn't
think it was worth the trouble to try to model these simple and mostly standard operations in a
more complex way. YMMV.

## Joins with Computed Records

TBD
