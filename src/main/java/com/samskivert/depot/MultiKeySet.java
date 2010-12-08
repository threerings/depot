/**
 *
 */
package com.samskivert.depot;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

/**
 * This class handles the construction of a Where clause from a set of multi-column keys.
 * The naive implementation would construct logical structures like
 *   (A=1 and B="foo" and C=5.95) or (A=1 and B="foo" and C=7.98) or (A=1 and B="foo" and C=11.3)
 * for a large number of the keysets we see in practice. Sending such structures to the database
 * is needlessly verbose and it's not known to which degree the database is able to optimize index
 * access from them.
 *
 * Thus we do our own optimization here; the example above would be turned into
 *   (A=1 and B="foo" and C in (5.95, 7.98, 11.3))
 *
 */
class MultiKeySet<T extends PersistentRecord> extends KeySet<T>
{
    public MultiKeySet (Class<T> pClass, ColumnExp<?>[] keyFields, Comparable<?>[][] keys)
    {
        super(pClass);
        _keys = keys;
        _keyFields = keyFields;
    }

    @Override public SQLExpression getWhereExpression ()
    {
        Set<Integer> columns = Sets.newHashSet();
        for (int ii = 0; ii < _keyFields.length; ii ++) {
            columns.add(ii);
        }
        return rowsToSQLExpression(Lists.newLinkedList(Arrays.asList(_keys)), columns);
    }

    // from Iterable<Key<T>>
    public Iterator<Key<T>> iterator () {
        return Iterators.transform(
            Iterators.forArray(_keys), new Function<Comparable<?>[], Key<T>>() {
                public Key<T> apply (Comparable<?>[] key) {
                    return new Key<T>(_pClass, key);
                }
        });
    }

    @Override public int size () {
        return _keys.length;
    }

    @Override public boolean equals (Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MultiKeySet<?>)) {
            return false;
        }
        MultiKeySet<?> oset = (MultiKeySet<?>)obj;
        return _pClass.equals(oset._pClass) && Arrays.equals(_keys, oset._keys);
    }

    @Override public int hashCode () {
        return 31 * _pClass.hashCode() + Arrays.hashCode(_keys);
    }

    @Override public String toString () {
        return DepotUtil.justClassName(_pClass) + StringUtil.toString(_keys);
    }

    // note: this method will destructively modify its arguments
    protected SQLExpression rowsToSQLExpression (
        List<Comparable<?>[]> keys, Set<Integer> columnsLeft)
    {
        List<SQLExpression> matches = Lists.newArrayList();

        while (!keys.isEmpty()) {
            // go through each column that is still in play, finding the single largest common
            // chunk of any single value in each column
            int maxSize = 0;
            int maxColumn = -1;
            Comparable<?> maxValue = null;

            for (int column : columnsLeft) {
                Tuple<Comparable<?>, Integer> colChunk = findBiggestChunk(keys, column);
                if (colChunk.right > maxSize) {
                    maxColumn = column;
                    maxSize = colChunk.right;
                    maxValue = colChunk.left;
                }
            }

            if (maxSize > 3) {
                // if there's a reasonable chunk, extract it & modify 'keys' in the process
                matches.add(extractChunk(keys, columnsLeft, maxColumn, maxValue));

            } else {
                // but if there are no large chunks (left), revert to the traditional
                //   (A=1 and B=2) or (A=1 and B=3) or ...
                // algorithm for the remaining rows.
                matches.addAll(gatherDetritus(keys, columnsLeft));
            }
        }
        return Ops.or(matches);
    }

    // iterate key rows and find the most common value across those rows, in the given column
    protected Tuple<Comparable<?>, Integer> findBiggestChunk (List<Comparable<?>[]> rows, int col)
    {
        int maxCount = 0;
        Comparable<?> maxValue = null;

        // was Ray writing a CountingMap?
        Map<Comparable<?>, Integer> countMap = Maps.newHashMap();
        for (Comparable<?>[] row : rows) {
            Comparable<?> element = row[col];

            Integer count = countMap.get(element);
            if (count == null) {
                countMap.put(element, count = 1);
            } else {
                countMap.put(element, ++count);
            }
            if (count > maxCount) {
                maxCount = count;
                maxValue = element;
            }
        }
        return new Tuple<Comparable<?>, Integer>(maxValue, maxCount);
    }

    // find all the rows that contain the given chunk value in the given column. delete these
    // (destructively modifying the input argument) and replace them with an optimized
    // SQLExpression, which is returned
    protected SQLExpression extractChunk (List<Comparable<?>[]> rows, Set<Integer> columnsLeft,
        int column, Comparable<?> value)
    {
        Iterator<Comparable<?>[]> iterator = rows.iterator();

        LinkedList<Comparable<?>[]> newRows = Lists.newLinkedList();
        while (iterator.hasNext()) {
            Comparable<?>[] row = iterator.next();
            if (row[column].equals(value)) {
                newRows.add(row);
                iterator.remove();
            }
        }

        Set<Integer> otherColumns = Sets.newHashSet(columnsLeft);
        otherColumns.remove(column);

        SQLExpression otherCondition;
        if (otherColumns.size() == 1) {
            // if there's just two columns, we're doing (A = ? and B in (?, ?, ?, ...))
            int otherColumn = otherColumns.iterator().next();

            List<Comparable<?>> otherValues = Lists.newArrayList();
            for (Comparable<?>[] row : newRows) {
                otherValues.add(row[otherColumn]);
            }
            otherCondition = _keyFields[otherColumn].in(otherValues);

        } else {
            // otherwise we'll be recursing into i.e.
            //   (A = ? and ((B = ? and C = ?) or (B = ? and C = ?) or ...))
            otherCondition = rowsToSQLExpression(newRows, otherColumns);
        }

        return Ops.and(_keyFields[column].eq(value), otherCondition);

    }

    // given unoptimizable key rows, gather them up into simple SQLExpressions according to
    // the naive algorithm
    protected List<SQLExpression> gatherDetritus (
        List<Comparable<?>[]> keys, Set<Integer> columnsLeft)
    {
        List<SQLExpression> conditions = Lists.newArrayList();

        Iterator<Comparable<?>[]> iterator = keys.iterator();
        while (iterator.hasNext()) {
            Comparable<?>[] row = iterator.next();
            List<SQLExpression> bits = Lists.newArrayList();
            for (int column : columnsLeft) {
                bits.add(_keyFields[column].eq(row[column]));
            }
            conditions.add(Ops.and(bits));
            iterator.remove();
        }
        return conditions;
    }

    protected Comparable<?>[][] _keys;
    protected ColumnExp<?>[] _keyFields;
}
