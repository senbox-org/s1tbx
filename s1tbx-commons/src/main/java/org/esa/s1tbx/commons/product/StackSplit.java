package org.esa.s1tbx.commons.product;

import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.StackUtils;

import java.io.IOException;
import java.util.*;

public class StackSplit {

    private final Product srcProduct;
    private final Subset referenceSubset;
    private final List<Subset> secondarySubsetList = new ArrayList<>();

    public StackSplit(final Product sourceProduct) throws IOException {
        this.srcProduct = sourceProduct;
        final InputProductValidator validator = new InputProductValidator(srcProduct);
        if(!validator.isCollocated() && !validator.isCoregisteredStack()) {
            throw new IOException("Source product should be a collocated or coregistered stack");
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final String referenceProductName = absRoot.getAttributeString(AbstractMetadata.PRODUCT, srcProduct.getName());
        final String[] referenceBandNames = StackUtils.getMasterBandNames(srcProduct);
        referenceSubset = createSubset(srcProduct, referenceProductName, getBandNames(srcProduct, referenceBandNames));

        final String[] secondaryProductNames = StackUtils.getSlaveProductNames(sourceProduct);
        for(String secondaryProductName : secondaryProductNames) {
            final String[] secondaryBandNames = StackUtils.getSlaveBandNames(sourceProduct, secondaryProductName);
            Subset secondarySubset = createSubset(srcProduct, secondaryProductName, getBandNames(srcProduct, secondaryBandNames));
            secondarySubsetList.add(secondarySubset);
        }
    }

    public Subset getReferenceSubset() {
        return referenceSubset;
    }

    public Subset[] getSecondarySubsets() {
        return secondarySubsetList.toArray(new Subset[0]);
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

    public Subset createSubset(final Product srcProduct, final String productName, final String[] bandNames) throws IOException {

        final int width = srcProduct.getSceneRasterWidth();
        final int height = srcProduct.getSceneRasterHeight();

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(srcProduct.getTiePointGridNames());
        subsetDef.addNodeNames(bandNames);
        subsetDef.setSubsetRegion(new PixelSubsetRegion(0, 0, width, height, 0));
        subsetDef.setSubSampling(1, 1);
        subsetDef.setIgnoreMetadata(true);

        final Subset subset = new Subset();
        subset.productName = productName;
        subset.subsetBuilder = new ProductSubsetBuilder();
        subset.subsetProduct = subset.subsetBuilder.readProductNodes(srcProduct, subsetDef);

        // update band name
        for(Band trgBand : subset.subsetProduct.getBands()) {
            final String newBandName = StackUtils.getBandNameWithoutDate(trgBand.getName());
            subset.newBandNamingMap.put(newBandName, trgBand.getName());
            trgBand.setName(newBandName);

            // update virtual band expressions
            for(Band vBand : subset.subsetProduct.getBands()) {
                if(vBand instanceof VirtualBand) {
                    final VirtualBand virtBand = (VirtualBand)vBand;
                    String expression = virtBand.getExpression().replaceAll(trgBand.getName(), newBandName);
                    virtBand.setExpression(expression);
                }
            }
        }

        return subset;
    }

    public static class Subset {
        public Product subsetProduct;
        public String productName;
        public ProductSubsetBuilder subsetBuilder;
        public final Map<String, String> newBandNamingMap = new HashMap<>();
    }
}
