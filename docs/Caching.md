_Overview of Caching._

Records are cached by primary key. Lookups by primary key first check the cache and on a miss, load
the record from the database and insert it into the cache.

Queries for records that have primary keys are automatically split into phases:

  * A query is made to the database for the primary keys of all rows that match the query.
  * Any records in the cache are obtained from the cache.
  * All remaining records are loaded by primary key in a single additional query and placed into
    the cache.

For deletions using a `Where` clause, first the primary keys that match the deletion clause are
loaded, then those records are deleted from the database and the cache using their primary key.

Decomposition for updates using a `Where` clause is not yet implemented, but a fallback mechanism
to invalidate the cache manually is provided for those cases.

If one already has the primary keys for the records they desire, it is possible to avoid the first
phase of the decomposed query using `loadAll()` instead of `findAll()`:

```java
@Entity
public class MemberNameRecord extends PersistentRecord
{
    /** This member's unique id. */
    @Id public int memberId;

    /** The name by which this member is known. */
    public String name;
}

/**
 * Looks up members' names by id.
 */
public Map<Integer, MemberName> loadMemberNames (final Set<Integer> memberIds)
{
    final Map<Integer, MemberName> names = Maps.newHashMap();
    for (MemberNameRecord name : loadAll(MemberNameRecord.class, memberIds)) {
        names.put(name.memberId, name.name);
    }
    return names;
}
```

This will efficiently fetch all records that it can from the cache and then load and cache any
remaining records.
