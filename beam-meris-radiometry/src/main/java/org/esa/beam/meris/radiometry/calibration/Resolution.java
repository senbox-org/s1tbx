package org.esa.beam.meris.radiometry.calibration;


public enum Resolution {

    FR(740 * 5),
    RR(185 * 5);

    private final int pixelCount;

    private Resolution(int pixelCount) {
        this.pixelCount = pixelCount;
    }

    int getPixelCount() {
        return pixelCount;
    }
}
