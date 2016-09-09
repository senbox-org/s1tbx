/*
 * Copyright (C) 2002-2007 by ?
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
package org.esa.snap.cluster;

import com.bc.ceres.core.ProgressMonitor;
import de.gkss.hs.datev2004.Clucov;
import de.gkss.hs.datev2004.DataSet;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
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

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Set;

/**
 * Implements a cluster analysis using the CLUCOV algorithm.
 */
@OperatorMetadata(alias = "ClucovClusterAnalysis",
                  category = "Raster/Classification/Unsupervised Classification",
                  version = "1.0",
                  authors = "Helmut Schiller, Norman Fomferra",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Cluster analysis using the CLUCOV algorithm.",
                  internal = true)
public class ClucovClusterOp extends Operator {
    @SourceProduct(alias = "source")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;
    @Parameter(label = "Source band names",
               description = "The names of the bands being used for the cluster analysis.",
               rasterDataNodeType = Band.class)
    String[] sourceBandNames;
    @Parameter
    String roiExpression;

    private transient Band[] featureBands;
    //    private transient Band[] featureProbBands;
    private transient Band groupBand;
    private transient Clucov clucov;


    @Override
    public void initialize() throws OperatorException {

        featureBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            String featureBandName = sourceBandNames[i];
            Band band = sourceProduct.getBand(featureBandName);
            if (band == null) {
                throw new OperatorException("Feature band not found: " + featureBandName);
            }
            featureBands[i] = band;
        }
        if (featureBands.length == 0) {
            throw new OperatorException("Need at least a single feature band.");
        }
        ensureSingleRasterSize(featureBands);

        int width = featureBands[0].getRasterWidth();
        int height = featureBands[0].getRasterHeight();
        targetProduct = new Product("clucov", "clucov", width, height);
// todo - use option to decide if to output probability bands         
//        for (int i = 0; i < featureBands.length; i++) {
//            Band featureBand = featureBands[i];
//            Band propabBand = targetProduct.addBand(featureBand.getName() + "_prob", ProductData.TYPE_UINT8);
//            propabBand.setScalingFactor(1.0);
//            propabBand.setUnit("%");
//            propabBand.setDescription("Probability of " + featureBand.getName() + " in percent");
//        }
        groupBand = targetProduct.addBand("group", ProductData.TYPE_UINT8);
        groupBand.setUnit("-");
        groupBand.setDescription("Cluster group number");
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (clucov == null) {
            try {
                computeClusters(pm);
                storeClustersInProduct();
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }

        if (band == groupBand) {
            Rectangle rectangle = targetTile.getRectangle();
            final int sourceWidth = sourceProduct.getSceneRasterWidth();
            DataSet ds = clucov.ds;
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    int dsIndex = y * sourceWidth + x;
                    targetTile.setSample(x, y, ds.group[dsIndex]);
                }
                checkForCancellation();
            }
        }
    }

    private void storeClustersInProduct() {
        MetadataElement metadataRoot = targetProduct.getMetadataRoot();
        Set<Short> shorts = clucov.clusters.keySet();
        MetadataElement clustersElement = new MetadataElement("clusters");
        metadataRoot.addElement(clustersElement);
        for (Short aShort : shorts) {
            Clucov.Cluster cluster = clucov.clusters.get(aShort);
            MetadataElement clusterElement = new MetadataElement("cluster");
            clusterElement.addAttribute(new MetadataAttribute("group", ProductData.createInstance(new short[]{cluster.group}), true));
            clusterElement.addAttribute(new MetadataAttribute("gauss.normfactor", ProductData.createInstance(new double[]{cluster.gauss.normfactor}), true));
            clusterElement.addAttribute(new MetadataAttribute("gauss.cog", ProductData.createInstance(cluster.gauss.cog), true));
            double[][] array = cluster.gauss.covinv.getArray();
            for (int i = 0; i < array.length; i++) {
                clusterElement.addAttribute(new MetadataAttribute("gauss.covinv." + i, ProductData.createInstance(array[i]), true));
            }
            clustersElement.addElement(clusterElement);
        }
    }

    private void computeClusters(ProgressMonitor pm) throws IOException {
        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        double[] scanLine = new double[width];
        double[][] dsVectors = new double[width][featureBands.length];

        // todo - handle valid expression! 
        DataSet ds = new DataSet(width * height, featureBands.length);
        for (int y = 0; y < height; y++) {
            for (int i = 0; i < featureBands.length; i++) {
                Band featureBand = featureBands[i];
                featureBand.readPixels(0, y, width, 1, scanLine, ProgressMonitor.NULL);
                // todo - handle no-data!
                for (int x = 0; x < width; x++) {
                    dsVectors[x][i] = scanLine[x];
                }
            }
            for (int x = 0; x < width; x++) {
                ds.add(dsVectors[x]);
            }
            checkForCancellation();
        }
        clucov = new Clucov(ds);
        //clucov.
        clucov.initialize(30);
        clucov.run();
    }

    @Override
    public void dispose() {
        super.dispose();
        clucov = null;
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ClucovClusterOp.class);
        }
    }
}
