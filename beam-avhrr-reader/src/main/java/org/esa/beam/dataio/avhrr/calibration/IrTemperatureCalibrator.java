/*
 * $Id: IrTemperatureCalibrator.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
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
 * Computes blackbody temperatures TE for the thermal AVHRR channels 3b, 4 and 5 from counts.
 */
public class IrTemperatureCalibrator extends IrRadianceCalibrator {

    private Radiance2TemperatureCalibrator radianceCalibrator;

    public IrTemperatureCalibrator(int channel) {
        super(channel);
    }

    public String getBandName() {
        return AvhrrConstants.TEMPERATURE_BAND_NAME_PREFIX + AvhrrConstants.CH_STRINGS[channel];
    }

    public String getBandUnit() {
        return AvhrrConstants.TEMPERATURE_UNIT;
    }

    public String getBandDescription() {
        return AvhrrReader.format(AvhrrConstants.TEMPERATURE_DESCRIPTION, AvhrrConstants.CH_STRINGS[channel]);
    }

    public void setHeaderConstants(double constant1, double constant2, double vc) {
    	radianceCalibrator = new Radiance2TemperatureCalibrator(constant1, constant2, vc);
    }

    public float calibrate(int counts) {
        final float radiance = super.calibrate(counts);
        return radianceCalibrator.calibrate(radiance);
    }
}
