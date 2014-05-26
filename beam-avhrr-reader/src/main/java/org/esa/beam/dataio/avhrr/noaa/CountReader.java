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

package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

abstract class CountReader implements BandReader {

    protected final Calibrator calibrator;

    protected final int[] calibrationData;

    protected final int[] lineOfCounts;

    protected final int channel;

    protected final KlmAvhrrFile noaaFile;

    public CountReader(int channel, KlmAvhrrFile noaaFile, Calibrator calibrator, int dataWidth) {
        this.channel = channel;
        this.noaaFile = noaaFile;
        this.calibrator = calibrator;
        calibrationData = new int[AvhrrConstants.CALIB_COEFF_LENGTH];
        lineOfCounts = new int[dataWidth];
    }

    @Override
    public String getBandName() {
        return calibrator.getBandName();
    }

    @Override
    public String getBandUnit() {
        return calibrator.getBandUnit();
    }

    @Override
    public String getBandDescription() {
        return calibrator.getBandDescription();
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    @Override
    public synchronized void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                   int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws
                                                                                                                 IOException {

        AvhrrFile.RawCoordinates rawCoord = noaaFile.getRawCoordinates(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);

        final float[] targetData = (float[]) destBuffer.getElems();

        int targetIdx = rawCoord.targetStart;
        pm.beginTask("Reading AVHRR band '" + getBandName() + "'...", rawCoord.maxY - rawCoord.minY);
        try {
            for (int rawY = rawCoord.minY; rawY <= rawCoord.maxY; rawY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                boolean validData = hasData(rawY);
                if (validData) {
                    if (calibrator.requiresCalibrationData()) {
                        readCalibrationCoefficients(rawY, calibrationData);
                        validData = calibrator.processCalibrationData(calibrationData);
                    }
                    if (validData) {
                        readData(rawY);
                        validData = containsValidCounts();
                        if (validData) {
                            for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                                targetData[targetIdx] = calibrator.calibrate(lineOfCounts[sourceX]);
                                targetIdx += rawCoord.targetIncrement;
                            }
                        }
                    }
                }
                if (!validData) {
                    for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                        targetData[targetIdx] = AvhrrConstants.NO_DATA_VALUE;
                        targetIdx += rawCoord.targetIncrement;
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private boolean hasData(int rawY) throws IOException {
        if (channel != AvhrrConstants.CH_3A && channel != AvhrrConstants.CH_3B) {
            return true;
        }
        final int bitField = noaaFile.getScanlineBitfield(rawY);
        final int channel3ab = bitField & 3;
        return (channel3ab == 1 && channel == AvhrrConstants.CH_3A)
               || (channel3ab == 0 && channel == AvhrrConstants.CH_3B);
    }

    private boolean containsValidCounts() {
        for (final int i : lineOfCounts) {
            if (i <= 0 || i >= 1024) {
                return false;
            }
        }
        return true;
    }

    private void readCalibrationCoefficients(int rawY, int[] calibCoeff) throws IOException {
        CompoundData dataRecord = noaaFile.getDataRecord(rawY);
        SequenceData calibration_coefficients = dataRecord.getSequence("CALIBRATION_COEFFICIENTS");
        for (int i = 0; i < calibCoeff.length; i++) {
            calibCoeff[i] = calibration_coefficients.getInt(i);
        }
    }

    protected abstract void readData(int rawY) throws IOException;
}
