package org.esa.snap.binning;

/**
 * A writable vector of {@code float} elements.
 *
 * @author Norman Fomferra
 */
public interface WritableVector extends Vector {
    /**
     * Sets a new {@code float} element at the given index.
     *
     * @param index   The element index.
     * @param element The new {@code float} element.
     */
    void set(int index, float element);

}
