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
package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@OperatorMetadata(alias = "Merge",
        category = "Raster",
        description = "Allows merging of several source products by using specified 'master' as reference product.",
        authors = "SNAP Team",
        version = "1.2",
        copyright = "(c) 2012 by Brockmann Consult")
public class MergeOp extends Operator {

    @SourceProduct(description = "The master, which serves as the reference, e.g. providing the geo-information.")
    private Product masterProduct;

    @SourceProducts(description = "The products to be merged into the master product.")
    private Product[] sourceProducts;

    @TargetProduct
    private Product targetProduct;

    @Parameter(itemAlias = "include",
            description = "Defines nodes to be included in the master product. If no includes are provided, all" +
                          " nodes are copied.")
    private NodeDescriptor[] includes;

    @Parameter(itemAlias = "exclude",
            description = "Defines nodes to be excluded from the target product. Excludes have precedence above includes.")
    private NodeDescriptor[] excludes;

    @Parameter(defaultValue = "1.0E-5f",
            description = "Defines the maximum lat/lon error in degree between the products. If set to NaN no check " +
                          "for compatible geographic boundary is performed")
    private float geographicError;

    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product(masterProduct.getName(),
                                    masterProduct.getProductType(),
                                    masterProduct.getSceneRasterWidth(),
                                    masterProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(masterProduct, targetProduct);

        validateSourceProducts();

        ArrayList<Product> sourceProductList = new ArrayList<>();
        sourceProductList.addAll(Arrays.asList(sourceProducts));
        sourceProductList.add(0, masterProduct);
        Map<String, List<NodeDescriptor>> inclusionMap = createDesciptorMap(includes == null ? new NodeDescriptor[0] : includes);
        addDefaultInclusions(sourceProductList, inclusionMap);

        Map<String, List<NodeDescriptor>> exclusionMap = createDesciptorMap(excludes == null ? new NodeDescriptor[0] : excludes);

        Set<Product> usedProducts = new HashSet<>();
        for (Map.Entry<String, List<NodeDescriptor>> entry : inclusionMap.entrySet()) {
            String productId = entry.getKey();
            Product product = getSourceProduct(productId);
            List<NodeDescriptor> includesList = entry.getValue();
            for (NodeDescriptor includeDescriptor : includesList) {
                Pattern inclPattern = includeDescriptor.getCompiledNamePattern();
                for (String bandName : product.getBandNames()) {
                    Matcher inclMatcher = inclPattern.matcher(bandName);
                    if (inclMatcher.matches()) {
                        if (!shallBandBeExcluded(bandName, productId, exclusionMap)) {
                            String newName = StringUtils.isNotNullAndNotEmpty(includeDescriptor.newName) ? includeDescriptor.newName : bandName;
                            copyBandWithFeatures(product, bandName, newName);
                            usedProducts.add(product);
                        }
                    }
                }
            }
        }

        for (Product product : usedProducts) {
            mergeAutoGrouping(product);
            ProductUtils.copyMasks(product, targetProduct);
            ProductUtils.copyOverlayMasks(product, targetProduct);
        }
    }

    private boolean shallBandBeExcluded(String bandName, String productId, Map<String, List<NodeDescriptor>> exclusionMap) {
        List<NodeDescriptor> excludesList = exclusionMap.get(productId);
        if (excludesList != null) {
            for (NodeDescriptor excludeDescriptor : excludesList) {
                Pattern exclPattern = excludeDescriptor.getCompiledNamePattern();
                Matcher exclMatcher = exclPattern.matcher(bandName);
                if (exclMatcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    // if no inclusions is defined, all bands of the product shall be included
    private void addDefaultInclusions(ArrayList<Product> sourceProductList, Map<String, List<NodeDescriptor>> inclusionMap) {
        for (final Product sourceProduct : sourceProductList) {
            String productId = getSourceProductId(sourceProduct);
            if (!inclusionMap.containsKey(productId)) {
                final NodeDescriptor nodeDescriptor = new NodeDescriptor();
                nodeDescriptor.namePattern = ".*";
                nodeDescriptor.productId = productId;
                inclusionMap.put(nodeDescriptor.productId, Collections.singletonList(nodeDescriptor));
            }
        }
    }

    private Map<String, List<NodeDescriptor>> createDesciptorMap(NodeDescriptor[] descriptors) {
        Map<String, List<NodeDescriptor>> map = new LinkedHashMap<>();
        for (NodeDescriptor nd : descriptors) {
            validateNodeDescriptor(nd);
            List<NodeDescriptor> descriptorList = map.get(nd.productId);
            if (descriptorList == null) {
                ArrayList<NodeDescriptor> list = new ArrayList<>();
                list.add(nd);
                map.put(nd.productId, list);
            } else {
                descriptorList.add(nd);
            }
        }
        return map;
    }

    private void validateNodeDescriptor(NodeDescriptor nd) {
        if (StringUtils.isNullOrEmpty(nd.productId)) {
            throw new OperatorException("Missing product id for an include or exclude description");
        }
        if (StringUtils.isNullOrEmpty(nd.name) && StringUtils.isNullOrEmpty(nd.namePattern)) {
            throw new OperatorException(String.format("Neither 'name' nor 'namePattern' given node descriptor with product id '%s'", nd.productId));
        }
        if (StringUtils.isNotNullAndNotEmpty(nd.newName) && StringUtils.isNotNullAndNotEmpty(nd.namePattern)) {
            throw new OperatorException(
                    String.format("Property 'newName' cannot be used with 'namePattern' in node descriptor with product id '%s'", nd.productId));
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
            if (!Float.isNaN(geographicError) && !targetProduct.isCompatibleProduct(sourceProduct, geographicError)) {
                throw new OperatorException(String.format("Product [%s] is not compatible to master product.",
                                                          getSourceProductId(sourceProduct)));
            }
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        getLogger().warning("Wrongly configured operator. Tiles of Band '" + band.getName() + "' should not be requested.");
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static class NodeDescriptor {

        private String productId;
        private String name;
        private String newName;
        private String namePattern;

        private String exclRegex;

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

        private Pattern getCompiledNamePattern() {
            if (exclRegex == null) {
                exclRegex = StringUtils.isNotNullAndNotEmpty(name) ? name : namePattern;
            }
            return Pattern.compile(exclRegex);
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeOp.class);
        }
    }
}
