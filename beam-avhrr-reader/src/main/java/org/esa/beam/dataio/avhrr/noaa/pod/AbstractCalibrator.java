package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.avhrr.calibration.Calibrator;

/**
 * @author Ralf Quast
 */
abstract class AbstractCalibrator implements Calibrator {

    private final CalibratorFactory calibratorFactory;

    protected AbstractCalibrator(CalibratorFactory calibratorFactory) {
        this.calibratorFactory = calibratorFactory;
    }

    @Override
    public final String getBandName() {
        return calibratorFactory.getBandName();
    }

    @Override
    public final String getBandUnit() {
        return calibratorFactory.getBandName();
    }

    @Override
    public final String getBandDescription() {
        return calibratorFactory.getBandDescription();
    }

    @Override
    public final boolean processCalibrationData(int[] calibrationData) {
        return false;
    }

    @Override
    public final boolean requiresCalibrationData() {
        return false;
    }
}
