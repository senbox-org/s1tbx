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
package org.esa.nest.supervised.svm;

import org.esa.nest.clustering.fuzzykmeans.*;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataElement;
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
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.mahout.clustering.fuzzykmeans.SoftCluster;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;

/**
 * Operator for k-means cluster analysis.
 */
@OperatorMetadata(alias = "SVMClassificationAnalysis",
        category = "SAR Tools\\Speckle Filtering",
        authors = "Emanuela Boros",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Performs a SVMClassificationAnalysis analysis.")
public class SVMClassificationOp extends Operator {

    private static final int NO_DATA_VALUE = 0xFF;
    @SourceProduct(alias = "source", label = "Source product")
    private final Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;
    @Parameter(label = "Number of clusters", defaultValue = "5", interval = "(0,100]")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "10", interval = "(0,10000]")
    private int maxIterations;
    @Parameter(label = "Random seed", defaultValue = "31415",
            description = "Seed for the random generator, used for initialising the algorithm.")
    private int randomSeed;
    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;
    @Parameter(description = "The name of the ROI-Mask that should be used.",
            alias = "roiMask", defaultValue = "",
            rasterDataNodeType = Mask.class, label = "ROI-mask")
    private String roiMaskName;
    private final DistanceMeasure measure = new EuclideanDistanceMeasure();
    @Parameter(label = "t1", defaultValue = "80", interval = "(0,100]")
    double t1;
    @Parameter(label = "t2", defaultValue = "55", interval = "(0,100]")
    double t2;
    @Parameter(label = "m", defaultValue = "2.0", interval = "(0,5]")
    private final double m = 2.0;
    @Parameter(label = "fuzziness", defaultValue = "2.0f", interval = "(0,5]")
    float fuzziness;
    @Parameter(label = "convergenceDelta", defaultValue = "0.5", interval = "(0,1]")
    double convergenceDelta;
    private transient Roi roi;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient List<SoftCluster> clusterSet = new ArrayList<>();
    private transient MetadataElement clusterAnalysis;
    private static final double MINIMAL_VALUE = 0.0000000001;

    public SVMClassificationOp() {
    }

    @Override
    public void initialize() {

        try {
            int width = sourceProduct.getSceneRasterWidth();
            int height = sourceProduct.getSceneRasterHeight();
            final String name = sourceProduct.getName() + "_CLUSTERS";
            final String type = sourceProduct.getProductType() + "_CLUSTERS";

            targetProduct = new Product(name, type, width, height);
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
            ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());

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

            sourceBands = collectSourceBands();
            setTargetProduct(targetProduct);
            roi = new Roi(sourceProduct, sourceBands, "");

        } catch (Throwable e) {
            e.printStackTrace();
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private Band[] collectSourceBands() {
        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (band.getUnit() != null && band.getUnit().equals(Unit.INTENSITY)) {
                    bandNameList.add(band.getName());
                    System.err.println(band.getName());
                }
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }
        sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final String sourceBandName = sourceBandNames[i];
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            System.err.println(sourceBandNames[i]);
            if (sourceBand == null) {
                throw new OperatorException("Source band not found: " + sourceBandName);
            }
            sourceBands[i] = sourceBand;
        }
        return sourceBands;
    }
    int index = 0;

//    @Override
//    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
//        System.err.println("Computing clusters..." + index);
//        index++;
//        String root = "D:\\";
//
//        pm.beginTask("Computing clusters...", 10);
//        try {
//            final List<SoftCluster> theClusterSet = getClusterSet(SubProgressMonitor.create(pm, 9));
//
//            final Rectangle targetRectangle = targetTile.getRectangle();
//            final Tile[] sourceTiles = new Tile[sourceBands.length];
//            for (int i = 0; i < sourceTiles.length; i++) {
//                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle);
//            }
//            double[] point = new double[sourceTiles.length];
//            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
//                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
//                    try {
//                        if (roi.contains(x, y)) {
//                            for (int i = 0; i < sourceTiles.length; i++) {
//                                point[i] = sourceTiles[i].getSampleDouble(x, y);
//                            }
//                            targetTile.setSample(x, y, getMembership(new DenseVector(point), theClusterSet));
//                        } else {
//                            targetTile.setSample(x, y, NO_DATA_VALUE);
//                        }
//                    } catch (ArrayIndexOutOfBoundsException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
////            ImageIO.write(targetBand.getSourceImage().getAsBufferedImage(targetRectangle, targetBand.getSourceImage().getColorModel()), "png", new File(root + "FuzzyKMeansOp" + index + ".png"));
//            pm.worked(1);
//        } catch (Exception ex) {
//            Logger.getLogger(FuzzyKMeansOp.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            pm.done();
//        }
//
//    }
    public int getMembership(Vector point, List<SoftCluster> clusterList) {

        List<Double> clusterDistanceList = new ArrayList<>();
        for (SoftCluster cluster : clusterList) {
            System.out.println(cluster.asFormatString(null) + cluster.getNumObservations());
            clusterDistanceList.add(measure.distance(cluster.getCenter(), point));
        }
        Vector pi = computePi(clusterList, clusterDistanceList);
        try {
            int clusterId = emitMostLikelyCluster(point, clusterList, pi);
            if (clusterId > 0) {
                return clusterId;
            }
        } catch (IOException ex) {
            Logger.getLogger(FuzzyKMeansOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return NO_DATA_VALUE;
    }

    private static int emitMostLikelyCluster(Vector point, List<SoftCluster> clusters, Vector pi)
            throws IOException {
        int clusterId = -1;
        double clusterPdf = 0;
        for (int i = 0; i < clusters.size(); i++) {
            double pdf = pi.get(i);
            if (pdf > clusterPdf) {
                clusterId = clusters.get(i).getId();
                clusterPdf = pdf;
            }
        }
        return clusterId;
    }

    /**
     * Computes the probability of a point belonging to a cluster
     */
    public double computeProbWeight(double clusterDistance, Iterable<Double> clusterDistanceList) {
        if (clusterDistance == 0) {
            clusterDistance = MINIMAL_VALUE;
        }
        double denom = 0.0;
        for (double eachCDist : clusterDistanceList) {
            if (eachCDist == 0.0) {
                eachCDist = MINIMAL_VALUE;
            }
            denom += Math.pow(clusterDistance / eachCDist, 2.0 / (m - 1));
        }
        return 1.0 / denom;
    }

    public Vector computePi(List<SoftCluster> clusters, List<Double> clusterDistanceList) {
        Vector pi = new DenseVector(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            double probWeight = computeProbWeight(clusterDistanceList.get(i), clusterDistanceList);
            pi.set(i, probWeight);
        }
        return pi;
    }

    private synchronized List<SoftCluster> getClusterSet(ProgressMonitor pm) {
        if (clusterSet.isEmpty()) {
            Rectangle[] tileRectangles = getAllTileRectangles();
            pm.beginTask("Extracting data points...", tileRectangles.length * maxIterations * 2 + 2);
            System.err.println("Extracting data points..." + tileRectangles.length);
            try {
                roi = new Roi(sourceProduct, sourceBands, "");
                pm.worked(1);
//                final FuzzyKMeansClusterer clusterer = createClusterer();
                pm.worked(1);

                boolean converged = false;
                for (int i = 0; (i < maxIterations && !converged); ++i) {
//                    clusterer.startIteration();
                    int index = 0;
                    for (Rectangle rectangle : tileRectangles) {
                        checkForCancellation();
                        System.err.println("Tile " + index);
                        index++;
//                        KMeansIteration pixelIterr = createPixelIter(rectangle, SubProgressMonitor.create(pm, 1));
//                        converged = clusterer.iterateTile(pixelIterr);
                        pm.worked(1);
                    }
//                    endIteration = clusterer.endIteration();testConvergence
                }
//                clusterSet = clusterer.getClusters();
//                ClusterMetaDataUtils.addCenterToIndexCoding(
//                        clusterMapBand.getIndexCoding(), sourceBands, clusterSet.getMeans());
//                ClusterMetaDataUtils.addCenterToMetadata(
//                        clusterAnalysis, sourceBands, clusterSet.getMeans());
            } finally {
                pm.done();
            }
        }
        return clusterSet;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
            ProgressMonitor pm) throws OperatorException {
        int totalWork = targetRectangle.height;
        if (clusterSet.isEmpty()) {
            totalWork += 100;
        }
        System.err.println("Computing clusters..." + index + ", " + totalWork + "%");
        index++;
        pm.beginTask("Computing clusters...", totalWork);
        try {
            synchronized (this) {
                if (clusterSet.isEmpty()) {
                    clusterSet = getClusterSet(SubProgressMonitor.create(pm, 100));
                }
            }
            for (SoftCluster cluster : clusterSet) {
                System.err.println(cluster.asFormatString());
            }
            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle);
            }

            final Tile clusterMapTile = targetTileMap.get(clusterMapBand);
            final Tile[] targetTiles = new Tile[clusterCount];
//            if (includeProbabilityBands) {
//                for (int i = 0; i < targetTiles.length; i++) {
//                    targetTiles[i] = targetTileMap.get(probabilityBands[i]);
//                }
//            }

            final double[] point = new double[sourceTiles.length];
            final double[] posteriors = new double[clusterCount];

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();

                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (roi == null || roi.contains(x, y)) {
//                        for (int i = 0; i < sourceTiles.length; i++) {
//                            point[i] = sourceTiles[i].getSampleDouble(x, y);
//                            System.err.println(point[i]);
//                        }
//
////                        probabilityCalculator.calculate(point, posteriors);
//
////                        if (includeProbabilityBands) {
//                        for (int i = 0; i < clusterCount; ++i) {
////                                targetTiles[i].setSample(x, y, posteriors[i]);
//                            targetTiles[i].setSample(x, y, getMembership(new DenseVector(point), clusterSet));
//                        }
//                        }
                        clusterMapTile.setSample(x, y, getMembership(new DenseVector(point), clusterSet));
                    } else {
//                        if (includeProbabilityBands) {
//                        for (int i = 0; i < clusterCount; ++i) {
//                            targetTiles[i].setSample(x, y, NO_DATA_VALUE);
//                        }
//                        }
                        clusterMapTile.setSample(x, y, NO_DATA_VALUE);
                    }
                }

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

//    private FuzzyKMeansClusterer createClusterer() {
//        final FuzzyKMeansClusterer clusterer = new FuzzyKMeansClusterer(clusterCount, sourceBands.length, measure, convergenceDelta, m, maxIterations);
//        System.err.println("Creating clusterer " + clusterCount + ", " + sourceBands.length + "," + clusterSet.size());
//        RandomSceneIteration randomSceneIter = new RandomSceneIteration(this, sourceBands, roi, randomSeed);
//        if (randomSceneIter.getRoiMemberCount() < clusterCount) {
//            throw new OperatorException("The combination of ROI and valid pixel masks contain "
//                    + randomSceneIter.getRoiMemberCount() + " pixel. These are too few to initialize the clustering.");
//        }
//        clusterer.initialize(randomSceneIter);
//        return clusterer;
//    }

    private Rectangle[] getAllTileRectangles() {
        Dimension tileSize = ImageManager.getPreferredTileSize(sourceProduct);
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);

        Rectangle[] rectangles = new Rectangle[tileCountX * tileCountY];
        int index = 0;
        for (int tileY = 0; tileY < tileCountY; tileY++) {
            for (int tileX = 0; tileX < tileCountX; tileX++) {
                final Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                        tileY * tileSize.height,
                        tileSize.width,
                        tileSize.height);
                final Rectangle intersection = boundary.intersection(tileRectangle);
                rectangles[index] = intersection;
                index++;
            }
        }
        return rectangles;
    }

//    private KMeansIteration createPixelIter(Rectangle rectangle, ProgressMonitor pm) {
//        final Tile[] sourceTiles = new Tile[sourceBands.length];
//        try {
//            pm.beginTask("Extracting data points...", sourceBands.length);
//            System.err.println("Extracting data points from band " + sourceBands.length + ", rectangle [" + rectangle.x + "," + rectangle.y + "]");
//            for (int i = 0; i < sourceBands.length; i++) {
//                sourceTiles[i] = getSourceTile(sourceBands[i], rectangle);
//                pm.worked(1);
//            }
//        } finally {
//            pm.done();
//        }
//        return new KMeansIteration(sourceTiles, roi);
//    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FuzzyKMeansOp.class);
        }
    }
}
