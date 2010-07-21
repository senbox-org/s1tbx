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
package org.esa.beam.processor.cloud.internal.util;


/**
 * <p>
 * Represents a multi-dimensional lookup table (LUT). A LUT comprises a multi-dimensional array ({@link MDArray})
 * containing the lookup table values and optional axes for each dimension. Each axis is represented by an array of
 * tabulated <code>double</code> elements. The number of elements in each axis must be equal to the size of the
 * corresponding dimension within the multi-dimensional value array.</p>
 * 
 * <p><i><b>IMPORTANT NOTE:</b> 
 * This class belongs to a preliminary API. 
 * It is not (yet) intended to be used by clients and may change in the future.</i></p>
 */
public class LUT {

    /**
     * The multi-dimensional array of primitive <code>float</code> or <code>double</code> elements.
     */
    private final MDArray _array;
    /**
     * Tabulated values for each dimension.
     */
    private final double[][] _tabs;

    /**
     * Constructs a new LUT from the given array object.
     *
     * @param javaArray an instance of a single- or multi-dimensional, regular Java array of type <code>float</code> or
     *                  <code>double</code>.
     */
    public LUT(final Object javaArray) {
        this(new MDArray(javaArray));
    }

    /**
     * Constructs a new LUT from the given array object.
     *
     * @param array a multi-dimensional array. Must not be <code>null</code>.
     */
    public LUT(final MDArray array) {
        _array = array;
        _tabs = new double[_array.getRank()][];
    }

    /**
     * Constructs a LUT from the given dimension sizes and flat <code>float</code> array.
     *
     * @param sizes    the dimension sizes describing the memory layout of the given flat array
     * @param elements a flat <code>float</code> array
     */
    public LUT(final int[] sizes, final float[] elements) {
        this(new MDArray(float.class, sizes, elements));
    }

    /**
     * Constructs a LUT from the given dimension sizes and flat <code>float</code> array.
     *
     * @param sizes    the dimension sizes describing the memory layout of the given flat array
     * @param elements a flat <code>float</code> array
     */
    public LUT(final int[] sizes, final double[] elements) {
        this(new MDArray(double.class, sizes, elements));
    }

    /**
     * Gets the underlying multi-dimensional array of LUT values.
     *
     * @return the elements, never <code>null</code>
     */
    public MDArray getArray() {
        return _array;
    }

    /**
     * Gets the elements of this LUT as a multi-dimensional, regular Java array of primitive <code>float</code> or
     * <code>double</code> elements.
     *
     * @return the elements, never <code>null</code>
     */
    public Object getJavaArray() {
        return _array.getJavaArray();
    }

    /**
     * Gets the rank of this LUT.
     *
     * @return the number of dimensions
     */
    public int getRank() {
        return _array.getRank();
    }

    /**
     * Gets the total number of elements in this LUT.
     *
     * @return the total number of elements
     */
    public int getElementCount() {
        return _array.getElementCount();
    }

    /**
     * Gets the number of elements for the given dimension index.
     *
     * @param dim the index of the dimension, must be within <code>0</code> and <code>{@link #getRank()} - 1</code>
     * @return the size of the given dimension
     */
    public int getDimSize(final int dim) {
        return _array.getDimSize(dim);
    }

    /**
     * Gets the tabulated values for the given dimension.
     *
     * @param dim the index of the dimension, must be within <code>0</code> and <code>{@link #getRank()} - 1</code>
     * @return the the tabulated values for the given dimension
     */
    public double[] getTab(final int dim) {
        return _tabs[dim];
    }

    /**
     * Sets the tabulated values for the given dimension.
     *
     * @param dim the index of the dimension, must be within <code>0</code> and <code>{@link #getRank()} - 1</code>
     * @param tab the tabualted values for the given dimension, can be <code>null</code>.
     * @throws IllegalArgumentException If <code>tab</code> is not <code>null</code> and <code>{@link #getDimSize(int)
     *                                  getNumBytes(dim)} != tab.length</code>
     */
    public void setTab(final int dim, final double[] tab) {
        if (tab != null && getDimSize(dim) != tab.length) {
            throw new IllegalArgumentException("illegal tab.length = " + tab.length + ", expected " + getDimSize(dim));
        }
        _tabs[dim] = tab;
    }
}

