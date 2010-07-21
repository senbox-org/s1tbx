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

import java.io.IOException;

import javax.imageio.stream.ImageInputStream;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;


class CountReader10Bit extends CountReader {
    public static final int DATA_RECORD_LENGTH = 15872;

    private static final int SCAN_LINE_LENGTH = 3414;
    private static final int[] first = {0, 0, 0, 1, 1};
    private static final int[][] increment = {{1, 2, 2}, {2, 1, 2}, {2, 2, 1}, {1, 2, 2}, {2, 1, 2}};
    private static final int[][] shift = {{20, 0, 10}, {10, 20, 0}, {0, 10, 20}, {20, 0, 10}, {10, 20, 0}};

    private int[] scanLineBuffer;

    public CountReader10Bit(int channel, NoaaFile noaaFile, ImageInputStream inputStream, Calibrator calibrator) {
    	super(channel, noaaFile, inputStream, calibrator);
        scanLineBuffer = new int[SCAN_LINE_LENGTH];
    }

    @Override
    protected void readData(int dataOffset) throws IOException {
        synchronized (inputStream) {
            inputStream.seek(dataOffset);
            inputStream.readFully(scanLineBuffer, 0, SCAN_LINE_LENGTH);
        }
        extractCounts(scanLineBuffer);
    }

    private void extractCounts(int[] rawData) {
        int j = 0;
        int bandNo = AvhrrConstants.CH_DATASET_INDEXES[channel];
        int indexRaw = first[bandNo];
        for (int i = 0; i < AvhrrConstants.RAW_SCENE_RASTER_WIDTH; i++) {
            lineOfCounts[i] = (rawData[indexRaw] & (0x3FF << shift[bandNo][j])) >> shift[bandNo][j];
            indexRaw += increment[bandNo][j];
            j = j == 2 ? 0 : j + 1;
        }
    }

    /**
     * The same as {@link #extractCounts(int, int[], int[])}  but better readable ;-)
     * However this method documents much more clearly the algorithm used for the 10-bit decoding.
     * Although this method is unused, DO NOT REMOVE IT!
     */
    private void extractCountsSlowButSimple(int[] rawData) {
        int bandNo = AvhrrConstants.CH_DATASET_INDEXES[channel];
        int indexInBand = 0;
        int bandNum = 0;
        int c[] = new int[3];
        for (int i = 0; i < SCAN_LINE_LENGTH; i++) {

            int rawValue = rawData[i];
            c[0] = (rawValue & (0x3FF << 20)) >> 20;
            c[1] = (rawValue & (0x3FF << 10)) >> 10;
            c[2] = (rawValue & 0x3FF);

            for (int ci = 0; ci < 3; ci++) {
                if (bandNum == bandNo) {
                    lineOfCounts[indexInBand] = c[ci];
                }
                bandNum++;
                if (bandNum == 5) {
                    bandNum = 0;
                    indexInBand++;
                }
                if (i == SCAN_LINE_LENGTH - 1) {
                    break;
                }
            }
        }
    }

}
