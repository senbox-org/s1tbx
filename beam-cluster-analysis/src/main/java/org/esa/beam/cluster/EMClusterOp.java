/*
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.cluster;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

import javax.media.jai.ROI;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
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
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

/**
 * Operator for cluster analysis.
 *
 * @author Ralf Quast
 * @version $Revision: 2223 $ $Date: 2008-06-16 11:59:40 +0200 (Mo, 16 Jun 2008) $
 */
@OperatorMetadata(alias = "EMClusterAnalysis",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Performs an expectation-maximization (EM) cluster analysis.")
public class EMClusterOp extends Operator {

    private static final int NO_DATA_VALUE = -1;

    @SourceProduct(alias = "source")
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
    @Parameter(label = "Source band names",
               description = "The names of the bands being used for the cluster analysis.",
               sourceProductId = "source")
    private String[] sourceBandNames;
    @Parameter(label = "Region of interest",
               description = "The name of the band which contains a ROI that should be used.",
               sourceProductId = "source")
    private String roiBandName;
    @Parameter(label = "Include probabilities", defaultValue = "false",
               description = "Determines whether the posterior probabilities are included as band data.")
    private boolean includeProbabilityBands;

    private transient Comparator<EMCluster> clusterComparator;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient Band[] probabilityBands;
    private transient ROI roi;
    private transient MetadataElement clusterAnalysis;
    private EMClusterSet clusterSet;

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

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName() + "_CLUSTERS";
        final String type = sourceProduct.getProductType() + "_CLUSTERS";

        targetProduct = new Product(name, type, width, height);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        targetProduct.setPreferredTileSize(width, height);  //TODO ????

        if (includeProbabilityBands) {
            createProbabilityBands(targetProduct);
        }

        clusterMapBand = new Band("class_indices", ProductData.TYPE_INT16, width, height);
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
        Band[] sourceBands;
        if (sourceBandNames != null && sourceBandNames.length > 0) {
            sourceBands = new Band[sourceBandNames.length];
            for (int i = 0; i < sourceBandNames.length; i++) {
                final Band sourceBand = sourceProduct.getBand(sourceBandNames[i]);
                if (sourceBand == null) {
                    throw new OperatorException("source band not found: " + sourceBandNames[i]);
                }
                sourceBands[i] = sourceBand;
            }
        } else {
            sourceBands = sourceProduct.getBands();
        }
        return sourceBands;
    }

    private void createProbabilityBands(Product targetProduct) {
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
        pm.beginTask("Computing clusters...", 2);

        try {
            final EMClusterSet theClusterSet = getClusterSet(SubProgressMonitor.create(pm, 1));

            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle, ProgressMonitor.NULL);
            }

            final Tile clusterMapTile = targetTileMap.get(clusterMapBand);
            final Tile[] targetTiles = new Tile[clusterCount];
            if (includeProbabilityBands) {
                for (int i = 0; i < targetTiles.length; i++) {
                    targetTiles[i] = targetTileMap.get(probabilityBands[i]);
                }
            }
            double[] point = new double[sourceTiles.length];
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (roi == null || roi.contains(x, y)) {
                        for (int i = 0; i < sourceTiles.length; i++) {
                            point[i] = sourceTiles[i].getSampleDouble(x, y);
                        }
                        double[] p = theClusterSet.getPosteriorProbabilities(point);
                        if (includeProbabilityBands) {
                            for (int i = 0; i < clusterCount; ++i) {
                                targetTiles[i].setSample(x, y, p[i]);
                            }
                        }
                        clusterMapTile.setSample(x, y, findMaxIndex(p));
                    } else {
                        if (includeProbabilityBands) {
                            for (int i = 0; i < clusterCount; ++i) {
                                targetTiles[i].setSample(x, y, NO_DATA_VALUE);
                            }
                        }
                        clusterMapTile.setSample(x, y, NO_DATA_VALUE);
                    }
                }
            }
            pm.worked(1);
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

    private synchronized EMClusterSet getClusterSet(ProgressMonitor pm) {
        if (clusterSet == null) {
            pm.beginTask("Extracting data points...", iterationCount + 2);
            try {
                Band roiBand = null;
                if (StringUtils.isNotNullAndNotEmpty(roiBandName)) {
                    roiBand = sourceProduct.getBand(roiBandName);
                }
                RoiCombiner roiCombiner = new RoiCombiner(sourceBands, roiBand);
                roi = roiCombiner.getCombinedRoi();
                pm.worked(1);

                final EMClusterer clusterer = createClusterer(SubProgressMonitor.create(pm, 1));

                for (int i = 0; i < iterationCount; ++i) {
                    checkForCancelation(pm);
                    clusterer.iterate();
                    pm.worked(1);
                }

                if (clusterComparator == null) {
                    clusterSet = clusterer.getClusters();
                } else {
                    clusterSet = clusterer.getClusters(clusterComparator);
                }
                double[][] means = new double[clusterCount][0];
                double[][][] covariances = new double[clusterCount][0][0];
                double[] priorProbabilities = new double[clusterCount];
                for (int i = 0; i < clusterCount; i++) {
                    means[i] = clusterSet.getMean(i);
                    EMCluster cluster = clusterSet.getEMCluster(i);
                    MultinormalDistribution distribution = (MultinormalDistribution) cluster.getDistribution();
                    covariances[i] = distribution.getCovariances();
                    priorProbabilities[i] = cluster.priorProbability;
                }
                ClusterMetaDataUtils.addCenterToIndexCoding(
                        clusterMapBand.getIndexCoding(), sourceBands, means);
                ClusterMetaDataUtils.addCenterToMetadata(
                        clusterAnalysis, sourceBands, means);
                ClusterMetaDataUtils.addEMInfoToMetadata(
                        clusterAnalysis, covariances, priorProbabilities);
                
            } catch (IOException e) {
                throw new OperatorException(e);
            } finally {
                pm.done();
            }
        }
        return clusterSet;
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
                    final Tile sourceTile = getSourceTile(sourceBands[i], new Rectangle(0, y, sceneWidth, 1), pm);
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
