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
package org.esa.nest.clustering.dirichlet;

import org.esa.nest.clustering.meanshift.*;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.mahout.clustering.dirichlet.models.GaussianClusterDistribution;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.esa.nest.datamodel.Unit;

/**
 * Operator for k-means cluster analysis.
 *
 */
@OperatorMetadata(alias = "DirichletClusterAnalysis",
        version = "1.0",
        category = "Classification\\Clustering",
        description = "Performs a Dirichlet cluster analysis.")
public class DirichletOp extends Operator {

    private static final int NO_DATA_VALUE = 0xFF;
    @SourceProduct(alias = "source", label = "Source product")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(label = "Number of clusters", defaultValue = "14", interval = "(0,100]")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "30", interval = "(0,10000]")
    private int iterationCount;
    @Parameter(label = "Random seed", defaultValue = "31415",
            description = "Seed for the random generator, used for initialising the algorithm.")
    private int randomSeed;
    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;
    @Parameter(label = "ROI-mask",
            description = "The name of the ROI-Mask that should be used.", defaultValue = "",
            rasterDataNodeType = Mask.class)
    private String roiMaskName;
    DistanceMeasure measure = new EuclideanDistanceMeasure();
    double t1 = 80;
    double t2 = 55;

    private boolean emitMostLikely = true;

    private double threshold = 0.5;

    int maxIterations = 10;
    float fuzziness = 2.0f;
    double convergenceDelta = 0.5;
    private transient Roi roi;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient List<DirichletCluster> clusters;
    private transient MetadataElement clusterAnalysis;
    private double m = 2.0; // default value
    private static final double MINIMAL_VALUE = 0.0000000001;

    public DirichletOp() {
    }

    @Override
    public void initialize() throws OperatorException {
        collectSourceBands();

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

        setTargetProduct(targetProduct);
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

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing clusters...", 10);
        try {
            final List<DirichletCluster> theClusterSet = getClusterSet(SubProgressMonitor.create(pm, 9));
            final Rectangle targetRectangle = targetTile.getRectangle();
            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle);
            }

            double[] point = new double[sourceTiles.length];
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    try {
                        if (roi.contains(x, y)) {
                            for (int i = 0; i < sourceTiles.length; i++) {
                                point[i] = sourceTiles[i].getSampleDouble(x, y);
                            }
                            targetTile.setSample(x, y, getMembership(new VectorWritable(new DenseVector(point)), theClusterSet));
                        } else {
                            targetTile.setSample(x, y, NO_DATA_VALUE);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    public boolean getMembership(VectorWritable point, List<DirichletCluster> clusters) {

        Vector pi = new DenseVector(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            pi.set(i, clusters.get(i).getModel().pdf(point));
        }
        int clusterId = emitMostLikelyCluster(point, clusters, pi);
        if (clusterId > 0) {
            return true;
        }
        return false;
    }

    /**
     * Emit the point to the most likely cluster
     *
     * @param point a VectorWritable holding the Vector
     * @param clusters a List of DirichletClusters
     * @param pi the normalized pdf Vector for the point
     * @param context a Mapper.Context to emit to
     */
    private int emitMostLikelyCluster(VectorWritable point,
            Collection<DirichletCluster> clusters,
            Vector pi) {
        int clusterId = -1;
        double clusterPdf = 0;
        for (int i = 0; i < clusters.size(); i++) {
            double pdf = pi.get(i);
            if (pdf > clusterPdf) {
                clusterId = i;
                clusterPdf = pdf;
            }
        }
        return clusterId;
    }

    private synchronized List<DirichletCluster> getClusterSet(ProgressMonitor pm) {
        if (clusters == null) {
            Rectangle[] tileRectangles = getAllTileRectangles();
            pm.beginTask("Extracting data points...", tileRectangles.length * iterationCount * 2 + 2);
            try {
                roi = new Roi(sourceProduct, sourceBands, roiMaskName);
                pm.worked(1);
                final DirichletClusterer clusterer = createClusterer();
                pm.worked(1);

                boolean converged = false;
                for (int i = 0; (i < iterationCount && !converged); ++i) {
                    for (Rectangle rectangle : tileRectangles) {
                        checkForCancellation();
                        ClusterIteration pixelIterr = createPixelIter(rectangle, SubProgressMonitor.create(pm, 1));
                        clusterer.iterateTile(pixelIterr, i);
                        pm.worked(1);
                    }
                }
                clusters = clusterer.getClusters();
//                ClusterMetaDataUtils.addCenterToIndexCoding(
//                        clusterMapBand.getIndexCoding(), sourceBands, clusterSet.getMeans());
//                ClusterMetaDataUtils.addCenterToMetadata(
//                        clusterAnalysis, sourceBands, clusterSet.getMeans());
            } finally {
                pm.done();
            }
        }
        return clusters;
    }

    private DirichletClusterer createClusterer() {
        final DirichletClusterer clusterer = new DirichletClusterer(sourceBands.length, 1.0, clusterCount, 1, 1);
        RandomSceneIteration randomSceneIter = new RandomSceneIteration(this, sourceBands, roi, randomSeed);
        if (randomSceneIter.getRoiMemberCount() < clusterCount) {
            throw new OperatorException("The combination of ROI and valid pixel masks contain "
                    + randomSceneIter.getRoiMemberCount() + " pixel. These are too few to initialize the clustering.");
        }
        clusterer.initialize(randomSceneIter);
        return clusterer;
    }

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

    private ClusterIteration createPixelIter(Rectangle rectangle, ProgressMonitor pm) {
        final Tile[] sourceTiles = new Tile[sourceBands.length];
        try {
            pm.beginTask("Extracting data points...", sourceBands.length);
            for (int i = 0; i < sourceBands.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], rectangle);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return new ClusterIteration(sourceTiles, roi);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(DirichletOp.class);
        }
    }
}
