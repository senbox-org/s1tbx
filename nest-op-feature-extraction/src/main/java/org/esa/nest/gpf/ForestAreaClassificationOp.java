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
package org.esa.nest.gpf;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.eo.Constants;

import java.awt.*;
import java.util.ArrayList;

/**
 * The operator perform land cover classification using maximum likelihood classifier with the following
 * texture features computed from GLCM:
 * 1. Contrast
 * 2. Dissimilarity (DIS)
 * 3. Homogeneity (HOM)
 * 4. Angular Second Moment (ASM)
 * 5. Energy
 * 6. Maximum Probability (MAX)
 * 7. Entropy (ENT)
 * 8. GLCM Mean
 * 9. GLCM Variance
 * 10. GLCM Correlation
 *
 * Pixels with ratio value in range [3.76, 6.55] are initially classified as forest. Other classes are
 * selected based on the number of classes and their ratio value ranges.
 */

@OperatorMetadata(alias="Forest-Area-Classification",
                  category = "Classification\\Feature Extraction",
                  authors = "Jun Lu, Luis Veci",
                  copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
                  description="Detect forest area")
public final class ForestAreaClassificationOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "The number of classes", interval = "[3, 20]", defaultValue = "3",
               label="Number of Classes")
    private int numClasses = 3;

    @Parameter(description = "The maximum number of iterations", interval = "[1, 100]", defaultValue = "10",
               label="Maximum Number of Iterations")
    private int maxIterations = 10;

    @Parameter(description = "The convergence threshold", interval = "[1, 100]", defaultValue = "95",
               label="Convergence Threshold (%)")
    private int convergenceThreshold = 95;

    private int srcWidth = 0;
    private int srcHeight = 0;
    private boolean clusterCentersComputed = false;
    private byte[][] mask = null; // record for each pixel the class index (1: forest, 2 to numClasses: others)
    private double T_Ratio_Low = 3.76;
    private double T_Ratio_High = 6.55;
    private String[] srcBandNames = null;


    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        srcWidth = sourceProduct.getSceneRasterWidth();
        srcHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    srcWidth,
                                    srcHeight);

        addSelectedBands();

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        boolean hasRatio = false;
        srcBandNames = new String[sourceBandNames.length];

        int k = 1;
        for (String bandName : sourceBandNames) {
            if(!hasRatio && bandName.contains("ratio")) {
                srcBandNames[0] = bandName;
                hasRatio = true;
                continue;
            }

            srcBandNames[k++] = bandName;
        }

        if (!hasRatio) {
            throw new OperatorException("Please select ratio and feature bands.");
        }

        for (String bandName : sourceBandNames) {
            ProductUtils.copyBand(bandName, sourceProduct, bandName, targetProduct, true);
        }

        final Band targetBand = new Band("land_cover_classes",
                                         ProductData.TYPE_UINT8,
                                         targetProduct.getSceneRasterWidth(),
                                         targetProduct.getSceneRasterHeight());

        targetBand.setUnit("class_index");
        targetBand.setNoDataValue(255);
        targetBand.setNoDataValueUsed(true);
        targetProduct.addBand(targetBand);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        if (!clusterCentersComputed) {
            performClustering();
        }

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int tx0 = targetRectangle.x;
        final int ty0 = targetRectangle.y;
        final int tw  = targetRectangle.width;
        final int th  = targetRectangle.height;
        final int maxY = ty0 + th;
        final int maxX = tx0 + tw;
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(targetTile);
        //System.out.println("x0 = " + tx0 + ", y0 = " + ty0 + ", w = " + tw + ", h = " + th);

        for (int y = ty0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = tx0; x < maxX; ++x) {
                targetData.setElemIntAt(trgIndex.getIndex(x), mask[y][x]);
            }
        }
    }

    /**
     * Compute centers for all clusters
     */
    private synchronized void performClustering() {

        if (clusterCentersComputed) {
            return;
        }

        final java.util.List<ClusterInfo> clusterList = new ArrayList<>(numClasses);
        mask = new byte[srcHeight][srcWidth];
        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(sourceProduct, tileSize, 0);

        computeInitialClusterCenters(clusterList, tileRectangles);

        computeClusterCovarianceMatrices(clusterList, tileRectangles);

        computeFinalClusterCenters(clusterList, tileRectangles);

        clusterCentersComputed = true;
    }

    /**
     * Compute initial cluster centers.
     * @param clusterList Cluster list.
     * @param tileRectangles Tile rectangle array.
     */
    private void computeInitialClusterCenters(final java.util.List<ClusterInfo> clusterList,
                                              final Rectangle[] tileRectangles) {

        setInitialClusterBoundaries(clusterList);

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Creating Initial Clusters... ");
        int tileCnt = 0;

        final int numSrcBands = srcBandNames.length;

        final int[] counter = new int[numClasses]; // number of pixels in each class

        final ThreadManager threadManager = new ThreadManager();

        final double[][] clusterSum = new double[numClasses][numSrcBands-1];

        try {
            for (final Rectangle rectangle : tileRectangles) {
                checkForCancellation();

                final Thread worker = new Thread() {

                    final Tile[] sourceTiles = new Tile[numSrcBands];
                    final ProductData[] dataBuffers = new ProductData[numSrcBands];
                    final double[] u = new double[numSrcBands-1];

                    @Override
                    public void run() {
                        final int x0 = rectangle.x;
                        final int y0 = rectangle.y;
                        final int w = rectangle.width;
                        final int h = rectangle.height;
                        final int xMax = x0 + w;
                        final int yMax = y0 + h;
                        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

                        for (int i = 0; i < numSrcBands; ++i) {
                            sourceTiles[i] = getSourceTile(sourceProduct.getBand(srcBandNames[i]), rectangle);
                            dataBuffers[i] = sourceTiles[i].getDataBuffer();
                        }

                        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                        for (int y = y0; y < yMax; ++y) {
                            srcIndex.calculateStride(y);
                            for (int x = x0; x < xMax; ++x) {
                                final int idx = srcIndex.getIndex(x);

                                final double ratio = dataBuffers[0].getElemDoubleAt(idx);

                                getCurrentPoint(idx, dataBuffers, u);

                                synchronized (counter) {

                                    if (!Double.isNaN(ratio)){
                                        for (int i = 0; i < numClasses; i++) {
                                            if (ratio >= clusterList.get(i).initLowBound &&
                                                ratio < clusterList.get(i).initHighBound) {
                                                mask[y][x] = (byte)i;
                                                addClusterSum(i, clusterSum, u);
                                                counter[i]++;
                                                break;
                                            }
                                        }
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

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId()+ " computeInitialClusterCenters ", e);
        } finally {
            status.done();
        }

        double[] center = new double[numSrcBands-1];
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numSrcBands-1; j++) {
                center[j] = clusterSum[i][j]/counter[i];
            }
            clusterList.get(i).setClusterCenter(center, counter[i]);
        }
    }

    private void setInitialClusterBoundaries(final java.util.List<ClusterInfo> clusterList) {

        final Band ratio = sourceProduct.getBand(srcBandNames[0]);
        final double bandMin = ratio.getStx(true,ProgressMonitor.NULL).getMinimum();
        final double bandMax = ratio.getStx(true,ProgressMonitor.NULL).getMaximum();
        final int numLowerClasses = Math.max(1, (int)Math.round((T_Ratio_Low - bandMin)/(bandMax - bandMin -
                                                                 T_Ratio_High + T_Ratio_Low)*(numClasses - 1)));
        final int numHighClasses = numClasses - 1 - numLowerClasses;
        final double dl = (T_Ratio_Low - bandMin) / numLowerClasses;
        final double dh = (bandMax - T_Ratio_High) / numHighClasses;

        for (int i = 0; i < numClasses; i++) {
            ClusterInfo cluster = new ClusterInfo(i);
            if (i == 0) {
                cluster.setInitialClusterBounds(T_Ratio_Low, T_Ratio_High);
            } else if (i <= numLowerClasses) {
                cluster.setInitialClusterBounds(bandMin + (i-1)*dl, bandMin + i*dl);
            } else {
                cluster.setInitialClusterBounds(T_Ratio_High + (i-numLowerClasses-1)*dh, T_Ratio_High + (i-numLowerClasses)*dh);
            }
            clusterList.add(cluster);
        }
    }

    private void getCurrentPoint(final int idx, final ProductData[] dataBuffers, final double[] u) {

        for (int i = 1; i < dataBuffers.length; i++) {
            u[i-1] = dataBuffers[i].getElemDoubleAt(idx);
        }
    }

    private void addClusterSum(final int classIdx, final double[][] clusterSum, final double[] u) {
        for (int j = 0 ; j < u.length; j++) {
            clusterSum[classIdx][j] += u[j];
        }
    }

    /**
     * Compute covariance matrices for all clusters.
     * @param clusterList The cluster list.
     * @param tileRectangles The tile rectangles.
     */
    private void computeClusterCovarianceMatrices(final java.util.List<ClusterInfo> clusterList,
                                                  final Rectangle[] tileRectangles) {

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Computing Cluster Covariance Matrices... ");
        int tileCnt = 0;

        final int numSrcBands = srcBandNames.length;

        final ThreadManager threadManager = new ThreadManager();

        final double[][][] clusterCov = new double[numClasses][numSrcBands-1][numSrcBands-1];

        try {
            for (final Rectangle rectangle : tileRectangles) {
                checkForCancellation();

                final Thread worker = new Thread() {

                    final Tile[] sourceTiles = new Tile[numSrcBands];
                    final ProductData[] dataBuffers = new ProductData[numSrcBands];
                    final double [][] C = new double[numSrcBands-1][numSrcBands-1];
                    final double[] u = new double[numSrcBands-1];

                    @Override
                    public void run() {
                        final int x0 = rectangle.x;
                        final int y0 = rectangle.y;
                        final int w = rectangle.width;
                        final int h = rectangle.height;
                        final int xMax = x0 + w;
                        final int yMax = y0 + h;
                        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

                        for (int i = 0; i < numSrcBands; ++i) {
                            sourceTiles[i] = getSourceTile(sourceProduct.getBand(srcBandNames[i]), rectangle);
                            dataBuffers[i] = sourceTiles[i].getDataBuffer();
                        }

                        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                        for (int y = y0; y < yMax; ++y) {
                            srcIndex.calculateStride(y);
                            for (int x = x0; x < xMax; ++x) {
                                final int idx = srcIndex.getIndex(x);

                                getCurrentPoint(idx, dataBuffers, u);

                                final int classIdx = mask[y][x];

                                computeCovarianceMatrix(clusterList.get(classIdx).center, u, C);

                                synchronized (clusterCov) {

                                    for (int i = 0; i < numSrcBands-1; i++) {
                                        for (int j = 0; j < numSrcBands-1; j++) {
                                            clusterCov[classIdx][i][j] += C[i][j];
                                        }
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

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId()+ " computeClusterCovarianceMatrices ", e);
        } finally {
            status.done();
        }

        for (int c = 0; c < numClasses; c++) {
            for (int i = 0; i < numSrcBands-1; i++) {
                for (int j = 0; j < numSrcBands-1; j++) {
                    clusterCov[c][i][j] /= clusterList.get(c).size;
                }
            }
            clusterList.get(c).setClusterCovarianceMatrix(clusterCov[c]);
        }
    }

    private void computeCovarianceMatrix(final double [] center, final double[] u, final double[][] C) {
        for (int i = 0; i < u.length; i++) {
            for (int j = 0; j < u.length; j++) {
                C[i][j] = (u[i] - center[i])*(u[j] - center[j]);
            }
        }
    }

    private void computeFinalClusterCenters(final java.util.List<ClusterInfo> clusterList,
                                            final Rectangle[] tileRectangles) {

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length*maxIterations,
                "Computing Final Cluster Centres... ");
        int tileCnt = 0;

        final int numSrcBands = srcBandNames.length;
        final int[] clusterCounter = new int[numClasses];
        final int[] clusterPixelChangeCounter = new int[numClasses];
        final double[][] clusterSum = new double[numClasses][numSrcBands-1];

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (int it = 0; it < maxIterations; ++it) {

                java.util.Arrays.fill(clusterCounter, 0);
                java.util.Arrays.fill(clusterPixelChangeCounter, 0);
                for (double[] row:clusterSum) {
                    java.util.Arrays.fill(row, 0.0);
                }

                for (final Rectangle rectangle : tileRectangles) {

                    final Thread worker = new Thread() {

                        final Tile[] sourceTiles = new Tile[numSrcBands];
                        final ProductData[] dataBuffers = new ProductData[numSrcBands];

                        final double[] u = new double[numSrcBands-1];

                        @Override
                        public void run() {
                            checkForCancellation();

                            final int x0 = rectangle.x;
                            final int y0 = rectangle.y;
                            final int w = rectangle.width;
                            final int h = rectangle.height;
                            final int xMax = x0 + w;
                            final int yMax = y0 + h;

                            for (int i = 0; i < numSrcBands; ++i) {
                                sourceTiles[i] = getSourceTile(sourceProduct.getBand(srcBandNames[i]), rectangle);
                                dataBuffers[i] = sourceTiles[i].getDataBuffer();
                            }
                            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                            for (int y = y0; y < yMax; ++y) {
                                srcIndex.calculateStride(y);
                                for (int x = x0; x < xMax; ++x) {
                                    final int idx = srcIndex.getIndex(x);

                                    getCurrentPoint(idx, dataBuffers, u);
                                    int clusterIdx = findClosestCluster(u, clusterList);

                                    synchronized (clusterCounter) {

                                        if (mask[y][x] != clusterIdx) {
                                            clusterPixelChangeCounter[mask[y][x]]++;
                                            mask[y][x] = (byte)clusterIdx;
                                        }
                                        addClusterSum(clusterIdx, clusterSum, u);
                                        clusterCounter[clusterIdx]++;
                                    }
                                }
                            }
                        }
                    };
                    threadManager.add(worker);

                    status.worked(tileCnt++);
                }
                threadManager.finish();


                if (isConvergent(clusterList, clusterPixelChangeCounter)){
                    break;
                }

                updateClusterCenter(clusterList, clusterCounter, clusterSum);

                computeClusterCovarianceMatrices(clusterList, tileRectangles);
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId()+ " computeFinalClusterCenters ", e);
        } finally {
            status.done();
        }
    }

    private int findClosestCluster(final double[] u, final java.util.List<ClusterInfo> clusterList) {

        double minDistance = Double.MAX_VALUE;
        int clusterIndex = -1;
        for (int c = 0; c < clusterList.size(); ++c) {
            final double d = computeMLD(u, clusterList.get(c));
            if (minDistance > d) {
                minDistance = d;
                clusterIndex = c;
            }
        }

        return clusterIndex;
    }

    private double computeMLD(final double[] u, final ClusterInfo cluster) {

        Matrix uMat = new Matrix(u, u.length);
        Matrix uMatNew = uMat.minus(new Matrix(cluster.center, cluster.center.length));
        return uMatNew.transpose().times(cluster.invCov).times(uMatNew).get(0,0) + cluster.logDet;
    }

    private boolean isConvergent(final java.util.List<ClusterInfo> clusterList, final int[] clusterPixelChangeCounter) {

        for (int c = 0; c < numClasses; c++) {
            final double unchangedPercentage = 100.0*(1.0 - (double)clusterPixelChangeCounter[c] / (double)clusterList.get(c).size);
            if (unchangedPercentage < convergenceThreshold) {
                return false;
            }
        }
        return true;
    }

    private static void updateClusterCenter(final java.util.List<ClusterInfo> clusterList, final int[] clusterCounter,
                                            final double[][] clusterSum) {

        for (int c = 0; c < clusterList.size(); c++) {
            final double[] center = new double[clusterList.get(c).center.length];
            for (int i = 0; i < center.length; i++) {
                center[i] = clusterSum[c][i] / clusterCounter[c];
            }
            clusterList.get(c).setClusterCenter(center, clusterCounter[c]);
        }
    }


    public static class ClusterInfo {
        int classIndex;
        int size;
        double initLowBound;
        double initHighBound;
        double[] center = null;
        double logDet;
        Matrix invCov = null;

        public ClusterInfo(final int classIdx) {
            this.classIndex = classIdx;
        }

        public void setInitialClusterBounds(final double lowBound, final double highBound) {
            this.initLowBound = lowBound;
            this.initHighBound = highBound;
        }

        public void setClusterCenter(final double[] center, final int size) {
            this.size = size;
            this.center = new double[center.length];
            System.arraycopy(center, 0, this.center, 0, center.length);
        }

        public void setClusterCovarianceMatrix(final double[][] Cov) {
            final Matrix CMat = new Matrix(Cov);
            this.logDet = Math.log(Math.max(Math.abs(CMat.det()), Constants.EPS));
            this.invCov = CMat.inverse();
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ForestAreaClassificationOp.class);
        }
    }
}