/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.product;

import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.StackUtils;

import java.io.IOException;
import java.util.*;

public class StackSplit {

    private final Product srcProduct;
    private final SplitProduct referenceSplitProduct;
    private final List<SplitProduct> secondarySplitProductList = new ArrayList<>();
    private final boolean keepStackBandNames;

    public StackSplit(final Product sourceProduct, final boolean keepStackBandNames) throws IOException {
        this.srcProduct = sourceProduct;
        this.keepStackBandNames = keepStackBandNames;

        final InputProductValidator validator = new InputProductValidator(srcProduct);
        if(!validator.isCollocated() && !validator.isCoregisteredStack()) {
            throw new IOException("Source product should be a collocated or coregistered stack");
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final String[] referenceBandNames = StackUtils.getMasterBandNames(srcProduct);
        if(referenceBandNames.length == 0) {
            // loosely coupled stack
            referenceSplitProduct = null;

            // group secondary products by virtual dependencies
            final Map<Band, RasterDataSymbol[]> virtualBandDependencyMap = findVirtualBandReferences(srcProduct);
            final List<String> bandNamesUsed = new ArrayList<>();
            for(Band virtualBand : virtualBandDependencyMap.keySet()) {
                final List<String> secondaryProductNames = new ArrayList<>();
                secondaryProductNames.add(virtualBand.getName());
                bandNamesUsed.add(virtualBand.getName());

                final RasterDataSymbol[] rasterSymbols = virtualBandDependencyMap.get(virtualBand);
                for(RasterDataSymbol rasterDataSymbol : rasterSymbols) {
                    secondaryProductNames.add(rasterDataSymbol.getName());
                    bandNamesUsed.add(rasterDataSymbol.getName());
                }

                SplitProduct secondarySplitProduct = createSubset(sourceProduct, "product_" + virtualBand.getName(),
                        secondaryProductNames.toArray(new String[0]));
                secondarySplitProductList.add(secondarySplitProduct);
            }

            for(Band band : sourceProduct.getBands()) {
                if(!bandNamesUsed.contains(band.getName())) {
                    final String[] secondaryProductNames = new String[]{band.getName()};
                    SplitProduct secondarySplitProduct = createSubset(sourceProduct, "product_" + band.getName(), secondaryProductNames);
                    secondarySplitProductList.add(secondarySplitProduct);
                }
            }
        } else {
            // coregistered stack

            String referenceProductName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
            if(referenceProductName == null || referenceProductName.equals(AbstractMetadata.NO_METADATA_STRING)) {
                referenceProductName = srcProduct.getName();
            }
            referenceSplitProduct = createSubset(srcProduct, referenceProductName, getBandNames(srcProduct, referenceBandNames));

            final String[] secondaryProductNames = StackUtils.getSlaveProductNames(sourceProduct);
            for (String secondaryProductName : secondaryProductNames) {
                final String[] secondaryBandNames = StackUtils.getSlaveBandNames(sourceProduct, secondaryProductName);
                SplitProduct secondarySplitProduct = createSubset(srcProduct, secondaryProductName, getBandNames(srcProduct, secondaryBandNames));
                secondarySplitProductList.add(secondarySplitProduct);
            }
        }
    }

    private static Map<Band, RasterDataSymbol[]> findVirtualBandReferences(final Product srcProduct) {

        final WritableNamespace namespace = BandArithmetic.createDefaultNamespace(new Product[] {srcProduct}, 0);
        final Parser parser = new ParserImpl(namespace, false);

        final Map<Band, RasterDataSymbol[]> virtualBandDependencyMap = new HashMap<>();
        for(Band band : srcProduct.getBands()) {
            if(band instanceof VirtualBand) {
                try {
                    final VirtualBand vBand = (VirtualBand) band;
                    final Term term = parser.parse(vBand.getExpression());
                    final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
                    virtualBandDependencyMap.put(band, refRasterDataSymbols);
                } catch (Exception e) {
                    SystemUtils.LOG.warning("Error parsing virtual band " + band.getName());
                }
            }
        }

        return virtualBandDependencyMap;
    }

    public SplitProduct getReferenceSubset() {
        return referenceSplitProduct;
    }

    public SplitProduct[] getSecondarySubsets() {
        return secondarySplitProductList.toArray(new SplitProduct[0]);
    }

    public static String[] getBandNames(final Product srcProduct, final String[] names) {
        final Set<String> bandNames = new HashSet<>();
        for(String name : names) {
            final String suffix = StackUtils.getBandSuffix(name);
            for(String srcBandName : srcProduct.getBandNames()) {
                if(srcBandName.endsWith(suffix)) {
                    bandNames.add(srcBandName);
                }
            }
        }
        return bandNames.toArray(new String[0]);
    }

    public SplitProduct createSubset(final Product srcProduct, final String productName, final String[] bandNames) throws IOException {

        final int width = srcProduct.getSceneRasterWidth();
        final int height = srcProduct.getSceneRasterHeight();

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(srcProduct.getTiePointGridNames());
        subsetDef.addNodeNames(bandNames);
        subsetDef.setSubsetRegion(new PixelSubsetRegion(0, 0, width, height, 0));
        subsetDef.setSubSampling(1, 1);
        subsetDef.setIgnoreMetadata(true);

        final SplitProduct splitProduct = new SplitProduct(srcProduct, productName, bandNames, subsetDef);

        if(keepStackBandNames) {
            for (Band trgBand : splitProduct.subsetProduct.getBands()) {
                splitProduct.newBandNamingMap.put(trgBand.getName(), trgBand.getName());
            }
        } else {
            // update band name
            for (Band trgBand : splitProduct.subsetProduct.getBands()) {
                final String newBandName = StackUtils.getBandNameWithoutDate(trgBand.getName());
                splitProduct.newBandNamingMap.put(newBandName, trgBand.getName());
                trgBand.setName(newBandName);

                // update virtual band expressions
                for (Band vBand : splitProduct.subsetProduct.getBands()) {
                    if (vBand instanceof VirtualBand) {
                        final VirtualBand virtBand = (VirtualBand) vBand;
                        String expression = virtBand.getExpression().replaceAll(trgBand.getName(), newBandName);
                        virtBand.setExpression(expression);
                    }
                }
            }
        }

        return splitProduct;
    }

    public static class SplitProduct {
        public final Product subsetProduct;
        public final String productName;
        public final ProductSubsetDef subsetDef;
        public final String[] srcBandNames;
        public final Map<String, String> newBandNamingMap = new HashMap<>();

        public SplitProduct(final Product srcProduct, final String productName, final String[] srcBandNames,
                            final ProductSubsetDef subsetDef) throws IOException {
            this.productName = productName;
            this.srcBandNames = srcBandNames;
            this.subsetDef = subsetDef;

            ProductSubsetBuilder subsetBuilder = new ProductSubsetBuilder();
            this.subsetProduct = subsetBuilder.readProductNodes(srcProduct, subsetDef);
        }
    }
}
