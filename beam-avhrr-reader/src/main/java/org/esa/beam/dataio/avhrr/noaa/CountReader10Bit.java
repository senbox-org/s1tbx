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
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;

import java.io.IOException;


class CountReader10Bit extends CountReader {
    private static final int TEN_BITS = 0b1111111111;

    private static final int[] FIRST = {0, 0, 0, 1, 1};
    private static final int[][] INCREMENT = {{1, 2, 2}, {2, 1, 2}, {2, 2, 1}, {1, 2, 2}, {2, 1, 2}};
    private static final int[][] SHIFT = {{20, 0, 10}, {10, 20, 0}, {0, 10, 20}, {20, 0, 10}, {10, 20, 0}};

    private int[] scanLineBuffer;
    private final int elementCount;

    public CountReader10Bit(int channel, KlmAvhrrFile noaaFile, Calibrator calibrator, int elementCount, int dataWidth) {
    	super(channel, noaaFile, calibrator, dataWidth);
        this.elementCount = elementCount;
        scanLineBuffer = new int[elementCount];
    }

    @Override
    protected void readData(int rawY) throws IOException {
        CompoundData dataRecord = noaaFile.getDataRecord(rawY);
        SequenceData avhrr_sensor_data = dataRecord.getSequence("AVHRR_SENSOR_DATA");
        for (int i = 0; i < scanLineBuffer.length; i++) {
            scanLineBuffer[i] = avhrr_sensor_data.getInt(i);
        }
        extractCounts(scanLineBuffer);
    }

    private void extractCounts(int[] rawData) {
        int j = 0;
        int bandNo = AvhrrConstants.CH_DATASET_INDEXES[channel];
        int indexRaw = FIRST[bandNo];
        for (int i = 0; i < lineOfCounts.length; i++) {
            lineOfCounts[i] = (rawData[indexRaw] & (TEN_BITS << SHIFT[bandNo][j])) >> SHIFT[bandNo][j];
            indexRaw += INCREMENT[bandNo][j];
            j = j == 2 ? 0 : j + 1;
        }
    }

    /**
     * The same as {@link #extractCounts(int[])}  but better readable ;-)
     * However this method documents much more clearly the algorithm used for the 10-bit decoding.
     * Although this method is unused, DO NOT REMOVE IT!
     */
    private void extractCountsSlowButSimple(int[] rawData) {
        int bandNo = AvhrrConstants.CH_DATASET_INDEXES[channel];
        int indexInBand = 0;
        int bandNum = 0;
        int c[] = new int[3];
        for (int i = 0; i < elementCount; i++) {

            int rawValue = rawData[i];
            c[0] = (rawValue & (TEN_BITS << 20)) >> 20;
            c[1] = (rawValue & (TEN_BITS << 10)) >> 10;
            c[2] = (rawValue & TEN_BITS);

            for (int ci = 0; ci < 3; ci++) {
                if (bandNum == bandNo) {
                    lineOfCounts[indexInBand] = c[ci];
                }
                bandNum++;
                if (bandNum == 5) {
                    bandNum = 0;
                    indexInBand++;
                }
                if (i == elementCount - 1) {
                    break;
                }
            }
        }
    }

}
