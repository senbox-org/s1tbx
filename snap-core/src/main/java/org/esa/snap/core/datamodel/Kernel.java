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
package org.esa.snap.core.datamodel;

/**
 * The <code>Kernel</code> class defines a matrix that describes how a
 * specified pixel and its surrounding pixels affect the value
 * computed for the pixel's position in the output image of a filtering
 * operation.  The X origin and Y origin indicate the filter matrix element
 * that corresponds to the pixel position for which an output value is
 * being computed.
 */
public class Kernel implements Cloneable {

    private int width;
    private int height;
    private int xOrigin;
    private int yOrigin;
    private double factor;
    private double[] data;

    /**
     * Constructs a <code>Kernel</code> object from an array of floats.
     * The first <code>width</code>*<code>height</code> elements of
     * the <code>data</code> array are copied.
     * If the length of the <code>data</code> array is less
     * than width*height, an <code>IllegalArgumentException</code> is thrown.
     * The X origin is (width-1)/2 and the Y origin is (height-1)/2.
     *
     * @param width  width of the filter
     * @param height height of the filter
     * @param data   filter data in row major order
     * @throws IllegalArgumentException if the length of <code>data</code>
     *                                  is less than the product of <code>width</code> and
     *                                  <code>height</code>
     */
    public Kernel(int width, int height, double data[]) {
        this(width, height, 1.0, data);
    }

    /**
     * Constructs a <code>Kernel</code> object from an array of floats.
     * The first <code>width</code>*<code>height</code> elements of
     * the <code>data</code> array are copied.
     * If the length of the <code>data</code> array is less
     * than width*height, an <code>IllegalArgumentException</code> is thrown.
     * The X origin is (width-1)/2 and the Y origin is (height-1)/2.
     *
     * @param width  width of the filter
     * @param height height of the filter
     * @param factor factor to be applied to each element of <code>data</code>
     * @param data   filter data in row major order
     * @throws IllegalArgumentException if the length of <code>data</code>
     *                                  is less than the product of <code>width</code> and
     *                                  <code>height</code>
     */
    public Kernel(int width, int height, double factor, double data[]) {
        this(width, height, (width - 1) / 2, (height - 1) / 2, factor, data);
    }

    /**
     * Constructs a <code>Kernel</code> object from an array of floats.
     * The first <code>width</code>*<code>height</code> elements of
     * the <code>data</code> array are copied.
     * If the length of the <code>data</code> array is less
     * than width*height, an <code>IllegalArgumentException</code> is thrown.
     * The X origin is (width-1)/2 and the Y origin is (height-1)/2.
     *
     * @param width   width of the filter
     * @param height  height of the filter
     * @param xOrigin X origin of the filter
     * @param yOrigin Y origin of the filter
     * @param factor  factor to be applied to each element of <code>data</code>
     * @param data    filter data in row major order
     * @throws IllegalArgumentException if the length of <code>data</code>
     *                                  is less than the product of <code>width</code> and
     *                                  <code>height</code>
     */
    public Kernel(int width, int height, int xOrigin, int yOrigin, double factor, double data[]) {
        this.width = width;
        this.height = height;
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
        int len = width * height;
        if (data.length < len) {
            throw new IllegalArgumentException("Data array too small " +
                                                       "(is " + data.length +
                                                       " and should be " + len);
        }
        this.factor = factor;
        this.data = new double[len];
        System.arraycopy(data, 0, this.data, 0, len);

    }

    /**
     * Returns the X origin of this <code>Kernel</code>.
     *
     * @return the X origin.
     */
    final public int getXOrigin() {
        return xOrigin;
    }

    /**
     * Returns the Y origin of this <code>Kernel</code>.
     *
     * @return the Y origin.
     */
    final public int getYOrigin() {
        return yOrigin;
    }

    /**
     * Returns the width of this <code>Kernel</code>.
     *
     * @return the width of this <code>Kernel</code>.
     */
    final public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this <code>Kernel</code>.
     *
     * @return the height of this <code>Kernel</code>.
     */
    final public int getHeight() {
        return height;
    }

    /**
     * Returns the factor of this <code>Kernel</code>.
     *
     * @return the factor of this <code>Kernel</code>.
     */
    final public double getFactor() {
        return factor;
    }

    /**
     * Returns the filter data in row major order.
     * The <code>data</code> array is returned.  If <code>data</code>
     * is <code>null</code>, a new array is allocated.
     *
     * @param data if non-null, contains the returned filter data
     * @return the <code>data</code> array containing the filter data
     * in row major order or, if <code>data</code> is
     * <code>null</code>, a newly allocated array containing
     * the filter data in row major order
     * @throws IllegalArgumentException if <code>data</code> is less
     *                                  than the size of this <code>Kernel</code>
     */
    final public double[] getKernelData(double[] data) {
        if (data == null) {
            data = new double[this.data.length];
        } else if (data.length < this.data.length) {
            throw new IllegalArgumentException("Data array too small " +
                                                       "(should be " + this.data.length +
                                                       " but is " +
                                                       data.length + " )");
        }
        System.arraycopy(this.data, 0, data, 0, this.data.length);

        return data;
    }

    /**
     * Clones this object.
     *
     * @return a clone of this object.
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
}






