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
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

class CloudReader implements BandReader {

    private KlmAvhrrFile noaaFile;

    private byte[] rawBuffer;

    private byte[] flagBuffer;

    public CloudReader(KlmAvhrrFile noaaFile) {
        this.noaaFile = noaaFile;
        ProductFormat productFormat = noaaFile.getProductFormat();
        int dataWidth = productFormat.getProductDimension().getDataWidth();
        int cloudBytes = productFormat.getProductDimension().getCloudBytes();
        rawBuffer = new byte[cloudBytes];
        flagBuffer = new byte[dataWidth];
    }

    public String getBandDescription() {
        return "CLAVR-x cloud mask";
    }

    public String getBandName() {
        return "cloudFlag";
    }

    public String getBandUnit() {
        return null;
    }

    public int getDataType() {
        return ProductData.TYPE_UINT8;
    }

    public double getScalingFactor() {
        return 1f;
    }

    public synchronized void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                                int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws
            IOException {

        AvhrrFile.RawCoordinates rawCoord = noaaFile.getRawCoordinates(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);

        final byte[] flagsData = (byte[]) destBuffer.getElems();

        pm.beginTask("Reading AVHRR band '" + getBandName() + "'...", rawCoord.maxY - rawCoord.minY);   /*I18N*/
        int targetIdx = rawCoord.targetStart;
        try {
            for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                if (hasClouds(sourceY)) {
                    readClouds(sourceY);
                    for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                        flagsData[targetIdx] = flagBuffer[sourceX];
                        targetIdx += rawCoord.targetIncrement;
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

    }

    private boolean hasClouds(int sourceY) throws IOException {
        CompoundData dataRecord = noaaFile.getDataRecord(sourceY);
        int cloudFlag = dataRecord.getInt("CLAVR_STATUS_BIT_FIELD");
        return cloudFlag == 1;
    }

    private void readClouds(int sourceY) throws IOException {
        CompoundData dataRecord = noaaFile.getDataRecord(sourceY);
        SequenceData ccm = dataRecord.getSequence("CCM");
        for (int i = 0; i < rawBuffer.length; i++) {
            rawBuffer[i] = ccm.getByte(i);
        }
        for (int i = 0; i < flagBuffer.length; i++) {
            flagBuffer[i] = (byte) (rawBuffer[i / 4] >> (3 - (i % 4)) & 3);
        }
    }
}
