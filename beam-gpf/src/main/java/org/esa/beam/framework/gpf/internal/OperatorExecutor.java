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

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This executor triggers the computation of all tiles that the bands of the target product
 * of the given operator have. The computation of these tiles is parallelized to use all available
 * CPUs (cores) using the JAI {@link TileScheduler}.
 *
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OperatorExecutor {

    public enum ExecutionOrder {
        ROW_COLUMN_BAND,
        ROW_BAND_COLUMN,
        /**
         * Minimize disk seeks if following conditions are met:<br/>
         * 1. Bands can be computed independently of each other<br/>
         * 2. I/O-bound processing (time to compute band pixels will less than time for I/O).<br/>
         */
        BAND_ROW_COLUMN,
    }

    private final Operator operator;
    private OperatorContext operatorContext;
    private int tileCountX;
    private int tileCountY;
    private Map<Band, PlanarImage> imageMap;
    private Band[] targetBands;
    private TileScheduler tileScheduler;

    public OperatorExecutor(Operator operator) {
        this.operator = operator;
    }

    public void execute(ProgressMonitor pm) {
        execute(ExecutionOrder.ROW_BAND_COLUMN, pm);
    }

    public void execute(ExecutionOrder executionOrder, ProgressMonitor pm) {
        init();

        if (executionOrder == ExecutionOrder.ROW_BAND_COLUMN) {
            executeRowBandColumn(pm);
        } else if (executionOrder == ExecutionOrder.ROW_COLUMN_BAND) {
            executeRowColumnBand(pm);
        } else if (executionOrder == ExecutionOrder.BAND_ROW_COLUMN) {
            executeBandRowColumn(pm);
        }  else {
            throw new IllegalArgumentException("executionOrder");
        }
    }

    private void executeBandRowColumn(ProgressMonitor pm) {
        throw new IllegalStateException("Not implemented.");
    }

    private void executeRowBandColumn(ProgressMonitor pm) {
        final AtomicInteger scheduledTiles = new AtomicInteger(0);
        final int parallelism = tileScheduler.getParallelism();
        try {
            pm.beginTask("Executing operator...", tileCountX * tileCountY * targetBands.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (final Band band : targetBands) {
                    for (int tileX = 0; tileX < tileCountX; tileX++) {
                        checkForCancelation(pm);
                        scheduleTile(band, tileX, tileY, scheduledTiles);
                        waitForScheduler(scheduledTiles, parallelism);
                        pm.worked(1);
                    }
                }
            }
            waitForScheduler(scheduledTiles, 0);
        } finally {
            pm.done();
        }
    }

    private void executeRowColumnBand(ProgressMonitor pm) {
        final AtomicInteger scheduledTiles = new AtomicInteger(0);
        final int parallelism = tileScheduler.getParallelism();
        try {
            pm.beginTask("Executing operator...", tileCountX * tileCountY * targetBands.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    for (final Band band : targetBands) {
                        checkForCancelation(pm);
                        scheduleTile(band, tileX, tileY, scheduledTiles);
                        waitForScheduler(scheduledTiles, parallelism);
                        pm.worked(1);
                    }
                }
            }
            waitForScheduler(scheduledTiles, 0);
        } finally {
            pm.done();
        }
    }

    private static void waitForScheduler(AtomicInteger scheduledTiles, int threshold) {
        while (scheduledTiles.intValue() > threshold) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new OperatorException(e);
            }
        }
    }

    private void init() {
        initOperatorContext();
        final Product targetProduct = operator.getTargetProduct();
        final Dimension tileSize = targetProduct.getPreferredTileSize();

        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
        targetBands = targetProduct.getBands();

        createImageMap();
        tileScheduler = JAI.getDefaultInstance().getTileScheduler();
    }

    private void initOperatorContext() {
        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            operatorContext = (OperatorContext) field.get(operator);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createImageMap() {
        imageMap = new HashMap<Band, PlanarImage>(targetBands.length * 2);
        for (final Band band : targetBands) {
            OperatorImage operatorImage = operatorContext.getTargetImage(band);
            if (operatorImage != null) {
                imageMap.put(band, operatorImage);
            } else {
                String message = String.format("The band '%s' of the '%s' does not have an associated target image.",
                                               band.getName(), operator.getClass().getSimpleName());
                throw new OperatorException(message);
            }
        }
    }

    private void checkForCancelation(ProgressMonitor pm) {
        if (pm.isCanceled()) {
            throw new OperatorException("Operation canceled.");
        }
    }

    private void scheduleTile(Band band, int tileX, int tileY, AtomicInteger scheduledTiles) {
        final PlanarImage planarImage = imageMap.get(band);
        final TileComputationListener tcl = new OperatorTileComputationListener(scheduledTiles);
        final TileComputationListener[] listeners = new TileComputationListener[]{tcl};
        Point[] points = new Point[]{new Point(tileX, tileY)};
        scheduledTiles.incrementAndGet();
        tileScheduler.scheduleTiles(planarImage, points, listeners);
    }

    private static class OperatorTileComputationListener implements TileComputationListener {

        private final AtomicInteger scheduledTiles;

        OperatorTileComputationListener(AtomicInteger scheduledTiles) {
            this.scheduledTiles = scheduledTiles;
        }

        @Override
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                 int tileY,
                                 Raster raster) {
            scheduledTiles.decrementAndGet();
        }

        @Override
        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                  int tileY) {
            scheduledTiles.getAndSet(0);
            throw new OperatorException("Operation cancelled.");
        }

        @Override
        public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                           int tileY, Throwable situation) {
            scheduledTiles.getAndSet(0);
            throw new OperatorException("Operation failed.", situation);
        }
    }
}
