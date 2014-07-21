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
package org.esa.beam.dataio.arcbin;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;


class GridTileOpImage extends SingleBandedOpImage {

    private final Header header;
    private final Dimension gridTileSize;
    private final GridTileProvider gridTileProvider;

    GridTileOpImage(int sourceWidth, int sourceHeight, Dimension imageTileSize,
                    int databufferType, ResolutionLevel level,
                    Header header, Dimension gridTileSize,
                    GridTileProvider gridTileProvider) {
        super(databufferType,
              sourceWidth,
              sourceHeight,
              imageTileSize,
              null, // no configuration
              level);
        this.header = header;
        this.gridTileSize = gridTileSize;
        this.gridTileProvider = gridTileProvider;
    }

    @Override
    protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        DataBuffer dataBuffer = targetRaster.getDataBuffer();

        int tileXStart = xToGridTileX(targetRaster.getMinX());
        int tileXEnd = xToGridTileX(targetRaster.getMinX() + targetRaster.getWidth() - 1);
        int tileYStart = yToGridTileY(targetRaster.getMinY());
        int tileYEnd = yToGridTileY(targetRaster.getMinY() + targetRaster.getHeight() - 1);

        int subsampling = (int) getScale();
        double tileStepY = Math.ceil(subsampling / (double) gridTileSize.height);
        double tileStepX = Math.ceil(subsampling / (double) gridTileSize.width);

        for (int tileY = tileYStart; tileY <= tileYEnd; tileY += tileStepY) {
            int sourceY = gridTileYToY(tileY);
            int tileIndexY = (sourceY / header.tileYSize) * header.tilesPerRow;

            int numTilesY = tileY - tileYStart;
            int numLines = (int) Math.ceil((numTilesY * gridTileSize.height) / (double) subsampling);
            int rasterOffsetY = numLines * targetRaster.getWidth();

            for (int tileX = tileXStart; tileX <= tileXEnd; tileX += tileStepX) {
                int sourceX = gridTileXToX(tileX);
                int gridTileIndex = (sourceX / header.tileXSize) + tileIndexY;
                ProductData data = gridTileProvider.getData(gridTileIndex);

                int numTilesX = tileX - tileXStart;
                int rasterOffset = rasterOffsetY + numTilesX * gridTileSize.width / subsampling;

                int writtenLines = 0;
                for (int y = 0; y < gridTileSize.height; y += subsampling) {
                    int targetIndex = rasterOffset + writtenLines * targetRaster.getWidth();
                    for (int x = 0; x < gridTileSize.width; x += subsampling) {
                        int sourceIndex = x + y * gridTileSize.width;
                        gridTileProvider.transferData(data, sourceIndex, dataBuffer, targetIndex);
                        targetIndex++;
                    }
                    writtenLines++;
                }
            }
        }
    }

    private int gridTileXToX(int tileX) {
        return tileX * gridTileSize.width;
    }

    private int gridTileYToY(int tileY) {
        return tileY * gridTileSize.height;
    }

    private int yToGridTileY(int targetY) {
        return getSourceY(targetY) / gridTileSize.height;
    }

    private int xToGridTileX(int targetX) {
        return getSourceX(targetX) / gridTileSize.width;
    }
}
