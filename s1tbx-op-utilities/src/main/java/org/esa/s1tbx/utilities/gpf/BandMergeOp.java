/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s1tbx.utilities.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

/**
 * The merge operator allows copying raster data from other products to a specified product. The first product provided
 * is considered the 'master product', into which the raster data coming from the other products is copied. Existing
 * nodes are kept.
 * <p>
 * It is mandatory that the products share the same scene, that is, their width and height need to match with those of
 * the master product as well as their geographic position.
 *
 * @author Olaf Danne
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "BandMerge",
                  category = "Raster",
                  description = "Allows copying raster data from any number of source products to a specified 'master'" +
                          " product.",
                  authors = "BEAM team",
                  version = "1.0",
                  copyright = "(c) 2012 by Brockmann Consult",
                  internal = false)
public class BandMergeOp extends Operator {

    @SourceProducts(description = "The products to be merged into the master product.")
    private Product[] sourceProducts;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(defaultValue = "1.0E-5f",
               description = "Defines the maximum lat/lon error in degree between the products.")
    private float geographicError;

    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product(sourceProducts[0].getName(),
                                                  sourceProducts[0].getProductType(),
                                                  sourceProducts[0].getSceneRasterWidth(),
                                                  sourceProducts[0].getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProducts[0], targetProduct);

        for (Product prod : sourceProducts) {
            for (Band band : prod.getBands()) {
                final Band sourceBand = targetProduct.getBand(band.getName());
                String targetBandName = band.getName();
                if (sourceBand != null) {
                    int cnt = 2;
                    targetBandName = band.getName() + "_"+cnt;
                    while(targetProduct.containsRasterDataNode(targetBandName)) {
                        ++cnt;
                        targetBandName = band.getName() + "_"+cnt;
                    }
                }
                ProductUtils.copyBand(band.getName(), prod, targetBandName, targetProduct, true);
            }
        }

        validateSourceProducts();
  /*      if (sourceBandNames == null || sourceBandNames.length == 0) {
            List<NodeDescriptor> nodeDescriptorList = new ArrayList<>();
            for (final Product sourceProduct : sourceProducts) {
                if (sourceProduct != sourceProducts[0]) {
                    for (String bandName : sourceProduct.getBandNames()) {
                        final NodeDescriptor nodeDescriptor = new NodeDescriptor();
                        nodeDescriptor.name = bandName;
                        nodeDescriptor.productId = getSourceProductId(sourceProduct);
                        nodeDescriptorList.add(nodeDescriptor);
                    }
                }
            }
            includes = nodeDescriptorList.toArray(new NodeDescriptor[nodeDescriptorList.size()]);
        }

        Set<Product> allSrcProducts = new HashSet<>();
        for (NodeDescriptor nodeDescriptor : includes) {
            Product srcProduct = getSourceProduct(nodeDescriptor.productId);
            if (srcProduct == targetProduct) {
                continue;
            }
            if (StringUtils.isNotNullAndNotEmpty(nodeDescriptor.name)) {
                if (StringUtils.isNotNullAndNotEmpty(nodeDescriptor.newName)) {
                    copyBandWithFeatures(srcProduct, nodeDescriptor.name, nodeDescriptor.newName);
                } else {
                    copyBandWithFeatures(srcProduct, nodeDescriptor.name, nodeDescriptor.name);
                }
                allSrcProducts.add(srcProduct);
            } else if (StringUtils.isNotNullAndNotEmpty(nodeDescriptor.namePattern)) {
                Pattern pattern = Pattern.compile(nodeDescriptor.namePattern);
                for (String bandName : srcProduct.getBandNames()) {
                    Matcher matcher = pattern.matcher(bandName);
                    if (matcher.matches()) {
                        copyBandWithFeatures(srcProduct, bandName, bandName);
                        allSrcProducts.add(srcProduct);
                    }
                }
            }
        }*/

        for (Product srcProduct : sourceProducts) {
            if (srcProduct != sourceProducts[0]) {
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

    private void copyBandWithFeatures(Product sourceProduct, String oldBandName, String newBandName) {
        Band sourceBand = sourceProduct.getBand(oldBandName);
        if (sourceBand == null) {
            final String msg = String.format("Source product [%s] does not contain a band with name [%s]",
                                             sourceProduct.getName(), oldBandName);
            throw new OperatorException(msg);
        }

        if (targetProduct.containsBand(newBandName)) {
            return;
        }
        ProductUtils.copyBand(oldBandName, sourceProduct, newBandName, targetProduct, true);
    }

    private void validateSourceProducts() {
        for (Product sourceProduct : getSourceProducts()) {
            if (!targetProduct.isCompatibleProduct(sourceProduct, geographicError)) {
                throw new OperatorException(String.format("Product [%s] is not compatible to master product.",
                                                          getSourceProductId(sourceProduct)));
            }
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        getLogger().warning("Wrongly configured operator. Tiles should not be requested.");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BandMergeOp.class);
        }
    }
}
