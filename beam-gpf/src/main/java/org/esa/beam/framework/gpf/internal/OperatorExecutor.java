/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;

/**
 * todo - what's this thing ???
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
public class OperatorExecutor {

    private final Operator operator;

    public OperatorExecutor(Operator operator) {
        this.operator = operator;
    }
    
    // TODO options to pull in other ways
    public void execute(ProgressMonitor pm) {
        final Product targetProduct = operator.getTargetProduct();
        Dimension tileSize = targetProduct.getPreferredTileSize();

        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
        final Band[] targetBands = targetProduct.getBands();

        Map<Band, PlanarImage> imageMap = new HashMap<Band, PlanarImage>(targetBands.length*2);
        for (final Band band : targetBands) {
          final RenderedImage image = band.getSourceImage().getImage(0);
          final PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
          imageMap.put(band, planarImage);
        }
        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        AtomicInteger scheduledTiles = new AtomicInteger(0);
        try {
            pm.beginTask("Writing product...", tileCountX * tileCountY * targetBands.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (final Band band : targetBands) {
                    checkForCancelation(pm);
                    Point[] points = new Point[tileCountX];
                    for (int tileX = 0; tileX < tileCountX; tileX++) {
                        points[tileX] = new Point(tileX, tileY);
                    }
                    final PlanarImage planarImage = imageMap.get(band);
                    final TileComputationListener tcl = new OperatorTileComputationListener(band, scheduledTiles);
                    final TileComputationListener[] listeners = new TileComputationListener[] {tcl};
                    scheduledTiles.addAndGet(tileCountX);
                    tileScheduler.scheduleTiles(planarImage, points, listeners);
                    while (scheduledTiles.intValue() > tileScheduler.getParallelism()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            throw new OperatorException(e);
                        }
                    }
                    pm.worked(tileCountX);
                }
            }
            while (scheduledTiles.get() > 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new OperatorException(e);
                }
            }
        } finally {
            pm.done();
        }
    }
    
    private void checkForCancelation(ProgressMonitor pm) {
        if (pm.isCanceled()) {
            throw new OperatorException("Operation canceled.");
        }
    }

    private class OperatorTileComputationListener implements TileComputationListener {

        private final Band band;
        private final AtomicInteger scheduledTiles;

        OperatorTileComputationListener(Band band, AtomicInteger scheduledTiles) {
            this.band = band;
            this.scheduledTiles = scheduledTiles;
        }

        @Override
            public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                 int tileY,
                                 Raster raster) {
            final int rasterHeight = band.getSceneRasterHeight();
            final int rasterWidth = band.getSceneRasterWidth();
            final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
            Rectangle rect = boundary.intersection(raster.getBounds());
            final TileImpl tile = new TileImpl(band, raster, rect, false);
            operator.computeTile(band, tile, ProgressMonitor.NULL);
            scheduledTiles.decrementAndGet();
        }

        @Override
            public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                  int tileY) {
            System.out.println("tileCancelled");
        }

        @Override
            public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                           int tileY, Throwable situation) {
            System.out.println("tileComputationFailure");
            situation.printStackTrace();
            System.out.println("==========================");
        }
    }    
}
