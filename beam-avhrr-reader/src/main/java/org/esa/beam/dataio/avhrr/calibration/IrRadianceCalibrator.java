/*
 * $Id: IrRadianceCalibrator.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
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
import org.esa.beam.util.Guardian;

/**
 * Computes the "Earth scene radiances" NE for the thermal AVHRR channels 3b, 4 and 5.
 */
public class IrRadianceCalibrator extends AbstractCalibrator {
    private static final int[] OPERATIONAL_DATA_OFFSET = {45, 51, 57};
    private static final float[] SCALING_FACTORS_666 = {1E-6f, 1E-6f, 1E-6f};
    private static final float[] SCALING_FACTORS_667 = {1E-6f, 1E-6f, 1E-7f};
    private int operationalDataIndex;
    private float[] scalingFactors;
    private double a1;
    private double a2;
    private double a3;

    public IrRadianceCalibrator(int channel) {
        super(channel);
        Guardian.assertWithinRange("channel", channel, AvhrrConstants.CH_3B, AvhrrConstants.CH_5);
        this.operationalDataIndex = channel - AvhrrConstants.CH_3B;
    }

    @Override
    public String getBandName() {
        return AvhrrConstants.RADIANCE_BAND_NAME_PREFIX + AvhrrConstants.CH_STRINGS[channel];
    }

    @Override
    public String getBandUnit() {
        return AvhrrConstants.IR_RADIANCE_UNIT;
    }

    @Override
    public String getBandDescription() {
        return AvhrrReader.format(AvhrrConstants.RADIANCE_DESCRIPTION_IR, AvhrrConstants.CH_STRINGS[channel]);
    }

    @Override
    public boolean requiresCalibrationData() {
        return true;
    }

    @Override
    public boolean processCalibrationData(int[] calibrationData) {
        final int offset = OPERATIONAL_DATA_OFFSET[operationalDataIndex];
        // coeff 3 is zero for channel 3b, so it is not included in this check
        boolean valid = calibrationData[offset] != 0 && calibrationData[offset + 1] != 0;
        if (valid) {
            a1 = calibrationData[offset]     * scalingFactors[0];
            a2 = calibrationData[offset + 1] * scalingFactors[1];
            a3 = calibrationData[offset + 2] * scalingFactors[2];
        }
        return valid;
    }

    @Override
    public float calibrate(int counts) {
        final double ne = a1 + a2 * counts + a3 * counts * counts;
        return (float) ne;
    }

    /**
     * Sets the format version of the NOAA L1b product.
     * Unsupported version are at the parsing stage recognized.
     *
     * @param formatVersion
     */
    public void setFormatVersion(int formatVersion) {
        if (formatVersion == 2 ||
                (formatVersion == 3 && channel == AvhrrConstants.CH_3B )) {
            scalingFactors = SCALING_FACTORS_666;
        } else{
            scalingFactors = SCALING_FACTORS_667;
        }
    }
}