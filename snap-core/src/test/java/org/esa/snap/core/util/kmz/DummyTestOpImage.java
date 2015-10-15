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

package org.esa.snap.core.util.kmz;

import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.image.DataBuffer;

class DummyTestOpImage extends SourcelessOpImage {

    DummyTestOpImage(int width, int height) {
        super(ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_BYTE, width, height, width, height),
              null,
              ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_BYTE, width, height),
              0, 0, width, height);
    }

    @Override
    protected void computeRect(PlanarImage[] sources, java.awt.image.WritableRaster dest,
                               java.awt.Rectangle destRect) {
        double[] value = new double[1];
        for (int y = 0; y < destRect.height; y++) {
            for (int x = 0; x < destRect.width; x++) {
                value[0] = x + y;
                dest.setPixel(x, y, value);
            }
        }
    }
}
