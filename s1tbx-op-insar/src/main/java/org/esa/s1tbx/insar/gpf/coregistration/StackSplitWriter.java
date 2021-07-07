
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.product.StackSplit;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Split a stack product into individual products
 */
@OperatorMetadata(alias = "Stack-Split",
        description = "Writes all bands to files.",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        autoWriteDisabled = true,
        category = "Radar/Coregistration/Stack Tools")
public class StackSplitWriter extends Operator {

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
                if(!targetFolder.mkdirs()) {
                    throw new IOException("Failed to create directory '" + targetFolder + "'.");
                }
            }

            targetProduct = sourceProduct;
            targetProduct.setPreferredTileSize(
                    new Dimension(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight()));

            final StackSplit stackSplit = new StackSplit(sourceProduct, false);

            createSubset(stackSplit.getReferenceSubset());

            final StackSplit.SplitProduct[] secondarySplitProducts = stackSplit.getSecondarySubsets();
            for(StackSplit.SplitProduct secondarySplitProduct : secondarySplitProducts) {
                createSubset(secondarySplitProduct);
            }

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private void createSubset(final StackSplit.SplitProduct splitProduct) {

        final SubsetInfo subsetInfo = new SubsetInfo();
        subsetInfo.splitProduct = splitProduct;
        subsetInfo.file = new File(targetFolder, splitProduct.productName);

        subsetInfo.productWriter = ProductIO.getProductWriter(formatName);
        if (subsetInfo.productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        subsetInfo.productWriter.setFormatName(formatName);
        subsetInfo.productWriter.setIncrementalMode(false);
        subsetInfo.splitProduct.subsetProduct.setProductWriter(subsetInfo.productWriter);
        for (String bandName : splitProduct.srcBandNames) {
            Band band = targetProduct.getBand(bandName);
            if (!(band instanceof VirtualBand)) {
                bandMap.put(band, subsetInfo);
                break;
            }
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final SubsetInfo subsetInfo = bandMap.get(targetBand);
            if(subsetInfo == null)
                return;

            subsetInfo.productWriter.writeProductNodes(subsetInfo.splitProduct.subsetProduct, subsetInfo.file);

            final Rectangle trgRect = subsetInfo.splitProduct.subsetDef.getRegion();
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

    private synchronized void writeTile(final SubsetInfo info, final Rectangle trgRect) throws IOException {
        if (info.written) return;

        for(Band trgBand : info.splitProduct.subsetProduct.getBands()) {
            final String oldBandName = info.splitProduct.newBandNamingMap.get(trgBand.getName());
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
            for (Band band : bandMap.keySet()) {
                SubsetInfo info = bandMap.get(band);
                info.productWriter.close();
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class SubsetInfo {
        StackSplit.SplitProduct splitProduct;
        File file;
        ProductWriter productWriter;
        boolean written = false;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(StackSplitWriter.class);
        }
    }
}
