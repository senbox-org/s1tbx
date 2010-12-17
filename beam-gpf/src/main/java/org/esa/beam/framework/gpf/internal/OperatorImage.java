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

package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;

public class OperatorImage extends SourcelessOpImage {

    private static final String DISABLE_TILE_CACHING_PROPERTY = "beam.gpf.disableOperatorTileCaching";

    private final OperatorContext operatorContext;
    private Band targetBand;
    private final int numXTiles;
    private final int numYTiles;
    private TileComputationStatistic[] tileComputationStatistics;

    public OperatorImage(Band targetBand, OperatorContext operatorContext) {
        this(targetBand, operatorContext, ImageManager.createSingleBandedImageLayout(targetBand));
    }

    private OperatorImage(Band targetBand, OperatorContext operatorContext, ImageLayout imageLayout) {
        super(imageLayout,
              operatorContext.getRenderingHints(),
              imageLayout.getSampleModel(null),
              imageLayout.getMinX(null),
              imageLayout.getMinY(null),
              imageLayout.getWidth(null),
              imageLayout.getHeight(null));
        this.targetBand = targetBand;
        this.operatorContext = operatorContext;
        final boolean disableTileCaching = Boolean.getBoolean(DISABLE_TILE_CACHING_PROPERTY);
        if (disableTileCaching) {
            setTileCache(null);
        } else if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
        numXTiles = getNumXTiles();
        numYTiles = getNumYTiles();
        tileComputationStatistics = new TileComputationStatistic[numYTiles * numXTiles];
    }

    protected OperatorContext getOperatorContext() {
        return operatorContext;
    }

    protected Band getTargetBand() {
        return targetBand;
    }


    @Override
    protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {
        long startNanos = System.nanoTime();

        Tile targetTile;
        if (getOperatorContext().isComputing(getTargetBand())) {
            targetTile = createTargetTile(getTargetBand(), tile, destRect);
        } else if (requiresAllBands()) {
            targetTile = getOperatorContext().getSourceTile(getTargetBand(), destRect);
        } else {
            targetTile = null;
        }
        // computeTile() may have been deactivated
        if (targetTile != null && getOperatorContext().isComputeTileMethodUsable()) {
            getOperatorContext().getOperator().computeTile(getTargetBand(), targetTile, ProgressMonitor.NULL);
        }

        updateMetrics(destRect, startNanos);
    }


    public TileComputationStatistic[] getTileComputationStatistics() {
        return tileComputationStatistics;
    }

    protected void updateMetrics(Rectangle destRect, long startNanos) {
        if (getOperatorContext().isTracePerformance()) {
            int tileX = this.XToTileX(destRect.x);
            int tileY = this.YToTileY(destRect.y);
            TileComputationStatistic tileComputationStatistic = tileComputationStatistics[numXTiles * tileY + tileX];
            if (tileComputationStatistic == null) {
                tileComputationStatistic = new TileComputationStatistic(tileX, tileY);
                tileComputationStatistics[numXTiles * tileY + tileX] = tileComputationStatistic;
            }
            tileComputationStatistic.tileComputed(System.nanoTime() - startNanos);
        }
    }

    protected boolean requiresAllBands() {
        return operatorContext.requiresAllBands();
    }

    @Override
    public synchronized void dispose() {
        targetBand = null;
        super.dispose();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[");
        sb.append(operatorContext.getOperatorSpi().getOperatorAlias());
        sb.append(",");
        if (targetBand != null) {
            sb.append(targetBand.getName());
        }
        sb.append("]");
        return sb.toString();
    }

    protected static TileImpl createTargetTile(Band band, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        return new TileImpl(band, targetTileRaster, targetRectangle, true);
    }

}
