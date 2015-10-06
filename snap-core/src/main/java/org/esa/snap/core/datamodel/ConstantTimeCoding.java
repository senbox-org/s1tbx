package org.esa.snap.core.datamodel;

/**
 * Time coding which returns a constant value.
 */
public class ConstantTimeCoding implements TimeCoding {

    private final double time;

    public ConstantTimeCoding(double time) {
        this.time = time;
    }

    @Override
    public double getMJD(PixelPos pixelPos) {
        return time;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public PixelPos getPixelPos(double mjd, PixelPos pixelPos) {
        return new PixelPos(0, 0);
    }
}
