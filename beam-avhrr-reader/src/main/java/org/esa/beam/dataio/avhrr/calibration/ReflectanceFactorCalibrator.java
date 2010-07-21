/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.dataio.avhrr.calibration;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrReader;
import org.esa.beam.util.Guardian;

/**
 * Computes radiances for the visual AVHRR channels 1, 2 and 3a.
 */
public class ReflectanceFactorCalibrator extends AbstractCalibrator {
    private static final int[] OPERATIONAL_DATA_OFFSET = {0, 15, 30};

    private int operationalDataIndex;

    private double slope1;
    private double intercept1;
    private double slope2;
    private double intercept2;
    private int intersection;
    private boolean dataRequired;

    public ReflectanceFactorCalibrator(int channel) {
        super(channel);
        Guardian.assertWithinRange("channel", channel, AvhrrConstants.CH_1, AvhrrConstants.CH_3A);
        this.operationalDataIndex = channel;
        this.dataRequired = true;
    }

    @Override
    public String getBandName() {
        return AvhrrConstants.REFLECTANCE_BAND_NAME_PREFIX + AvhrrConstants.CH_STRINGS[channel];
    }

    @Override
    public String getBandUnit() {
        return AvhrrConstants.REFLECTANCE_UNIT;
    }

    @Override
    public String getBandDescription() {
        return AvhrrReader.format(AvhrrConstants.REFLECTANCE_FACTOR_DESCRIPTION, AvhrrConstants.CH_STRINGS[channel]);
    }

    @Override
    public boolean requiresCalibrationData() {
        return dataRequired;
    }

    @Override
    public boolean processCalibrationData(int[] calibrationData) {
        final int offset = OPERATIONAL_DATA_OFFSET[operationalDataIndex];
        boolean valid = calibrationData[offset + 0] != 0 &&
                calibrationData[offset + 1] != 0 &&
                calibrationData[offset + 2] != 0 &&
                calibrationData[offset + 3] != 0 &&
                calibrationData[offset + 4] != 0;
        if (valid) {
            slope1 = calibrationData[offset + 0] * 1E-7;
            intercept1 = calibrationData[offset + 1] * 1E-6;
            slope2 = calibrationData[offset + 2] * 1E-7;
            intercept2 = calibrationData[offset + 3] * 1E-6;
            intersection = calibrationData[offset + 4];
            dataRequired = false;
        }
        return valid;
    }

    @Override
    public float calibrate(int counts) {
        final double reflectanceFactor;
        if (counts <= intersection) {
            reflectanceFactor = counts * slope1 + intercept1;
        } else {
            reflectanceFactor = counts * slope2 + intercept2;
        }
        return (float) (reflectanceFactor);
    }
}
