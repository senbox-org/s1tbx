package org.esa.beam.dataio.avhrr.noaa.pod;

/**
* @author Ralf Quast
*/
final class CoefficientCalibrator extends AbstractCalibrator {

    private final double slope;
    private final double intercept;

    CoefficientCalibrator(CalibratorFactory calibratorFactory, double slope, double intercept) {
        super(calibratorFactory);
        this.slope = slope;
        this.intercept = intercept;
    }

    @Override
    public float calibrate(int counts) {
        return (float) (slope * counts + intercept);
    }
}
