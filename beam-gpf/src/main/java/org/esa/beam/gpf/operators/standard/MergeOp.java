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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OperatorMetadata(alias = "Merge",
                  description = "Merges an arbitrary number of source bands into the target product.",
                  internal = true)
public class MergeOp extends Operator {

    @Parameter(defaultValue = "mergedProduct", description = "The name of the target product.")
    private String productName;
    @Parameter(defaultValue = "UNKNOWN", description = "The type of the target product.")
    private String productType;
    // todo - remove alias, property name is very clear.
    @Parameter(alias = "baseGeoInfo", description = "The ID of the source product providing the geo-coding.")
    private String copyGeoCodingFrom;
    @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
    @Parameter(itemAlias = "band", itemsInlined = true,
               description = "Defines a band to be included in the target product.")
    private BandDesc[] bands;
    @TargetProduct
    private Product targetProduct;


    @Override
    public void initialize() throws OperatorException {

        if (StringUtils.isNotNullAndNotEmpty(copyGeoCodingFrom)) {
            Product baseGeoProduct = getSourceProduct(copyGeoCodingFrom);
            final int sceneRasterWidth = baseGeoProduct.getSceneRasterWidth();
            final int sceneRasterHeight = baseGeoProduct.getSceneRasterHeight();
            targetProduct = new Product(productName, productType,
                                        sceneRasterWidth, sceneRasterHeight);

            copyGeoCoding(baseGeoProduct, targetProduct);
        } else {
            BandDesc bandDesc = bands[0];
            Product srcProduct = getSourceProduct(bandDesc.product);
            final int sceneRasterWidth = srcProduct.getSceneRasterWidth();
            final int sceneRasterHeight = srcProduct.getSceneRasterHeight();
            targetProduct = new Product(productName, productType,
                                        sceneRasterWidth, sceneRasterHeight);
        }

        Set<Product> allSrcProducts = new HashSet<Product>();
        for (BandDesc bandDesc : bands) {
            Product srcProduct = getSourceProduct(bandDesc.product);
            if (StringUtils.isNotNullAndNotEmpty(bandDesc.name)) {
                if (StringUtils.isNotNullAndNotEmpty(bandDesc.newName)) {
                    copyBandWithFeatures(srcProduct, targetProduct, bandDesc.name, bandDesc.newName);
                } else {
                    copyBandWithFeatures(srcProduct, targetProduct, bandDesc.name);
                }
                allSrcProducts.add(srcProduct);
            } else if (StringUtils.isNotNullAndNotEmpty(bandDesc.namePattern)) {
                Pattern pattern = Pattern.compile(bandDesc.namePattern);
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
            mergeAutoGrouping(srcProduct);
            ProductUtils.copyMasks(srcProduct, targetProduct);
            ProductUtils.copyOverlayMasks(srcProduct, targetProduct);
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

    public static class BandDesc {

        private String product;
        private String name;
        private String newName;
        private String namePattern;

        public void setProduct(String product) {
            this.product = product;
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