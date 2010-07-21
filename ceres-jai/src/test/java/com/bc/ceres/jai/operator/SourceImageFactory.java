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

package com.bc.ceres.jai.operator;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;

class SourceImageFactory {
    static BufferedImage createOneBandedUShortImage(int w, int h, short[] data) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        System.arraycopy(data, 0, buffer.getData(), 0, w * h);
        return image;
    }
}
