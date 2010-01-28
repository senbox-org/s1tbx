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
import java.lang.reflect.Field;
import java.util.concurrent.Semaphore;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;

/**
 * This executor triggers the computation of all tiles that the bands of the
 * target product of the given operator have. The computation of these tiles is
 * parallelized to use all available CPUs (cores) using the JAI
 * {@link TileScheduler}.
 * 
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
public class OperatorExecutor {

    public static OperatorExecutor create(Operator op) {
        OperatorContext operatorContext = initOperatorContext(op);
        final Product targetProduct = op.getTargetProduct();
        final Dimension tileSize = targetProduct.getPreferredTileSize();

        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
        Band[] targetBands = targetProduct.getBands();
        PlanarImage[] images = createImages(targetBands, operatorContext);
        return new OperatorExecutor(images, tileCountX, tileCountY);
    }

    public enum ExecutionOrder {
        ROW_COLUMN_BAND, ROW_BAND_COLUMN,
        /**
         * Minimize disk seeks if following conditions are met:<br/>
         * 1. Bands can be computed independently of each other<br/>
         * 2. I/O-bound processing (time to compute band pixels will less than
         * time for I/O).<br/>
         */
        BAND_ROW_COLUMN,
    }

    private final int tileCountX;
    private final int tileCountY;
    private final PlanarImage[] images;
    private final TileScheduler tileScheduler;

    public OperatorExecutor(PlanarImage[] images, int tileCountX, int tileCountY) {
        this.images = images;
        this.tileCountX = tileCountX;
        this.tileCountY = tileCountY;
        this.tileScheduler = JAI.getDefaultInstance().getTileScheduler();
    }

    public void execute(ProgressMonitor pm) {
        execute(ExecutionOrder.ROW_BAND_COLUMN, tileScheduler.getParallelism(), pm);
    }

    public void execute(ExecutionOrder executionOrder, ProgressMonitor pm) {
        execute(executionOrder, tileScheduler.getParallelism(), pm);
    }
    
    public void execute(ExecutionOrder executionOrder, int parallelism, ProgressMonitor pm) {
        final Semaphore semaphore = new Semaphore(parallelism, true);
        final TileComputationListener tcl = new OperatorTileComputationListener(semaphore, parallelism);
        final TileComputationListener[] listeners = new TileComputationListener[] { tcl };
        
        if (executionOrder == ExecutionOrder.ROW_BAND_COLUMN) {
//             executeRowBandColumn(pm);
            scheduleRowBandColumn(semaphore, parallelism, listeners, pm);
        } else if (executionOrder == ExecutionOrder.ROW_COLUMN_BAND) {
            ScheduleRowColumnBand(semaphore, parallelism, listeners, pm);
        } else if (executionOrder == ExecutionOrder.BAND_ROW_COLUMN) {
            scheduleBandRowColumn(semaphore, parallelism, listeners, pm);
        } else {
            throw new IllegalArgumentException("executionOrder");
        }
    }

    private void scheduleBandRowColumn(Semaphore semaphore, int parallelism, TileComputationListener[] listeners, ProgressMonitor pm) {
        try {
            pm.beginTask("Executing operator...", tileCountX * tileCountY * images.length);
            for (final PlanarImage image : images) {
                for (int tileY = 0; tileY < tileCountY; tileY++) {
                    for (int tileX = 0; tileX < tileCountX; tileX++) {
                        scheduleTile(image, tileX, tileY, semaphore, listeners, pm);
                    }
                }
            }
            acquirePermits(semaphore, parallelism);
        } finally {
            pm.done();
        }
    }

    private void scheduleRowBandColumn(Semaphore semaphore, int parallelism, TileComputationListener[] listeners, ProgressMonitor pm) {
        try {
            pm.beginTask("Executing operator...", tileCountX * tileCountY * images.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (final PlanarImage image : images) {
                    for (int tileX = 0; tileX < tileCountX; tileX++) {
                        scheduleTile(image, tileX, tileY, semaphore, listeners, pm);
                    }
                }
            }
            acquirePermits(semaphore, parallelism);
        } finally {
            pm.done();
        }
    }

    // unused (mz)
    // does not schedule tile but instead calls getTile blocking
    private void executeRowBandColumn(ProgressMonitor pm) {
        try {
            pm.beginTask("Executing operator...", tileCountX * tileCountY * images.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (final PlanarImage image : images) {
                    for (int tileX = 0; tileX < tileCountX; tileX++) {
                        checkForCancelation(pm);
                        image.getTile(tileX, tileY);
                        pm.worked(1);
                    }
                }
            }
        } finally {
            pm.done();
        }
    }

    private void ScheduleRowColumnBand(Semaphore semaphore, int parallelism, TileComputationListener[] listeners, ProgressMonitor pm) {
        try {
            pm.beginTask("Executing operator...", tileCountX * tileCountY * images.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    for (final PlanarImage image : images) {
                        scheduleTile(image, tileX, tileY, semaphore, listeners, pm);
                    }
                }
            }
            acquirePermits(semaphore, parallelism);
        } finally {
            pm.done();
        }
    }

    private void scheduleTile(final PlanarImage image, int tileX, int tileY, Semaphore semaphore,
                              TileComputationListener[] listeners, ProgressMonitor pm) {
        checkForCancelation(pm);
        acquirePermits(semaphore, 1);
        Point[] points = new Point[] { new Point(tileX, tileY) };
        tileScheduler.scheduleTiles(image, points, listeners);
        pm.worked(1);
    }

    private static void acquirePermits(Semaphore semaphore, int permits) {
        try {
            semaphore.acquire(permits);
        } catch (InterruptedException e) {
            throw new OperatorException(e);
        }
    }

    private static OperatorContext initOperatorContext(Operator operator) {
        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            OperatorContext operatorContext = (OperatorContext) field.get(operator);
            field.setAccessible(false);
            return operatorContext;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PlanarImage[] createImages(Band[] targetBands, OperatorContext operatorContext) {
        PlanarImage[] images = new PlanarImage[targetBands.length];
        int index = 0;
        for (final Band band : targetBands) {
            OperatorImage operatorImage = operatorContext.getTargetImage(band);
            if (operatorImage != null) {
                images[index++] = operatorImage;
            } else {
                String message = String.format("The band '%s' of the '%s' does not have an associated target image.",
                                               band.getName(), operatorContext.getOperator().getClass().getSimpleName());
                throw new OperatorException(message);
            }
        }
        return images;
    }

    private static void checkForCancelation(ProgressMonitor pm) {
        if (pm.isCanceled()) {
            throw new OperatorException("Operation canceled.");
        }
    }

    private static class OperatorTileComputationListener implements TileComputationListener {

        private final Semaphore semaphore;
        private final int parallelism;

        OperatorTileComputationListener(Semaphore scheduledTiles, int parallelism) {
            this.semaphore = scheduledTiles;
            this.parallelism = parallelism;
        }

        @Override
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY,
                                 Raster raster) {
            semaphore.release();
        }

        @Override
        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX, int tileY) {
            semaphore.release(parallelism);
            throw new OperatorException("Operation cancelled.");
        }

        @Override
        public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                           int tileY, Throwable situation) {
            semaphore.release(parallelism);
            throw new OperatorException("Operation failed.", situation);
        }
    }
}
