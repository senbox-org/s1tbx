package org.esa.snap.binning.support;

import org.esa.snap.binning.WritableVector;

import java.util.Arrays;

/**
 * The {@code VectorImpl} class is a light-weight implementation of
 * the {@link org.esa.snap.binning.WritableVector} interface. It operates on an array of {@code float}
 * elements. The array object is used by reference and is passed into the constructor.
 * The class is final for allowing method in-lining.
 *
 * @author Norman Fomferra
 */
public final class VectorImpl implements WritableVector {
    private final float[] elements;
    private int offset;
    private int size;

    /**
     * Constructs a new writable {@code float} vector.
     * The given array is passed by reference and will be queried by calls to {@link #get(int)} and modified
     * by {@link #set(int, float)}.
     *
     * @param elements The underlying array of {@code float} elements.
     */
    public VectorImpl(float[] elements) {
        this.elements = elements;
        this.size = elements.length;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public float get(int index) {
        return elements[offset + index];
    }

    @Override
    public void set(int index, float element) {
        elements[offset + index] = element;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOfRange(elements, offset, offset + size));
    }

    public void setOffsetAndSize(int offset, int size) {
        this.offset = offset;
        this.size = size;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }
}
