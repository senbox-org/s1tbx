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

package org.esa.snap.core.image;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;

import java.awt.Rectangle;
import java.io.IOException;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class TiePointGridOpImage extends RasterDataNodeOpImage {

    public TiePointGridOpImage(TiePointGrid band, ResolutionLevel level) {
        super(band, level);
    }

    public TiePointGrid getTiePointGrid() {
        return (TiePointGrid) getRasterDataNode();
    }

    @Override
    protected void computeProductData(ProductData productData, Rectangle destRect) throws IOException {
        if (getLevel() == 0) {
            getTiePointGrid().readPixels(destRect.x, destRect.y,
                                         destRect.width, destRect.height,
                                         (float[]) productData.getElems(),
                                         ProgressMonitor.NULL);
        } else {
            final int sourceWidth = getSourceWidth(destRect.width);
            final ProductData lineData = ProductData.createInstance(getTiePointGrid().getDataType(), sourceWidth);
            final int[] sourceCoords = getSourceCoords(sourceWidth, destRect.width);
            final int srcX = getSourceX(destRect.x);
            for (int y = 0; y < destRect.height; y++) {
                getTiePointGrid().readPixels(srcX,
                                             getSourceY(destRect.y + y),
                                             sourceWidth, 1,
                                             (float[]) lineData.getElems(),
                                             ProgressMonitor.NULL);
                copyLine(y, destRect.width, lineData, productData, sourceCoords);
            }
        }
    }

}
