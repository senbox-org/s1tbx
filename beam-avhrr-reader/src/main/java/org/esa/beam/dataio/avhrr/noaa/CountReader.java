/*
 * $Id: CountReader.java,v 1.3 2007/03/22 09:17:00 ralf Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

abstract class CountReader implements BandReader {

    protected static final int DATA_OFFSET = 1264;

    protected Calibrator calibrator;

    protected int[] calibrationData;

    protected ImageInputStream inputStream;

    protected int[] lineOfCounts;

    protected int channel;

    private NoaaFile noaaFile;

    public CountReader(int channel, NoaaFile noaaFile,
                       ImageInputStream inputStream, Calibrator calibrator) {
        this.channel = channel;
        this.noaaFile = noaaFile;
        this.inputStream = inputStream;
        this.calibrator = calibrator;
        calibrationData = new int[AvhrrConstants.CALIB_COEFF_LENGTH];
        lineOfCounts = new int[AvhrrConstants.RAW_SCENE_RASTER_WIDTH];
    }

    public String getBandName() {
        return calibrator.getBandName();
    }

    public String getBandUnit() {
        return calibrator.getBandUnit();
    }

    public String getBandDescription() {
        return calibrator.getBandDescription();
    }

    public float getScalingFactor() {
        return 1f;
    }

    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    public void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                   int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws
                                                                                                                 IOException {

        AvhrrFile.RawCoordinates rawCoord = noaaFile.getRawCoordiantes(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);

        final float[] targetData = (float[]) destBuffer.getElems();

        int targetIdx = rawCoord.targetStart;
        pm.beginTask("Reading AVHRR band '" + getBandName() + "'...", rawCoord.maxY - rawCoord.minY);
        try {
            for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                boolean validData = hasData(sourceY);
                if (validData) {
                    if (calibrator.requiresCalibrationData()) {
                        readCalibCoefficients(sourceY, calibrationData);
                        validData = calibrator.processCalibrationData(calibrationData);
                    }
                    if (validData) {
                        final int dataOffset = getScanLineDataOffset(sourceY);
                        readData(dataOffset);
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
        final int bitField = noaaFile.readScanLineBitField(rawY);
        final int channel3ab = bitField & 3;
        return (channel3ab == 1 && channel == AvhrrConstants.CH_3A)
               || (channel3ab == 0 && channel == AvhrrConstants.CH_3B);
    }

    private boolean containsValidCounts() {
        for (int i = 0; i < AvhrrConstants.RAW_SCENE_RASTER_WIDTH; i++) {
            if (lineOfCounts[i] <= 0 || lineOfCounts[i] >= 1024) {
                return false;
            }
        }
        return true;
    }

    private void readCalibCoefficients(int rawY, int[] calibCoeff)
            throws IOException {
        int calOffset = noaaFile.getScanLineOffset(rawY)
                        + AvhrrConstants.CALIB_COEFF_OFFSET;
        synchronized (inputStream) {
            inputStream.seek(calOffset);
            inputStream.readFully(calibCoeff, 0,
                                  AvhrrConstants.CALIB_COEFF_LENGTH);
        }
    }

    private int getScanLineDataOffset(int rawY) {
        return noaaFile.getScanLineOffset(rawY) + DATA_OFFSET;
    }

    protected abstract void readData(int dataOffset) throws IOException;
}
