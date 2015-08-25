package org.esa.snap.framework.datamodel;

public class CenterTimeCoding implements TimeCoding {

    private final double centerTime;

    public CenterTimeCoding(double firstTimeMJD, double secondTimeMJD ) {
        centerTime = (secondTimeMJD - firstTimeMJD) / 2.0 + firstTimeMJD;
    }

    @Override
    public double getMJD(PixelPos pixelPos) {
        return centerTime;
    }
}
