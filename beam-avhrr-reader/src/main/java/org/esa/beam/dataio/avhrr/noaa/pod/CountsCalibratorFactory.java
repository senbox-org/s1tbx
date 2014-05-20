package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.avhrr.calibration.Calibrator;

/**
* @author Ralf Quast
*/
final class CountsCalibratorFactory implements CalibratorFactory {

    private final int channelIndex;
    private final Calibrator calibrator;

    CountsCalibratorFactory(int channelIndex) {
        this.channelIndex = channelIndex;
        this.calibrator = new CountsCalibrator(this);
    }

    @Override
    public Calibrator createCalibrator(int i) {
        return calibrator;
    }

    @Override
    public String getBandName() {
        return "counts_" + (channelIndex + 1);
    }

    @Override
    public String getBandUnit() {
        return "counts";
    }

    @Override
    public String getBandDescription() {
        return "Raw AVHRR video data";
    }
}
