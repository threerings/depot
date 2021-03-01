//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Transform;
import com.samskivert.depot.util.ByteEnum;

import static com.samskivert.depot.Log.log;

/**
 * Contains various generally useful {@link Transformer} implementations. To use a transformer, you
 * specify it via a {@link Column} annotation. For example:
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     &#064;Transform(Transformers.StringArray.class)
 *     public String[] cities;
 * }
 * </pre>
 */
public class Transformers
{
    /**
     * Transform a Set of ByteEnums into an Integer.
     * Does not presently support the immutable or interning hints.
     */
    public static class ByteEnumSet<E extends Enum<E> & ByteEnum>
        extends Transformer<Set<E>, Integer>
    {
        @Override @SuppressWarnings("unchecked")
        public void init (Type fieldType, Transform annotation)
        {
            _eclass = (Class<E>) ((ParameterizedType) fieldType).getActualTypeArguments()[0];
        }

        @Override
        public Integer toPersistent (Set<E> value)
        {
            return (value == null) ? null : ByteEnum.Util.setToInt(value);
        }

        @Override
        public Set<E> fromPersistent (Integer encoded)
        {
            return (encoded == null) ? null : ByteEnum.Util.intToSet(_eclass, encoded);
        }

        /** The enum class token. */
        protected Class<E> _eclass;
    }

    /**
     * Combines the contents of a String[] column into a single String, terminating each String
     * element with a newline. A backslash ('\') in Strings will be prefixed by another backslash,
     * newlines will be encoded as "\n", and null elements will be encoded as "\0" (but not
     * terminated by a newline).
     */
    public static class StringArray extends StringBase<String[]>
    {
        @Override protected Iterable<String> asIterable (String[] value)
        {
            return Arrays.asList(value);
        }

        @Override protected Builder<String, String[]> createBuilder (String encoded)
        {
            // jog through and count the elements so that we can populate the array directly
            final String[] result = new String[countElements(encoded)];
            return new Builder<String, String[]>() {
                public void add (String s) {
                    result[idx++] = s;
                }
                public String[] build () {
                    return result;
                }
                protected int idx = 0;
            };
        }
    }

    /**
     * Combines the contents of a {@code List/Set/Collection/Iterable<String>} column into a single
     * String, terminating each String element with a newline. Null-tolerant. A backslash ('\') in
     * Strings will be prefixed by another backslash, newlines will be encoded as "\n", and null
     * elements will be encoded as "\0" (but not terminated by a newline).
     */
    public static class StringIterable extends StringBase<Iterable<String>>
    {
        @Override protected Iterable<String> asIterable (Iterable<String> value)
        {
            return value;
        }

        @Override protected Builder<String, Iterable<String>> createBuilder (String encoded)
        {
            return createCollectionBuilder(_ftype, String.class, _immutable, _intern, -1);
        }
    }

    /**
     * A Transformer for anything that is an {@code Iterable<Enum>}. Often used for Enum sets.
     */
    public static class EnumIterable<E extends Enum<E>> extends StringBase<Iterable<E>>
    {
        @Override @SuppressWarnings("unchecked")
        public void init (Type fieldType, Transform annotation)
        {
            super.init(fieldType, annotation);
            _eclass = (Class<E>) ((ParameterizedType) fieldType).getActualTypeArguments()[0];
            _internStrings = false; // don't waste time interning strings
        }

        @Override protected Iterable<String> asIterable (Iterable<E> value)
        {
            return Iterables.transform(value, new Function<E, String>() {
                public String apply (E val) {
                    return (val == null) ? null : val.name();
                }
            });
        }

        @Override protected Builder<String, Iterable<E>> createBuilder (String encoded)
        {
            // create a Builder for our field type
            final Builder<E, Iterable<E>> ebuilder = createCollectionBuilder(
                _ftype, hasNullElement(encoded) ? null : _eclass, _immutable, _intern, -1);
            // wrap that builder in one that accepts String elements
            return new Builder<String, Iterable<E>>() {
                public void add (String s) {
                    E value;
                    if (s == null) {
                        value = null;

                    } else {
                        try {
                            value = Enum.valueOf(_eclass, s);
                        } catch (IllegalArgumentException iae) {
                            log.warning("Invalid enum cannot be unpersisted", "e", s, iae);
                            return;
                        }
                    }
                    ebuilder.add(value);
                }
                public Iterable<E> build ()  {
                    return ebuilder.build();
                }
            };
        }

        /** The enum class. */
        protected Class<E> _eclass;
    }

    /**
     * Can transform a List/Set/Collection/Iterable to an int[] for storage in the db.
     * Null-tolerant.
     */
    public static class IntegerIterable extends Transformer<Iterable<Integer>, int[]>
    {
        @Override
        public void init (Type fieldType, Transform annotation)
        {
            _ftype = fieldType;
            _immutable = annotation.immutable();
            _intern = annotation.intern();
        }

        @Override
        public int[] toPersistent (Iterable<Integer> itr)
        {
            if (itr == null) {
                return null;
            }
            Collection<Integer> coll = (itr instanceof Collection<?>)
                ? (Collection<Integer>)itr
                : Lists.newArrayList(itr);
            return Ints.toArray(coll);
        }

        @Override
        public Iterable<Integer> fromPersistent (int[] value)
        {
            if (value == null) {
                return null;
            }
            Builder<Integer, Iterable<Integer>> builder =
                createCollectionBuilder(_ftype, Integer.class, _immutable, _intern, value.length);
            for (int v : value) {
                builder.add(v);
            }
            return builder.build();
        }

        /** The type of the field. */
        protected Type _ftype;

        /** Immutable hint. */
        protected boolean _immutable;

        /** Intern hint. */
        protected boolean _intern;
    }

    /**
     * An interface used by some of these transformers to build their result.
     */
    protected interface Builder<E, B>
    {
        void add (E element);

        B build ();
    }

    /**
     * Create a builder that populates a collection.
     *
     * @param elementType if non-null and an enum, will be used to possibly create an EnumSet.
     * @param sizeHint -1 or an *exact size* hint.
     */
    protected static <E> Builder<E, Iterable<E>> createCollectionBuilder (
        Type fieldType, Class<E> elementType, boolean immutable, boolean intern, int sizeHint)
    {
        Collection<E> adder;
        Collection<E> retval = null;
        Type clazz = (fieldType instanceof ParameterizedType) ?
            ((ParameterizedType)fieldType).getRawType() : fieldType;
        // TODO: fill out the collection types
        // TODO: also, it might be nice to build into an ImmutableCollection if making
        // immutable (instead of wrapping in a unmodifiableCollection), but that results in
        // an extra copy... Perhaps we can add another flag that opts-in to that? Aiya.
        if (clazz == HashSet.class || clazz == Set.class || clazz == EnumSet.class) {
            Set<E> set;
            if (clazz == HashSet.class || (elementType == null) || !elementType.isEnum()) {
                Preconditions.checkArgument(clazz != EnumSet.class,
                    "Cannot proceed: EnumSet field is to be populated with a null element.");
                set = (sizeHint < 0)
                    ? Sets.<E>newHashSet()
                    : Sets.<E>newHashSetWithExpectedSize(sizeHint);
            } else {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Class<Enum> eclazz = (Class<Enum>)elementType;
                @SuppressWarnings("unchecked")
                EnumSet<E> eSet = EnumSet.noneOf(eclazz);
                @SuppressWarnings("unchecked")
                set = (Set<E>)eSet;
            }
            adder = set;
            if (immutable && (clazz == Set.class)) {
                retval = Collections.unmodifiableSet(set);
            }

        } else if (clazz == LinkedList.class) {
            adder = Lists.newLinkedList();

        } else {
            List<E> list = (sizeHint < 0)
                ? Lists.<E>newArrayList()
                : Lists.<E>newArrayListWithCapacity(sizeHint);
            adder = list;
            if (immutable &&
                    ((clazz == List.class) ||
                     (clazz == Iterable.class) ||
                     (clazz == Collection.class))) {
                retval = Collections.unmodifiableList(list);
            }
        }

        // ok, now we're ready
        final Collection<E> fadder = adder;
        final Collection<E> fretval = (retval == null) ? adder : retval;
        final boolean fintern = (retval != null) && immutable && intern;
        return new Builder<E, Iterable<E>>() {
            public void add (E element) {
                fadder.add(element);
            }
            public Iterable<E> build () {
                if (fintern) {
                    Object interned = INTERNER.intern(fretval);
                    @SuppressWarnings("unchecked") Iterable<E> built = (Iterable<E>)interned;
                    return built;
                }
                return fretval;
            }
        };
    }

    /**
     * Building-block used to create other Transformers.
     */
    protected abstract static class StringBase<F> extends Transformer<F, String>
    {
        @Override
        public void init (Type fieldType, Transform annotation)
        {
            _ftype = fieldType;
            _immutable = annotation.immutable();
            _internStrings = _intern = annotation.intern();
        }

        @Override
        public String toPersistent (F value)
        {
            if (value == null) {
                return null;
            }

            StringBuilder buf = new StringBuilder();
            for (String s : asIterable(value)) {
                if (s == null) {
                    buf.append("\\\n"); // encode nulls as slash followed by the terminator
                } else {
                    s = s.replace("\\", "\\\\"); // turn \ into \\
                    s = s.replace("\n", "\\n");  // turn a newline in a String to "\n"
                    buf.append(s).append('\n');
                }
            }
            return buf.toString();
        }

        @Override
        public F fromPersistent (String encoded)
        {
            if (encoded == null) {
                return null;
            }

            Builder<String, F> builder = createBuilder(encoded);
            StringBuilder buf = new StringBuilder(encoded.length());
            for (int ii = 0, nn = encoded.length(); ii < nn; ii++) {
                char c = encoded.charAt(ii);
                switch (c) {
                case '\n':
                    String s = buf.toString();
                    builder.add(_internStrings ? s.intern() : s);
                    buf.setLength(0);
                    break;

                case '\\':
                    Preconditions.checkArgument(++ii < nn, "Invalid encoded string");
                    char slashed = encoded.charAt(ii);
                    switch (slashed) {
                    case '\n': // turn back into a null element
                        Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
                        builder.add(null);
                        break;
                    case 'n': // turn \n back into a newline
                        buf.append('\n');
                        break;
                    default: // this should only be a slash...
                        buf.append(slashed);
                        break;
                    }
                    break;

                default:
                    buf.append(c);
                    break;
                }
            }
            // make sure the last element was terminated
            Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
            return builder.build();
        }

        protected abstract Iterable<String> asIterable (F value);

        protected abstract Builder<String, F> createBuilder (String encoded);

        /**
         * Utility to count the number of elements in the encoded non-null string.
         */
        protected static int countElements (String encoded)
        {
            int count = 0;
            for (int pos = 0; 0 != (pos = 1 + encoded.indexOf('\n', pos)); count++) {}
            return count;
        }

        /**
         * Utility to see if there are any nulls in the encoded string.
         */
        protected static boolean hasNullElement (String encoded)
        {
            for (int pos = 0; -1 != (pos = encoded.indexOf("\\\n", pos)); pos += 2) {
                // make sure there isn't another slash before this token
                if ((pos == 0) || ('\\' != encoded.charAt(pos - 1))) {
                    return true;
                }
            }
            return false;
        }

        protected Type _ftype;

        /** Immutable hint. */
        protected boolean _immutable;

        /** Interning hint.*/
        protected boolean _intern;

        /** Do we intern the actual strings? */
        protected boolean _internStrings;
    }

    /** The interner we use for <em>immutable</em> values. */
    protected static final Interner<Object> INTERNER = Interners.newWeakInterner();
}
