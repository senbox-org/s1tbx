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
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.StackUtils;

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

    private final Map<String, SubsetInfo> bandMap = new HashMap<>();

    public ProductGroupWriterOp() {
        setRequiresAllBands(true);
    }

    public ProductGroupWriterOp(Product sourceProduct, File targetFolder, String formatName) {
        this();
        Guardian.assertNotNull("targetFolder", targetFolder);
        this.sourceProduct = sourceProduct;
        this.targetFolder = targetFolder;
        this.formatName = formatName;
    }

    public void writeProduct(ProgressMonitor pm) {
        long startNanos = System.nanoTime();
        getLogger().info("Start writing product " + getTargetProduct().getName() + " to " + targetFolder);
        OperatorExecutor operatorExecutor = OperatorExecutor.create(this);
        try {
//            if (clearCacheAfterRowWrite && writeEntireTileRows) {
//                operatorExecutor.setScheduleRowsSeparate(true);
//            }
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
                ProductUtils.copyBand(band.getName(), sourceProduct, band.getName(), targetProduct, false);
            }

            targetProduct.setPreferredTileSize(
                    new Dimension(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight()));

            final StackSplit stackSplit = new StackSplit(sourceProduct);

            final List<SubsetInfo> assetList = new ArrayList<>();
            final String[] mstNames = StackUtils.getMasterBandNames(sourceProduct);
            if(mstNames.length == 0) {
                for(Band band : sourceProduct.getBands()) {
                    final String[] subsetBandNames = new String[] {band.getName()};
                    StackSplit.Subset subset = stackSplit.createSubset(sourceProduct, "product_"+band.getName(), subsetBandNames);
                    SubsetInfo secSubset = createSubset(StackSplit.getBandNames(sourceProduct, subsetBandNames), subset);
                    assetList.add(secSubset);
                }
            } else {
                SubsetInfo refSubset = createSubset(StackSplit.getBandNames(sourceProduct, mstNames), stackSplit.getReferenceSubset());
                assetList.add(refSubset);

                final StackSplit.Subset[] secondarySubsets = stackSplit.getSecondarySubsets();
                for (StackSplit.Subset subset : secondarySubsets) {
                    final String[] subsetBandNames = StackUtils.getSlaveBandNames(sourceProduct, subset.productName);
                    SubsetInfo secSubset = createSubset(StackSplit.getBandNames(sourceProduct, subsetBandNames), subset);
                    assetList.add(secSubset);
                }
            }

            writeProductGroupMetadataFile(assetList);

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private SubsetInfo createSubset(final String[] bandNames, final StackSplit.Subset subset) {

        final SubsetInfo subsetInfo = new SubsetInfo();
        subsetInfo.subset = subset;
        subsetInfo.file = new File(targetFolder, subset.productName);

        subsetInfo.productWriter = ProductIO.getProductWriter(formatName);
        if (subsetInfo.productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        subsetInfo.productWriter.setFormatName(formatName);
        subsetInfo.productWriter.setIncrementalMode(false);
        subsetInfo.subset.subsetProduct.setProductWriter(subsetInfo.productWriter);
        for (String bandName : bandNames) {
            Band band = sourceProduct.getBand(bandName);
            if (!(band instanceof VirtualBand)) {
                bandMap.put(band.getName(), subsetInfo);
                break;
            }
        }
        return subsetInfo;
    }

    private void writeProductGroupMetadataFile(final List<SubsetInfo> assetList) throws Exception {
        final ProductGroupMetadataFile metadataFile = new ProductGroupMetadataFile();
        final File file = new File(targetFolder, "product_group.json");
        if(file.exists()) {
            metadataFile.read(file);
        }

        for(ProductGroupWriterOp.SubsetInfo subsetInfo : assetList) {
            ProductGroupMetadataFile.Asset asset = new ProductGroupMetadataFile.Asset(
                    subsetInfo.subset.productName, subsetInfo.file.getAbsolutePath() + ".dim", formatName);

            metadataFile.addAsset(asset);
        }

        metadataFile.write(sourceProduct.getName(), sourceProduct.getProductType(), file);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final SubsetInfo subsetInfo = bandMap.get(targetBand.getName());
            if(subsetInfo == null) {
                // only one band per product is in the bandMap
                return;
            }

            subsetInfo.productWriter.writeProductNodes(subsetInfo.subset.subsetProduct, subsetInfo.file);

            if (!subsetInfo.written) {
                writeTile(subsetInfo, targetTile.getRectangle());
            }
        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private synchronized void writeTile(final SubsetInfo info, final Rectangle trgRect) throws IOException {
        if (info.written) return;

        for(Band trgBand : info.subset.subsetProduct.getBands()) {
            final String oldBandName = info.subset.newBandNamingMap.get(trgBand.getName());
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(oldBandName), trgRect);
            final ProductData rawSamples = sourceTile.getRawSamples();

            info.productWriter.writeBandRasterData(trgBand,
                    0, 0, trgBand.getRasterWidth(), trgBand.getRasterHeight(), rawSamples, ProgressMonitor.NULL);
        }
        info.written = true;
    }

    @Override
    public void dispose() {
        try {
            for (String bandName : bandMap.keySet()) {
                SubsetInfo info = bandMap.get(bandName);
                info.productWriter.close();
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class SubsetInfo {
        StackSplit.Subset subset;
        File file;
        ProductWriter productWriter;
        boolean written = false;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProductGroupWriterOp.class);
        }
    }
}
