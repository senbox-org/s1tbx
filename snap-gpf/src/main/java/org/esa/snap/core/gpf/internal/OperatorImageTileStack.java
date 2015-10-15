/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class overrides computeTile in order to synchronise it for a given tile index.
 * This is required, in order to avoid parallel computation of tiles belonging to the same tile stack.
 */
public class OperatorImageTileStack extends OperatorImage {

    /**
     * The array of locks is the same for all images contributing to a given tile stack.
     */
    private final Object[][] locks;

    public OperatorImageTileStack(Band targetBand, OperatorContext operatorContext, Object[][] locks) {
        super(targetBand, operatorContext);
        this.locks = locks;
    }


    // CHECK: Check whether this is an option to avoid removing single tiles from a previously computed tile stack
    /*
    @Override
    public Raster getTile(int tileX, int tileY) {
        Raster tile = getOperatorContext().getTileFromLocalCache(getTargetBand(), tileX, tileY);
        if (tile != null) {
            return tile;
        }
        return super.getTile(tileX, tileY);
    }
    */

    @Override
    public Raster computeTile(int tileX, int tileY) {
        // Lock to prevent multiple simultaneous computations.
        // Q: Why should multiple threads want to compute the same tile index?
        // A:
        // todo - check: can we avoid waiting here?
        synchronized (locks[tileX][tileY]) {
            Raster tileFromCache = getTileFromCache(tileX, tileY);
            if (tileFromCache != null) {
                return tileFromCache;
            } else {
                /* Create a new WritableRaster to represent this tile. */
                Point location = new Point(tileXToX(tileX), tileYToY(tileY));
                WritableRaster dest = createWritableRaster(sampleModel, location);

                /* Clip output rectangle to image bounds. */
                Rectangle rect = new Rectangle(location.x, location.y,
                                               sampleModel.getWidth(),
                                               sampleModel.getHeight());
                Rectangle destRect = rect.intersection(getBounds());
                computeRect((PlanarImage[]) null, dest, destRect);
                return dest;
            }
        }
    }

    @Override
    protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {

        final OperatorContext operatorContext = getOperatorContext();
        operatorContext.executeOperator(ProgressMonitor.NULL);

        long startNanos = System.nanoTime();

        Band[] targetBands = operatorContext.getTargetProduct().getBands();
        Map<Band, Tile> targetTiles = new HashMap<Band, Tile>(targetBands.length * 2);
        Map<Band, WritableRaster> writableRasters = new HashMap<Band, WritableRaster>(targetBands.length);

        for (Band band : targetBands) {
            if (band == getTargetBand() || operatorContext.isComputingImageOf(band)) {
                WritableRaster tileRaster = getWritableRaster(band, tile);
                writableRasters.put(band, tileRaster);
                Tile targetTile = createTargetTile(band, tileRaster, destRect);
                targetTiles.put(band, targetTile);
            } else if (requiresAllBands()) {
                Tile targetTile = operatorContext.getSourceTile(band, destRect);
                targetTiles.put(band, targetTile);
            }
        }

        operatorContext.startWatch();
        operatorContext.getOperator().computeTileStack(targetTiles, destRect, ProgressMonitor.NULL);
        operatorContext.stopWatch();
//        long nettoNanos = operatorContext.getNettoTime();

        final int tileX = XToTileX(destRect.x);
        final int tileY = YToTileY(destRect.y);
        for (Entry<Band, WritableRaster> entry : writableRasters.entrySet()) {
            Band band = entry.getKey();
            WritableRaster writableRaster = entry.getValue();
            // casting to access "addTileToCache" method
            OperatorImageTileStack operatorImage = (OperatorImageTileStack) operatorContext.getTargetImage(band);
            //put raster into cache after computing them.
            operatorImage.addTileToCache(tileX, tileY, writableRaster);

            // CHECK: Check whether this is an option to avoid removing single tiles from a previously computed tile stack
            /*
            getOperatorContext().addTileToLocalCache(band, tileX, tileY, writableRaster);
            */
            operatorContext.fireTileComputed(operatorImage, destRect, startNanos);
        }
    }

    private WritableRaster getWritableRaster(Band band, WritableRaster targetTileRaster) {
        WritableRaster tileRaster;
        if (band == getTargetBand()) {
            tileRaster = targetTileRaster;
        } else {
            OperatorContext operatorContext = getOperatorContext();
            // casting to access "getWritableRaster" method
            OperatorImageTileStack operatorImage = (OperatorImageTileStack) operatorContext.getTargetImage(band);
            Assert.state(operatorImage != this);
            tileRaster = operatorImage.getWritableRaster(targetTileRaster.getBounds());
        }
        return tileRaster;
    }

    private WritableRaster getWritableRaster(Rectangle tileRectangle) {
        Assert.argument(tileRectangle.x % getTileWidth() == 0, "rectangle");
        Assert.argument(tileRectangle.y % getTileHeight() == 0, "rectangle");
        Assert.argument(tileRectangle.width == getTileWidth(), "rectangle");
        Assert.argument(tileRectangle.height == getTileHeight(), "rectangle");
        final int tileX = XToTileX(tileRectangle.x);
        final int tileY = YToTileY(tileRectangle.y);
        final Raster tileFromCache = getTileFromCache(tileX, tileY);
        final WritableRaster writableRaster;
        if (tileFromCache instanceof WritableRaster) {
            // we already have a WritableRaster in the cache
            writableRaster = (WritableRaster) tileFromCache;
        } else {
            writableRaster = createWritableRaster(tileRectangle);
        }
        return writableRaster;
    }

    private WritableRaster createWritableRaster(Rectangle rectangle) {
        final int dataBufferType = ImageManager.getDataBufferType(getTargetBand().getDataType());
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType, rectangle.width,
                                                                           rectangle.height);
        final Point location = new Point(rectangle.x, rectangle.y);
        return createWritableRaster(sampleModel, location);
    }

    /**
     * Create a lock objects for each tile. These locks are used by all images in the tile stack.
     * This prevent multiple computation of tiles.
     */
    static Object[][] createLocks(int width, int height, Dimension tileSize) {
        int tw = tileSize.width;
        int numXTiles = PlanarImage.XToTileX(width - 1, 0, tw) - PlanarImage.XToTileX(0, 0, tw) + 1;
        int th = tileSize.height;
        int numYTiles = PlanarImage.YToTileY(height - 1, 0, th) - PlanarImage.YToTileY(0, 0, th) + 1;
        final Object[][] lock = new Object[numXTiles][numYTiles];
        for (int x = 0; x < numXTiles; x++) {
            for (int y = 0; y < numYTiles; y++) {
                lock[x][y] = new Object();
            }
        }
        return lock;
    }
}
