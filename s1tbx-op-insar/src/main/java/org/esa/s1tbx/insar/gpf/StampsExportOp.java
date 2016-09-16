
package org.esa.s1tbx.insar.gpf;

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
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

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
        category = "Radar/Interferometric/PSI\\SBAS",
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

    // TODO not working
    @Parameter(description = "The output folder to which the data product is written.")
    private File targetFolder;

    final static private String formatName = "Gamma";
    final static private String[] folder = {"rslc", "diff0", "geo"};

    //private HashMap<String, String> folderToSuffixExtensionMap = new HashMap<>();
    private static final HashMap<String, String> folderToSuffixExtensionMap;
    static
    {
        folderToSuffixExtensionMap = new HashMap<>();
        folderToSuffixExtensionMap.put("rslc", ".rslc");
        folderToSuffixExtensionMap.put("diff0", ".diff");
        folderToSuffixExtensionMap.put("geo", "_dem.rdc");
    }


    private HashMap<Band, Info> tgtBandToInfoMap = new HashMap<>();

    public StampsExportOp() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            if (sourceProduct.length != 2) {
                throw new OperatorException("Require two source products");
            }

            //SystemUtils.LOG.info("StampsExportOp: SLC product: " + sourceProduct[0]);
            //SystemUtils.LOG.info("StampsExportOp: IGF product: " + sourceProduct[1]);

            // First source product should be a stack of coregistered SLC products
            final InputProductValidator validator = new InputProductValidator(sourceProduct[0]);
            validator.checkIfCoregisteredStack();
            validator.checkIfSLC();

            // TODO 2nd source product should ne interferogram generated from 1st source product
            // TODO check that the two products have the same raster size

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

            ProductUtils.copyProductNodes(sourceProduct[1], targetProduct); // TODO is this needed?

            // TODO update metadata with TBD
            try {
                final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
                // TODO
            } catch (Throwable e){
                OperatorUtils.catchOperatorException(getId() + "Metadata of input product is not in the format compatible for StaMPS export.", e);
            }

            for (int i = 0; i < sourceProduct.length; i++) {
                // Each "folder" has its own baseInfo. "rslc" and "diff0" correspond to the SLC and IFG product
                // respectively, so create the baseInfo here for the product.
                final BaseInfo baseInfo = createBaseInfo(folder[i]);
                for (Band srcBand : sourceProduct[i].getBands()) {
                    if (srcBand.getName().startsWith("i_")) {
                        final String targetBandName = "i_" + extractDate(srcBand.getName(), i) + folderToSuffixExtensionMap.get(folder[i]);
                        final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct[i], targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, createInfo(baseInfo));
                        System.out.println("copy/add " + srcBand.getName() + " to " + targetBand.getName());
                    } else if (srcBand.getName().startsWith("q_")) {
                        // It is necessary to copy the q bands to target product because the product writer will look
                        // for them in the target product
                        final String targetBandName = "q_" + extractDate(srcBand.getName(), i) + folderToSuffixExtensionMap.get(folder[i]);
                        ProductUtils.copyBand(srcBand.getName(), sourceProduct[i], targetBandName, targetProduct, true);
                        System.out.println("copy " + srcBand.getName() + " to " + targetBandName);
                    } else if (srcBand.getName().equals("elevation")) {
                        final String targetBandName = srcBand.getName() + folderToSuffixExtensionMap.get(folder[folder.length - 1]);
                        final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct[i], targetBandName, targetProduct, true);
                        tgtBandToInfoMap.put(targetBand, createInfo(createBaseInfo(folder[folder.length - 1])));
                        System.out.println("copy/add " + srcBand.getName() + " to " + targetBand.getName());
                    }
                }
            }

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private String convertFormat(String rawDate) {
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

    private String extractDate(final String bandName, int i) {
        int idx = bandName.length();
        for (int j = 0; j < i+1; j++) {
            idx =  bandName.substring(0, idx).lastIndexOf("_");
        }
        final String rawDate = bandName.substring(idx+1);
        if (rawDate.contains("_")) {
            final int idx1 = rawDate.lastIndexOf("_");
            return convertFormat(rawDate.substring(0, idx1)) + "_" + convertFormat(rawDate.substring(idx1+1));
        } else {
            return convertFormat(rawDate);
        }
    }

    private BaseInfo createBaseInfo(final String folderName) {
        final BaseInfo baseInfo = new BaseInfo();
        baseInfo.file = new File(targetFolder + "/" + folderName, targetProduct.getName()); // TODO pass in binary file name with extension
        baseInfo.productWriter = ProductIO.getProductWriter(formatName);
        if (baseInfo.productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        baseInfo.productWriter.setFormatName(formatName);

        // TODO need these...???
        baseInfo.productWriter.setIncrementalMode(false); // TODO
        targetProduct.setProductWriter(baseInfo.productWriter); /// TODO

        return baseInfo;
    }

    private Info createInfo(final BaseInfo baseInfo) {
        final Info info = new Info();
        info.baseInfo = baseInfo;
        return info;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        for (Band targetBand : tgtBandToInfoMap.keySet()) {
            final Info info = tgtBandToInfoMap.get(targetBand);
            final Tile targetTile = targetTiles.get(targetBand);
            try {
                writeHeader(info);
                final Rectangle trgRect = targetTile.getRectangle();
                final Tile sourceTile = getSourceTile(targetBand, trgRect);
                final ProductData rawSamples = sourceTile.getRawSamples();
                info.baseInfo.productWriter.writeBandRasterData(targetBand,
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

    private synchronized void writeHeader(final Info info) throws Exception {
        if (info.baseInfo.written) return;

        info.baseInfo.productWriter.writeProductNodes(targetProduct, info.baseInfo.file);

        info.baseInfo.written = true;
    }

    @Override
    public void dispose() {
        try {
            for (Info info : tgtBandToInfoMap.values()) {
                if (info != null) {
                    if (info.baseInfo.productWriter != null) {
                        info.baseInfo.productWriter.close();
                        info.baseInfo.productWriter = null;
                    }
                }
            }
        } catch (IOException ignore) {
        }
        super.dispose();
    }

    private static class Info {
        BaseInfo baseInfo;
        // This is for future needs
        // Add anything that pertains to the band
    }

    private static class BaseInfo {
        // different bands can share same BaseInfo
        File file;
        ProductWriter productWriter;
        boolean written = false;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(StampsExportOp.class);
        }
    }
}
