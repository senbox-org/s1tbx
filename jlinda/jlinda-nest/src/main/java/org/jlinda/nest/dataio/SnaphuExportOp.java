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
package org.jlinda.nest.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Split a stack product into individual products
 */
@OperatorMetadata(alias = "SnaphuExport",
        category = "Radar/Interferometric/Unwrapping",
        authors = "Petar Marinkovic, Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        autoWriteDisabled = true,
        description = "Export data and prepare conf file for SNAPHU processing")
public class SnaphuExportOp extends Operator {

    @TargetProduct
    private Product targetProduct;

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(description = "The output folder to which the data product is written.")
    private File targetFolder;

    @Parameter(valueSet = {"TOPO", "DEFO", "SMOOTH", "NOSTATCOSTS"},
            description = "Size of coherence estimation window in Azimuth direction",
            defaultValue = "DEFO",
            label = "Statistical-cost mode")
    private String statCostMode = "DEFO";

    @Parameter(valueSet = {"MST", "MCF"},
            description = "Algorithm used for initialization of the wrapped phase values",
            defaultValue = "MST",
            label = "Initial method")
    private String initMethod = "MST";

    private SubsetInfo subsetInfo;
    private String formatName = "snaphu";

    public SnaphuExportOp() {
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
                    SystemUtils.LOG.severe("Unable to create folders in "+targetFolder);
                }
            }

            targetProduct = new Product(sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            // update metadata with SNAPHU processing flags: the only way to pass info to the writer
            try {
                final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
                AbstractMetadata.addAbstractedAttribute(absTgt, "temp_1", ProductData.TYPE_ASCII, "", "Temp entry");
                AbstractMetadata.addAbstractedAttribute(absTgt, "temp_2", ProductData.TYPE_ASCII, "", "Temp entry");
                AbstractMetadata.setAttribute(absTgt, "temp_1", statCostMode.toUpperCase());
                AbstractMetadata.setAttribute(absTgt, "temp_2", initMethod.toUpperCase());
            } catch (Throwable e){
                OperatorUtils.catchOperatorException(getId() + "Metadata of input product is not in the format compatible for SNAPHU export.", e);
            }

            boolean coherenceBandFound = false, phaseBandFound = false;
            for(Band srcBand : sourceProduct.getBands()) {
                if(srcBand.getUnit().contains(Unit.COHERENCE)) {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                    coherenceBandFound = true;
                } else if(srcBand.getUnit().contains(Unit.PHASE) && !srcBand.getName().toLowerCase().contains("topo")) {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                    phaseBandFound = true;
                }
            }

            if(!coherenceBandFound) {
                throw new OperatorException("Coherence band required. Please reprocess to include a coherence band");
            }
            if(!phaseBandFound) {
                throw new OperatorException("Wrapped phase band required");
            }

            subsetInfo = new SubsetInfo();

            subsetInfo.subsetProduct = targetProduct;
            subsetInfo.file = new File(targetFolder, targetProduct.getName());

            subsetInfo.productWriter = ProductIO.getProductWriter(formatName);
            if (subsetInfo.productWriter == null) {
                throw new OperatorException("No data product writer for the '" + formatName + "' format available");
            }
            subsetInfo.productWriter.setFormatName(formatName);
            subsetInfo.productWriter.setIncrementalMode(false);
            targetProduct.setProductWriter(subsetInfo.productWriter);

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    @Override
    public synchronized void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            writeHeader(subsetInfo);

            final Rectangle trgRect = targetTile.getRectangle();
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(targetBand.getName()), trgRect);
            final ProductData rawSamples = sourceTile.getRawSamples();

            subsetInfo.productWriter.writeBandRasterData(targetBand,
                    trgRect.x, trgRect.y, trgRect.width, trgRect.height, rawSamples, ProgressMonitor.NULL);
        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private synchronized void writeHeader(final SubsetInfo info) throws Exception {
        if (info.written) return;

        subsetInfo.productWriter.writeProductNodes(subsetInfo.subsetProduct, subsetInfo.file);

        info.written = true;
    }

    @Override
    public void dispose() {
        try {
            if(subsetInfo != null) {
                if(subsetInfo.productWriter != null) {
                    subsetInfo.productWriter.close();
                }
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class SubsetInfo {
        Product subsetProduct;
        File file;
        ProductWriter productWriter;
        boolean written = false;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnaphuExportOp.class);
        }
    }
}
