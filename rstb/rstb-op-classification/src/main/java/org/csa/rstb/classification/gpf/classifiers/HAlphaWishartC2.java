/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.classification.gpf.classifiers;

import org.csa.rstb.polarimetric.gpf.DualPolOpUtils;
import org.csa.rstb.polarimetric.gpf.HaAlphaDescriptor;
import org.csa.rstb.polarimetric.gpf.PolOpUtils;
import org.csa.rstb.classification.gpf.PolarimetricClassificationOp;
import org.csa.rstb.polarimetric.gpf.decompositions.HAlphaC2;
import org.esa.s1tbx.io.PolBandUtils;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.StatusProgressMonitor;
import org.esa.snap.gpf.ThreadManager;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.SystemUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;

/**
 * The operator performs unsupervised H-Alpha classification on dual pol product.
 * <p>
 * [1] S. R. Cloude, D. G. Goodenough, H. Chen, "Compact Decomposition Theory",
 * IEEE Geoscience and Remote Sensing Letters, Vol. 9, No. 1, Jan. 2012.
 */
public class HAlphaWishartC2 extends PolClassifierBase implements PolClassifier {

    private static final String UNSUPERVISED_WISHART_CLASS = "H_alpha_wishart_class";

    protected ClusterInfoC2[][] clusterCenters = null;
    private boolean[] clusterCentersComputed = null;
    private final int maxIterations;
    protected final boolean useLeeHAlphaPlaneDefinition;

    public HAlphaWishartC2(final PolBandUtils.MATRIX srcProductType,
                           final int srcWidth, final int srcHeight,
                           final int windowSizeX, final int windowSizeY,
                           final Map<Band, PolBandUtils.PolSourceBand> bandMap,
                           final int maxIterations, final PolarimetricClassificationOp op) {

        super(srcProductType, srcWidth, srcHeight, windowSizeX, windowSizeY, bandMap, op);
        this.maxIterations = maxIterations;
        this.useLeeHAlphaPlaneDefinition = Boolean.getBoolean(SystemUtils.getApplicationContextId() +
                ".useLeeHAlphaPlaneDefinition");
    }

    /**
     * Return the band name for the target product
     *
     * @return band name
     */
    public String getTargetBandName() {
        return UNSUPERVISED_WISHART_CLASS;
    }

    /**
     * returns the number of classes
     *
     * @return num classes
     */
    public int getNumClasses() {
        return 9;
    }

    private boolean noData(final double noDataValue, final ProductData[] dataBuffers, final int index) {
        // It is assumed that all the data buffers have the same no data value.
        int numNoDataBuf = 0;
        for (ProductData buf : dataBuffers) {
            if (buf.getElemDoubleAt(index) == noDataValue) {
                numNoDataBuf++;
            }
        }
        return (numNoDataBuf == dataBuffers.length);
    }

    /**
     * Perform decomposition for given tile.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Band targetBand, final Tile targetTile) {

        PolBandUtils.PolSourceBand srcBandList = bandMap.get(targetBand);
        final int numTargetBands = targetBand.getProduct().getNumBands();
        final int targetBandIndex = targetBand.getProduct().getBandIndex(targetBand.getName());

        if (clusterCentersComputed == null || !clusterCentersComputed[targetBandIndex]) {
            computeClusterCenters(numTargetBands, targetBandIndex, srcBandList, op);
        }

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
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

        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];

        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            srcIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int index = trgIndex.getIndex(x);
                if (noData(noDataValue, dataBuffers, srcIndex.getIndex(x))) {
                    targetData.setElemIntAt(index, NODATACLASS);
                } else {
                    DualPolOpUtils.getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, srcWidth, srcHeight,
                                                             sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    targetData.setElemIntAt(index, findZoneIndex(Cr, Ci, clusterCenters[targetBandIndex]));
                }
            }
        }

    }

    /**
     * Compute cluster centers for all 9 zones
     *
     * @param numTargetBands  Number of target bands
     * @param targetBandIndex Target band index
     * @param srcBandList     the input bands
     * @param op              the operator
     */
    private synchronized void computeClusterCenters(final int numTargetBands,
                                                    final int targetBandIndex,
                                                    final PolBandUtils.PolSourceBand srcBandList,
                                                    final PolarimetricClassificationOp op) {

        if (clusterCentersComputed != null && clusterCentersComputed[targetBandIndex]) {
            return;
        }

        if (clusterCentersComputed == null) {
            clusterCentersComputed = new boolean[numTargetBands];
            Arrays.fill(clusterCentersComputed, false);
            clusterCenters = new ClusterInfoC2[numTargetBands][9];
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
     *
     * @param targetBandIndex Target band index
     * @param srcBandList     the input bands
     * @param tileRectangles  Array of rectangles for all source tiles of the image
     * @param op              the operator
     */
    protected void computeInitialClusterCenters(final int targetBandIndex,
                                              final PolBandUtils.PolSourceBand srcBandList,
                                              final Rectangle[] tileRectangles,
                                              final PolarimetricClassificationOp op) {

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Computing Initial Cluster Centres... ");
        int tileCnt = 0;

        final double[][][] sumRe = new double[9][2][2];
        final double[][][] sumIm = new double[9][2][2];
        final double[][][] centerRe = new double[9][2][2];
        final double[][][] centerIm = new double[9][2][2];
        final int[] counter = new int[9];
        final double noDataValue = srcBandList.srcBands[0].getNoDataValue();

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (final Rectangle rectangle : tileRectangles) {
                op.checkIfCancelled();

                final Thread worker = new Thread() {

                    final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
                    final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];

                    final double[][] Cr = new double[2][2]; // real part of covariance matrix
                    final double[][] Ci = new double[2][2]; // imaginary part of covariance matrix

                    @Override
                    public void run() {
                        final int x0 = rectangle.x, y0 = rectangle.y;
                        final int w = rectangle.width, h = rectangle.height;
                        final int xMax = x0 + w, yMax = y0 + h;
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
                                if (noData(noDataValue, dataBuffers, srcIndex.getIndex(x)))
                                    continue;

                                DualPolOpUtils.getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, srcWidth,
                                        srcHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                                HAlphaC2.HAAlpha data = HAlphaC2.computeHAAlphaByC2(Cr, Ci);

                                if (!Double.isNaN(data.entropy) && !Double.isNaN(data.anisotropy) && !Double.isNaN(data.alpha)) {
                                    synchronized (counter) {
                                        final int zoneIndex = HaAlphaDescriptor.getZoneIndex(data.entropy, data.alpha,
                                                useLeeHAlphaPlaneDefinition);
                                        counter[zoneIndex - 1] += 1;
                                        computeSummationOfC2(zoneIndex, Cr, Ci, sumRe, sumIm);
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
                    for (int i = 0; i < 2; ++i) {
                        for (int j = 0; j < 2; ++j) {
                            centerRe[z][i][j] = sumRe[z][i][j] / count;
                            centerIm[z][i][j] = sumIm[z][i][j] / count;
                        }
                    }
                    clusterCenters[targetBandIndex][z] = new ClusterInfoC2();
                    clusterCenters[targetBandIndex][z].setClusterCenter(z + 1, centerRe[z], centerIm[z], counter[z]);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " computeInitialClusterCenters ", e);
        } finally {
            status.done();
        }

    }

    /**
     * Compute final cluster centers for all 9 zones using K-mean clustering method
     *
     * @param targetBandIndex Target band index
     * @param srcBandList     the input bands
     * @param tileRectangles  Array of rectangles for all source tiles of the image
     * @param op              the operator
     */
    private void computeFinalClusterCenters(final int targetBandIndex,
                                            final PolBandUtils.PolSourceBand srcBandList,
                                            final Rectangle[] tileRectangles,
                                            final PolarimetricClassificationOp op) {

        final double[][][] centerRe = new double[9][2][2];
        final double[][][] centerIm = new double[9][2][2];
        boolean endIteration = false;
        final double noDataValue = srcBandList.srcBands[0].getNoDataValue();

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length * maxIterations,
                "Computing Final Cluster Centres... ");
        int tileCnt = 0;

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (int it = 0; (it < maxIterations && !endIteration); ++it) {
                //System.out.println("Iteration: " + it);

                final double[][][] sumRe = new double[9][2][2];
                final double[][][] sumIm = new double[9][2][2];
                final int[] counter = new int[9];

                for (final Rectangle rectangle : tileRectangles) {

                    final Thread worker = new Thread() {

                        final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
                        final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];

                        final double[][] Cr = new double[2][2];
                        final double[][] Ci = new double[2][2];

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
                                    if (noData(noDataValue, dataBuffers, srcIndex.getIndex(x)))
                                        continue;

                                    DualPolOpUtils.getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, srcWidth,
                                            srcHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                                    synchronized (counter) {
                                        final int zoneIdx = findZoneIndex(Cr, Ci, clusterCenters[targetBandIndex]);
                                        counter[zoneIdx - 1]++;
                                        computeSummationOfC2(zoneIdx, Cr, Ci, sumRe, sumIm);
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
                        for (int i = 0; i < 2; ++i) {
                            for (int j = 0; j < 2; ++j) {
                                centerRe[z][i][j] = sumRe[z][i][j] / count;
                                centerIm[z][i][j] = sumIm[z][i][j] / count;
                                diff += (clusterCenters[targetBandIndex][z].centerRe[i][j] - centerRe[z][i][j]) *
                                        (clusterCenters[targetBandIndex][z].centerRe[i][j] - centerRe[z][i][j]) +
                                        (clusterCenters[targetBandIndex][z].centerIm[i][j] - centerIm[z][i][j]) *
                                                (clusterCenters[targetBandIndex][z].centerIm[i][j] - centerIm[z][i][j]);
                            }
                        }
                        clusterCenters[targetBandIndex][z].setClusterCenter(z + 1, centerRe[z], centerIm[z], counter[z]);
                    }
                }

                if (diff == 0) {
                    endIteration = true;
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " computeFinalClusterCenters ", e);
        } finally {
            status.done();
        }

    }

    /**
     * Find the nearest cluster for a given C2 matrix using Wishart distance
     *
     * @param Cr             Real part of the C2 matrix
     * @param Ci             Imaginary part of the C2 matrix
     * @param clusterCenters The cluster centers
     * @return The zone index for the nearest cluster
     */

    public static int findZoneIndex(final double[][] Cr, final double[][] Ci, final ClusterInfoC2[] clusterCenters) {
        double minDistance = Double.MAX_VALUE;
        int zoneIndex = -1;
        for (int z = 0; z < clusterCenters.length; ++z) {
            if (clusterCenters[z] != null) {
                final double d = computeWishartDistance(Cr, Ci, clusterCenters[z]);
                if (minDistance > d) {
                    minDistance = d;
                    zoneIndex = z + 1;
                }
            }
        }

        return zoneIndex;
    }

    /**
     * Compute Wishart distance for given 2x2 covariance matrix and given cluster
     *
     * @param Cr      Real part of the 2x2 covariance matrix
     * @param Ci      Imaginary part of the 2x2 covariance matrix
     * @param cluster The cluster object
     * @return The Wishart distance
     */

    static double computeWishartDistance(final double[][] Cr, final double[][] Ci, final ClusterInfoC2 cluster) {

        return cluster.invCenterRe[0][0] * Cr[0][0] + cluster.invCenterRe[1][1] * Cr[1][1] +
                2 * (cluster.invCenterRe[0][1] * Cr[0][1] + cluster.invCenterIm[0][1] * Ci[0][1]) + cluster.logDet;
    }

    /**
     * Compute determinant of a 2x2 Hermitian matrix
     *
     * @param Cr Real part of the 2x2 Hermitian matrix
     * @param Ci Imaginary part of the 2x2 Hermitian matrix
     * @return The determinant
     */
    private static double determinantCmplxMatrix2(final double[][] Cr, final double[][] Ci) {
        double detR = Cr[0][0] * Cr[1][1] - Cr[0][1] * Cr[0][1] - Ci[0][1] * Ci[0][1];
        if (detR < PolOpUtils.EPS) {
            detR = PolOpUtils.EPS;
        }
        return detR;
    }
    /**
     * Compute inverse of a 2x2 Hermitian matrix
     *
     * @param Cr Real part of the 2x2 Hermitian matrix
     * @param Ci Imaginary part of the 2x2 Hermitian matrix
     * @param iCr Real part of the inversed 2x2 Hermitian matrix
     * @param iCi Imaginary part of the inversed 2x2 Hermitian matrix
     */
    private static void inverseCmplxMatrix2(final double[][] Cr, final double[][] Ci, double[][] iCr, double[][] iCi) {
        iCr[0][0] = Cr[1][1];
        iCi[0][0] = 0.0;
        iCr[0][1] = -Cr[0][1];
        iCi[0][1] = -Ci[0][1];
        iCr[1][0] = -Cr[0][1];
        iCi[1][0] = Ci[0][1];
        iCr[1][1] = Cr[0][0];
        iCi[1][1] = 0.0;
        final double det = determinantCmplxMatrix2(Cr, Ci);
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
                iCr[i][j] /= det;
                iCi[i][j] /= det;
            }
        }
    }

    public static class ClusterInfoC2 {
        int zoneIndex;
        int size;
        double logDet;
        final double[][] centerRe = new double[2][2];
        final double[][] centerIm = new double[2][2];
        final double[][] invCenterRe = new double[2][2];
        final double[][] invCenterIm = new double[2][2];
        public ClusterInfoC2() {
        }
        public void setClusterCenter(final int zoneIdx, final double[][] Cr, final double[][] Ci, final int size) {
            this.zoneIndex = zoneIdx;
            this.size = size;
            for (int i = 0; i < 2; ++i) {
                for (int j = 0; j < 2; ++j) {
                    this.centerRe[i][j] = Cr[i][j];
                    this.centerIm[i][j] = Ci[i][j];
                }
            }
            this.logDet = Math.log(Math.abs(determinantCmplxMatrix2(Cr, Ci)));
            inverseCmplxMatrix2(Cr, Ci, invCenterRe, invCenterIm);
        }
    }
}
