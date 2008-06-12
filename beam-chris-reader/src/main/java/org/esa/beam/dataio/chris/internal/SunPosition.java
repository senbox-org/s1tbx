package org.esa.beam.dataio.chris.internal;

/**
 * Class representing the angular position of the Sun.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SunPosition {

    private double aa;
    private double za;

    public SunPosition(final double za, final double aa) {
        if (aa < 0.0) {
            throw new IllegalArgumentException("aa < 0.0");
        }
        if (aa > 360.0) {
            throw new IllegalArgumentException("aa > 360.0");
        }
        if (za < 0.0) {
            throw new IllegalArgumentException("za < 0.0");
        }
        if (za > 180.0) {
            throw new IllegalArgumentException("za > 180.0");
        }

        this.aa = aa;
        this.za = za;
    }

    public final double getAzimuthAngle() {
        return aa;
    }

    public final double getZenithAngle() {
        return za;
    }
}
