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
import org.esa.s1tbx.insar.gpf.support.ProjectedDEM;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.*;
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
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
        copyright = "Copyright (C) 2017 by Array Systems Computing Inc.",
        autoWriteDisabled = true,
        description = "Export data for StaMPS processing")
public class StampsExportOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The output folder to which the data product is written.")
    private File targetFolder;

    @Parameter(description = "Format for PSI or SBAS", defaultValue = "true")
    private Boolean psiFormat = true;

    private static final String formatName = "Gamma";

    private static final String[] folder = {"rslc", "diff0", "geo", "dem"};
    private static final String[] ext = {".rslc", ".diff", "_dem.rdc", "_dem"};
    private enum FOLDERS {RSLC, DIFF, GEO, DEM}

    private final DateFormat rawDateFormat = ProductData.UTC.createDateFormat("ddMMMyyyy");
    private final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMdd");

    private final HashMap<Band, WriterInfo> tgtBandToInfoMap = new HashMap<>();
    private ProjectedDEM projectedDEM;
    private WriterInfo projectedDEMInfo;
    private boolean projectedDEMWritten = false;
    private WriterInfo latInfo;
    private WriterInfo lonInfo;
    private TiePointGrid latGrid = null;
    private TiePointGrid lonGrid = null;
    private Band latBand = null;
    private Band lonBand = null;

    public StampsExportOp() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            if (sourceProduct.length != 2) {
                throw new OperatorException("Input requires a coregistered stack (1st product) and an interferogram product of at least 4 interferograms");
            }

            if(!psiFormat) {
                throw new OperatorException("SBAS format is not yet supported.");
            }

            //SystemUtils.LOG.info("StampsExportOp: SLC product: " + sourceProduct[0]);
            //SystemUtils.LOG.info("StampsExportOp: IGF product: " + sourceProduct[1]);

            // First source product should be a stack of coregistered SLC products
            final InputProductValidator validator = new InputProductValidator(sourceProduct[0]);
            validator.checkIfCoregisteredStack();
            validator.checkIfSLC();
            validator.checkIfTOPSARBurstProduct(false);
            validator.checkIfCompatibleProducts(sourceProduct);

            final InputProductValidator validator2 = new InputProductValidator(sourceProduct[1]);
            validator2.checkIfCoregisteredStack();
            validator2.checkIfSLC();
            validator2.checkIfTOPSARBurstProduct(false);

            // First product should be stack product
            String bandnames[] = sourceProduct[0].getBandNames();
            boolean foundmst = false;
            for (String bandname : bandnames) {
                if (bandname.toLowerCase().contains("mst")) {
                    foundmst = true;
                    break;
                }
            }
            if (!foundmst) {
                throw new OperatorException("The 1st product should be a stack of coregistered SLC products, the 2nd should be interferogram");
            }


            if (targetFolder == null) {
                throw new OperatorException("Please add a target folder");
            }
            if (!targetFolder.exists()) {
                if (!targetFolder.mkdirs()) {
                    SystemUtils.LOG.severe("Unable to create folders in " + targetFolder);
                }
            }

            targetProduct = new Product(sourceProduct[1].getName(),
                    sourceProduct[1].getProductType(),
                    sourceProduct[1].getSceneRasterWidth(),
                    sourceProduct[1].getSceneRasterHeight());

            ProductUtils.copyProductNodes(sourceProduct[1], targetProduct);

            boolean includesElevation = false;
            boolean includesLat = false;
            boolean includesLon = false;
            for (Product aSourceProduct : sourceProduct) {

                for (Band srcBand : aSourceProduct.getBands()) {
                    final String srcBandName = srcBand.getName();
                    if (srcBandName.startsWith("i_")) {
                        final FOLDERS folderType = srcBandName.startsWith("i_ifg") ? FOLDERS.DIFF : FOLDERS.RSLC;
                        final String targetBandName =
                                "i_" + extractDate(srcBandName, folderType) + ext[folderType.ordinal()];
                        final Band targetBand = ProductUtils.copyBand(
                                srcBandName, aSourceProduct, targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, new WriterInfo(
                                folder[folderType.ordinal()], targetBandName, targetProduct));

                        //System.out.println("copy/add " + srcBandName + " to " + targetBand.getName());
                    } else if (srcBandName.startsWith("q_")) {
                        // It is necessary to copy the q bands to target product because the product writer will look
                        // for them in the target product
                        final FOLDERS folderType = srcBandName.startsWith("q_ifg") ? FOLDERS.DIFF : FOLDERS.RSLC;
                        final String targetBandName =
                                "q_" + extractDate(srcBandName, folderType) + ext[folderType.ordinal()];
                        ProductUtils.copyBand(srcBandName, aSourceProduct, targetBandName, targetProduct, true);

                        //System.out.println("copy " + srcBandName + " to " + targetBandName);
                    } else if (srcBandName.startsWith("elevation")) {
                        final String targetBandName = srcBandName + ext[FOLDERS.GEO.ordinal()];
                        final Band targetBand = ProductUtils.copyBand(
                                srcBandName, aSourceProduct, targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, new WriterInfo(
                                folder[FOLDERS.GEO.ordinal()], targetBandName, targetProduct));
                        includesElevation = true;

                        //System.out.println("copy/add " + srcBandName + " to " + targetBand.getName());
                    } else if (srcBandName.equals("orthorectifiedLat") || srcBandName.equals("orthorectifiedLon")) {
                        final String masterDateStr = extractDate(sourceProduct[0].getBandAt(0).getName(), FOLDERS.GEO);
                        String targetBandName = masterDateStr;
                        if (srcBandName.equals("orthorectifiedLat")) {
                            targetBandName = targetBandName + ".lat";
                            includesLat = true;
                        } else {
                            targetBandName = targetBandName + ".lon";
                            includesLon = true;
                        }
                        final Band targetBand = ProductUtils.copyBand(
                                srcBandName, aSourceProduct, targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, new WriterInfo(
                                folder[FOLDERS.GEO.ordinal()], targetBandName, targetProduct));
                    } else if (srcBandName.equals("orthorectifiedLon")) {

                    }
                }
            }

            String projectedDEMName = "projected" + ext[FOLDERS.DEM.ordinal()];
            projectedDEM = new ProjectedDEM(projectedDEMName, sourceProduct[0]);
            projectedDEMInfo = new WriterInfo(
                    folder[FOLDERS.DEM.ordinal()], projectedDEMName, projectedDEM.getTargetProduct());

            if (!includesElevation) {
                throw new OperatorException(
                        "Elevation band required. Please add an elevation band to the interferogram product.");
            }
            if (!includesLat || !includesLon) {
                throw new OperatorException(
                        "Orthorectified lat/lon bands required. Please add the bands to the interferogram product.");
            }

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private String convertFormat(String rawDate) {
        try {
            final ProductData.UTC utc = ProductData.UTC.parse(rawDate, rawDateFormat);
            final String date = dateFormat.format(utc.getAsDate());
            //System.out.println("rawdate = " + rawDate + " date = " + date);
            return date;
        } catch (Exception e) {
            SystemUtils.LOG.severe("failed to convert date" + e.getMessage());
            return rawDate;
        }
    }

    private String extractDate(final String bandName, final FOLDERS folderType) {
        String dateStr = bandName.substring(bandName.lastIndexOf('_') + 1, bandName.length());
        if (folderType.equals(FOLDERS.DIFF)) {
            String mstStr = bandName.substring(0, bandName.lastIndexOf('_'));
            String mstDate = mstStr.substring(mstStr.lastIndexOf('_') + 1, mstStr.length());
            return convertFormat(mstDate) + '_' + convertFormat(dateStr);
        }
        return convertFormat(dateStr);
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

        if(!projectedDEMWritten) {
            writeProjectedDEM();
        }
    }

    private synchronized void writeProjectedDEM() {
        if(projectedDEMWritten)
            return;

        try {
            writeHeader(projectedDEMInfo);

            final Band elevationBand = projectedDEM.getElevationBand();
            final Rectangle trgRect = new Rectangle(0, 0, elevationBand.getRasterWidth(), elevationBand.getRasterHeight());
            projectedDEM.computeTile(trgRect);

            final ProductData rawSamples = elevationBand.getData();
            projectedDEMInfo.productWriter.writeBandRasterData(elevationBand,
                    trgRect.x, trgRect.y, trgRect.width, trgRect.height, rawSamples, ProgressMonitor.NULL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        projectedDEMWritten = true;
    }

    private synchronized void writeHeader(final WriterInfo info) throws Exception {
        if (info.written) return;

        final File outputFile = targetFolder.toPath().resolve(info.folderName).resolve(info.targetBandName + ".par").toFile();
        info.productWriter.writeProductNodes(info.product, outputFile);

        if (info.folderName.equals("diff0")) {
            writeBaselineFile(info);
        }

        info.written = true;
    }

    private void writeBaselineFile(final WriterInfo info) throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);
        final double prf = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.pulse_repetition_frequency);

        final double height = 0.0;
        final double firstLine = 0.0;
        final double lastLine = sourceProduct[0].getSceneRasterHeight() - 1;
        final double refPixel = (sourceProduct[0].getSceneRasterWidth() - 1) / 2.0;
        final double t0 = firstLine / prf;
        final double tN = lastLine / prf;

        InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(sourceProduct[0]);

        String masterDate = info.targetBandName.substring(0, info.targetBandName.indexOf('_'));
        String slaveDate = info.targetBandName.substring(info.targetBandName.indexOf('_')+1, info.targetBandName.indexOf('.'));

        // find correct master slave pair
        int mstIndex = 0, slvIndex = 0;
        for(int i=0; i < stackOverview.length; ++i) {
            double mstMJD = stackOverview[i].getMasterSlave()[0].getMasterMetadata().getMjd();
            final String mstDate = dateFormat.format(new ProductData.UTC(mstMJD).getAsDate());

            if(masterDate.equals(mstDate)) {
                mstIndex = i;
                for(int j=0; j< stackOverview[i].getMasterSlave().length; ++j) {
                    double slvMJD = stackOverview[i].getMasterSlave()[j].getSlaveMetadata().getMjd();
                    final String slvDate = dateFormat.format(new ProductData.UTC(slvMJD).getAsDate());

                    if (slaveDate.equals(slvDate)) {
                        slvIndex = j;
                        break;
                    }
                }
                break;
            }
        }

        final double bh0 = stackOverview[mstIndex].getMasterSlave()[slvIndex].getHorizontalBaseline(firstLine, refPixel, height);
        final double bhN = stackOverview[mstIndex].getMasterSlave()[slvIndex].getHorizontalBaseline(lastLine, refPixel, height);
        final double bhm = (bh0 + bhN) * 0.5;
        final double bhr = (bhN - bh0) / (tN - t0);

        final double bv0 = stackOverview[mstIndex].getMasterSlave()[slvIndex].getVerticalBaseline(firstLine, refPixel, height);
        final double bvN = stackOverview[mstIndex].getMasterSlave()[slvIndex].getVerticalBaseline(lastLine, refPixel, height);
        final double bvm = (bv0 + bvN) * 0.5;
        final double bvr = (bvN - bv0) / (tN - t0);

        String baselineFilename = info.targetBandName + ".base";
        baselineFilename = baselineFilename.replace(".diff", "");

        final File outputBaselineFile =
                targetFolder.toPath().resolve(info.folderName).resolve(baselineFilename).toFile();
        final String oldEOL = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
        final FileOutputStream out = new FileOutputStream(outputBaselineFile);
        try (final PrintStream p = new PrintStream(out)) {

            p.println("initial_baseline(TCN)" + ":\t" + "0.0000000" + '\t' + bhm + '\t' + bvm + '\t' + "m   m   m");
            p.println("initial_baseline_rate" + ":\t" + "0.0000000" + '\t' + bhr + '\t' + bvr + '\t' + "m/s   m/s   m/s");
            p.println("precision_baseline(TCN)" + ":\t" + "0.0000000        0.0000000        0.0000000   m   m   m");
            p.println("precision_baseline_rate" + ":\t" + "0.0000000        0.0000000        0.0000000   m/s m/s m/s");
            p.println("unwrap_phase_constant" + ":\t" + "0.00000     radians");

            p.flush();
        } catch (Exception e) {
            throw new IOException("StampsExportOp unable to write baseline file " + e.getMessage());
        } finally {
            System.setProperty("line.separator", oldEOL);
        }
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
        ProductWriter productWriter;
        boolean written = false;

        final String folderName;
        final String targetBandName;
        final Product product;

        WriterInfo(final String folderName, final String targetBandName, final Product product) {
            this.folderName = folderName;
            this.targetBandName = targetBandName.startsWith("i_") ?
                    targetBandName.substring(2, targetBandName.length()) : targetBandName;
            this.product = product;

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
