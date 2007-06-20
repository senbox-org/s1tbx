/*
 * $Id: VisibleRadianceCalibrator.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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

    public String getBandName() {
        return AvhrrConstants.RADIANCE_BAND_NAME_PREFIX + AvhrrConstants.CH_STRINGS[channel];
    }

    public String getBandUnit() {
        return AvhrrConstants.VIS_RADIANCE_UNIT;
    }

    public String getBandDescription() {
        return AvhrrReader.format(AvhrrConstants.RADIANCE_DESCRIPTION_VIS, AvhrrConstants.CH_STRINGS[channel]);
    }

    public float calibrate(int counts) {
        return radianceCalibrator.getConversionFactor() * super.calibrate(counts);
    }
}
