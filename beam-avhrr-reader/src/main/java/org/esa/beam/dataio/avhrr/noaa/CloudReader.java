/*
 * $Id: CloudReader.java,v 1.3 2007/03/22 09:17:00 ralf Exp $
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
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision$ $Date$
 */
public class CloudReader implements BandReader {

    private NoaaFile noaaFile;

    private ImageInputStream inputStream;

    private byte[] rawBuffer;

    private byte[] flagBuffer;

    public CloudReader(NoaaFile noaaFile, ImageInputStream inputStream) {
        this.noaaFile = noaaFile;
        this.inputStream = inputStream;
        rawBuffer = new byte[512];
        flagBuffer = new byte[2048];
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

    public float getScalingFactor() {
        return 1f;
    }

    public void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                   int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws
                                                                                                                 IOException {

        AvhrrFile.RawCoordinates rawCoord = noaaFile.getRawCoordiantes(
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
        final int offset = noaaFile.getScanLineOffset(sourceY) + 14976;
        boolean hasCloud = false;
        synchronized (inputStream) {
            inputStream.seek(offset);
            final long cloudFlag = inputStream.readUnsignedInt();
            if (cloudFlag == 1) {
                hasCloud = true;
            }
        }
        return hasCloud;
    }

    private void readClouds(int sourceY) throws IOException {
        final int offset = noaaFile.getScanLineOffset(sourceY) + 14984;
        synchronized (inputStream) {
            inputStream.seek(offset);
            inputStream.read(rawBuffer);
        }
        for (int i = 0; i < flagBuffer.length; i++) {
            flagBuffer[i] = (byte) (rawBuffer[i / 4] >> (3 - (i % 4)) & 3);
        }
    }
}
