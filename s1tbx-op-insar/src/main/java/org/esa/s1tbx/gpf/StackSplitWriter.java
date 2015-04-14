/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.dataio.ProductSubsetBuilder;
import org.esa.snap.framework.dataio.ProductSubsetDef;
import org.esa.snap.framework.dataio.ProductWriter;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.framework.gpf.experimental.Output;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.StackUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Split a stack product into individual products
 */
@OperatorMetadata(alias = "Stack-Split",
        description = "Writes all bands to files.",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        category = "SAR Processing/Coregistration/Stack Tools")
public class StackSplitWriter extends Operator implements Output {

    @TargetProduct
    private Product targetProduct;

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(defaultValue = "target", description = "The output folder to which the data product is written.")
    private File targetFolder;

    @Parameter(defaultValue = "BEAM-DIMAP",
            description = "The name of the output file format.")
    private String formatName;

    private final Map<Band, SubsetInfo> bandMap = new HashMap<>();

    public StackSplitWriter() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfCoregisteredStack();

            if(targetFolder == null) {
                throw new OperatorException("Please add a target folder");
            }
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }

            final int width = sourceProduct.getSceneRasterWidth();
            final int height = sourceProduct.getSceneRasterHeight();

            targetProduct = sourceProduct;
            targetProduct.setPreferredTileSize(new Dimension(width, height));

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            final String mstProductName = absRoot.getAttributeString(AbstractMetadata.PRODUCT, sourceProduct.getName());
            final String[] mstNames = StackUtils.getMasterBandNames(sourceProduct);
            createSubset(mstProductName, getBandNames(mstNames));

            final String[] slvProductNames = StackUtils.getSlaveProductNames(sourceProduct);
            for(String slvProductName : slvProductNames) {
                final String[] slvBandNames = StackUtils.getSlaveBandNames(sourceProduct, slvProductName);
                createSubset(slvProductName, getBandNames(slvBandNames));
            }

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private String[] getBandNames(final String[] names) {
        final Set<String> bandNames = new HashSet<>();
        for(String name : names) {
            final String suffix = StackUtils.getBandSuffix(name);
            for(String srcBandName : sourceProduct.getBandNames()) {
                if(srcBandName.endsWith(suffix)) {
                    bandNames.add(srcBandName);
                }
            }
        }
        return bandNames.toArray(new String[bandNames.size()]);
    }

    private void createSubset(final String productName, final String[] bandNames) throws IOException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        subsetDef.addNodeNames(bandNames);
        subsetDef.setRegion(0, 0, width, height);
        subsetDef.setSubSampling(1, 1);
        subsetDef.setIgnoreMetadata(true);

        SubsetInfo subsetInfo = new SubsetInfo();
        subsetInfo.subsetBuilder = new ProductSubsetBuilder();
        subsetInfo.subsetProduct = subsetInfo.subsetBuilder.readProductNodes(sourceProduct, subsetDef);
        subsetInfo.file = new File(targetFolder, productName);

        // update band name
        for(Band trgBand : subsetInfo.subsetProduct.getBands()) {
            final String newBandName = StackUtils.getBandNameWithoutDate(trgBand.getName());
            subsetInfo.newBandNamingMap.put(newBandName, trgBand.getName());
            trgBand.setName(newBandName);

            // update virtual band expressions
            for(Band vBand : subsetInfo.subsetProduct.getBands()) {
                if(vBand instanceof VirtualBand) {
                    final VirtualBand virtBand = (VirtualBand)vBand;
                    String expression = virtBand.getExpression().replaceAll(trgBand.getName(), newBandName);
                    virtBand.setExpression(expression);
                }
            }
        }

        subsetInfo.productWriter = ProductIO.getProductWriter(formatName);
        if (subsetInfo.productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        subsetInfo.productWriter.setFormatName(formatName);
        subsetInfo.productWriter.setIncrementalMode(false);
        subsetInfo.subsetProduct.setProductWriter(subsetInfo.productWriter);
        bandMap.put(targetProduct.getBand(bandNames[0]), subsetInfo);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final SubsetInfo subsetInfo = bandMap.get(targetBand);
            if(subsetInfo == null)
                return;

            subsetInfo.productWriter.writeProductNodes(subsetInfo.subsetProduct, subsetInfo.file);

            final Rectangle trgRect = subsetInfo.subsetBuilder.getSubsetDef().getRegion();
            if (!subsetInfo.written) {
                writeTile(subsetInfo, trgRect);
            }
        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private synchronized void writeTile(final SubsetInfo info, final Rectangle trgRect)
            throws IOException {
        if (info.written) return;

        for(Band trgBand : info.subsetProduct.getBands()) {
            final String oldBandName = info.newBandNamingMap.get(trgBand.getName());
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(oldBandName), trgRect);
            final ProductData rawSamples = sourceTile.getRawSamples();

            //final String newBandName = StackUtils.getBandNameWithoutDate(bandName);
            info.productWriter.writeBandRasterData(trgBand,
                    0, 0, trgBand.getSceneRasterWidth(), trgBand.getSceneRasterHeight(), rawSamples, ProgressMonitor.NULL);
        }
        info.written = true;
    }

    @Override
    public void dispose() {
        try {
            for (Band band : bandMap.keySet()) {
                SubsetInfo info = bandMap.get(band);
                info.productWriter.close();
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class SubsetInfo {
        Product subsetProduct;
        ProductSubsetBuilder subsetBuilder;
        File file;
        ProductWriter productWriter;
        boolean written = false;
        final Map<String, String> newBandNamingMap = new HashMap<>();
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(StackSplitWriter.class);
        }
    }

}
