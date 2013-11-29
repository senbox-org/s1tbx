/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf.classifiers;

import org.csa.rstb.gpf.PolOpUtils;
import org.csa.rstb.gpf.PolarimetricClassificationOp;
import org.csa.rstb.gpf.decompositions.hAAlpha;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.gpf.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**

 */
public class Wishart extends PolClassifierBase implements PolClassifier {

    private static final String UNSUPERVISED_WISHART_CLASS = "H_alpha_wishart_class";

    private ClusterInfo[][] clusterCenters = null;
    private boolean[] clusterCentersComputed = null;
    private final int maxIterations;
    private final boolean useLeeHAlphaPlaneDefinition;

    public Wishart(final PolBandUtils.MATRIX srcProductType,
                   final int srcWidth, final int srcHeight, final int windowSize,
                   final Map<Band, PolBandUtils.QuadSourceBand> bandMap,
                   final int maxIterations) {
        super(srcProductType, srcWidth, srcHeight, windowSize, bandMap);
        this.maxIterations = maxIterations;

        useLeeHAlphaPlaneDefinition = Boolean.getBoolean(SystemUtils.getApplicationContextId()+
                                                             ".useLeeHAlphaPlaneDefinition");
    }

     /**
        Return the band name for the target product
        @return band name
     */
    public String getTargetBandName() {
        return UNSUPERVISED_WISHART_CLASS;
    }

    /**
     * returns the number of classes
     * @return num classes
     */
    public int getNumClasses() {
        return 9;
    }

    /**
     * Perform decomposition for given tile.
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param op the polarimetric decomposition operator
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Band targetBand, final Tile targetTile, final PolarimetricClassificationOp op) {

        PolBandUtils.QuadSourceBand srcBandList = bandMap.get(targetBand);
        final int numTargetBands = targetBand.getProduct().getNumBands();
        final int targetBandIndex = targetBand.getProduct().getBandIndex(targetBand.getName());

        if (clusterCentersComputed == null || !clusterCentersComputed[targetBandIndex]) {
            computeClusterCenters(numTargetBands, targetBandIndex, srcBandList, op);
        }

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
        final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];
        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
        for (int i = 0; i < sourceTiles.length; ++i) {
            sourceTiles[i] = op.getSourceTile(srcBandList.srcBands[i], sourceRectangle);
            dataBuffers[i] = sourceTiles[i].getDataBuffer();
        }
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(targetTile);
        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final double noDataValue = srcBandList.srcBands[0].getNoDataValue();

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            srcIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int index = trgIndex.getIndex(x);
                if(dataBuffers[0].getElemDoubleAt(srcIndex.getIndex(x)) == noDataValue) {
                    targetData.setElemIntAt(index, NODATACLASS);    
                } else {
                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, srcWidth, srcHeight,
                                                  sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    targetData.setElemIntAt(index, findZoneIndex(Tr, Ti, clusterCenters[targetBandIndex]));
                }
            }
        }
    }

    /**
     * Compute cluster centers for all 9 zones
     * @param srcBandList the input bands
     * @param op the operator
     */
    private synchronized void computeClusterCenters(final int numTargetBands,
                                                    final int targetBandIndex,
                                                    final PolBandUtils.QuadSourceBand srcBandList,
                                                    final PolarimetricClassificationOp op) {

        if (clusterCentersComputed != null && clusterCentersComputed[targetBandIndex]) {
            return;
        }

        if (clusterCentersComputed == null) {
            clusterCentersComputed = new boolean[numTargetBands];
            Arrays.fill(clusterCentersComputed, false);
            clusterCenters = new ClusterInfo[numTargetBands][9];
        }

        //final Dimension tileSize = ImageManager.getPreferredTileSize(sourceProduct);
        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(op.getSourceProduct(), tileSize, 0);

        computeInitialClusterCenters(targetBandIndex, srcBandList, tileRectangles, op);

        computeFinalClusterCenters(targetBandIndex, srcBandList, tileRectangles, op);

        clusterCentersComputed[targetBandIndex] = true;
    }

    /**
     * Compute initial cluster centers for all 9 zones using H-Alpha
     * @param srcBandList the input bands
     * @param op the operator
     * @param tileRectangles Array of rectangles for all source tiles of the image
     */
    private void computeInitialClusterCenters(final int targetBandIndex,
                                              final PolBandUtils.QuadSourceBand srcBandList,
                                              final Rectangle[] tileRectangles, final PolarimetricClassificationOp op) {

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Computing Initial Cluster Centres... ");
        int tileCnt = 0;

        final double[][][] sumRe = new double[9][3][3];
        final double[][][] sumIm = new double[9][3][3];
        final double[][][] centerRe = new double[9][3][3];
        final double[][][] centerIm = new double[9][3][3];
        final int[] counter = new int[9];
        final double noDataValue = srcBandList.srcBands[0].getNoDataValue();

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (final Rectangle rectangle : tileRectangles) {
                op.checkIfCancelled();

                final Thread worker = new Thread() {

                    final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
                    final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];

                    final double[][] Tr = new double[3][3];
                    final double[][] Ti = new double[3][3];

                    @Override
                    public void run() {
                        final int x0 = rectangle.x;
                        final int y0 = rectangle.y;
                        final int w = rectangle.width;
                        final int h = rectangle.height;
                        final int xMax = x0 + w;
                        final int yMax = y0 + h;
                        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

                        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
                        for (int i = 0; i < sourceTiles.length; ++i) {
                            sourceTiles[i] = op.getSourceTile(srcBandList.srcBands[i], sourceRectangle);
                            dataBuffers[i] = sourceTiles[i].getDataBuffer();
                        }
                        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                        for (int y = y0; y < yMax; ++y) {
                            srcIndex.calculateStride(y);
                            for (int x = x0; x < xMax; ++x) {
                                if(dataBuffers[0].getElemDoubleAt(srcIndex.getIndex(x)) == noDataValue)
                                    continue;

                                PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, srcWidth, srcHeight,
                                        sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                                final hAAlpha.HAAlpha data = hAAlpha.computeHAAlpha(Tr, Ti);
                                if (!Double.isNaN(data.entropy) && !Double.isNaN(data.anisotropy) && !Double.isNaN(data.alpha)) {
                                    synchronized (counter) {
                                        final int zoneIndex = CloudePottier.getZoneIndex(data.entropy, data.alpha,
                                                                                        useLeeHAlphaPlaneDefinition);
                                        counter[zoneIndex-1] += 1;
                                        computeSummationOfT3(zoneIndex, Tr, Ti, sumRe, sumIm);
                                    }
                                }
                            }
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(tileCnt++);
            }

            threadManager.finish();

            for (int z = 0; z < 9; ++z) {
                final int count = counter[z];
                //System.out.println("z = " + z + ", counter[z] = " + count);
                if (count > 0) {
                    for (int i = 0; i < 3; ++i) {
                        for (int j = 0; j < 3; ++j) {
                            centerRe[z][i][j] = sumRe[z][i][j] / count;
                            centerIm[z][i][j] = sumIm[z][i][j] / count;
                        }
                    }
                    clusterCenters[targetBandIndex][z] = new ClusterInfo();
                    clusterCenters[targetBandIndex][z].setClusterCenter(z+1, centerRe[z], centerIm[z], counter[z]);
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(op.getId()+ " computeInitialClusterCenters ", e);
        } finally {
            status.done();
        }
    }

    /**
     * Compute final cluster centers for all 9 zones using K-mean clustering method
     * @param srcBandList the input bands
     * @param op the operator
     * @param tileRectangles Array of rectangles for all source tiles of the image
     */
    private void computeFinalClusterCenters(final int targetBandIndex,
                                            final PolBandUtils.QuadSourceBand srcBandList,
                                            final Rectangle[] tileRectangles,
                                            final PolarimetricClassificationOp op) {

        final double[][][] centerRe = new double[9][3][3];
        final double[][][] centerIm = new double[9][3][3];
        boolean endIteration = false;
        final double noDataValue = srcBandList.srcBands[0].getNoDataValue();
        
        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length*maxIterations,
                "Computing Final Cluster Centres... ");
        int tileCnt = 0;

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (int it = 0; (it < maxIterations && !endIteration); ++it) {
                //System.out.println("Iteration: " + it);

                final double[][][] sumRe = new double[9][3][3];
                final double[][][] sumIm = new double[9][3][3];
                final int[] counter = new int[9];

                for (final Rectangle rectangle : tileRectangles) {

                    final Thread worker = new Thread() {

                        final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
                        final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];

                        final double[][] Tr = new double[3][3];
                        final double[][] Ti = new double[3][3];

                        @Override
                        public void run() {
                            op.checkIfCancelled();

                            final int x0 = rectangle.x;
                            final int y0 = rectangle.y;
                            final int w = rectangle.width;
                            final int h = rectangle.height;
                            final int xMax = x0 + w;
                            final int yMax = y0 + h;

                            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
                            for (int i = 0; i < sourceTiles.length; ++i) {
                                sourceTiles[i] = op.getSourceTile(srcBandList.srcBands[i], sourceRectangle);
                                dataBuffers[i] = sourceTiles[i].getDataBuffer();
                            }
                            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
                            
                            for (int y = y0; y < yMax; ++y) {
                                srcIndex.calculateStride(y);
                                for (int x = x0; x < xMax; ++x) {
                                    if(dataBuffers[0].getElemDoubleAt(srcIndex.getIndex(x)) == noDataValue)
                                        continue;

                                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, srcWidth, srcHeight,
                                            sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                                    synchronized (counter) {
                                        final int zoneIdx = findZoneIndex(Tr, Ti, clusterCenters[targetBandIndex]);
                                        counter[zoneIdx-1]++;
                                        computeSummationOfT3(zoneIdx, Tr, Ti, sumRe, sumIm);
                                    }
                                }
                            }
                        }
                    };
                    threadManager.add(worker);

                    status.worked(tileCnt++);
                }

                double diff = 0.0;
                for (int z = 0; z < 9; ++z) {
                    final int count = counter[z];
                    //System.out.println("counter[" + z + "] = " + count);
                    if (counter[z] > 0) {
                        for (int i = 0; i < 3; ++i) {
                            for (int j = 0; j < 3; ++j) {
                                centerRe[z][i][j] = sumRe[z][i][j] / count;
                                centerIm[z][i][j] = sumIm[z][i][j] / count;
                                diff += (clusterCenters[targetBandIndex][z].centerRe[i][j] - centerRe[z][i][j]) *
                                        (clusterCenters[targetBandIndex][z].centerRe[i][j] - centerRe[z][i][j]) +
                                        (clusterCenters[targetBandIndex][z].centerIm[i][j] - centerIm[z][i][j]) *
                                        (clusterCenters[targetBandIndex][z].centerIm[i][j] - centerIm[z][i][j]);
                            }
                        }
                        clusterCenters[targetBandIndex][z].setClusterCenter(z+1, centerRe[z], centerIm[z], counter[z]);
                    }
                }

                if (diff == 0) {
                    endIteration = true;
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(op.getId()+ " computeFinalClusterCenters ", e);
        } finally {
            status.done();
        }
    }

    /**
     * Find the nearest cluster for a given T3 matrix using Wishart distance
     * @param Tr Real part of the T3 matrix
     * @param Ti Imaginary part of the T3 matrix
     * @param clusterCenters The cluster centers
     * @return The zone index for the nearest cluster
     */
    public static int findZoneIndex(final double[][] Tr, final double[][] Ti, final ClusterInfo[] clusterCenters) {
        double minDistance = 1e30;
        int zoneIndex = -1;
        for (int z = 0; z < clusterCenters.length; ++z) {
            if (clusterCenters[z] != null) {
                final double d = computeWishartDistance(Tr, Ti, clusterCenters[z]);
                if (minDistance > d) {
                    minDistance = d;
                    zoneIndex = z + 1;
                }
            }
        }

        return zoneIndex;
    }

    /**
     * Compute Wishart distance for given coherency matrix and given cluster
     * @param Tr Real part of the coherency matrix
     * @param Ti Imaginary part of the coherency matrix
     * @param cluster The cluster object
     * @return The Wishart distance
     */
    static double computeWishartDistance(final double[][] Tr, final double[][] Ti, final ClusterInfo cluster) {

        return cluster.invCenterRe[0][0]*Tr[0][0] - cluster.invCenterIm[0][0]*Ti[0][0] +
               cluster.invCenterRe[1][1]*Tr[1][1] - cluster.invCenterIm[1][1]*Ti[1][1] +
               cluster.invCenterRe[2][2]*Tr[2][2] - cluster.invCenterIm[2][2]*Ti[2][2] +
               2*(cluster.invCenterRe[0][1]*Tr[0][1] + cluster.invCenterIm[0][1]*Ti[0][1]) +
               2*(cluster.invCenterRe[0][2]*Tr[0][2] + cluster.invCenterIm[0][2]*Ti[0][2]) +
               2*(cluster.invCenterRe[1][2]*Tr[1][2] + cluster.invCenterIm[1][2]*Ti[1][2]) + cluster.logDet;
    }
}
