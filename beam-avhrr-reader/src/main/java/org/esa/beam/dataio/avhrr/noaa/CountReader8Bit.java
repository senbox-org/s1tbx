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

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 14.06.2005
 * Time: 16:28:31
 * To change this template use File | Settings | File Templates.
 */
class CountReader8Bit extends CountReader {
    public static final int DATA_RECORD_LENGTH = 12288;

    private static final int SCAN_LINE_LENGTH = 2048 * 5;
    
    private byte[] scanLineBuffer;

    public CountReader8Bit(int channel, NoaaFile noaaFile, ImageInputStream inputStream, Calibrator calibrator) {
    	super(channel, noaaFile, inputStream, calibrator);
        scanLineBuffer = new byte[SCAN_LINE_LENGTH];
    }

    @Override
    protected void readData(int dataOffset) throws IOException {
        synchronized (inputStream) {
            inputStream.seek(dataOffset);
            inputStream.readFully(scanLineBuffer, 0, SCAN_LINE_LENGTH);
        }
        extractCounts(scanLineBuffer);
    }

    private void extractCounts(byte[] rawData) {
        int indexRaw = AvhrrConstants.CH_DATASET_INDEXES[channel];
        for (int i = 0; i < AvhrrConstants.RAW_SCENE_RASTER_WIDTH; i++) {
            lineOfCounts[i] = ((int) rawData[indexRaw]) & 0xff;
            indexRaw += 5;
        }
    }
}
