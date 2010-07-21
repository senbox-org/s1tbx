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


/**
 * Computes radiances for the visual AVHRR channels 1, 2 and 3a.
 */
public class VisibleRadianceCalibrator extends ReflectanceFactorCalibrator {

    private Radiance2ReflectanceFactorCalibrator radianceCalibrator;

    public VisibleRadianceCalibrator(int channel) {
        super(channel);
    }

    public void setHeaderConstants(double equivalentWidth, double solarIrradiance, double earthSunDistance) {
    	radianceCalibrator = new Radiance2ReflectanceFactorCalibrator(equivalentWidth, solarIrradiance, earthSunDistance);
    }

    @Override
    public String getBandName() {
        return AvhrrConstants.RADIANCE_BAND_NAME_PREFIX + AvhrrConstants.CH_STRINGS[channel];
    }

    @Override
    public String getBandUnit() {
        return AvhrrConstants.VIS_RADIANCE_UNIT;
    }

    @Override
    public String getBandDescription() {
        return AvhrrReader.format(AvhrrConstants.RADIANCE_DESCRIPTION_VIS, AvhrrConstants.CH_STRINGS[channel]);
    }

    @Override
    public float calibrate(int counts) {
        return radianceCalibrator.getConversionFactor() * super.calibrate(counts);
    }
}
