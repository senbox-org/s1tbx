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

package org.esa.beam.dataio.landsat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * The class <code>LandsatByteBandReader</code> is an implementation of the abstract LandsatBandReader
 * class to be able to read in byte oriented band data
 */
public final class LandsatByteBandReader extends LandsatBandReader {

    private static final int pixelSize = 1;

    /**
     * @param width
     * @param bandName
     * @param stream
     */
    public LandsatByteBandReader(final int width, final String bandName,
                                 final ImageInputStream stream) {
        super(width, bandName, stream);
    }

    @Override
    void readBandData(final int sourceOffsetX,
                      final int sourceOffsetY,
                      final int sourceWidth,
                      final int sourceHeight,
                      final int sourceStepX,
                      final int sourceStepY,
                      final int destOffsetX,
                      final int destOffsetY,
                      final int destWidth,
                      final int destHeight,
                      final ProductData destBuffer,
                      ProgressMonitor pm) throws IOException {

        setStreamPos(sourceOffsetX, sourceOffsetY, pixelSize);

        final byte[] targetData = (byte[]) destBuffer.getElems();
        final byte[] line = new byte[sourceWidth];

        int targetIdx = 0;

        pm.beginTask("Reading band '" + getBandName() + "'...", sourceHeight - 1);
        try {
            for (int y = 0; y < sourceHeight; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                stream.readFully(line, 0, line.length);

                for (int x = 0; x < sourceWidth; x += sourceStepX) {
                    targetData[targetIdx] = line[x];
                    ++targetIdx;
                }
                updateStreamPos(width - sourceWidth, pixelSize);

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
