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
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Split a stack product into individual products
 */
@OperatorMetadata(alias = "SnaphuExport",
        category = "SAR Processing/Interferometric/Unwrapping",
        authors = "Petar Marinkovic, Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Export data and prepare conf file for SNAPHU processing")
public class SnaphuExportOp extends Operator implements Output {

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
                targetFolder.mkdirs();
            }

            final int width = sourceProduct.getSceneRasterWidth();
            final int height = sourceProduct.getSceneRasterHeight();

            targetProduct = sourceProduct;
            //targetProduct.setPreferredTileSize(new Dimension(width, height));

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

            final List<String> bandNames = new ArrayList<>();
            for(Band srcBand : sourceProduct.getBands()) {
                if(srcBand.getUnit().contains(Unit.COHERENCE) || srcBand.getUnit().contains(Unit.PHASE)) {
                    bandNames.add(srcBand.getName());
                }
            }

            subsetInfo = new SubsetInfo();

            final ProductSubsetDef subsetDef = new ProductSubsetDef();
            subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
            subsetDef.addNodeNames(bandNames.toArray(new String[bandNames.size()]));
            subsetDef.setRegion(0, 0, width, height);
            subsetDef.setSubSampling(1, 1);
            subsetDef.setIgnoreMetadata(false);

            ProductSubsetBuilder subsetBuilder = new ProductSubsetBuilder();
            subsetInfo.subsetProduct = subsetBuilder.readProductNodes(sourceProduct, subsetDef);
            subsetInfo.file = new File(targetFolder, targetProduct.getName());

            subsetInfo.productWriter = ProductIO.getProductWriter(formatName);
            if (subsetInfo.productWriter == null) {
                throw new OperatorException("No data product writer for the '" + formatName + "' format available");
            }
            subsetInfo.productWriter.setFormatName(formatName);
            subsetInfo.productWriter.setIncrementalMode(false);
            subsetInfo.subsetProduct.setProductWriter(subsetInfo.productWriter);

            targetProduct.setModified(false);
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    @Override
    public synchronized void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            if (!subsetInfo.written) {

                writeTile(subsetInfo, new Rectangle(0,0, targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
                //writeTile(subsetInfo, targetTile.getRectangle());
            }
        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private synchronized void writeTile(final SubsetInfo info, final Rectangle trgRect) throws Exception {
        if (info.written) return;

        subsetInfo.productWriter.writeProductNodes(subsetInfo.subsetProduct, subsetInfo.file);

        for(Band trgBand : info.subsetProduct.getBands()) {
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(trgBand.getName()), trgRect);
            final ProductData rawSamples = sourceTile.getRawSamples();

            info.productWriter.writeBandRasterData(trgBand,
                    0, 0, trgBand.getSceneRasterWidth(), trgBand.getSceneRasterHeight(), rawSamples, ProgressMonitor.NULL);
            //info.productWriter.writeBandRasterData(trgBand,
            //        trgRect.x, trgRect.y, trgRect.width, trgRect.height, rawSamples, ProgressMonitor.NULL);
        }
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
        final Map<String, String> newBandNamingMap = new HashMap<>();
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnaphuExportOp.class);
        }
    }
}