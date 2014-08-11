package org.esa.snap.datamodel;

/**
 * Generic vector position
 */
public final class PosVector {
    public double x, y, z;

    public PosVector() {
    }

    public PosVector(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
