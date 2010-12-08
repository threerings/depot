//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.depot.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Transformer;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Transform;
import com.samskivert.jdbc.ColumnDefinition;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.ByteEnumUtil;
import com.samskivert.util.Logger;
import com.samskivert.util.StringUtil;

/**
 * Handles the marshalling and unmarshalling of a particular field of a persistent object.
 *
 * @see DepotMarshaller
 */
public abstract class FieldMarshaller<T>
{
    /** Used by the {@link SQLBuilder} implementations. We factor this into an interface to avoid a
     * bunch of instanceof casts and to ensure that if a new supported type is added, all of the
     * builders will fail to compile instead of failing at runtime. */
    public static interface ColumnTyper {
        String getBooleanType (int length);
        String getByteType (int length);
        String getShortType (int length);
        String getIntType (int length);
        String getLongType (int length);
        String getFloatType (int length);
        String getDoubleType (int length);
        String getStringType (int length);
        String getDateType (int length);
        String getTimeType (int length);
        String getTimestampType (int length);
        String getBlobType (int length);
        String getClobType (int length);
    }

    /**
     * Creates and returns a field marshaller for the specified field. Throws an exception if the
     * field in question cannot be marshalled.
     */
    public static FieldMarshaller<?> createMarshaller (Field field)
    {
        // first, look for a @Transform annotation on the field (cheap)
        Transform xform = field.getAnnotation(Transform.class);
        if (xform == null) {
            // next look for a marshaller for the basic type (cheapish)
            FieldMarshaller<?> marshaller = createMarshaller(field.getType());
            if (marshaller != null) {
                marshaller.create(field);
                return marshaller;
            }

            // finally look for an @Transform annotation on the field type (expensive)
            xform = findTransformAnnotation(field.getType());
            if (xform == null) {
                throw new IllegalArgumentException("Cannot marshall " + field + ".");
            }
        }

        try {
            // weirdly if we inline the xformer creation into the createTransformingMarshaller
            // call, we get a type error; must be some subtlety of wildcard capture...
            Transformer<?, ?> xformer = xform.value().newInstance();
            return createTransformingMarshaller(xformer, field, xform);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                Logger.format("Unable to create Transformer", "xclass", xform.value(),
                              "field", field), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                Logger.format("Unable to create Transformer", "xclass", xform.value(),
                              "field", field), e);
        }
    }

    protected static <F,T> FieldMarshaller<T> createTransformingMarshaller (
        final Transformer<F,T> xformer, Field field, Transform annotation)
    {
        Class<?> pojoType = getTransformerType(xformer, "from");
        if (!pojoType.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException(
                "@Transform error on " + field.getType().getName() + "." +
                field.getName() + ": " + xformer.getClass().getName() + " cannot convert " +
                field.getType().getName());
        }
        xformer.init(field.getGenericType(), annotation);

        @SuppressWarnings("unchecked") final FieldMarshaller<T> delegate =
            (FieldMarshaller<T>)createMarshaller(getTransformerType(xformer, "to"));
        delegate.create(field);

        FieldMarshaller<T> xmarsh = new FieldMarshaller<T>() {
            @Override public String getColumnType (ColumnTyper typer, int length) {
                return delegate.getColumnType(typer, length);
            }
            @Override public T getFromObject (Object po)
                throws IllegalArgumentException, IllegalAccessException {
                @SuppressWarnings("unchecked") F value = (F)_field.get(po);
                return xformer.toPersistent(value);
            }
            @Override public T getFromSet (ResultSet rs) throws SQLException {
                return delegate.getFromSet(rs);
            }
            @Override public T getFromSet (ResultSet rs, int index) throws SQLException {
                return delegate.getFromSet(rs, index);
            }
            @Override public void writeToObject (Object po, T value)
                throws IllegalArgumentException, IllegalAccessException {
                _field.set(po, xformer.fromPersistent(value));
            }
            @Override public void writeToStatement (PreparedStatement ps, int column, T value)
                throws SQLException {
                delegate.writeToStatement(ps, column, value);
            }
        };
        xmarsh.create(field);
        return xmarsh;
    }

    /**
     * Initializes this field marshaller with a SQL builder which it uses to construct its column
     * definition according to the appropriate database dialect.
     */
    public void init (SQLBuilder builder)
        throws DatabaseException
    {
        _columnDefinition = builder.buildColumnDefinition(this);
    }

    /**
     * Returns the {@link Field} handled by this marshaller.
     */
    public Field getField ()
    {
        return _field;
    }

    /**
     * Returns the Computed annotation on this field, if any.
     */
    public Computed getComputed ()
    {
        return _computed;
    }

    /**
     * Returns the GeneratedValue annotation on this field, if any.
     */
    public GeneratedValue getGeneratedValue ()
    {
        return _generatedValue;
    }

    /**
     * Returns the name of the table column used to store this field.
     */
    public String getColumnName ()
    {
        return _columnName;
    }

    /**
     * Returns the SQL used to define this field's column.
     */
    public ColumnDefinition getColumnDefinition ()
    {
        return _columnDefinition;
    }

    /**
     * Returns the appropriate column type for this field, given the database specific typer
     * supplied as an argument.
     */
    public abstract String getColumnType (ColumnTyper typer, int length);

    /**
     * Reads this field from the given persistent object.
     */
    public abstract T getFromObject (Object po)
        throws IllegalArgumentException, IllegalAccessException;

    /**
     * Sets the specified column of the given prepared statement to the given value.
     */
    public abstract void writeToStatement (PreparedStatement ps, int column, T value)
        throws SQLException;

    /**
     * Reads the value of our field from the supplied persistent object and sets that value into
     * the specified column of the supplied prepared statement.
     */
    public void getAndWriteToStatement (PreparedStatement ps, int column, Object po)
        throws SQLException, IllegalAccessException
    {
        writeToStatement(ps, column, getFromObject(po));
    }

    /**
     * Reads and returns this field from the result set.
     */
    public abstract T getFromSet (ResultSet rs)
        throws SQLException;

    /**
     * Reads and returns this field from the result set.
     */
    public abstract T getFromSet (ResultSet rs, int index)
        throws SQLException;

    /**
     * Writes the given value to the given persistent value.
     */
    public abstract void writeToObject (Object po, T value)
        throws IllegalArgumentException, IllegalAccessException;

    /**
     * Reads the specified column from the supplied result set and writes it to the appropriate
     * field of the persistent object.
     */
    public void getAndWriteToObject (ResultSet rset, Object po)
        throws SQLException, IllegalAccessException
    {
        writeToObject(po, getFromSet(rset));
    }

    protected void create (Field field)
    {
        _field = field;
        _columnName = field.getName();
        _computed = field.getAnnotation(Computed.class);

        Column column = _field.getAnnotation(Column.class);
        if (column == null) {
            // look for a column annotation on the shadowed field, if any
            Computed dcomputed = (_computed == null) ?
                field.getDeclaringClass().getAnnotation(Computed.class) : _computed;
            if (dcomputed != null) {
                Class<? extends PersistentRecord> sclass = dcomputed.shadowOf();
                if (!PersistentRecord.class.equals(sclass)) {
                    try {
                        column = sclass.getField(field.getName()).getAnnotation(Column.class);
                    } catch (NoSuchFieldException e) {
                        // no problem; assume that it will be defined in the query
                    }
                }
            }
        }
        if (column != null) {
            if (!StringUtil.isBlank(column.name())) {
                _columnName = column.name();
            }
        }

        if (_computed != null) {
            return;
        }

        // figure out how we're going to generate our primary key values
        _generatedValue = field.getAnnotation(GeneratedValue.class);
    }

    protected static FieldMarshaller<?> createMarshaller (Class<?> ftype)
    {
        // primitive types
        if (ftype.equals(Boolean.TYPE)) {
            return new BooleanMarshaller();
        } else if (ftype.equals(Byte.TYPE)) {
            return new ByteMarshaller();
        } else if (ftype.equals(Short.TYPE)) {
            return new ShortMarshaller();
        } else if (ftype.equals(Integer.TYPE)) {
            return new IntMarshaller();
        } else if (ftype.equals(Long.TYPE)) {
            return new LongMarshaller();
        } else if (ftype.equals(Float.TYPE)) {
            return new FloatMarshaller();
        } else if (ftype.equals(Double.TYPE)) {
            return new DoubleMarshaller();

        // boxed primitive types
        } else if (ftype.equals(Boolean.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getBooleanType(length);
                }
            };
        } else if (ftype.equals(Byte.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getByteType(length);
                }
                @Override public Object getFromSet (ResultSet rs) throws SQLException {
                    return massageResult(super.getFromSet(rs));
                }
                // works around the fact that HSQLDB (at least) returns Integer rather than Byte
                // for TINYINT columns
                protected Object massageResult (Object value) {
                    return (value == null) ? null : ((Number)value).byteValue();
                }
            };
        } else if (ftype.equals(Short.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getShortType(length);
                }
                @Override public Object getFromSet (ResultSet rs) throws SQLException {
                    return massageResult(super.getFromSet(rs));
                }
                // works around the fact that HSQLDB (at least) returns Integer rather than Short
                // for SMALLINT columns
                protected Object massageResult (Object value) {
                    return (value == null) ? null : ((Number)value).shortValue();
                }
            };
        } else if (ftype.equals(Integer.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getIntType(length);
                }
            };
        } else if (ftype.equals(Long.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getLongType(length);
                }
            };
        } else if (ftype.equals(Float.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getFloatType(length);
                }
                @Override public Object getFromSet (ResultSet rs) throws SQLException {
                    return massageResult(super.getFromSet(rs));
                }
                // works around the fact that HSQLDB (at least) returns Double rather than Float
                // for REAL columns
                protected Object massageResult (Object value) {
                    return (value == null) ? null : ((Number)value).floatValue();
                }
            };
        } else if (ftype.equals(Double.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getDoubleType(length);
                }
            };
        } else if (ftype.equals(String.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getStringType(length);
                }
            };

        // some primitive array types
        } else if (ftype.equals(byte[].class)) {
            return new ByteArrayMarshaller();

        } else if (ftype.equals(int[].class)) {
            return new IntArrayMarshaller();

        // SQL types
        } else if (ftype.equals(Date.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getDateType(length);
                }
            };
        } else if (ftype.equals(Time.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getTimeType(length);
                }
            };
        } else if (ftype.equals(Timestamp.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getTimestampType(length);
                }
            };
        } else if (ftype.equals(Blob.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getBlobType(length);
                }
            };
        } else if (ftype.equals(Clob.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getClobType(length);
                }
            };

        // special Enum type hackery (it would be nice to handle this with @Transform, but that
        // introduces some undesirable samskivert-Depot dependencies)
        } else if (ByteEnum.class.isAssignableFrom(ftype)) {
            @SuppressWarnings("unchecked") Class<Dummy> dtype = (Class<Dummy>)ftype;
            return new ByteEnumMarshaller<Dummy>(dtype);

        } else if (Enum.class.isAssignableFrom(ftype)) {
            @SuppressWarnings("unchecked") Class<Dummy> dtype = (Class<Dummy>)ftype;
            return new EnumMarshaller<Dummy>(dtype);

        } else {
            return null;
        }
    }

    protected static Class<?> getTransformerType (Transformer<?, ?> xformer, String which)
    {
        Class<?> ttype = null;
        String methodName = which + "Persistent";
        for (Method method : xformer.getClass().getMethods()) {
            if (method.getName().equals(methodName)) {
                if (ttype == null || ttype.isAssignableFrom(method.getReturnType())) {
                    ttype = method.getReturnType();
                }
            }
        }
        if (ttype == null) {
            throw new IllegalArgumentException(
                Logger.format("Transformer lacks " + which + "Persistent() method!?",
                              "xclass", xformer.getClass()));
        }
        return ttype;
    }

    protected static Transform findTransformAnnotation (Class<?> ftype)
    {
        Transform xform = ftype.getAnnotation(Transform.class);
        if (xform != null) {
            return xform;
        }
        for (Class<?> iface : ftype.getInterfaces()) {
            xform = iface.getAnnotation(Transform.class);
            if (xform != null) {
                return xform;
            }
        }
        Class<?> parent = ftype.getSuperclass();
        return (parent == null) ? null : findTransformAnnotation(parent);
    }

    protected static class BooleanMarshaller extends FieldMarshaller<Boolean> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getBooleanType(length);
        }
        @Override public Boolean getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getBoolean(po);
        }
        @Override public Boolean getFromSet (ResultSet rs) throws SQLException {
            return rs.getBoolean(getColumnName());
        }
        @Override public Boolean getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getBoolean(index);
        }
        @Override public void writeToObject (Object po, Boolean value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setBoolean(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Boolean value)
            throws SQLException {
            ps.setBoolean(column, value);
        }
    }

    protected static class ByteMarshaller extends FieldMarshaller<Byte> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getByteType(length);
        }
        @Override public Byte getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getByte(po);
        }
        @Override public Byte getFromSet (ResultSet rs) throws SQLException {
            return rs.getByte(getColumnName());
        }
        @Override public Byte getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getByte(index);
        }
        @Override public void writeToObject (Object po, Byte value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setByte(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Byte value)
            throws SQLException {
            ps.setByte(column, value);
        }
    }

    protected static class ShortMarshaller extends FieldMarshaller<Short> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getShortType(length);
        }
        @Override public Short getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getShort(po);
        }
        @Override public Short getFromSet (ResultSet rs) throws SQLException {
            return rs.getShort(getColumnName());
        }
        @Override public Short getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getShort(index);
        }
        @Override public void writeToObject (Object po, Short value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setShort(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Short value)
            throws SQLException {
            ps.setShort(column, value);
        }
    }

    protected static class IntMarshaller extends FieldMarshaller<Integer> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getIntType(length);
        }
        @Override public Integer getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getInt(po);
        }
        @Override public Integer getFromSet (ResultSet rs) throws SQLException {
            return rs.getInt(getColumnName());
        }
        @Override public Integer getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getInt(index);
        }
        @Override public void writeToObject (Object po, Integer value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setInt(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Integer value)
            throws SQLException {
            ps.setInt(column, value);
        }
    }

    protected static class LongMarshaller extends FieldMarshaller<Long> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getLongType(length);
        }
        @Override public Long getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getLong(po);
        }
        @Override public Long getFromSet (ResultSet rs) throws SQLException {
            return rs.getLong(getColumnName());
        }
        @Override public Long getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getLong(index);
        }
        @Override public void writeToObject (Object po, Long value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setLong(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Long value)
            throws SQLException {
            ps.setLong(column, value);
        }
    }

    protected static class FloatMarshaller extends FieldMarshaller<Float> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getFloatType(length);
        }
        @Override public Float getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getFloat(po);
        }
        @Override public Float getFromSet (ResultSet rs) throws SQLException {
            return rs.getFloat(getColumnName());
        }
        @Override public Float getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getFloat(index);
        }
        @Override public void writeToObject (Object po, Float value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setFloat(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Float value)
            throws SQLException {
            ps.setFloat(column, value);
        }
    }

    protected static class DoubleMarshaller extends FieldMarshaller<Double> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getDoubleType(length);
        }
        @Override public Double getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.getDouble(po);
        }
        @Override public Double getFromSet (ResultSet rs) throws SQLException {
            return rs.getDouble(getColumnName());
        }
        @Override public Double getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getDouble(index);
        }
        @Override public void writeToObject (Object po, Double value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.setDouble(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Double value)
            throws SQLException {
            ps.setDouble(column, value);
        }
    }

    protected static abstract class ObjectMarshaller extends FieldMarshaller<Object> {
        @Override public Object getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return _field.get(po);
        }
        @Override public Object getFromSet (ResultSet rs) throws SQLException {
            return rs.getObject(getColumnName());
        }
        @Override public Object getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getObject(index);
        }
        @Override public void writeToObject (Object po, Object value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.set(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, Object value)
            throws SQLException {
            ps.setObject(column, value);
        }
    }

    protected static class ByteArrayMarshaller extends FieldMarshaller<byte[]> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getBlobType(length);
        }
        @Override public byte[] getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return (byte[]) _field.get(po);
        }
        @Override public byte[] getFromSet (ResultSet rs) throws SQLException {
            return rs.getBytes(getColumnName());
        }
        @Override public byte[] getFromSet (ResultSet rs, int index) throws SQLException {
            return rs.getBytes(index);
        }
        @Override public void writeToObject (Object po, byte[] value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.set(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, byte[] value)
            throws SQLException {
            ps.setBytes(column, value);
        }
    }

    protected static class IntArrayMarshaller extends FieldMarshaller<int[]> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getBlobType(length*4);
        }
        @Override public int[] getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return (int[]) _field.get(po);
        }
        @Override public int[] getFromSet (ResultSet rs) throws SQLException {
            // TODO: why not use getBytes()?
            return fromBytes((byte[]) rs.getObject(getColumnName()));
        }
        @Override public int[] getFromSet (ResultSet rs, int index) throws SQLException {
            // TODO: why not use getBytes()?
            return fromBytes((byte[]) rs.getObject(index));
        }
        @Override public void writeToObject (Object po, int[] value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.set(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, int[] value)
            throws SQLException {
            byte[] raw;
            if (value == null) {
                raw = null;
            } else {
                ByteBuffer bbuf = ByteBuffer.allocate(value.length*4);
                bbuf.asIntBuffer().put(value);
                raw = bbuf.array();
            }
            ps.setObject(column, raw);
        }
        protected final int[] fromBytes (byte[] raw) {
            int[] value;
            if (raw == null) {
                value = null;
            } else {
                value = new int[raw.length/4];
                ByteBuffer.wrap(raw).asIntBuffer().get(value);
            }
            return value;
        }
    }

    protected static class ByteEnumMarshaller<E extends Enum<E> & ByteEnum>
        extends FieldMarshaller<ByteEnum> {
        public ByteEnumMarshaller (Class<E> clazz) {
            _eclass = clazz;
        }

        @Override public void create (Field field) {
            super.create(field);
            // do some sanity checking so that the unsafe business we do below is safer
            if (!Enum.class.isAssignableFrom(_eclass)) {
                throw new IllegalArgumentException(
                    "ByteEnum not implemented by real Enum: " + field);
            }
        }

        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getByteType(length);
        }
        @Override public ByteEnum getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            return (ByteEnum) _field.get(po);
        }
        @Override public ByteEnum getFromSet (ResultSet rs) throws SQLException {
            return ByteEnumUtil.fromByte(_eclass, rs.getByte(getColumnName()));
        }
        @Override public ByteEnum getFromSet (ResultSet rs, int index) throws SQLException {
            return ByteEnumUtil.fromByte(_eclass, rs.getByte(index));
        }
        @Override public void writeToObject (Object po, ByteEnum value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.set(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, ByteEnum value)
            throws SQLException {
            ps.setByte(column, value.toByte());
        }

        protected Class<E> _eclass;
    }

    protected static class EnumMarshaller<E extends Enum<E>> extends FieldMarshaller<E> {
        public EnumMarshaller (Class<E> clazz) {
            _eclass = clazz;
        }

        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getStringType(length);
        }
        @Override public E getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            @SuppressWarnings("unchecked") E value = (E) _field.get(po);
            return value;
        }
        @Override public E getFromSet (ResultSet rs) throws SQLException {
            return fromString(rs.getString(getColumnName()));
        }
        @Override public E getFromSet (ResultSet rs, int index) throws SQLException {
            return fromString(rs.getString(index));
        }
        @Override public void writeToObject (Object po, E value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.set(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, E value)
            throws SQLException {
            String svalue = (value == null) ? null : value.name();
            ps.setString(column, svalue);
        }
        protected final E fromString (String svalue) {
            return (svalue == null) ? null : Enum.valueOf(_eclass, svalue);
        }

        protected Class<E> _eclass;
    }

    // used to fool the type system when creating ByteEnumMarshallers; look away
    protected static enum Dummy implements ByteEnum {
        DUMMY;
        public byte toByte () { throw new UnsupportedOperationException("Dummy!"); }
    }

    protected Field _field;
    protected String _columnName;
    protected ColumnDefinition _columnDefinition;
    protected Computed _computed;
    protected GeneratedValue _generatedValue;
}
