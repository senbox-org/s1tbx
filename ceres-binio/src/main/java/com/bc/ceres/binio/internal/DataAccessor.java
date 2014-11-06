/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.util.ByteArrayCodec;

import java.nio.ByteOrder;

abstract class DataAccessor {

    protected final ByteArrayCodec codec;

    protected DataAccessor(ByteOrder byteOrder) {
        this.codec = ByteArrayCodec.getInstance(byteOrder);
    }

    public final ByteOrder getByteOrder() {
        return codec.getByteOrder();
    }

    public abstract byte getByte(byte[] array, int position);

    public abstract short getShort(byte[] array, int position);

    public abstract int getInt(byte[] array, int position);

    public abstract long getLong(byte[] array, int position);

    public abstract float getFloat(byte[] array, int position);

    public abstract double getDouble(byte[] array, int position);

    public abstract void setByte(byte[] array, int position, byte value);

    public abstract void setShort(byte[] array, int position, short value);

    public abstract void setInt(byte[] array, int position, int value);

    public abstract void setLong(byte[] array, int position, long value);

    public abstract void setFloat(byte[] array, int position, float value);

    public abstract void setDouble(byte[] array, int position, double value);

    public static DataAccessor getInstance(Type type, ByteOrder byteOrder) {
        if (type == SimpleType.BYTE) {
            return new Byte(byteOrder);
        } else if (type == SimpleType.UBYTE) {
            return new UByte(byteOrder);
        } else if (type == SimpleType.SHORT) {
            return new Short(byteOrder);
        } else if (type == SimpleType.USHORT) {
            return new UShort(byteOrder);
        } else if (type == SimpleType.INT) {
            return new Int(byteOrder);
        } else if (type == SimpleType.UINT) {
            return new UInt(byteOrder);
        } else if (type == SimpleType.LONG) {
            return new Long(byteOrder);
        } else if (type == SimpleType.ULONG) {
            return new Long(byteOrder); // Note: ULONG handled as signed long!
        } else if (type == SimpleType.FLOAT) {
            return new Float(byteOrder);
        } else if (type == SimpleType.DOUBLE) {
            return new Double(byteOrder);
        } else {
            throw new IllegalArgumentException("type: DataAccessor not implemented for " + type);
        }
    }

    static final class Byte extends DataAccessor {
        Byte(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return array[position];
        }

        @Override
        public short getShort(byte[] array, int position) {
            return getByte(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return getByte(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return getByte(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getByte(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getByte(array, position);
        }


        @Override
        public void setByte(byte[] array, int position, byte value) {
            array[position] = value;
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setByte(array, position, (byte) value);
        }
    }

    static final class UByte extends DataAccessor {
        UByte(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return array[position];
        }

        @Override
        public short getShort(byte[] array, int position) {
            return (short) getInt(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return getByte(array, position) & 0xFF;
        }

        @Override
        public long getLong(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            array[position] = value;
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setByte(array, position, (byte) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setByte(array, position, (byte) value);
        }
    }

    static final class Short extends DataAccessor {
        Short(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getShort(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return codec.getShort(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return getShort(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return getShort(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getShort(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getShort(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setShort(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            codec.setShort(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setShort(array, position, (short) value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setShort(array, position, (short) value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setShort(array, position, (short) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setShort(array, position, (short) value);
        }
    }

    static final class UShort extends DataAccessor {
        UShort(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getInt(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return codec.getShort(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return getShort(array, position) & 0xFFFF;
        }

        @Override
        public long getLong(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setShort(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            codec.setShort(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setShort(array, position, (short) value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setShort(array, position, (short) value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setShort(array, position, (short) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setShort(array, position, (short) value);
        }
    }

    static final class Int extends DataAccessor {
        Int(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getInt(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return (short) getInt(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return codec.getInt(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getInt(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setInt(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setInt(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            codec.setInt(array, position, value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setInt(array, position, (int) value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setInt(array, position, (int) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setInt(array, position, (int) value);
        }
    }

    static final class UInt extends DataAccessor {
        UInt(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getLong(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return (short) getLong(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return codec.getInt(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return getInt(array, position) & 0xFFFFFFFFL;
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getLong(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getLong(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setInt(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setInt(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            codec.setInt(array, position, value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setInt(array, position, (int) value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setInt(array, position, (int) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setInt(array, position, (int) value);
        }
    }

    static final class Long extends DataAccessor {
        Long(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getLong(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return (short) getLong(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return (int) getLong(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return codec.getLong(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return getLong(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getLong(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setLong(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setLong(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setLong(array, position, value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            codec.setLong(array, position, value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setLong(array, position, (long) value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setLong(array, position, (long) value);
        }
    }

    static final class Float extends DataAccessor {
        Float(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getFloat(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return (short) getFloat(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return (int) getFloat(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return (long) getFloat(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return codec.getFloat(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return getFloat(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setFloat(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setFloat(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setFloat(array, position, value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setFloat(array, position, value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            codec.setFloat(array, position, value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            setFloat(array, position, (float) value);
        }
    }

    static final class Double extends DataAccessor {
        Double(ByteOrder byteOrder) {
            super(byteOrder);
        }

        @Override
        public byte getByte(byte[] array, int position) {
            return (byte) getDouble(array, position);
        }

        @Override
        public short getShort(byte[] array, int position) {
            return (short) getDouble(array, position);
        }

        @Override
        public int getInt(byte[] array, int position) {
            return (int) getDouble(array, position);
        }

        @Override
        public long getLong(byte[] array, int position) {
            return (long) getDouble(array, position);
        }

        @Override
        public float getFloat(byte[] array, int position) {
            return (float) getDouble(array, position);
        }

        @Override
        public double getDouble(byte[] array, int position) {
            return codec.getDouble(array, position);
        }

        @Override
        public void setByte(byte[] array, int position, byte value) {
            setDouble(array, position, value);
        }

        @Override
        public void setShort(byte[] array, int position, short value) {
            setDouble(array, position, value);
        }

        @Override
        public void setInt(byte[] array, int position, int value) {
            setDouble(array, position, value);
        }

        @Override
        public void setLong(byte[] array, int position, long value) {
            setDouble(array, position, value);
        }

        @Override
        public void setFloat(byte[] array, int position, float value) {
            setDouble(array, position, value);
        }

        @Override
        public void setDouble(byte[] array, int position, double value) {
            codec.setDouble(array, position, value);
        }
    }
}
