/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;

import org.esa.snap.core.datamodel.ProductData;

import java.awt.Rectangle;
import java.awt.geom.Area;

/**
 * A chunk / tile of the product to be written.
 *
 * @author MarcoZ
 */
public class Chunk {

    final Rectangle chunkRect;
    final ProductData chunkData;
    final Area subArea;

    public Chunk(Rectangle chunkRect, int type) {
        this.chunkRect = chunkRect;
        this.chunkData = ProductData.createInstance(type, chunkRect.width * chunkRect.height);
        this.subArea = new Area(chunkRect);
    }

    public void copyDataFrom(Rectangle dataRect, ProductData data) {
        Rectangle part = chunkRect.intersection(dataRect);
        for (int cy = part.y; cy < part.y + part.height; cy++) {
            int srcPos = (cy - dataRect.y) * dataRect.width + (part.x - dataRect.x);
            int destPos = (cy - chunkRect.y) * chunkRect.width + (part.x - chunkRect.x);
            System.arraycopy(data.getElems(), srcPos, chunkData.getElems(), destPos, part.width);
        }
        subArea.subtract(new Area(part));
    }

    public boolean complete() {
        return subArea.isEmpty();
    }

    public ProductData getData() {
        return chunkData;
    }
}
