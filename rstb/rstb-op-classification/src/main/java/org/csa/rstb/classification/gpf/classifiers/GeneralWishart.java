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

import org.csa.rstb.classification.gpf.PolarimetricClassificationOp;
import org.csa.rstb.polarimetric.gpf.PolOpUtils;
import org.csa.rstb.polarimetric.gpf.PolarimetricDecompositionOp;
import org.csa.rstb.polarimetric.gpf.decompositions.Cloude;
import org.csa.rstb.polarimetric.gpf.decompositions.FreemanDurden;
import org.csa.rstb.polarimetric.gpf.decompositions.GeneralizedFreemanDurden;
import org.csa.rstb.polarimetric.gpf.decompositions.Pauli;
import org.csa.rstb.polarimetric.gpf.decompositions.Sinclair;
import org.csa.rstb.polarimetric.gpf.decompositions.Touzi;
import org.csa.rstb.polarimetric.gpf.decompositions.Yamaguchi;
import org.csa.rstb.polarimetric.gpf.decompositions.hAAlpha;
import org.csa.rstb.polarimetric.gpf.decompositions.vanZyl;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**

 */
public class GeneralWishart extends PolClassifierBase implements PolClassifier {

    private final static String TERRAIN_CLASS = "General_wishart_class";
    private int numCategories;
    private int numInitialClusters; // number of initial clusters in each category

    private boolean clusterCentersComputed = false;
    private int maxIterations;
    private int numFinalClasses;
    private String decomposition = null;
    private String[] indexName = null;
    private String[] description = null;

    private int[][] category = null; // pixel category index
    private int[][] cluster = null;  // pixel cluster index

    private double mixedCategoryThreshold;
    private int maxClusterSize = 0;

    private ArrayList<ArrayList<Integer>> colourIndexMap = null;

    public GeneralWishart(final PolBandUtils.MATRIX srcProductType,
                          final int srcWidth, final int srcHeight, final int windowSize,
                          final Map<Band, PolBandUtils.PolSourceBand> bandMap,
                          final int maxIterations, final int numInitialClasses, final int numClasses,
                          final double mixedCategoryThreshold, final String decomposition,
                          final PolarimetricClassificationOp op) {
        super(srcProductType, srcWidth, srcHeight, windowSize, windowSize, bandMap, op);

        setIndexCodingParameters(decomposition);
        this.maxIterations = maxIterations;
        this.numFinalClasses = numClasses;
        this.numInitialClusters = numInitialClasses / numCategories;
        this.mixedCategoryThreshold = mixedCategoryThreshold;
        this.decomposition = decomposition;
    }

    @Override
    public boolean canProcessStacks() {
        return false;
    }

    /**
     * Return the band name for the target product
     *
     * @return band name
     */
    public String getTargetBandName() {
        return TERRAIN_CLASS;
    }

    /**
     * returns the number of classes
     *
     * @return num classes
     */
    public int getNumClasses() {
        return numFinalClasses;
    }

    public IndexCoding createIndexCoding() {
        final IndexCoding indexCoding = new IndexCoding("Cluster_classes");
        indexCoding.addIndex("no data", HAlphaWishart.NODATACLASS, "no data");

        for (int i = 0; i < numCategories; ++i) {
            for (int j = 1; j <= numInitialClusters; ++j) {
//            for (int j = 1; j <= colourIndexMap.get(i).size(); ++j) {
                indexCoding.addIndex(indexName[i] + "_" + j, j, description[i] + " " + j);
            }
        }

        return indexCoding;
    }

    private void setIndexCodingParameters(final String decomposition) {

        switch(decomposition) {
            case PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"sinclair_r", "sinclair_g", "sinclair_b"};
                description = new String[]{"Sinclair_r", "Sinclair_g", "Sinclair_b"};
                break;
            case PolarimetricDecompositionOp.PAULI_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"pauli_r", "pauli_g", "pauli_b"};
                description = new String[]{"Pauli_r", "Pauli_g", "Pauli_b"};
                break;
            case PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"vol", "dbl", "surf"};
                description = new String[]{"Volume", "Double", "Surface"};
                break;
            case PolarimetricDecompositionOp.GENERALIZED_FREEMAN_DURDEN_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"vol", "dbl", "surf"};
                description = new String[]{"Volume", "Double", "Surface"};
                break;
            case PolarimetricDecompositionOp.VANZYL_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"vol", "dbl", "surf"};
                description = new String[]{"Volume", "Double", "Surface"};
                break;
            case PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"vol", "dbl", "surf"};
                description = new String[]{"Volume", "Double", "Surface"};
                break;
            case PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION:
                numCategories = 3;
                indexName = new String[]{"entropy", "anisotropy", "alpha"};
                description = new String[]{"Entropy", "Anisotropy", "Alpha"};
                break;
            case PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION:
                numCategories = 4;
                indexName = new String[]{"vol", "dbl", "surf", "hlx"};
                description = new String[]{"Volume", "Double", "Surface", "Helix"};
                break;
            case PolarimetricDecompositionOp.TOUZI_DECOMPOSITION:
                numCategories = 4;
                indexName = new String[]{"psi", "tau", "alpha", "phi"};
                description = new String[]{"Psi", "Tau", "Alpha", "Phi"};
                break;
        }
    }

    /**
     * Perform decomposition for given tile.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Band targetBand, final Tile targetTile) {
        PolBandUtils.PolSourceBand srcBandList = bandMap.get(targetBand);

        if (!clusterCentersComputed) {
            computeTerrainClusterCenters(srcBandList, op);
        }

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(targetTile);
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                targetData.setElemIntAt(trgIndex.getIndex(x), getOutputClusterIndex(x, y));
            }
        }
    }

    /**
     * Compute centers for all numClasses clusters
     *
     * @param srcBandList the input bands
     * @param op          the operator
     */
    private synchronized void computeTerrainClusterCenters(final PolBandUtils.PolSourceBand srcBandList,
                                                           final PolarimetricClassificationOp op) {

        if (clusterCentersComputed) {
            return;
        }

        category = new int[srcHeight][srcWidth];
        cluster = new int[srcHeight][srcWidth];
        final double[][] dominantPower = new double[srcHeight][srcWidth];
        final ArrayList<ArrayList<ClusterInfo>> clusterCenterList = new ArrayList<ArrayList<ClusterInfo>>(numCategories);
        maxClusterSize = 2 * srcHeight * srcWidth / numFinalClasses;

        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(op.getSourceProduct(), tileSize, 0);

        computeInitialTerrainClusterCenters(dominantPower, clusterCenterList, srcBandList, tileRectangles, op);

        computeFinalTerrainClusterCenters(dominantPower, clusterCenterList, srcBandList, tileRectangles, op);

        clusterCentersComputed = true;
    }

    /**
     * Compute initial cluster centers for clusters in all 3 categories: vol, dbl, suf.
     *
     * @param dominantPower     dominant power of each pixel
     * @param clusterCenterList list of cluster centers of all categories
     * @param srcBandList       the input bands
     * @param tileRectangles    array of rectangles for all source tiles of the image
     * @param op                the operator
     */
    private void computeInitialTerrainClusterCenters(final double[][] dominantPower,
                                                     final ArrayList<ArrayList<ClusterInfo>> clusterCenterList,
                                                     final PolBandUtils.PolSourceBand srcBandList,
                                                     final Rectangle[] tileRectangles,
                                                     final PolarimetricClassificationOp op) {

        try {
            // Step 1. Create initial 30 clusters in each of the 3 categories (vol, dbl, surf).
            //System.out.println("Step 1");
            createInitialClusters(dominantPower, srcBandList, tileRectangles, op);

            // Step 2. Compute cluster centers for all 90 clusters in the 3 categories
            //System.out.println("Step 2");

            final ClusterCenter[][] clusterCenters = new ClusterCenter[numCategories][numInitialClusters];
            for (int i = 0; i < numCategories; ++i) {
                for (int j = 0; j < numInitialClusters; ++j) {
                    clusterCenters[i][j] = new ClusterCenter();
                }
            }

            getClusterCenters(clusterCenters, srcBandList, tileRectangles, op);

            // Step 3. Merge small clusters in each category until user specified total number of clusters is reached
            //System.out.println("Step 3");
            double[][] centerRe = new double[3][3];
            double[][] centerIm = new double[3][3];
            for (int i = 0; i < numCategories; ++i) {
                ArrayList<ClusterInfo> centerList= new ArrayList<>(numInitialClusters);
                for (int j = 0; j < numInitialClusters; ++j) {
                    if (clusterCenters[i][j].size > 0) {
                        clusterCenters[i][j].getCenter(centerRe, centerIm);
                        ClusterInfo clusterInfo = new ClusterInfo();
                        clusterInfo.setClusterCenter(j, centerRe, centerIm, clusterCenters[i][j].size);
                        centerList.add(clusterInfo);
                    }
                }
                clusterCenterList.add(centerList);
            }

            mergeInitialClusters(clusterCenterList);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " computeInitialClusterCenters ", e);
        }
    }

    /**
     * Create 30 initial clusters in each of the 3 categories (vol, dbl and surf).
     * The pixels are first classified into 4 categories (vol, dbl, urf and mixed) based on its Freeman-Durden
     * decomposition result. Then pixels in each category (not include mixed) are grouped into 30 clusters based
     * on their power values.
     *
     * @param srcBandList    the input bands
     * @param tileRectangles Array of rectangles for all source tiles of the image
     * @param op             the operator
     */
    private void createInitialClusters(final double[][] dominantPower,
                                       final PolBandUtils.PolSourceBand srcBandList,
                                       final Rectangle[] tileRectangles,
                                       final PolarimetricClassificationOp op) {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Creating Initial Clusters... ", tileRectangles.length);
        final ThreadManager threadManager = new ThreadManager();

        final int[] counter = new int[numCategories + 1]; // number of pixels in all categories, last category for mixed
        final double[][] pwr = new double[numCategories + 1][srcHeight * srcWidth];

        try {
            for (final Rectangle rectangle : tileRectangles) {
                op.checkIfCancelled();

                final Thread worker = new Thread() {

                    final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
                    final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];

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

                                final double[] power = getDecompositionPower(x, y, srcIndex, sourceTiles, dataBuffers);
                                if (power == null) {
                                    continue;
                                }

                                double domPower = -Double.MAX_VALUE;
                                double totalPower = 0.0;
                                int domPowerCategory = -1;
                                boolean validPixel = true;
                                for (int i = 0; i < numCategories; ++i) {
                                    if (Double.isNaN(power[i])) {
                                        validPixel = false;
                                        break;
                                    } else {
                                        totalPower += power[i];
                                        if (domPower < power[i]) {
                                            domPower = power[i];
                                            domPowerCategory = i;
                                        }
                                    }
                                }

                                int pixelCategory;
                                double pixelPower;
                                if (domPower / totalPower <= mixedCategoryThreshold) {
                                    pixelCategory = numCategories; // mixed
                                    pixelPower = totalPower / numCategories;
                                } else {
                                    pixelCategory = domPowerCategory;
                                    pixelPower = domPower;
                                }

                                synchronized (counter) {
                                    if (validPixel) {
                                        category[y][x] = pixelCategory;
                                        dominantPower[y][x] = pixelPower;
                                        pwr[pixelCategory][counter[pixelCategory]] = pixelPower;
                                        counter[pixelCategory] += 1;
                                    }
                                }
                            }
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(1);
            }
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " createInitialClusters ", e);
        } finally {
            status.done();
        }

        final double[][] pwrThreshold = new double[numCategories][numInitialClusters - 1];
        for (int i = 0; i < numCategories; ++i) {
            final int clusterSize = counter[i] / numInitialClusters;
            if (clusterSize > 0) {
                Arrays.sort(pwr[i], 0, counter[i] - 1);
            }

            for (int j = 0; j < numInitialClusters - 1; ++j) {
                pwrThreshold[i][j] = pwr[i][(j + 1) * clusterSize];
            }
        }

        // classify pixels into clusters within each category, record number of pixels in each cluster
        for (int y = 0; y < srcHeight; ++y) {
            for (int x = 0; x < srcWidth; ++x) {
                for (int i = 0; i < numCategories; ++i) {
                    if (category[y][x] == i) {
                        cluster[y][x] = computePixelClusterIdx(dominantPower[y][x], pwrThreshold[i], numInitialClusters);
                    }
                }
            }
        }
    }

    private double[] getDecompositionPower(
            final int x, final int y, final TileIndex srcIndex, final Tile[] sourceTiles, final ProductData[] dataBuffers) {

        final double[][] Mr = new double[3][3];
        final double[][] Mi = new double[3][3];

        switch(decomposition) {
            case PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION:

                PolOpUtils.getCovarianceMatrixC3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Mr, Mi);

                final Sinclair.RGB sdd = Sinclair.getSinclairDecomposition(Mr, Mi);

                return new double[]{sdd.r, sdd.g, sdd.b};

            case PolarimetricDecompositionOp.PAULI_DECOMPOSITION:

                PolOpUtils.getCovarianceMatrixC3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Mr, Mi);

                final Pauli.RGB pdd = Pauli.getPauliDecomposition(Mr, Mi);

                return new double[]{pdd.r, pdd.g, pdd.b};

            case PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION:

                PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                        sourceProductType, sourceTiles, dataBuffers, Mr, Mi);

                final FreemanDurden.FDD fdd = FreemanDurden.getFreemanDurdenDecomposition(Mr, Mi);

                return new double[]{fdd.pv, fdd.pd, fdd.ps};

            case PolarimetricDecompositionOp.GENERALIZED_FREEMAN_DURDEN_DECOMPOSITION:

                PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                        sourceProductType, sourceTiles, dataBuffers, Mr, Mi);

                final GeneralizedFreemanDurden.FDD gfdd =
                        GeneralizedFreemanDurden.getGeneralizedFreemanDurdenDecomposition(Mr, Mi);

                return new double[]{gfdd.pv, gfdd.pd, gfdd.ps};

            case PolarimetricDecompositionOp.VANZYL_DECOMPOSITION:

                PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                        sourceProductType, sourceTiles, dataBuffers, Mr, Mi);

                final vanZyl.VDD vdd = vanZyl.getVanZylDecomposition(Mr, Mi);

                return new double[]{vdd.pv, vdd.pd, vdd.ps};

            case PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION:

                PolOpUtils.getCoherencyMatrixT3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Mr, Mi);

                final Cloude.RGB cdd = Cloude.getCloudeDecomposition(Mr, Mi);

                return new double[]{cdd.r, cdd.g, cdd.b};

            case PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION:

                PolOpUtils.getCoherencyMatrixT3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Mr, Mi);

                final hAAlpha.HAAlpha hdd = hAAlpha.computeHAAlpha(Mr, Mi);

                return new double[]{hdd.entropy, hdd.anisotropy, hdd.alpha};

            case PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION:

                PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                        sourceProductType, sourceTiles, dataBuffers, Mr, Mi);

                final Yamaguchi.YDD ydd = Yamaguchi.getYamaguchiDecomposition(Mr, Mi);

                return new double[]{ydd.pv, ydd.pd, ydd.ps, ydd.pc};

            case PolarimetricDecompositionOp.TOUZI_DECOMPOSITION:

                PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSizeX, halfWindowSizeY, srcWidth,
                        srcHeight, sourceProductType, srcIndex, dataBuffers, Mr, Mi);

                final Touzi.TDD tdd = Touzi.getTouziDecomposition(Mr, Mi);

                return new double[]{tdd.psiMean, tdd.tauMean, tdd.alphaMean, tdd.phiMean};
        }

        return null;
    }

    /**
     * Compute the centers of the 90 clusters in the 3 categories.
     *
     * @param clusterCenters    cluster centers of all categories
     * @param srcBandList       the input bands
     * @param tileRectangles    array of rectangles for all source tiles of the image
     * @param op                the operator
     */
    private void getClusterCenters(final ClusterCenter[][] clusterCenters,
                                   final PolBandUtils.PolSourceBand srcBandList,
                                   final Rectangle[] tileRectangles,
                                   final PolarimetricClassificationOp op) {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Computing Initial Cluster Centres... ", tileRectangles.length);

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

                        for (int i = 0; i < sourceTiles.length; ++i) {
                            sourceTiles[i] = op.getSourceTile(srcBandList.srcBands[i], rectangle);
                            dataBuffers[i] = sourceTiles[i].getDataBuffer();
                        }

                        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
                        for (int y = y0; y < yMax; ++y) {
                            srcIndex.calculateStride(y);
                            for (int x = x0; x < xMax; ++x) {

                                if (category[y][x] == numCategories) {
                                    continue;
                                }

                                PolOpUtils.getCoherencyMatrixT3(
                                        srcIndex.getIndex(x), sourceProductType, dataBuffers, Tr, Ti);

                                synchronized (clusterCenters) {
                                    clusterCenters[category[y][x]][cluster[y][x]].addElem(Tr, Ti);
                                }
                            }
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(1);
            }
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " getClusterCenters ", e);
        } finally {
            status.done();
        }
    }

    /**
     * Merge clusters in each category until user specified total number of clusters is reached.
     */
    private void mergeInitialClusters(final ArrayList<ArrayList<ClusterInfo>> clusterCenterList) {

        int totalNumClasses = 0;
        for (int i = 0; i < numCategories; ++i) {
            totalNumClasses += clusterCenterList.get(i).size();
        }

        while (totalNumClasses > numFinalClasses) {

            final int[][] clusterPair = new int[numCategories][2];
            final double[] shortestDistance = new double[numCategories];
            double shortestDistanceOfAll = Double.MAX_VALUE;

            for (int i = 0; i < numCategories; ++i) {
                shortestDistance[i] = computeShortestDistance(clusterCenterList.get(i), clusterPair[i]);
                if (shortestDistance[i] < shortestDistanceOfAll) {
                    shortestDistanceOfAll = shortestDistance[i];
                }
            }

            ArrayList<Integer> categoriesWithSameShortestDistance = new ArrayList<>();
            for (int i = 0; i < numCategories; ++i) {
                if(shortestDistance[i] <= shortestDistanceOfAll) {
                    categoriesWithSameShortestDistance.add(i);
                }
            }

            final int c = getCategoryToMerge(categoriesWithSameShortestDistance, clusterCenterList, clusterPair);

            mergeClusters(clusterCenterList.get(c), clusterPair[c]);

            totalNumClasses--;
            //System.out.println("Total # of Classes: " + totalNumClasses);
        }
        //System.out.println("Step 3 end");
    }

    private int getCategoryToMerge(final ArrayList<Integer> categoriesList,
                                   final ArrayList<ArrayList<ClusterInfo>> clusterCenterList,
                                   final int[][] clusterPair) {

        if (categoriesList.size() == 1) {
            return categoriesList.get(0);
        }

        int minSize = maxClusterSize;
        int minSizeCluster = -1;
        for (int c : categoriesList) {
            final int size = clusterCenterList.get(c).get(clusterPair[c][0]).size +
                    clusterCenterList.get(c).get(clusterPair[c][1]).size;

            if (size < minSize) {
                minSize = size;
                minSizeCluster = c;
            }
        }
        return minSizeCluster;
    }

    /**
     * Classify a pixel with a given power to one of the clusters defined by an array of thresholds.
     *
     * @param value     The power value.
     * @param threshold Array of thresholds.
     * @return Index of the cluster that the pixel is classified into.
     */
    private static int computePixelClusterIdx(
            final double value, final double[] threshold, final int numInitialClusters) {
        for (int i = 0; i < numInitialClusters - 1; i++) {
            if (value < threshold[i]) {
                return i;
            }
        }
        return numInitialClusters - 1;
    }

    /**
     * Find the two clusters with the shortest Wishart distance among all clusters in the given cluster list.
     *
     * @param clusterCenterList The cluster list.
     * @param clusterPair       The indices of the two clusters with the shortest Wishart distance.
     * @return The shortest Wishart distance.
     */
    private double computeShortestDistance(final java.util.List<ClusterInfo> clusterCenterList, final int[] clusterPair) {

        final int numClusters = clusterCenterList.size();
        double shortestDistance = Double.MAX_VALUE;
        if (numClusters <= 3) {
            return shortestDistance;
        }

        double d;
        for (int i = 0; i < numClusters - 1; i++) {
            if (clusterCenterList.get(i).size >= maxClusterSize) {
                continue;
            }

            for (int j = i + 1; j < numClusters; j++) {
                if (clusterCenterList.get(j).size >= maxClusterSize) {
                    continue;
                }

                d = HAlphaWishart.computeWishartDistance(
                        clusterCenterList.get(i).centerRe, clusterCenterList.get(i).centerIm, clusterCenterList.get(j));

                if (d < shortestDistance) {
                    shortestDistance = d;
                    clusterPair[0] = i;
                    clusterPair[1] = j;
                }
            }
        }
        return shortestDistance;
    }

    /**
     * Merge two clusters in the cluster list for given cluster indices.
     *
     * @param clusterCenterList The list of cluster centers.
     * @param clusterPair       Indices of the two clusters to merge.
     */
    private static void mergeClusters(final java.util.List<ClusterInfo> clusterCenterList, final int[] clusterPair) {

        final int idx1 = clusterPair[0];
        final int idx2 = clusterPair[1];
        final int size1 = clusterCenterList.get(idx1).size;
        final int size2 = clusterCenterList.get(idx2).size;
        final int newClusterSize = size1 + size2;

        final double[][] newCenterRe = new double[3][3];
        final double[][] newCenterIm = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                newCenterRe[i][j] = (size1 * clusterCenterList.get(idx1).centerRe[i][j] +
                        size2 * clusterCenterList.get(idx2).centerRe[i][j]) / newClusterSize;

                newCenterIm[i][j] = (size1 * clusterCenterList.get(idx1).centerIm[i][j] +
                        size2 * clusterCenterList.get(idx2).centerIm[i][j]) / newClusterSize;
            }
        }

        if (idx1 < idx2) {
            clusterCenterList.remove(idx2);
            clusterCenterList.remove(idx1);
        } else {
            clusterCenterList.remove(idx1);
            clusterCenterList.remove(idx2);
        }

        ClusterInfo clusterCenter = new ClusterInfo();
        clusterCenter.setClusterCenter(clusterCenterList.size(), newCenterRe, newCenterIm, newClusterSize);
        clusterCenterList.add(clusterCenter);
    }

    /**
     * Compute final cluster centers for all clusters using K-mean clustering method
     *
     * @param srcBandList    the input bands
     * @param tileRectangles Array of rectangles for all source tiles of the image
     * @param op             the operator
     */
    private void computeFinalTerrainClusterCenters(final double[][] dominantPower,
                                                   final ArrayList<ArrayList<ClusterInfo>> clusterCenterList,
                                                   final PolBandUtils.PolSourceBand srcBandList,
                                                   final Rectangle[] tileRectangles,
                                                   final PolarimetricClassificationOp op) {

        boolean endIteration = false;

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Computing Final Cluster Centres... ", tileRectangles.length * maxIterations);

        final ThreadManager threadManager = new ThreadManager();

        try {
            for (int it = 0; (it < maxIterations && !endIteration); ++it) {
                //System.out.println("Iteration: " + it);
//                final long startTime = System.nanoTime();
//                final long endTime;

                ArrayList<ArrayList<ClusterCenter>> newClusterCenters =
                        new ArrayList<ArrayList<ClusterCenter>>(numCategories);

                for (int i = 0; i < numCategories; ++i) {
                    ArrayList<ClusterCenter> newCenters = new ArrayList<>();
                    for (int j = 0; j < clusterCenterList.get(i).size(); ++j) {
                        newCenters.add(new ClusterCenter());
                    }
                    newClusterCenters.add(newCenters);
                }

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
                                for (int x = x0; x < xMax; ++x) {

                                    PolOpUtils.getMeanCoherencyMatrix(
                                            x, y, halfWindowSizeX, halfWindowSizeY, srcWidth, srcHeight,
                                            sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                                    synchronized (newClusterCenters) {

                                        if (category[y][x] != numCategories) {
                                            cluster[y][x] = findClosestCluster(Tr, Ti, clusterCenterList.get(category[y][x]));
                                        } else { // mixed
                                            final int[] CategoryCluster =
                                                    findNearestCategoryCluster(Tr, Ti, clusterCenterList);

                                            category[y][x] = CategoryCluster[0];
                                            cluster[y][x] = CategoryCluster[1];
                                        }
                                        newClusterCenters.get(category[y][x]).get(cluster[y][x]).addElem(Tr, Ti);
                                    }
                                }
                            }
                        }
                    };
                    threadManager.add(worker);

                    status.worked(1);
                }
                threadManager.finish();

                /*
                endTime = System.nanoTime();
                final long duration = endTime - startTime;
                System.out.println("duration = " + duration);
                */
                double[][] centerRe = new double[3][3];
                double[][] centerIm = new double[3][3];
                for (int i = 0; i < numCategories; ++i) {
                    for (int j = 0; j < clusterCenterList.get(i).size(); ++j) {
                        if (newClusterCenters.get(i).get(j).size > 0) {
                            newClusterCenters.get(i).get(j).getCenter(centerRe, centerIm);
                            clusterCenterList.get(i).get(j).setClusterCenter(
                                    j, centerRe, centerIm, newClusterCenters.get(i).get(j).size);
                        }
                    }
                }
            }
            /*
            System.out.println("# of clusters in Pv: " + pvNumClusters);
            System.out.print("Pixels in each Pv cluster: ");
            for (int i = 0; i < pvNumClusters; i++) {
                System.out.print(clusterCounter[0][i] + ", ");
            }
            System.out.println();
            System.out.println("# of clusters in Pd: " + pdNumClusters);
            System.out.print("Pixels in each Pd cluster: ");
            for (int i = 0; i < pdNumClusters; i++) {
                System.out.print(clusterCounter[1][i] + ", ");
            }
            System.out.println();
            System.out.println("# of clusters in Ps: " + psNumClusters);
            System.out.print("Pixels in each Ps cluster: ");
            for (int i = 0; i < psNumClusters; i++) {
                System.out.print(clusterCounter[2][i] + ", ");
            }
            System.out.println();
            */

            // compute average power for each cluster
            final ArrayList<ArrayList<Double>> clusterPower = new ArrayList<ArrayList<Double>>(numCategories);
            colourIndexMap = new ArrayList<ArrayList<Integer>>(numCategories);
            for (int i = 0; i < numCategories; ++i) {
                ArrayList<Double> tmp1 = new ArrayList<>();
                ArrayList<Integer> tmp2 = new ArrayList<>();
                for (int j = 0; j < clusterCenterList.get(i).size(); ++j) {
                    tmp1.add(0.0);
                    tmp2.add(0);
                }
                clusterPower.add(tmp1);
                colourIndexMap.add(tmp2);
            }

            for (int y = 0; y < srcHeight; y++) {
                for (int x = 0; x < srcWidth; x++) {
                    clusterPower.get(category[y][x]).set(cluster[y][x],
                            clusterPower.get(category[y][x]).get(cluster[y][x]) + dominantPower[y][x]);
                }
            }

            for (int i = 0; i < numCategories; ++i) {
                for (int j = 0; j < clusterCenterList.get(i).size(); ++j) {
                    if (clusterCenterList.get(i).get(j).size > 0) {
                        clusterPower.get(i).set(j, clusterPower.get(i).get(j) / clusterCenterList.get(i).get(j).size);
                    }
                }
            }

//            int categoryStartIndex = 0;
            for (int i = 0; i < numCategories; ++i) {
                for (int j = 0; j < clusterPower.get(i).size(); ++j) {
                    colourIndexMap.get(i).set(j, i * numInitialClusters +
                            getColourIndex(j, clusterPower.get(i), numInitialClusters) + 1);
//                    colourIndexMap.get(i).set(j, categoryStartIndex +
//                            getColourIndex(j, clusterPower.get(i), clusterPower.get(i).size()) + 1);
                }
//                categoryStartIndex += clusterPower.get(i).size();
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " computeInitialClusterCenters ", e);
        } finally {
            status.done();
        }
    }

    private int[] findNearestCategoryCluster(final double[][] Tr, final double[][] Ti,
                                             final ArrayList<ArrayList<ClusterInfo>> clusterCenterList) {

        int nearestCluster = -1;
        int nearestCategory = -1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < numCategories; ++i) {
            final int j = findClosestCluster(Tr, Ti, clusterCenterList.get(i));

            final double d = HAlphaWishart.computeWishartDistance(
                    Tr, Ti, clusterCenterList.get(i).get(j));

            if(d < minDistance) {
                minDistance = d;
                nearestCategory = i;
                nearestCluster = j;
            }
        }
        return new int[]{nearestCategory, nearestCluster};
    }

    private static int getColourIndex(
            final int clusterIndex, final ArrayList<Double> pAvgClusterPower, final int numInitialClusters) {
        int n = 0;
        for (double p : pAvgClusterPower) {
            if (p > pAvgClusterPower.get(clusterIndex)) {
                n++;
            }
        }

        final int d = numInitialClusters / pAvgClusterPower.size();
        return n * d;
    }

    /**
     * Find the nearest cluster for a given T3 matrix using Wishart distance
     *
     * @param Tr             Real part of the T3 matrix
     * @param Ti             Imaginary part of the T3 matrix
     * @param clusterCenters The cluster centers
     * @return The zone index for the nearest cluster
     */
    public static int findClosestCluster(final double[][] Tr, final double[][] Ti,
                                         final java.util.List<ClusterInfo> clusterCenters) {

        double minDistance = Double.MAX_VALUE;
        int clusterIndex = -1;
        for (int c = 0; c < clusterCenters.size(); ++c) {
            final double d = HAlphaWishart.computeWishartDistance(Tr, Ti, clusterCenters.get(c));
            if (minDistance > d) {
                minDistance = d;
                clusterIndex = c;
            }
        }

        return clusterIndex;
    }

    private int getOutputClusterIndex(final int x, final int y) {
        return colourIndexMap.get(category[y][x]).get(cluster[y][x]);
    }

    public static class ClusterCenter {
        public double[][] sumRe = new double[3][3];
        public double[][] sumIm = new double[3][3];
        public int size = 0;

        public ClusterCenter() {
        }

        public void addElem(final double[][] Tr, final double[][] Ti) {
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    sumRe[i][j] += Tr[i][j];
                    sumIm[i][j] += Ti[i][j];
                }
            }
            size++;
        }

        public void getCenter(final double[][] Tr, final double[][] Ti) {
            if (size > 0) {
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        Tr[i][j] = sumRe[i][j] / size;
                        Ti[i][j] = sumIm[i][j] / size;
                    }
                }
            }
        }
    }
}
