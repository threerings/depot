//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Handy record for computing the count of something. In general, you need not use this directly,
 * but should instead use {@link Query#selectCount}. For example: {@code
 * from(ForumThreadRecord.class).where(ForumThreadRecord.GROUP_ID.eq(groupId)).selectCount()}
 */
@Computed @Entity
public class CountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CountRecord> _R = CountRecord.class;
    public static final ColumnExp<Integer> COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The computed count. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
