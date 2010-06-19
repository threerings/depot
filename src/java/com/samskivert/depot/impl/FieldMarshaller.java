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
            @SuppressWarnings("unchecked") Transformer<?,?> xformer = xform.value().newInstance();
            @SuppressWarnings("unchecked") TransformingMarshaller<?,?> xmarsh =
                new TransformingMarshaller(xformer, field);
            xmarsh.create(field);
            return xmarsh;
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

        Column column = _field.getAnnotation(Column.class);
        if (column != null) {
            if (!StringUtil.isBlank(column.name())) {
                _columnName = column.name();
            }
        }

        _computed = field.getAnnotation(Computed.class);
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

        // "natural" types
        } else if (ftype.equals(Byte.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getByteType(length);
                }
                @Override public Object getFromSet (ResultSet rs)
                    throws SQLException {
                    Object value = super.getFromSet(rs);
                    return (value == null) ? null : ((Number)value).byteValue();
                }
            };
        } else if (ftype.equals(Short.class)) {
            return new ObjectMarshaller() {
                @Override public String getColumnType (ColumnTyper typer, int length) {
                    return typer.getShortType(length);
                }
                @Override public Object getFromSet (ResultSet rs)
                    throws SQLException {
                    Object value = super.getFromSet(rs);
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
                @Override public Object getFromSet (ResultSet rs)
                    throws SQLException {
                    Object value = super.getFromSet(rs);
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
            @SuppressWarnings("unchecked") ByteEnumMarshaller<?> bem =
                new ByteEnumMarshaller(ftype);
            return bem;

        } else {
            return null;
        }
    }

    protected static Class<?> getTransformerType (Transformer<?, ?> xformer, String which)
    {
        Class<?> ttype = null;
        for (Method method : xformer.getClass().getDeclaredMethods()) {
            if (method.getName().equals(which + "Persistent")) {
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
        @Override public Boolean getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getBoolean(getColumnName());
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
        @Override public Byte getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getByte(getColumnName());
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
        @Override public Short getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getShort(getColumnName());
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
        @Override public Integer getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getInt(getColumnName());
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
        @Override public Long getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getLong(getColumnName());
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
        @Override public Float getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getFloat(getColumnName());
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
        @Override public Double getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getDouble(getColumnName());
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
        @Override public Object getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getObject(getColumnName());
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
        @Override public byte[] getFromSet (ResultSet rs)
            throws SQLException {
            return rs.getBytes(getColumnName());
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

    protected static class IntArrayMarshaller extends FieldMarshaller<byte[]> {
        @Override public String getColumnType (ColumnTyper typer, int length) {
            return typer.getBlobType(length);
        }
        @Override public byte[] getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            int[] values = (int[]) _field.get(po);
            if (values == null) {
                return null;
            }
            ByteBuffer bbuf = ByteBuffer.allocate(values.length * 4);
            bbuf.asIntBuffer().put(values);
            return bbuf.array();
        }
        @Override public byte[] getFromSet (ResultSet rs)
            throws SQLException {
            return (byte[]) rs.getObject(getColumnName());
        }
        @Override public void writeToObject (Object po, byte[] data)
            throws IllegalArgumentException, IllegalAccessException {
            int[] value = null;
            if (data != null) {
                value = new int[data.length/4];
                ByteBuffer.wrap(data).asIntBuffer().get(value);
            }
            _field.set(po, value);
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, byte[] value)
            throws SQLException {
            ps.setObject(column, value);
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

    protected static class TransformingMarshaller<F,T> extends FieldMarshaller<T> {
        public TransformingMarshaller (Transformer<F, T> xformer, Field field) {
            Class<?> pojoType = getTransformerType(xformer, "from");
            if (!pojoType.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException(
                    "@Transform error on " + field.getType().getName() + "." +
                    field.getName() + ": " + xformer.getClass().getName() + " cannot convert " +
                    field.getType().getName());
            }
            @SuppressWarnings("unchecked") FieldMarshaller<T> delegate =
                (FieldMarshaller<T>)createMarshaller(getTransformerType(xformer, "to"));
            _delegate = delegate;
            _xformer = xformer;
        }

        @Override public void create (Field field) {
            super.create(field);
            _delegate.create(field);
        }

        @Override public String getColumnType (ColumnTyper typer, int length) {
            return _delegate.getColumnType(typer, length);
        }
        @Override public T getFromObject (Object po)
            throws IllegalArgumentException, IllegalAccessException {
            @SuppressWarnings("unchecked") F value = (F)_field.get(po);
            return _xformer.toPersistent(value);
        }
        @Override public T getFromSet (ResultSet rs) throws SQLException {
            return _delegate.getFromSet(rs);
        }
        @Override public void writeToObject (Object po, T value)
            throws IllegalArgumentException, IllegalAccessException {
            _field.set(po, _xformer.fromPersistent(_field.getType(), value));
        }
        @Override public void writeToStatement (PreparedStatement ps, int column, T value)
            throws SQLException {
            _delegate.writeToStatement(ps, column, value);
        }

        protected Transformer<F, T> _xformer;
        protected FieldMarshaller<T> _delegate;
    }

    protected Field _field;
    protected String _columnName;
    protected ColumnDefinition _columnDefinition;
    protected Computed _computed;
    protected GeneratedValue _generatedValue;
}
