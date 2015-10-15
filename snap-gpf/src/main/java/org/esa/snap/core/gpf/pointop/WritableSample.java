package org.esa.snap.core.gpf.pointop;

/**
 * A writable sample is a {@link Sample} that can change its value.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public interface WritableSample extends Sample {

    /**
     * Sets the sample at the given bit index to the given {@code bit} value.
     *
     * @param bitIndex The bit index. Valid range is zero to n-1, where n is 8, 16, 32, 64,
     *                 depending on the actual raster data type.
     * @param v        The new value.
     */
    void set(int bitIndex, boolean v);

    /**
     * Sets this sample to the given {@code boolean} value.
     *
     * @param v The new value.
     */
    void set(boolean v);

    /**
     * Sets this sample to the given {@code int} value.
     *
     * @param v The new value.
     */
    void set(int v);

    /**
     * Sets this sample to the given {@code float} value.
     *
     * @param v The new value.
     */
    void set(float v);

    /**
     * Sets this sample to the given {@code double} value.
     *
     * @param v The new value.
     */
    void set(double v);
}
