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
package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public class FlagReader implements BandReader {

    private AvhrrFile avhrrFile;

    private ImageInputStream inputStream;

    public FlagReader(AvhrrFile avhrrFile, ImageInputStream inputStream) {
        this.avhrrFile = avhrrFile;
        this.inputStream = inputStream;
    }

    @Override
    public String getBandName() {
        return AvhrrConstants.FLAGS_DS_NAME;
    }

    @Override
    public String getBandUnit() {
        return null;
    }

    @Override
    public String getBandDescription() {
        return null;
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_UINT8;
    }

    @Override
    public synchronized void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                   int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws
                                                                                                                 IOException {

        AvhrrFile.RawCoordinates rawCoord = avhrrFile.getRawCoordinates(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);

        final byte[] flagsData = (byte[]) destBuffer.getElems();

        int targetIdx = rawCoord.targetStart;
        pm.beginTask("Reading AVHRR band '" + getBandName() + "'...", rawCoord.maxY - rawCoord.minY);
        try {
            for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                final byte flag = readFlags(sourceY);
                for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                    flagsData[targetIdx] = flag;
                    targetIdx += rawCoord.targetIncrement;
                }
                pm.done();
            }
        } finally {
            pm.done();
        }

    }

    private byte readFlags(int rawY) throws IOException {
        long[] flags = new long[6];
        int dataSetOffset = avhrrFile.getFlagOffset(rawY);

        synchronized (inputStream) {
            inputStream.seek(dataSetOffset);
            flags[0] = inputStream.readUnsignedInt();   //Quality Indicator Bit Field
            flags[1] = inputStream.readUnsignedInt();   //Scan Line Quality Flags
            flags[2] = inputStream.readUnsignedShort(); //Calibration Quality Flags 3b
            flags[3] = inputStream.readUnsignedShort(); //Calibration Quality Flags 4
            flags[4] = inputStream.readUnsignedShort(); //Calibration Quality Flags 5
            flags[5] = inputStream.readUnsignedShort(); //Count of Bit Errors in Frame Sync
        }

        byte flag = 0;
        for (int i = 0; i < flags.length; i++) {
            flag |= (flags[i] != 0 ? 1 : 0) << i;
        }
        return flag;
    }
}
