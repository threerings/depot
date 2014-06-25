_A simple Depot code example._

Here's a simple example to give you a quick overview of what code using Depot looks like.

Start by defining a persistent record, this maps to a database table:

```java
@Entity
public class PersonRecord extends PersistentRecord
{
    /** Increment this value if you change this record's schema. */
    public static final int SCHEMA_VERSION = 1;

    /** A unique identifier for this record. Automatically filled in at row insertion time. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int personId;

    /** This person's name. Note: one difference between EJB3 and Depot is that columns are
     * non-nullable by default. */
    @Column(length=100)
    public String name;

    /** This person's age. */
    public int age;
}
```

Then you run a simple Ant task or Maven plugin that adds some unfortunately non-POJO boilerplate
code to your record class, but this code allows you to talk about your record in queries in a way
that the compiler can check which is a huge win.

If and when Java adds field literals, Depot will absolutely take advantage of them and eliminate
this undesirable boilerplate.

```java
@Entity
public class PersonRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PersonRecord> _R = PersonRecord.class;
    public static final ColumnExp<Integer> PERSON_ID = colexp(_R, "personId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<Integer> AGE = colexp(_R, "age");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you change this record's schema. */
    public static final int SCHEMA_VERSION = 1;

    /** A unique identifier for this record. Automatically filled in at row insertion time. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int personId;

    /** This person's name. Note: one difference between EJB3 and Depot is that columns are
     * non-nullable by default. */
    @Column(length=100)
    public String name;

    /** This person's age. */
    public int age;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PersonRecord}
     * with the supplied key values.
     */
    public static Key<PersonRecord> getKey (int personId)
    {
        return newKey(_R, personId);
    }
    // AUTO-GENERATED: METHODS END
}
```

Next you define a repository class which will provide an application-specific persistence API. We
highly recommend preserving this boundary and having all Depot code inside repository classes and
only pass persistent record classes outside to your application.

```java
public class PersonRepository extends DepotRepository
{
    /**
     * Creates this repository and provides it with a context via which it will obtain JDBC
     * connections.
     */
    public PersonRepository (PersistenceContext ctx)
    {
        super(ctx);
    }
    
    /**
     * Loads and returns the person with the specified id, or null if no person exists with that
     * id.
     */
    public PersonRecord loadPerson (int personId)
    {
        return load(PersonRecord.getKey(personId));
    }

    /**
     * Loads records for all people with an age less than or equal to the specified maximum.
     */
    public List<PersonRecord> loadYoungPeople (int maxAge)
    {
        return from(PersonRecord.class).where(PersonRecord.AGE.lessEq(maxAge)).select();
    }

    /**
     * Loads the names of all people in the repository.
     */
    public Set<String> loadNames ()
    {
        Set<String> names = new HashSet<String>();
        names.addAll(from(PersonRecord.class).select(PersonRecord.NAME));
        return names;
    }

    /**
     * Inserts a newly created person record into the database. If record.personId is non-zero (or
     * non-null in the case of a non-primitive integer field) an exception will be thrown.
     */
    public void insertPerson (PersonRecord record)
    {
        insert(record);
    }

    /**
     * Updates a person record. If record.personId is zero (or null in the case of a non-primitive
     * integer field) an exception will be thrown.
     */
    public void updatePerson (PersonRecord record)
    {
        update(record);
    }

    /**
     * Updates a person record. If record.personId is zero (or null in the case of a non-primitive
     * integer field) a new row will be created for this record, if not the matching row will be
     * updated.
     */
    public void storePerson (PersonRecord record)
    {
        store(record);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(PersonRecord.class);
    }
}
```

See the [example queries](ExampleQueries) page for examples of other kinds of queries.
