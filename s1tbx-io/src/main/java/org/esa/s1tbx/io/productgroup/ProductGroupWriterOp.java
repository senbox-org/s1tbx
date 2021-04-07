/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.io.productgroup;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.product.StackSplit;
import org.esa.s1tbx.io.productgroup.support.ProductGroupAsset;
import org.esa.s1tbx.io.productgroup.support.ProductGroupMetadataFile;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Save a product as a Product Group
 */
@OperatorMetadata(alias = "ProductGroupWriter",
        authors = "Luis Veci",
        copyright = "Copyright (C) 2021 by SkyWatch Space Applications Inc.",
        version = "1.0",
        description = "Writes a stack as a product group",
        autoWriteDisabled = true,
        category = "Tools")
public class ProductGroupWriterOp extends Operator {

    @TargetProduct
    private Product targetProduct;

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(defaultValue = "target", description = "The output folder to which the data product is written.")
    private File targetFolder;

    @Parameter(defaultValue = "BEAM-DIMAP",
            description = "The name of the output file format.")
    private String formatName;

    private File metadataFile;
    private final Map<String, SubsetInfo> bandMap = new HashMap<>();
    private final Map<SubsetInfo, ProductGroupAsset> assetMap = new HashMap<>();
    private final static String ext = ".dim";

    private final static boolean DEBUG = true;

    public ProductGroupWriterOp() {
        setRequiresAllBands(true);
    }

    public ProductGroupWriterOp(Product sourceProduct, File targetFolder, String formatName) {
        this();
        Guardian.assertNotNull("targetFolder", targetFolder);

        if(targetFolder.getName().equals(ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE)) {
            targetFolder = targetFolder.getParentFile();
        }
        this.sourceProduct = sourceProduct;
        this.targetFolder = targetFolder;
        this.formatName = formatName;
    }

    public File getTargetFolder() {
        return targetFolder;
    }

    public File writeProduct(final ProgressMonitor pm) {
        long startNanos = System.nanoTime();
        getLogger().info("Start writing product " + getTargetProduct().getName() + " to " + targetFolder);
        OperatorExecutor operatorExecutor = OperatorExecutor.create(this);
        try {
            operatorExecutor.execute(OperatorExecutor.ExecutionOrder.SCHEDULE_ROW_COLUMN_BAND, "Writing...", pm);

            getLogger().info("End writing product " + getTargetProduct().getName() + " to " + targetFolder);

            double millis = (System.nanoTime() - startNanos) / 1.0E6;
            double seconds = millis / 1.0E3;
            int w = getTargetProduct().getSceneRasterWidth();
            int h = getTargetProduct().getSceneRasterHeight();

            getLogger().info(String.format("Time: %6.3f s total, %6.3f ms per line, %3.6f ms per pixel",
                    seconds,
                    millis / h,
                    millis / h / w));

            stopTileComputationObservation();

            return metadataFile;
        } catch (OperatorException e) {
            throw e;
        } finally {
            dispose();
        }
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            if(!validator.isCollocated() && !validator.isCoregisteredStack()) {
                throw new IOException("Source product should be a collocated or coregistered stack");
            }

            if(targetFolder == null) {
                throw new OperatorException("Please add a target folder");
            }
            if (!targetFolder.exists()) {
                if(!targetFolder.mkdirs()) {
                    throw new IOException("Failed to create directory '" + targetFolder + "'.");
                }
            }

            targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            ProductUtils.copyProductNodes(sourceProduct, targetProduct);
            for(Band band : sourceProduct.getBands()) {
                if (band instanceof VirtualBand) {
                    ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) band, band.getName());
                } else {
                    ProductUtils.copyBand(band.getName(), sourceProduct, band.getName(), targetProduct, false);
                }
            }

            final StackSplit stackSplit = new StackSplit(targetProduct, true);

            final List<SubsetInfo> assetList = new ArrayList<>();
            if (stackSplit.getReferenceSubset() != null) {
                SubsetInfo refSubset = createSubset(stackSplit.getReferenceSubset());
                assetList.add(refSubset);
            }

            final StackSplit.SplitProduct[] secondarySplitProducts = stackSplit.getSecondarySubsets();
            for (StackSplit.SplitProduct splitProduct : secondarySplitProducts) {
                SubsetInfo secSubset = createSubset(splitProduct);
                assetList.add(secSubset);
            }

            this.metadataFile = writeProductGroupMetadataFile(assetList);

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private SubsetInfo createSubset(final StackSplit.SplitProduct splitProduct) {

        final SubsetInfo subsetInfo = new SubsetInfo(splitProduct, formatName, new File(targetFolder, splitProduct.productName));

        for (String bandName : splitProduct.srcBandNames) {
            Band band = sourceProduct.getBand(bandName);
            if (!(band instanceof VirtualBand)) {
                bandMap.put(band.getName(), subsetInfo);
                break;
            }
        }
        return subsetInfo;
    }

    private File writeProductGroupMetadataFile(final List<SubsetInfo> assetList) throws Exception {
        final ProductGroupMetadataFile metadataFile = new ProductGroupMetadataFile();
        final File file = new File(targetFolder, ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE);
        if(file.exists()) {
            metadataFile.read(file);
        }

        for(ProductGroupWriterOp.SubsetInfo subsetInfo : assetList) {
            ProductGroupAsset newAsset = new ProductGroupAsset(
                    subsetInfo.splitProduct.productName, relativePath(subsetInfo.file) + ext, formatName);

            ProductGroupAsset origAsset = metadataFile.findAsset(newAsset);
            if(origAsset == null) {
                metadataFile.addAsset(newAsset);
                origAsset = newAsset;
            } else {
                if(!origAsset.getFormat().equals(newAsset.getFormat())) {
                    throw new IOException("ProductGroup of format "+formatName
                            +" cannot be saved to an existing folder of format "+ origAsset.getFormat());
                }
            }
            assetMap.put(subsetInfo, origAsset);
        }

        return metadataFile.write(sourceProduct.getName(), sourceProduct.getProductType(), file);
    }

    private String relativePath(final File file) {
        return targetFolder.toPath().relativize(file.toPath()).toString();
    }

    @Override
    public void computeTile(final Band targetBand, final Tile targetTile, final ProgressMonitor pm) throws OperatorException {
        try {
            computeBand(targetBand, targetTile.getRectangle());

        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    public void computeBand(final Band targetBand, final Rectangle rect) throws IOException {
        final SubsetInfo subsetInfo = bandMap.get(targetBand.getName());
        if (subsetInfo == null) {
            // only one band per product is in the bandMap
            return;
        }

        final ProductGroupAsset asset = assetMap.get(subsetInfo);
        if (asset.isModified()) {
            print("Computing "+ targetBand.getName() + " for " + rect);

            subsetInfo.productWriter.writeProductNodes(subsetInfo.splitProduct.subsetProduct, subsetInfo.file);

            writeTile(subsetInfo, rect);
        } else {
            print(targetBand.getName() + " not modified for " + rect);
        }
    }

    private synchronized void writeTile(final SubsetInfo info, final Rectangle trgRect) throws IOException {
        for(Band trgBand : info.splitProduct.subsetProduct.getBands()) {
            if (trgBand instanceof VirtualBand) {
                continue;
            }
            final String oldBandName = info.splitProduct.newBandNamingMap.get(trgBand.getName());
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(oldBandName), trgRect);
            final ProductData rawSamples = sourceTile.getRawSamples();

            info.productWriter.writeBandRasterData(trgBand,
                    trgRect.x, trgRect.y, trgRect.width, trgRect.height,
                    rawSamples, ProgressMonitor.NULL);
        }
    }

    @Override
    public void dispose() {
        try {
            for (String bandName : bandMap.keySet()) {
                SubsetInfo subsetInfo = bandMap.get(bandName);
                subsetInfo.productWriter.close();
                subsetInfo.splitProduct.subsetProduct.dispose();
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class SubsetInfo {
        final StackSplit.SplitProduct splitProduct;
        final File file;
        final ProductWriter productWriter;
        boolean written = false;

        SubsetInfo(final StackSplit.SplitProduct splitProduct, final String formatName, final File file) throws OperatorException {
            this.splitProduct = splitProduct;
            this.file = file;

            this.productWriter = ProductIO.getProductWriter(formatName);
            if (productWriter == null) {
                throw new OperatorException("No data product writer for the '" + formatName + "' format available");
            }
            productWriter.setFormatName(formatName);
            productWriter.setIncrementalMode(false);
            splitProduct.subsetProduct.setProductWriter(productWriter);
        }
    }

    private static void print(final String str) {
        if(DEBUG) {
            SystemUtils.LOG.fine(str);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProductGroupWriterOp.class);
        }
    }
}
