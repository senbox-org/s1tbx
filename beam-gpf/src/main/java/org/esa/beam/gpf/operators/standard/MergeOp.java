/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.gpf.operators.standard;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The merge operator allows copying raster data from other products to a specified product. The first given product is
 * considered the 'master product', into which the raster data coming from the other products is copied.
 */
@OperatorMetadata(alias = "Merge",
                  description = "Merges an arbitrary number of source bands into the target product.")
public class MergeOp extends Operator {

    @Parameter(itemAlias = "include", itemsInlined = false,
               description = "Defines a node to be included in the target product.")
    private NodeDesc[] includes;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        targetProduct = getSourceProducts()[0];
        validateSourceProducts();
        if (includes == null || includes.length == 0) {
            List<NodeDesc> nodeDescList = new ArrayList<NodeDesc>();
            final Product[] sourceProducts = getSourceProducts();
            for (final Product sourceProduct : sourceProducts) {
                if (sourceProduct != targetProduct) {
                    for (String bandName : sourceProduct.getBandNames()) {
                        final NodeDesc nodeDesc = new NodeDesc();
                        nodeDesc.name = bandName;
                        nodeDesc.productId = getSourceProductId(sourceProduct);
                        nodeDescList.add(nodeDesc);
                    }
                }
            }
            includes = nodeDescList.toArray(new NodeDesc[nodeDescList.size()]);
        }

        Set<Product> allSrcProducts = new HashSet<Product>();
        for (NodeDesc nodeDesc : includes) {
            Product srcProduct = getSourceProduct(nodeDesc.productId);
            if (srcProduct == targetProduct) {
                continue;
            }
            if (StringUtils.isNotNullAndNotEmpty(nodeDesc.name)) {
                if (StringUtils.isNotNullAndNotEmpty(nodeDesc.newName)) {
                    copyBandWithFeatures(srcProduct, targetProduct, nodeDesc.name, nodeDesc.newName);
                } else {
                    copyBandWithFeatures(srcProduct, targetProduct, nodeDesc.name);
                }
                allSrcProducts.add(srcProduct);
            } else if (StringUtils.isNotNullAndNotEmpty(nodeDesc.namePattern)) {
                Pattern pattern = Pattern.compile(nodeDesc.namePattern);
                for (String bandName : srcProduct.getBandNames()) {
                    Matcher matcher = pattern.matcher(bandName);
                    if (matcher.matches()) {
                        copyBandWithFeatures(srcProduct, targetProduct, bandName);
                        allSrcProducts.add(srcProduct);
                    }
                }
            }
        }

        for (Product srcProduct : allSrcProducts) {
            if (srcProduct != targetProduct) {
                mergeAutoGrouping(srcProduct);
                ProductUtils.copyMasks(srcProduct, targetProduct);
                ProductUtils.copyOverlayMasks(srcProduct, targetProduct);
            }
        }
    }

    private void mergeAutoGrouping(Product srcProduct) {
        final Product.AutoGrouping srcAutoGrouping = srcProduct.getAutoGrouping();
        if (srcAutoGrouping != null && !srcAutoGrouping.isEmpty()) {
            final Product.AutoGrouping targetAutoGrouping = targetProduct.getAutoGrouping();
            if (targetAutoGrouping == null) {
                targetProduct.setAutoGrouping(srcAutoGrouping);
            } else {
                for (String[] grouping : srcAutoGrouping) {
                    if (!targetAutoGrouping.contains(grouping)) {
                        targetProduct.setAutoGrouping(targetAutoGrouping.toString() + ":" + srcAutoGrouping);
                    }
                }
            }
        }
    }

    /*
     * Copies the tie point data, geocoding and the start and stop time.
     */
    private static void copyGeoCoding(Product sourceProduct,
                                      Product destinationProduct) {
        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(sourceProduct, destinationProduct);
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, destinationProduct);
        destinationProduct.setStartTime(sourceProduct.getStartTime());
        destinationProduct.setEndTime(sourceProduct.getEndTime());
    }

    private void copyBandWithFeatures(Product srcProduct, Product outputProduct, String oldBandName,
                                      String newBandName) {
        Band destBand = copyBandWithFeatures(srcProduct, outputProduct, oldBandName);
        destBand.setName(newBandName);
    }

    private Band copyBandWithFeatures(Product srcProduct, Product outputProduct, String bandName) {
        Band destBand = ProductUtils.copyBand(bandName, srcProduct, outputProduct);
        Band srcBand = srcProduct.getBand(bandName);
        if (srcBand == null) {
            final String msg = String.format("Source product [%s] does not contain a band with the name [%s]",
                                             srcProduct.getName(), bandName);
            throw new OperatorException(msg);
        }
        destBand.setSourceImage(srcBand.getSourceImage());
        if (srcBand.getFlagCoding() != null) {
            FlagCoding srcFlagCoding = srcBand.getFlagCoding();
            ProductUtils.copyFlagCoding(srcFlagCoding, outputProduct);
            destBand.setSampleCoding(outputProduct.getFlagCodingGroup().get(srcFlagCoding.getName()));
        }
        if (srcBand.getIndexCoding() != null) {
            IndexCoding srcIndexCoding = srcBand.getIndexCoding();
            ProductUtils.copyIndexCoding(srcIndexCoding, outputProduct);
            destBand.setSampleCoding(outputProduct.getIndexCodingGroup().get(srcIndexCoding.getName()));
        }
        return destBand;
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        getLogger().warning("Wrongly configured ProductMerger operator. Tiles should not be requested.");
    }

    private void validateSourceProducts() {
        for (Product sourceProduct : getSourceProducts()) {
            if (!targetProduct.isCompatibleProduct(sourceProduct, 1.0E-5f)) {
                throw new OperatorException("Product '" + getSourceProductId(sourceProduct) + "' is not compatible to" +
                                            " master product.");
            }
        }
    }

    public static class NodeDesc {

        private String productId;
        private String name;
        private String newName;
        private String namePattern;

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }

        public void setNamePattern(String namePattern) {
            this.namePattern = namePattern;
        }

    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeOp.class);
        }
    }
}