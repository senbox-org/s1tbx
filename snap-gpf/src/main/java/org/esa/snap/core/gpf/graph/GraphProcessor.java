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

package org.esa.snap.core.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.internal.OperatorContext;
import org.esa.snap.core.gpf.internal.ProductSetHandler;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;
import javax.media.jai.util.ImagingListener;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * The {@code GraphProcessor} is responsible for executing processing
 * graphs.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zuehlke
 * @since 4.1
 */
public class GraphProcessor {

    private List<GraphProcessingObserver> observerList;
    private Logger logger;
    private volatile OperatorException error = null;


    /**
     * Creates a new instance og {@code GraphProcessor}.
     */
    public GraphProcessor() {
        observerList = new ArrayList<>(3);
        logger = SystemUtils.LOG;
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets a logger.
     *
     * @param logger a logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Adds an observer to this graph popcessor. {@link GraphProcessingObserver}s are informed about
     * processing steps of the currently running processing graph.
     *
     * @param processingObserver the observer
     *
     * @see GraphProcessingObserver
     */
    public void addObserver(GraphProcessingObserver processingObserver) {
        observerList.add(processingObserver);
    }

    /**
     * Gets all observers currently attached to this {@code GraphProcessor}.
     *
     * @return the observers
     */
    public GraphProcessingObserver[] getObservers() {
        return observerList.toArray(new GraphProcessingObserver[observerList.size()]);
    }

    /**
     * Executes the graph using a new default {@link GraphContext}.
     *
     * @param graph the {@link Graph}
     * @param pm    a progress monitor. Can be used to signal progress.
     *
     * @throws GraphException if any error occurs during execution
     * @see GraphProcessor#executeGraph(GraphContext, com.bc.ceres.core.ProgressMonitor)
     */
    public void executeGraph(Graph graph, ProgressMonitor pm) throws GraphException {
        GraphContext graphContext;
        try {
            pm.beginTask("Executing processing graph", 100);

            // handle product sets
            final ProductSetHandler productSet = new ProductSetHandler(graph);
            productSet.replaceProductSetsWithReaders();

            graphContext = new GraphContext(graph);
            executeGraph(graphContext, SubProgressMonitor.create(pm, 90));
            graphContext.dispose();
        } finally {
            pm.done();
        }
    }

    /**
     * Executes the graph given by {@link GraphContext}.
     *
     * @param graphContext the {@link GraphContext} to execute
     * @param pm           a progress monitor. Can be used to signal progress.
     *
     * @return the output products of the executed graph
     */
    public Product[] executeGraph(GraphContext graphContext, ProgressMonitor pm) {
        fireProcessingStarted(graphContext);

        //Header header = graphContext.getGraph().getHeader();
        // TODO use header to specify execution order (mz, 2009-11-16)
        NodeContext[] outputNodeContexts = graphContext.getOutputNodeContexts();
        Map<Dimension, List<NodeContext>> tileDimMap = buildTileDimensionMap(outputNodeContexts);

        List<Dimension> dimList = new ArrayList<>(tileDimMap.keySet());
        dimList.sort((d1, d2) -> {
            Long area1 = (long) (d1.width * d1.height);
            Long area2 = (long) (d2.width * d2.height);
            return area1.compareTo(area2);
        });

        int numPmTicks = 0;
        for (Dimension dimension : dimList) {
            numPmTicks += dimension.width * dimension.height * tileDimMap.get(dimension).size();
        }

        ImagingListener imagingListener = JAI.getDefaultInstance().getImagingListener();
        JAI.getDefaultInstance().setImagingListener(new GPFImagingListener());

        final TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        final int parallelism = tileScheduler.getParallelism();
        final Semaphore semaphore = new Semaphore(parallelism, true);
        final TileComputationListener tcl = new GraphTileComputationListener(semaphore, parallelism);
        final TileComputationListener[] listeners = new TileComputationListener[]{tcl};

        try {
            pm.beginTask("Computing raster data...", numPmTicks);
            for (Dimension dimension : dimList) {
                List<NodeContext> nodeContextList = tileDimMap.get(dimension);
                final int numXTiles = dimension.width;
                final int numYTiles = dimension.height;
                Dimension tileSize = nodeContextList.get(0).getTargetProduct().getPreferredTileSize();
                for (int tileY = 0; tileY < numYTiles; tileY++) {
                    for (int tileX = 0; tileX < numXTiles; tileX++) {
                        if (pm.isCanceled()) {
                            // todo - check: throw exception here? (nf, 2010.10.21)
                            return graphContext.getOutputProducts();
                        }
                        Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                                tileY * tileSize.height,
                                                                tileSize.width,
                                                                tileSize.height);
                        fireTileStarted(graphContext, tileRectangle);
                        for (NodeContext nodeContext : nodeContextList) {
                            Product targetProduct = nodeContext.getTargetProduct();
                            if (nodeContext.canComputeTileStack()) {

                                // (1) Pull tile from first OperatorImage we find. This will trigger pulling
                                // tiles of all other OperatorImage computed stack-wise.
                                //
                                for (Band band : targetProduct.getBands()) {
                                    PlanarImage image = nodeContext.getTargetImage(band);
                                    if (image != null) {
                                        forceTileComputation(image, tileX, tileY, semaphore, tileScheduler, listeners,
                                                             parallelism);
                                        break;
                                    }
                                }

                                // (2) Pull tile from source images of other regular bands.
                                //
                                for (Band band : targetProduct.getBands()) {
                                    PlanarImage image = nodeContext.getTargetImage(band);
                                    if (image == null) {
                                        if (OperatorContext.isRegularBand(band) && band.isSourceImageSet()) {
                                            forceTileComputation(band.getSourceImage(), tileX, tileY, semaphore,
                                                                 tileScheduler, listeners, parallelism);
                                        }
                                    }
                                }
                            } else {

                                // Simply pull tile from source images of regular bands.
                                //
                                for (Band band : targetProduct.getBands()) {
                                    PlanarImage image = nodeContext.getTargetImage(band);
                                    if (image != null) {
                                        forceTileComputation(image, tileX, tileY, semaphore, tileScheduler, listeners,
                                                             parallelism);
                                    } else if (OperatorContext.isRegularBand(band) && band.isSourceImageSet()) {
                                        forceTileComputation(band.getSourceImage(), tileX, tileY, semaphore,
                                                             tileScheduler, listeners, parallelism);
                                    }
                                }
                            }

                            pm.worked(1);
                        }
                        fireTileStopped(graphContext, tileRectangle);
                    }
                }
            }
            acquirePermits(semaphore, parallelism);
            if (error != null) {
                throw error;
            }
        } finally {
            semaphore.release(parallelism);
            pm.done();
            JAI.getDefaultInstance().setImagingListener(imagingListener);
            fireProcessingStopped(graphContext);
        }

        return graphContext.getOutputProducts();
    }

    private Map<Dimension, List<NodeContext>> buildTileDimensionMap(NodeContext[] outputNodeContexts) {
        final int mapSize = outputNodeContexts.length;
        Map<Dimension, List<NodeContext>> tileSizeMap = new HashMap<>(mapSize);
        for (NodeContext outputNodeContext : outputNodeContexts) {
            Product targetProduct = outputNodeContext.getTargetProduct();
            Dimension tileSize = targetProduct.getPreferredTileSize();
            final int numXTiles = MathUtils.ceilInt(targetProduct.getSceneRasterWidth() / (double) tileSize.width);
            final int numYTiles = MathUtils.ceilInt(targetProduct.getSceneRasterHeight() / (double) tileSize.height);
            Dimension tileDim = new Dimension(numXTiles, numYTiles);
            List<NodeContext> nodeContextList = tileSizeMap.computeIfAbsent(tileDim, k -> new ArrayList<>(mapSize));
            nodeContextList.add(outputNodeContext);
        }
        return tileSizeMap;
    }

    private void forceTileComputation(PlanarImage image, int tileX, int tileY, Semaphore semaphore,
                                      TileScheduler tileScheduler, TileComputationListener[] listeners,
                                      int parallelism) {
        Point[] points = new Point[]{new Point(tileX, tileY)};
        acquirePermits(semaphore, 1);
        if (error != null) {
            semaphore.release(parallelism);
            throw error;
        }
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!!!
        //
        tileScheduler.scheduleTiles(image, points, listeners);
        //
        /////////////////////////////////////////////////////////////////////
    }

    private static void acquirePermits(Semaphore semaphore, int permits) {
        try {
            semaphore.acquire(permits);
        } catch (InterruptedException e) {
            throw new OperatorException(e);
        }
    }


    private void fireProcessingStarted(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStarted(graphContext);
        }
    }

    private void fireProcessingStopped(GraphContext graphContext) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.graphProcessingStopped(graphContext);
        }
    }

    private void fireTileStarted(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStarted(graphContext, rect);
        }

    }

    private void fireTileStopped(GraphContext graphContext, Rectangle rect) {
        for (GraphProcessingObserver processingObserver : observerList) {
            processingObserver.tileProcessingStopped(graphContext, rect);
        }
    }

    private class GraphTileComputationListener implements TileComputationListener {

        private final Semaphore semaphore;
        private final int parallelism;

        GraphTileComputationListener(Semaphore semaphore, int parallelism) {
            this.semaphore = semaphore;
            this.parallelism = parallelism;
        }

        @Override
        public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                 int tileY,
                                 Raster raster) {
            semaphore.release();
        }

        @Override
        public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                  int tileY) {
            if (error == null) {
                error = new OperatorException("Operation cancelled.");
            }
            semaphore.release(parallelism);
        }

        @Override
        public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                           int tileY, Throwable situation) {
            if (error == null) {
                error = new OperatorException("Operation failed.", situation);
            }
            semaphore.release(parallelism);
        }
    }

    private class GPFImagingListener implements ImagingListener {

        @Override
        public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable)
                throws RuntimeException {
            if (error == null && !thrown.getClass().getSimpleName().equals("MediaLibLoadException")) {
                error = new OperatorException(thrown);
            }
            return false;
        }
    }

}
