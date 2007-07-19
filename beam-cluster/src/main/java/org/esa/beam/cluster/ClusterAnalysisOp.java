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
package org.esa.beam.cluster;

import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import de.gkss.hs.datev2004.Clucov;
import de.gkss.hs.datev2004.DataSet;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.support.CachingOperator;
import org.esa.beam.framework.gpf.support.ProductDataCache;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

/**
 * The CLUCOV operator implementation.
 */
public class ClusterAnalysisOp extends CachingOperator implements ParameterConverter {
    @SourceProduct
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;
    @Parameter
    String[] featureBandNames;
    @Parameter
    String roiExpression;

    private transient Band[] featureBands;
//    private transient Band[] featureProbBands;
    private transient Band groupBand;
    private transient Clucov clucov;

    public ClusterAnalysisOp(OperatorSpi spi) {
        super(spi);
    }

    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement
    }

    public void setParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        Xpp3Dom child = configuration.getChild("features");
        Xpp3Dom[] children = child.getChildren("feature");
        featureBandNames = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            featureBandNames[i] = children[i].getValue();
        }
    }

    public boolean isComputingAllBandsAtOnce() {
        return false;
    }

    public Product createTargetProduct(ProgressMonitor progressMonitor) throws OperatorException {
        featureBands = new Band[featureBandNames.length];
        for (int i = 0; i < featureBandNames.length; i++) {
            String featureBandName = featureBandNames[i];
            Band band = sourceProduct.getBand(featureBandName);
            if (band == null) {
                throw new OperatorException("Feature band not found: " + featureBandName);
            }
            featureBands[i] = band;
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product("clucov", "clucov", width, height);
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
        return targetProduct;
    }

    @SuppressWarnings("unused")
    public void computeTile(Band band, Rectangle rectangle, ProductDataCache cache, ProgressMonitor pm) throws OperatorException {
        if (clucov == null) {
            try {
                computeClusters();
                storeClustersInProduct();
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }

        if (band == groupBand)  {
            int width = sourceProduct.getSceneRasterWidth();
            DataSet ds = clucov.ds;
            ProductData data = cache.createData(groupBand);
            for (int y = 0; y < rectangle.height; y++) {
                for (int x = 0; x < rectangle.width; x++) {
                    int bufferIndex = y * rectangle.width + x;
                    int dsIndex = (rectangle.y + y) * width + rectangle.x + x;
                    data.setElemIntAt(bufferIndex, ds.group[dsIndex]);
                }
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

    private void computeClusters() throws IOException {
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
        }
        clucov = new Clucov(ds);
        //clucov.
        clucov.initialize(30);
        clucov.run();
    }

    public void dispose() {
        // todo - add any clean-up code here
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(ClusterAnalysisOp.class, "ClusterAnalysis");
        }
    }
}
