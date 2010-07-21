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

package org.esa.beam.framework.datamodel;

import javax.media.jai.PixelAccessor;
import java.awt.image.Raster;
import java.awt.Rectangle;

interface StxOp {
    String getName();

    void accumulateDataUByte(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataByte(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataUShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataInt(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataUInt(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataFloat(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

    void accumulateDataDouble(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r);

}
