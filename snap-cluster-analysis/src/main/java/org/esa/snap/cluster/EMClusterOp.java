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
package org.esa.snap.cluster;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Operator for cluster analysis.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "EMClusterAnalysis",
                  category = "Raster/Classification/Unsupervised Classification",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Performs an expectation-maximization (EM) cluster analysis.")
public class EMClusterOp extends Operator {

    private static final int NO_DATA_VALUE = 0xFF;

    @SourceProduct(alias = "source", label = "Source product", description = "The source product")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Number of clusters", description = "Number of clusters", defaultValue = "14", interval = "(0,100]")
    private int clusterCount;
    @Parameter(label = "Number of iterations", description = "Number of iterations", defaultValue = "30", interval = "(0,10000]")
    private int iterationCount;
    @Parameter(label = "Random seed", defaultValue = "31415",
               description = "Seed for the random generator, used for initialising the algorithm.")
    private int randomSeed;
    @Parameter(label = "Source band names",
               description = "The names of the bands being used for the cluster analysis.",
               rasterDataNodeType = Band.class)
    private String[] sourceBandNames;
    @Parameter(label = "ROI-mask",
               description = "The name of the ROI-Mask that should be used.",
               rasterDataNodeType = Mask.class)
    private String roiMaskName;
    @Parameter(label = "Include probabilities", defaultValue = "false",
               description = "Determines whether the posterior probabilities are included as band data.")
    private boolean includeProbabilityBands;

    private transient Comparator<EMCluster> clusterComparator;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient Band[] probabilityBands;
    private transient Roi roi;
    private transient MetadataElement clusterAnalysis;
    private transient volatile ProbabilityCalculator probabilityCalculator;

    public EMClusterOp() {
    }

    public EMClusterOp(Product sourceProduct,
                       int clusterCount,
                       int iterationCount,
                       String[] sourceBandNames,
                       boolean includeProbabilityBands,
                       Comparator<EMCluster> clusterComparator) {
        this.sourceProduct = sourceProduct;
        this.clusterCount = clusterCount;
        this.iterationCount = iterationCount;
        this.sourceBandNames = sourceBandNames;
        this.includeProbabilityBands = includeProbabilityBands;
        this.clusterComparator = clusterComparator;
    }

    @Override
    public void initialize() throws OperatorException {
        sourceBands = collectSourceBands();
        if (roiMaskName != null) {
            ensureSingleRasterSize(Stream.concat(Arrays.stream(sourceBands),
                                                 Stream.of(sourceProduct.getMaskGroup().get(roiMaskName))).toArray(Band[]::new));
        } else {
            ensureSingleRasterSize(sourceBands);
        }

        int width = sourceBands[0].getRasterWidth();
        int height = sourceBands[0].getRasterHeight();
        final String name = sourceProduct.getName() + "_CLUSTERS";
        final String type = sourceProduct.getProductType() + "_CLUSTERS";

        targetProduct = new Product(name, type, width, height);
        if (sourceProduct.getSceneRasterSize().equals(sourceBands[0].getRasterSize())) {
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        }
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        targetProduct.setPreferredTileSize(width, height);

        if (includeProbabilityBands) {
            createProbabilityBands();
        }

        clusterMapBand = new Band("class_indices", ProductData.TYPE_UINT8, width, height);
        clusterMapBand.setDescription("Class_indices");
        clusterMapBand.setNoDataValue(NO_DATA_VALUE);
        clusterMapBand.setNoDataValueUsed(true);
        targetProduct.addBand(clusterMapBand);

        final IndexCoding indexCoding = new IndexCoding("Cluster_classes");
        for (int i = 0; i < clusterCount; i++) {
            indexCoding.addIndex("class_" + (i + 1), i, "Cluster " + (i + 1));
        }
        targetProduct.getIndexCodingGroup().add(indexCoding);
        clusterMapBand.setSampleCoding(indexCoding);

        clusterAnalysis = new MetadataElement("Cluster_Analysis");
        targetProduct.getMetadataRoot().addElement(clusterAnalysis);

        setTargetProduct(targetProduct);
    }

    @Override
    public void dispose() {
        probabilityCalculator = null;

        super.dispose();
    }

    private Band[] collectSourceBands() {
        Band[] srcBands;
        if (sourceBandNames != null && sourceBandNames.length > 0) {
            srcBands = new Band[sourceBandNames.length];
            for (int i = 0; i < sourceBandNames.length; i++) {
                final Band sourceBand = sourceProduct.getBand(sourceBandNames[i]);
                if (sourceBand == null) {
                    throw new OperatorException("Source band not found: " + sourceBandNames[i]);
                }
                srcBands[i] = sourceBand;
            }
        } else {
            srcBands = sourceProduct.getBands();
        }
        return srcBands;
    }

    private void createProbabilityBands() {
        probabilityBands = new Band[clusterCount];
        for (int i = 0; i < clusterCount; ++i) {
            final Band targetBand = targetProduct.addBand("probability_" + i, ProductData.TYPE_FLOAT32);
            targetBand.setUnit("dl");
            targetBand.setDescription("Cluster posterior probabilities");
            targetBand.setNoDataValue(NO_DATA_VALUE);
            targetBand.setNoDataValueUsed(true);
            probabilityBands[i] = targetBand;
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        int totalWork = targetRectangle.height;
        if (probabilityCalculator == null) {
            totalWork += 100;
        }

        pm.beginTask("Computing clusters...", totalWork);
        try {
            synchronized (this) {
                if (probabilityCalculator == null) {
                    probabilityCalculator = performClustering(SubProgressMonitor.create(pm, 100));
                }
            }

            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle);
            }

            final Tile clusterMapTile = targetTileMap.get(clusterMapBand);
            final Tile[] targetTiles = new Tile[clusterCount];
            if (includeProbabilityBands) {
                for (int i = 0; i < targetTiles.length; i++) {
                    targetTiles[i] = targetTileMap.get(probabilityBands[i]);
                }
            }

            final double[] point = new double[sourceTiles.length];
            final double[] posteriors = new double[clusterCount];

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();

                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (roi == null || roi.contains(x, y)) {
                        for (int i = 0; i < sourceTiles.length; i++) {
                            point[i] = sourceTiles[i].getSampleDouble(x, y);
                        }

                        probabilityCalculator.calculate(point, posteriors);

                        if (includeProbabilityBands) {
                            for (int i = 0; i < clusterCount; ++i) {
                                targetTiles[i].setSample(x, y, posteriors[i]);
                            }
                        }
                        clusterMapTile.setSample(x, y, findMaxIndex(posteriors));
                    } else {
                        if (includeProbabilityBands) {
                            for (int i = 0; i < clusterCount; ++i) {
                                targetTiles[i].setSample(x, y, NO_DATA_VALUE);
                            }
                        }
                        clusterMapTile.setSample(x, y, NO_DATA_VALUE);
                    }
                }

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static int findMaxIndex(double[] samples) {
        int index = 0;

        for (int i = 1; i < samples.length; ++i) {
            if (samples[i] > samples[index]) {
                index = i;
            }
        }

        return index;
    }

    private synchronized ProbabilityCalculator performClustering(ProgressMonitor pm) {
        final EMCluster[] clusters;

        try {
            pm.beginTask("Extracting data points...", iterationCount + 100);

            roi = new Roi(sourceProduct, sourceBands, roiMaskName);

            final EMClusterer clusterer = createClusterer(SubProgressMonitor.create(pm, 100));

            for (int i = 0; i < iterationCount; ++i) {
                checkForCancellation();
                clusterer.iterate();
                pm.worked(1);
            }

            if (clusterComparator == null) {
                clusters = clusterer.getClusters();
            } else {
                clusters = clusterer.getClusters(clusterComparator);
            }

            double[][] means = new double[clusterCount][0];
            double[][][] covariances = new double[clusterCount][0][0];
            double[] priorProbabilities = new double[clusterCount];

            for (int i = 0; i < clusterCount; i++) {
                means[i] = clusters[i].getMean();
                covariances[i] = clusters[i].getCovariances();
                priorProbabilities[i] = clusters[i].getPriorProbability();
            }

            ClusterMetaDataUtils.addCenterToIndexCoding(
                    clusterMapBand.getIndexCoding(), sourceBands, means);
            ClusterMetaDataUtils.addCenterToMetadata(
                    clusterAnalysis, sourceBands, means);
            ClusterMetaDataUtils.addEMInfoToMetadata(
                    clusterAnalysis, covariances, priorProbabilities);

            return EMClusterer.createProbabilityCalculator(clusters);
        } catch (Exception e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private EMClusterer createClusterer(ProgressMonitor pm) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        // check ROI size
        int roiSize = 0;
        if (roi == null) {
            roiSize = sceneWidth * sceneHeight;
        } else {
            for (int y = 0; y < sceneHeight; y++) {
                for (int x = 0; x < sceneWidth; x++) {
                    if (roi.contains(x, y)) {
                        roiSize++;
                    }
                }
            }
        }

        if (roiSize < clusterCount) {
            throw new OperatorException("The combination of ROI and valid pixel masks contain " +
                                                roiSize + " pixel. These are too few to initialize the clustering.");
        }

        final double[][] points = new double[roiSize][sourceBands.length];

        try {
            pm.beginTask("Extracting data points...", sourceBands.length * sceneHeight);

            for (int i = 0; i < sourceBands.length; i++) {
                int index = 0;
                for (int y = 0; y < sceneHeight; y++) {
                    checkForCancellation();

                    final Tile sourceTile = getSourceTile(sourceBands[i], new Rectangle(0, y, sceneWidth, 1));
                    for (int x = 0; x < sceneWidth; x++) {
                        if (roi == null || roi.contains(x, y)) {
                            final double sample = sourceTile.getSampleDouble(x, y);
                            points[index][i] = sample;
                            index++;
                        }
                    }

                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }

        return new EMClusterer(points, clusterCount, randomSeed);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(EMClusterOp.class);
        }
    }
}
