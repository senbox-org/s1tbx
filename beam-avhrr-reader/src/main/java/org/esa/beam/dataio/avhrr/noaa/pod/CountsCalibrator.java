package org.esa.beam.dataio.avhrr.noaa.pod;

/**
* @author Ralf Quast
*/
final class CountsCalibrator extends AbstractCalibrator {

    CountsCalibrator(CalibratorFactory calibratorFactory) {
        super(calibratorFactory);
    }

    @Override
    public float calibrate(int counts) {
        return counts;
    }

}
