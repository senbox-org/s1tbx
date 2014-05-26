package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.avhrr.calibration.Calibrator;

import java.io.IOException;

/**
* @author Ralf Quast
*/
interface CalibratorFactory {

    Calibrator createCalibrator(int i) throws IOException;

    String getBandName();

    String getBandUnit();

    String getBandDescription();
}
