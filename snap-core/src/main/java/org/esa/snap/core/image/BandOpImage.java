/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

import javax.media.jai.PlanarImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class BandOpImage extends RasterDataNodeOpImage {

    public BandOpImage(Band band) {
        this(band, ResolutionLevel.MAXRES);
    }

    public BandOpImage(Band band, ResolutionLevel level) {
        super(band, level);
        if (Boolean.getBoolean("snap.imageManager.disableSourceTileCaching")) {
            setTileCache(null);
        }
    }

    public Band getBand() {
        return (Band) getRasterDataNode();
    }

    @Override
    protected void computeProductData(ProductData destData, Rectangle destRect) throws IOException {
        Band band = getBand();
        if (band.getProductReader() == null) {
            throw new IllegalStateException("no product reader for band '" + band.getDisplayName() + "'");
        }
        if (getLevel() == 0) {
            band.getProductReader().readBandRasterData(band, destRect.x, destRect.y,
                                                       destRect.width, destRect.height,
                                                       destData, ProgressMonitor.NULL);
        } else {
            readHigherLevelData(band, destData, destRect, getLevelImageSupport());
        }
    }

    private static void readHigherLevelData(Band band, ProductData destData, Rectangle destRect, LevelImageSupport lvlSupport) throws IOException {
        final int sourceWidth = lvlSupport.getSourceWidth(destRect.width);
        final int sourceHeight = lvlSupport.getSourceHeight(destRect.height);
        final int srcX = lvlSupport.getSourceX(destRect.x);
        final int srcY = lvlSupport.getSourceY(destRect.y);

        final MultiLevelImage img = band.getSourceImage();
        final int tileGridXOffset = img.getTileGridXOffset();
        final int tileWidth = img.getTileWidth();

        Point[] tileIndices = img.getTileIndices(new Rectangle(srcX, srcY, sourceWidth, sourceHeight));
        HashMap<Rectangle, ProductData> tileMap = new HashMap<>();
        for (Point tileIndex : tileIndices) {
            Rectangle tileRect = img.getTileRect(tileIndex.x, tileIndex.y);
            if (tileRect.isEmpty()) {
                continue;
            }
            final ProductData tileData = ProductData.createInstance(band.getDataType(), tileRect.width * tileRect.height);
            band.readRasterData(tileRect.x, tileRect.y, tileRect.width, tileRect.height, tileData, ProgressMonitor.NULL);
            tileMap.put(tileRect, tileData);
        }

        final int srcWidth = lvlSupport.getSourceWidth()-1;

        for (int y = 0; y < destRect.height; y++) {
            final int currentSrcYOffset = lvlSupport.getSourceY(destRect.y + y);
            int currentDestYOffset = y * destRect.width;
            int tileY = img.YToTileY(currentSrcYOffset);
            for (int x = 0; x < destRect.width; x++) {
                int sourceX =  lvlSupport.getSourceCoord(destRect.x + x, 0, srcWidth);
                Rectangle tileRect = img.getTileRect(PlanarImage.XToTileX(sourceX, tileGridXOffset, tileWidth), tileY);
                int currentX = sourceX - tileRect.x;
                int currentY = currentSrcYOffset - tileRect.y;
                double value = tileMap.get(tileRect).getElemDoubleAt(currentY * tileRect.width + currentX);

                destData.setElemDoubleAt(currentDestYOffset + x, value);
            }
        }
    }
}
