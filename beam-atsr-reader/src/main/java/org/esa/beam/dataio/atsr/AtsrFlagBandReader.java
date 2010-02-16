/*
 * $Id: AtsrFlagBandReader.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.atsr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * This class is responsible for reading ERS ATSR flag bands.
 */
public class AtsrFlagBandReader extends AtsrBandReader {

    static final int _pixelSize = 2;

    /**
     * Creates the object with given band name, file offset conversion multiplier and file stream.
     */
    AtsrFlagBandReader(String bandName, int offset, float mult, ImageInputStream stream) {
        super(bandName, offset, mult, stream);
    }

    @Override
    synchronized void readBandData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                   int sourceStepX,
                                   int sourceStepY, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                   ProductData destBuffer, ProgressMonitor pm) throws IOException {
        setStreamPos(sourceOffsetX, sourceOffsetY, _pixelSize);

        short[] targetData = (short[]) destBuffer.getElems();
        short[] line = new short[sourceWidth];
        int targetIdx = 0;

        pm.beginTask("Reading band '" + getBandName() + "'...", sourceHeight);
        // loop over lines
        try {
            for (int y = 0; y < sourceHeight; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                _stream.readFully(line, 0, line.length);

                // convert line - and eventually subsample
                for (int x = 0; x < sourceWidth; x += sourceStepX) {

                    targetData[targetIdx] = line[x];

                    ++targetIdx;
                }
                updateStreamPos(AtsrConstants.ATSR_SCENE_RASTER_WIDTH - sourceWidth, _pixelSize);

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
