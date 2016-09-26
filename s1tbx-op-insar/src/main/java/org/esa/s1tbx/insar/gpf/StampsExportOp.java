/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Export products into format suitable for import to StaMPS.
 */
@OperatorMetadata(alias = "StampsExport",
        category = "Radar/Interferometric/PSI \\ SBAS",
        authors = "Cecilia Wong, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        autoWriteDisabled = true,
        description = "Export data for StaMPS processing")
public class StampsExportOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The output folder to which the data product is written.")
    private File targetFolder;

    private static final String formatName = "Gamma";

    private static final String[] folder = {"rslc", "diff0", "geo"};
    private static final String[] ext = {".rslc", ".diff", "_dem.rdc"};
    private static enum FOLDERS { RSLC, DIFF, GEO}

    private final HashMap<Band, WriterInfo> tgtBandToInfoMap = new HashMap<>();

    public StampsExportOp() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            if (sourceProduct.length != 2) {
                throw new OperatorException("Input requires coregistered stack and interferogram");
            }

            //SystemUtils.LOG.info("StampsExportOp: SLC product: " + sourceProduct[0]);
            //SystemUtils.LOG.info("StampsExportOp: IGF product: " + sourceProduct[1]);

            // First source product should be a stack of coregistered SLC products
            final InputProductValidator validator = new InputProductValidator(sourceProduct[0]);
            validator.checkIfCoregisteredStack();
            validator.checkIfSLC();
            validator.checkIfCompatibleProducts(sourceProduct);

            if(targetFolder == null) {
                //throw new OperatorException("Please add a target folder");
                targetFolder = new File("H:\\PROJECTS\\sentinel\\2016_summer\\stamps\\output_data"); // TODO
            }
            if (!targetFolder.exists()) {
                if(!targetFolder.mkdirs()) {
                    SystemUtils.LOG.severe("Unable to create folders in "+targetFolder);
                }
            }

            targetProduct = new Product(sourceProduct[1].getName(),
                    sourceProduct[1].getProductType(),
                    sourceProduct[1].getSceneRasterWidth(),
                    sourceProduct[1].getSceneRasterHeight());

            ProductUtils.copyProductNodes(sourceProduct[1], targetProduct);

            for (int i = 0; i < sourceProduct.length; i++) {

                for (Band srcBand : sourceProduct[i].getBands()) {
                    final String srcBandName = srcBand.getName();
                    if (srcBandName.startsWith("i_")) {
                        final FOLDERS folderType = srcBandName.startsWith("i_ifg") ? FOLDERS.DIFF : FOLDERS.RSLC;
                        final String targetBandName = "i_" + extractDate(srcBandName, i) + ext[folderType.ordinal()];
                        final Band targetBand = ProductUtils.copyBand(srcBandName, sourceProduct[i], targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, new WriterInfo(folder[folderType.ordinal()], targetBandName));

                        System.out.println("copy/add " + srcBandName + " to " + targetBand.getName());
                    } else if (srcBandName.startsWith("q_")) {
                        // It is necessary to copy the q bands to target product because the product writer will look
                        // for them in the target product
                        final FOLDERS folderType = srcBandName.startsWith("q_ifg") ? FOLDERS.DIFF : FOLDERS.RSLC;
                        final String targetBandName = "q_" + extractDate(srcBandName, i) + ext[folderType.ordinal()];
                        ProductUtils.copyBand(srcBandName, sourceProduct[i], targetBandName, targetProduct, true);

                        System.out.println("copy " + srcBandName + " to " + targetBandName);
                    } else if (srcBandName.startsWith("elevation")) {
                        final String targetBandName = srcBandName + ext[FOLDERS.GEO.ordinal()];
                        final Band targetBand = ProductUtils.copyBand(srcBandName, sourceProduct[i], targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, new WriterInfo(folder[FOLDERS.GEO.ordinal()], targetBandName));

                        System.out.println("copy/add " + srcBandName + " to " + targetBand.getName());
                    }
                }
            }

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private static String convertFormat(String rawDate) {
        try {
            final DateFormat rawDateFormat = ProductData.UTC.createDateFormat("ddMMMyyyy");
            final ProductData.UTC utc = ProductData.UTC.parse(rawDate, rawDateFormat);
            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMdd");
            final String date = dateFormat.format(utc.getAsDate());
            //System.out.println("rawdate = " + rawDate + " date = " + date);
            return date;
        } catch (Exception e) {
            SystemUtils.LOG.severe("failed to convert date" + e.getMessage());
            return rawDate;
        }
    }

    private static String extractDate(final String bandName, int i) {
        final String rawDate = bandName.substring(bandName.lastIndexOf('_')+1, bandName.length());
        return convertFormat(rawDate);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        for (Band targetBand : tgtBandToInfoMap.keySet()) {

            try {
                final WriterInfo info = tgtBandToInfoMap.get(targetBand);
                final Tile targetTile = targetTiles.get(targetBand);

                writeHeader(info);
                final Rectangle trgRect = targetTile.getRectangle();
                final Tile sourceTile = getSourceTile(targetBand, trgRect);
                final ProductData rawSamples = sourceTile.getRawSamples();
                info.productWriter.writeBandRasterData(targetBand,
                        trgRect.x, trgRect.y, trgRect.width, trgRect.height, rawSamples, ProgressMonitor.NULL);
            } catch (Exception e) {
                if (e instanceof OperatorException) {
                    throw (OperatorException) e;
                } else {
                    throw new OperatorException(e);
                }
            }
        }
    }

    private synchronized void writeHeader(final WriterInfo info) throws Exception {
        if (info.written) return;

        final File outputFile = targetFolder.toPath().resolve(info.folderName).resolve(info.targetBandName+".par").toFile();
        info.productWriter.writeProductNodes(targetProduct, outputFile);

        info.written = true;
    }

    @Override
    public void dispose() {
        try {
            for (WriterInfo info : tgtBandToInfoMap.values()) {
                if (info != null) {
                    if (info.productWriter != null) {
                        info.productWriter.close();
                        info.productWriter = null;
                    }
                }
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class WriterInfo {
        // different bands can share same BaseInfo
        ProductWriter productWriter;
        boolean written = false;

        final String folderName;
        final String targetBandName;

        public WriterInfo(final String folderName, final String targetBandName) {
            this.folderName = folderName;
            this.targetBandName = targetBandName.startsWith("i_") ?
                    targetBandName.substring(2, targetBandName.length()) : targetBandName;

            productWriter = ProductIO.getProductWriter(formatName);
            if (productWriter == null) {
                throw new OperatorException("No data product writer for the '" + formatName + "' format available");
            }
            productWriter.setIncrementalMode(false);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(StampsExportOp.class);
        }
    }
}
