/*
 * $Id: DoubleList.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.util.math;

/**
 * The double list provides a generic access to an ordered list of values of type <code>double</code>.
 *
 * @author Norman Fomferra
 */
public interface DoubleList {

    int getSize();

    double getDouble(int index);

    /**
     * Wraps a {@link DoubleList} around an array of primitive bytes.
     */
    static class Byte implements DoubleList {

        private final byte[] _array;

        public Byte(byte[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive bytes interpreted as unsigned integers.
     */
    static class UByte implements DoubleList {

        private final byte[] _array;

        public UByte(byte[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index] & 0xff;
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive shorts.
     */
    static class Short implements DoubleList {

        private final short[] _array;

        public Short(short[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive shorts interpreted as unsigned integers.
     */
    static class UShort implements DoubleList {

        private final short[] _array;

        public UShort(short[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index] & 0xffff;
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive ints.
     */
    static class Int implements DoubleList {

        private final int[] _array;

        public Int(int[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive ints interpreted as unsigned integers.
     */
    static class UInt implements DoubleList {

        private final int[] _array;

        public UInt(int[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index] & 0xffffffffL;
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive longs.
     */
    static class Long implements DoubleList {

        private final long[] _array;

        public Long(long[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive longs interpreted as unsigned integers.
     */
    static class ULong implements DoubleList {

        private final long[] _array;

        public ULong(long[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive floats.
     */
    static class Float implements DoubleList {

        private final float[] _array;

        public Float(float[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }

    /**
     * Wraps a {@link DoubleList} around an array of primitive doubles.
     */
    static class Double implements DoubleList {

        private final double[] _array;

        public Double(double[] array) {
            _array = array;
        }

        public final int getSize() {
            return _array.length;
        }

        public final double getDouble(int index) {
            return _array[index];
        }
    }
}
