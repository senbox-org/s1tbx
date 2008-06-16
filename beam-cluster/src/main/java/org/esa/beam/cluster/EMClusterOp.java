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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
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

import java.awt.*;
import java.util.Comparator;
import java.util.Map;

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

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Number of clusters", defaultValue = "14")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "30")
    private int iterationCount;
    @Parameter(label = "Source band names",
               description = "The names of the bands being used for the cluster analysis.")
    private String[] sourceBandNames;
    @Parameter(label = "Include probabilities", defaultValue = "false",
               description = "Determines whether the posterior probabilities are included as band data.")
    private boolean includeProbabilityBands;

    private transient Comparator<EMCluster> clusterComparator;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient Band[] probabilityBands;

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

        final Product targetProduct = new Product(name, type, width, height);
        targetProduct.setPreferredTileSize(width, height);  //TODO ????

        if (includeProbabilityBands) {
            createProbabilityBands(targetProduct);
        }

        clusterMapBand = new Band("cluster_map", ProductData.TYPE_INT16, width, height);
        clusterMapBand.setDescription("Cluster map");
        targetProduct.addBand(clusterMapBand);

        final IndexCoding indexCoding = new IndexCoding("clusters");
        for (int i = 0; i < clusterCount; i++) {
            indexCoding.addIndex("cluster_" + (i + 1), i, "Cluster label");
        }
        targetProduct.getIndexCodingGroup().add(indexCoding);
        clusterMapBand.setSampleCoding(indexCoding);

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

            probabilityBands[i] = targetBand;
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing clusters...", iterationCount + 2);

        try {
            final EMClusterer clusterer = createClusterer(SubProgressMonitor.create(pm, 1));

            for (int i = 0; i < iterationCount; ++i) {
                checkForCancelation(pm);
                clusterer.iterate();
                pm.worked(1);
            }

            final EMClusterSet clusterSet;
            if (clusterComparator == null) {
                clusterSet = clusterer.getClusters();
            } else {
                clusterSet = clusterer.getClusters(clusterComparator);
            }

            final Tile[] sourceTiles = new Tile[sourceBands.length];
            for (int i = 0; i < sourceTiles.length; i++) {
                sourceTiles[i] = getSourceTile(sourceBands[i], targetRectangle, ProgressMonitor.NULL);
            }

            final Tile clusterMapTile = targetTileMap.get(clusterMapBand);
            final Tile[] targetTiles = new Tile[clusterCount];
            for (int i = 0; i < targetTiles.length; i++) {
                targetTiles[i] = targetTileMap.get(probabilityBands[i]);
            }
            double[] point = new double[sourceTiles.length];
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    for (int i = 0; i < sourceTiles.length; i++) {
                        point[i] = sourceTiles[i].getSampleDouble(x, y);
                    }
                    double p[] = clusterSet.getPosteriorProbabilities(point);
                    if (includeProbabilityBands) {
                        for (int i = 0; i < clusterCount; ++i) {
                            targetTiles[i].setSample(x, y, p[i]);
                        }
                    }
                    clusterMapTile.setSample(x, y, findMaxIndex(p));
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

    private EMClusterer createClusterer(ProgressMonitor pm) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        final double[][] points = new double[sceneWidth * sceneHeight][sourceBands.length];

        try {
            pm.beginTask("Extracting data points...", sourceBands.length * sceneHeight);

            for (int i = 0; i < sourceBands.length; i++) {
                for (int y = 0; y < sceneHeight; y++) {
                    final Tile sourceTile = getSourceTile(sourceBands[i], new Rectangle(0, y, sceneWidth, 1), pm);
                    for (int x = 0; x < sceneWidth; x++) {
                        final double sample = sourceTile.getSampleDouble(x, y);
                        points[y * sceneWidth + x][i] = sample;
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }

        return new EMClusterer(points, clusterCount);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(EMClusterOp.class);
        }
    }
}
