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


class CountReader8Bit extends CountReader {
    private byte[] scanLineBuffer;

    public CountReader8Bit(int channel, KlmAvhrrFile noaaFile, Calibrator calibrator, int elementCount, int dataWidth) {
    	super(channel, noaaFile, calibrator, dataWidth);
        scanLineBuffer = new byte[elementCount];
    }

    @Override
    protected void readData(int rawY) throws IOException {
        CompoundData dataRecord = noaaFile.getDataRecord(rawY);
        SequenceData avhrr_sensor_data = dataRecord.getSequence("AVHRR_SENSOR_DATA");
        for (int i = 0; i < scanLineBuffer.length; i++) {
            scanLineBuffer[i] = avhrr_sensor_data.getByte(i);
        }
        extractCounts(scanLineBuffer);
    }

    private void extractCounts(byte[] rawData) {
        int indexRaw = AvhrrConstants.CH_DATASET_INDEXES[channel];
        for (int i = 0; i < lineOfCounts.length; i++) {
            lineOfCounts[i] = ((int) rawData[indexRaw]) & 0xff;
            indexRaw += 5;
        }
    }
}
