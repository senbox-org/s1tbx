/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.terrasarx;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.FileImageInputStreamExtImpl;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.s1tbx.io.sunraster.SunRasterReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.util.Maths;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;

/**
 * This class represents a product directory.
 * @author Luis Veci, Carlos Hernandez, Esteban Aguilera
 */
public class TerraSarXProductDirectory extends XMLProductDirectory {

    private static final String TERRA_SAR_X = "TerraSAR-X";
    private static final String TAN_DEM_X = "TanDEM-X";

    private final File headerFile;
    private String productName = null;
    private String productType = null;
    private String productDescription = "";

    private final double[] latCorners = new double[4];
    private final double[] lonCorners = new double[4];
    private final double[] slantRangeCorners = new double[4];
    private final double[] incidenceCorners = new double[4];

    private final List<File> cosarFileList = new ArrayList<>(1);
    private final Map<String, ImageInputStream> cosarBandMap = new HashMap<>(1);

    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    // For TDM CoSSC products only
    private String masterProductName = null;
    private String slaveProductName = null;
    private boolean isTanDEMX = false;

    public TerraSarXProductDirectory(final File inputFile) {
        super(inputFile);
        headerFile = inputFile;
    }

    @Override
    protected String getHeaderFileName() {
        if (ZipUtils.isZip(headerFile)) {
            return ""; //todo
        } else {
            return headerFile.getName();
        }
    }

    @Override
    protected String getRelativePathToImageFolder() {
        return getRootFolder() + "IMAGEDATA" + '/';
    }

    private static void replaceAbstractedMetadataField(final MetadataElement abstractedMetadata, final String attrName, final String newValue) {

        final ProductData productData = abstractedMetadata.getAttribute(attrName).getData();
        productData.setElems(newValue);
    }

    private MetadataElement addMetaDataForTanDemX() throws IOException {

        // xmlDoc is the "main" annotation (i.e., the file with name "TDM... .xml")
        final Element mainRootElement = xmlDoc.getRootElement();

        final String inSARmasterID = mainRootElement.getChild("commonAcquisitionInfo")
                .getChild("inSARmasterID").getText().toLowerCase();
        final String inSARslaveID = inSARmasterID.endsWith("1") ? "sat2" : "sat1";
        final String masterSatellite = mainRootElement.getChild("commonAcquisitionInfo")
                .getChild("satelliteID" + inSARmasterID).getText();
        final String slaveSatellite = mainRootElement.getChild("commonAcquisitionInfo")
                .getChild("satelliteID" + inSARslaveID).getText();

        final List<Element> componentList = mainRootElement.getChild("productComponents").getChildren("component");
        Element masterAnnotationComponent = null;
        Element slaveAnnotationComponent = null;
        for (Element component : componentList) {
            final String satId = component.getChild("instrument").getChildText("satIDs");
            if (component.getChildText("name").startsWith("cossc_annotation")) {
                if (satId.equals(masterSatellite)) {
                    masterAnnotationComponent = component;
                }
                if (satId.equals(slaveSatellite)) {
                    slaveAnnotationComponent = component;
                }
            }
            if (masterAnnotationComponent != null && slaveAnnotationComponent != null) {
                break;
            }
        }
        if (masterAnnotationComponent == null) {
            throw new IOException("Cannot locate primary annotation component (master product) in main annotation of TDM product");
        }
        if (slaveAnnotationComponent == null) {
            throw new IOException("Cannot locate secondary annotation component (slave product) in main annotation of TDM product");
        }

        String masterHeader = masterAnnotationComponent.getChild("file").getChild("location").getChildText("name");
        masterProductName = masterHeader.substring(0, masterHeader.indexOf('/'));

        masterHeader = findHeaderFile(masterHeader, masterProductName);

        // Build the slave metadata

        String slaveHeader = slaveAnnotationComponent.getChild("file").getChild("location").getChildText("name");
        slaveProductName = slaveHeader.substring(0, slaveHeader.indexOf('/'));

        slaveHeader = findHeaderFile(slaveHeader, slaveProductName);

        final Document slaveDoc;
        try (final InputStream is = getInputStream(slaveHeader)) {
            slaveDoc = XMLSupport.LoadXML(is);
        }
        final Element slaveRootElement = slaveDoc.getRootElement();

        final MetadataElement slaveRoot = new MetadataElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        AbstractMetadataIO.AddXMLMetadata(slaveRootElement, AbstractMetadata.addOriginalProductMetadata(slaveRoot));
        addAbstractedMetadataHeader(slaveRoot);

        final MetadataElement slaveAbstractedMetadataElem = slaveRoot.getElement("Abstracted_Metadata");

        // Add Product_Information to slave Abstracted_Metadata
        final MetadataElement productInfo = new MetadataElement("Product_Information");
        final MetadataElement inputProd = new MetadataElement("InputProducts");
        productInfo.addElement(inputProd);
        inputProd.setAttributeString("InputProduct", slaveProductName);
        slaveAbstractedMetadataElem.addElement(productInfo);

        // Change the name from Abstracted_Metadata to the slave product name
        slaveAbstractedMetadataElem.setName(slaveProductName);

        // Use the master's annotation to build the Abstracted_Metadata and Original_Product_Metadata.

        final MetadataElement metadataRoot = new MetadataElement(Product.METADATA_ROOT_NAME);

        final Document masterDoc;
        try (final InputStream is = getInputStream(masterHeader)) {
            masterDoc = XMLSupport.LoadXML(is);
        }

        final Element masterRootElement = masterDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(masterRootElement, AbstractMetadata.addOriginalProductMetadata(metadataRoot));

        addAbstractedMetadataHeader(metadataRoot);

        // Replace the product name (which right now is the master product) with the TDM product.
        // Replace data in Abstracted_Metadata with TDM values.

        MetadataElement abstractedMetadata = metadataRoot.getElement("Abstracted_Metadata");

        // Turn on the coregistration flag
        abstractedMetadata.setAttributeInt("coregistered_stack", 1);

        // Replace PRODUCT
        productName = getHeaderFileName().substring(0, getHeaderFileName().length() - 4);
        replaceAbstractedMetadataField(abstractedMetadata, "PRODUCT", productName);

        // Replace PRODUCT_TYPE
        productType = mainRootElement.getChild("productInfo").getChildText("productType");
        replaceAbstractedMetadataField(abstractedMetadata, "PRODUCT_TYPE", productType);

        // Replace SPH_DESCRIPTOR
        replaceAbstractedMetadataField(abstractedMetadata, "SPH_DESCRIPTOR", mainRootElement.getChild("generalHeader").getChildText("itemName"));

        // Replace mission
        replaceAbstractedMetadataField(abstractedMetadata, "MISSION", "TDM");

        // Add the CoSSC metadata, i.e., the "main" annotation from the file with name "TDM... .xml"
        final MetadataElement cosscMetadataElem = new MetadataElement("CoSSC_Metadata");
        AbstractMetadataIO.AddXMLMetadata(mainRootElement, cosscMetadataElem);
        metadataRoot.addElement(cosscMetadataElem);

        // Turn on the bi-static flag
        abstractedMetadata.setAttributeInt("bistatic_stack", 1);

        // Add the slave metadata
        metadataRoot.addElement(slaveRoot);

        return metadataRoot;
    }

    private String findHeaderFile(final String slaveHeader, final String slaveProductName) throws IOException {
        try {
            // test file
            File slaveHeaderFile = getFile(slaveHeader);
        } catch (FileNotFoundException e) {
            File slaveProductFolder = getFile(slaveProductName);
            File[] files = slaveProductFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().toLowerCase().endsWith(".xml");
                }
            });
            if (files != null && files.length > 0) {
                return slaveProductName + '/' + files[0].getName();
            }
        }
        return slaveHeader;
    }

    public boolean isTanDEMX() {
        return isTanDEMX;
    }

    @Override
    protected MetadataElement addMetaData() throws IOException {

        if (getHeaderFileName().startsWith("TSX") || getHeaderFileName().startsWith("TDX")) {
            productName = TERRA_SAR_X;
            productType = TERRA_SAR_X;
            return super.addMetaData();
        } else if (getHeaderFileName().startsWith("TDM")) {
            productName = TAN_DEM_X;
            productType = TAN_DEM_X;
            isTanDEMX = true;
            return addMetaDataForTanDemX();
        } else {
            throw new IOException("Invalid header file: " + getHeaderFileName());
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement level1Elem = origProdRoot.getElementAt(0);
        final MetadataElement generalHeader = level1Elem.getElement("generalHeader");
        final MetadataElement productInfo = level1Elem.getElement("productInfo");
        final MetadataElement productSpecific = level1Elem.getElement("productSpecific");
        final MetadataElement missionInfo = productInfo.getElement("missionInfo");
        final MetadataElement productVariantInfo = productInfo.getElement("productVariantInfo");
        final MetadataElement imageDataInfo = productInfo.getElement("imageDataInfo");
        final MetadataElement sceneInfo = productInfo.getElement("sceneInfo");
        final MetadataElement processing = level1Elem.getElement("processing");
        final MetadataElement instrument = level1Elem.getElement("instrument");
        final MetadataElement platform = level1Elem.getElement("platform");
        final MetadataElement complexImageInfo = productSpecific.getElement("complexImageInfo");
        final MetadataElement geocodedImageInfo = productSpecific.getElement("geocodedImageInfo");

        MetadataAttribute attrib = generalHeader.getAttribute("fileName");
        if (attrib != null)
            productName = attrib.getData().getElemString().replace("_____", "_").replace("__", "_");
        if (productName.endsWith(".xml"))
            productName = productName.substring(0, productName.length() - 4);

        //mph
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, productName);
        productType = productVariantInfo.getAttributeString("productType", defStr).replace("_____", "_").replace("__", "_");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                generalHeader.getAttributeString("itemName", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, getMission());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(generalHeader, "generationTime", standardDateFormat));

        MetadataElement elem = generalHeader.getElement("generationSystem");
        if (elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                    elem.getAttributeString("generationSystem", defStr));
        }

        if (missionInfo != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, missionInfo.getAttributeInt("orbitCycle", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, missionInfo.getAttributeInt("relOrbit", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, missionInfo.getAttributeInt("absOrbit", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, missionInfo.getAttributeString("orbitDirection", defStr));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, imageDataInfo.getAttributeString("imageDataType", defStr));
        }

        final MetadataElement acquisitionInfo = productInfo.getElement("acquisitionInfo");
        final String imagingMode = getAcquisitionMode(acquisitionInfo.getAttributeString("imagingMode", defStr));
        if (acquisitionInfo != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, imagingMode);
            final String lookDirection = acquisitionInfo.getAttributeString("lookDirection", defStr);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, lookDirection.toLowerCase());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS,
                    acquisitionInfo.getAttributeString("elevationBeamConfiguration", defStr));
            productDescription = productType + ' ' + imagingMode;

            if (missionInfo == null) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, acquisitionInfo.getAttributeString("orbitDirection", defStr));
            }
        }

        final MetadataElement polarisationList = acquisitionInfo.getElement("polarisationList");
        final MetadataAttribute[] polList = polarisationList.getAttributes();
        for (int i = 0; i < polList.length; ++i) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.polarTags[i], polList[i].getData().getElemString());
        }

        final MetadataElement imageRaster = imageDataInfo.getElement("imageRaster");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                imageRaster.getAttributeDouble("azimuthLooks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                imageRaster.getAttributeDouble("rangeLooks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                imageRaster.getAttributeInt("numberOfColumns", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                imageRaster.getAttributeInt("numberOfRows", defInt));

        if (sceneInfo != null) {
            setStartStopTime(absRoot, sceneInfo, imageRaster.getAttributeInt("numberOfRows", defInt));

            getCornerCoords(sceneInfo, geocodedImageInfo);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height,
                    sceneInfo.getAttributeDouble("sceneAverageHeight", defInt));
        } else if (acquisitionInfo != null) {
            setStartStopTime(absRoot, acquisitionInfo, imageRaster.getAttributeInt("numberOfRows", defInt));
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latCorners[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonCorners[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latCorners[1]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonCorners[1]);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latCorners[2]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonCorners[2]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latCorners[3]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonCorners[3]);

        // See Andrea's email dated Sept. 30, 2010
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
        if (sampleType.contains("COMPLEX") && complexImageInfo != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    complexImageInfo.getAttributeDouble("projectedSpacingAzimuth", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    complexImageInfo.getElement("projectedSpacingRange").getAttributeDouble("slantRange", defInt));
        } else {
            final MetadataElement rowSpacing = imageDataInfo.getElement("imageRaster").getElement("rowSpacing");
            final MetadataElement colSpacing = imageDataInfo.getElement("imageRaster").getElement("columnSpacing");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    rowSpacing.getAttributeDouble("rowSpacing", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    colSpacing.getAttributeDouble("columnSpacing", defInt));
        }

        if (instrument != null) {
            final MetadataElement settings = instrument.getElement("settings");
            final MetadataElement settingRecord = settings.getElement("settingRecord");
            final MetadataElement PRF = settingRecord.getElement("PRF");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    PRF.getAttributeDouble("PRF", defInt));
            final MetadataElement RSF = settings.getElement("RSF");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    RSF.getAttributeDouble("RSF", defInt) / Constants.oneMillion);
            final MetadataElement radarParameters = instrument.getElement("radarParameters");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                    radarParameters.getAttributeDouble("centerFrequency", defInt) / Constants.oneMillion);
        }

        int srgr = 1;
        if (productVariantInfo.getAttributeString("projection", " ").equalsIgnoreCase("SLANTRANGE"))
            srgr = 0;
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, srgr);
        final String mapProjection = productVariantInfo.getAttributeString("mapProjection", " ").trim();
        if (!mapProjection.isEmpty()) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection, mapProjection);
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);

        final MetadataElement processingFlags = processing.getElement("processingFlags");
        if (processingFlags != null) {
            setFlag(processingFlags, "rangeSpreadingLossCorrectedFlag", "true", absRoot, AbstractMetadata.range_spread_comp_flag);
            setFlag(processingFlags, "elevationPatternCorrectedFlag", "true", absRoot, AbstractMetadata.ant_elev_corr_flag);
        }

        // add Range and Azimuth bandwidth
        final MetadataElement processingParameter = processing.getElement("processingParameter");
        if (processingParameter != null) {
            final double rangeBW = processingParameter.getAttributeDouble("totalProcessedRangeBandwidth"); // Hz
            final double azimuthBW = processingParameter.getAttributeDouble("totalProcessedAzimuthBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                (Math.min(slantRangeCorners[0], slantRangeCorners[2]) / Constants.oneBillion) * Constants.halfLightSpeed);
        // Note: Here we use the minimum of the slant range times of two corners because the original way cause
        //       problem for stripmap product when the two slant range times are different.

        final MetadataElement calibration = level1Elem.getElement("calibration");
        if (calibration != null) {
            final MetadataElement calibrationConstant = calibration.getElement("calibrationConstant");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor,
                    calibrationConstant.getAttributeDouble("calFactor", defInt));
        }

        if (platform != null) {
            final MetadataElement orbit = platform.getElement("orbit");
            addOrbitStateVectors(absRoot, orbit);
            addSRGRCoefficients(absRoot, productSpecific, productInfo);
        }

        final MetadataElement doppler = processing.getElement("doppler");
        if (doppler != null) {
            final MetadataElement dopplerCentroid = doppler.getElement("dopplerCentroid");
            addDopplerCentroidCoefficients(absRoot, dopplerCentroid);
        }

        // Add metadata for Spotlight
        if (imagingMode.equalsIgnoreCase("spotlight")) {
            final MetadataElement dopplerSpotlightElem = new MetadataElement("dopplerSpotlight");
            absRoot.addElement(dopplerSpotlightElem);
            addDopplerRateAndCentroidSpotlight(dopplerSpotlightElem, origProdRoot);
            addAzimuthTimeZpSpotlight(dopplerSpotlightElem);
        }

        // handle ATI products by copying abs metadata to slv metadata
        final String antennaReceiveConfiguration = acquisitionInfo.getAttributeString("antennaReceiveConfiguration", "");
        if (antennaReceiveConfiguration.equals("DRA")) {
            final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(root);

            // copy Abstracted Metadata
            for (File cosFile : cosarFileList) {
                final String fileName = cosFile.getName().toUpperCase();
                if (fileName.contains("_SRA_"))
                    continue;
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 1);
                final MetadataElement targetSlaveMetadata = new MetadataElement(fileName);
                targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
                ProductUtils.copyMetadata(absRoot, targetSlaveMetadata);
            }

            // modify abstracted metadata
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.bistatic_correction_applied, 1);
    }

    private void findImagesForTanDemX(final MetadataElement newRoot) throws IOException {

        String parentPath = masterProductName + '/' + getRelativePathToImageFolder();
        findImages(parentPath, newRoot);

        parentPath = slaveProductName + '/' + getRelativePathToImageFolder();
        findImages(parentPath, newRoot);
    }

    @Override
    protected void findImages(final MetadataElement newRoot) throws IOException {

        if (getHeaderFileName().startsWith("TSX") || getHeaderFileName().startsWith("TDX")) {
            super.findImages(newRoot);
        } else if (getHeaderFileName().startsWith("TDM")) {
            findImagesForTanDemX(newRoot);
        } else {
            throw new IOException("Invalid header file: " + getHeaderFileName());
        }
    }

    private void setStartStopTime(final MetadataElement absRoot, final MetadataElement elem, final int height) {
        final ProductData.UTC startTime = ReaderUtils.getTime(elem.getElement("start"), "timeUTC", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(elem.getElement("stop"), "timeUTC", standardDateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, stopTime, height));
    }

    private static String getAcquisitionMode(final String mode) {
        if (mode.equalsIgnoreCase("SM"))
            return "Stripmap";
        else if (mode.equalsIgnoreCase("SL") || mode.equalsIgnoreCase("HS") || mode.equalsIgnoreCase("ST"))
            return "Spotlight";
        else if (mode.equalsIgnoreCase("SC"))
            return "ScanSAR";
        return mode;
    }

    private static void setFlag(MetadataElement elem, String attribTag, String trueValue,
                                MetadataElement absRoot, String absTag) {
        int val = 0;
        if (elem.getAttributeString(attribTag, " ").equalsIgnoreCase(trueValue))
            val = 1;
        AbstractMetadata.setAttribute(absRoot, absTag, val);
    }

    private void getCornerCoords(MetadataElement sceneInfo, MetadataElement geocodedImageInfo) {

        int maxRow = 0, maxCol = 0;
        int minRow = Integer.MAX_VALUE, minCol = Integer.MAX_VALUE;
        final List<CornerCoord> coordList = new ArrayList<>();

        final MetadataElement[] children = sceneInfo.getElements();
        for (MetadataElement child : children) {
            if (child.getName().equals("sceneCornerCoord")) {
                final int refRow = child.getAttributeInt("refRow", 0);
                final int refCol = child.getAttributeInt("refColumn", 0);

                coordList.add(new CornerCoord(refRow, refCol,
                        child.getAttributeDouble("lat", 0),
                        child.getAttributeDouble("lon", 0),
                        child.getAttributeDouble("rangeTime", 0) * Constants.oneBillion,
                        child.getAttributeDouble("incidenceAngle", 0)));

                if (refRow > maxRow) maxRow = refRow;
                if (refCol > maxCol) maxCol = refCol;
                if (refRow < minRow) minRow = refRow;
                if (refCol < minCol) minCol = refCol;
            }
        }

        final int[] indexArray = {0, 1, 2, 3};
        if (minRow == maxRow && minCol == maxCol && geocodedImageInfo != null) {
            final MetadataElement geoParameter = geocodedImageInfo.getElement("geoParameter");
            final MetadataElement sceneCoordsGeographic = geoParameter.getElement("sceneCoordsGeographic");
            final double latUL = sceneCoordsGeographic.getAttributeDouble("upperLeftLatitude", 0);
            final double latUR = sceneCoordsGeographic.getAttributeDouble("upperRightLatitude", 0);
            final double latLL = sceneCoordsGeographic.getAttributeDouble("lowerLeftLatitude", 0);
            final double latLR = sceneCoordsGeographic.getAttributeDouble("lowerRightLatitude", 0);

            final double lonUL = sceneCoordsGeographic.getAttributeDouble("upperLeftLongitude", 0);
            final double lonUR = sceneCoordsGeographic.getAttributeDouble("upperRightLongitude", 0);
            final double lonLL = sceneCoordsGeographic.getAttributeDouble("lowerLeftLongitude", 0);
            final double lonLR = sceneCoordsGeographic.getAttributeDouble("lowerRightLongitude", 0);

            int k = 0;
            double d0, d1, d2, d3;
            for (CornerCoord coord : coordList) {
                d0 = Math.abs(coord.lat - latUL) + Math.abs(coord.lon - lonUL);
                d1 = Math.abs(coord.lat - latUR) + Math.abs(coord.lon - lonUR);
                d2 = Math.abs(coord.lat - latLL) + Math.abs(coord.lon - lonLL);
                d3 = Math.abs(coord.lat - latLR) + Math.abs(coord.lon - lonLR);

                if (d0 <= d1 && d0 <= d2 && d0 <= d3) {
                    indexArray[k] = 0;
                } else if (d1 <= d0 && d1 <= d2 && d1 <= d3) {
                    indexArray[k] = 1;
                } else if (d2 <= d0 && d2 <= d1 && d2 <= d3) {
                    indexArray[k] = 2;
                } else if (d3 <= d0 && d3 <= d1 && d3 <= d2) {
                    indexArray[k] = 3;
                }
                k++;
            }
        }

        int index = 0;
        for (CornerCoord coord : coordList) {
            if (minRow == maxRow && minCol == maxCol) {
                latCorners[indexArray[index]] = coord.lat;
                lonCorners[indexArray[index]] = coord.lon;
                slantRangeCorners[indexArray[index]] = coord.rangeTime;
                incidenceCorners[indexArray[index]] = coord.incidenceAngle;
                ++index;
            } else {
                index = -1;
                if (coord.refRow == minRow) {
                    if (Math.abs(coord.refCol - minCol) < Math.abs(coord.refCol - maxCol)) {            // UL
                        index = 0;
                    } else {     // UR
                        index = 1;
                    }
                } else if (coord.refRow == maxRow) {
                    if (Math.abs(coord.refCol - minCol) < Math.abs(coord.refCol - maxCol)) {            // LL
                        index = 2;
                    } else {     // LR
                        index = 3;
                    }
                }
                if (index >= 0) {
                    latCorners[index] = coord.lat;
                    lonCorners[index] = coord.lon;
                    slantRangeCorners[index] = coord.rangeTime;
                    incidenceCorners[index] = coord.incidenceAngle;
                }
            }
        }
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) throws IOException {
        if (imgPath.toUpperCase().endsWith("COS")) {
            final File file = new File(getBaseDir(), imgPath);

            cosarFileList.add(file);
            setSLC(true);

        } else {
            final String name = getBandFileNameFromImage(imgPath);
            if ((name.endsWith("tif") || name.endsWith("tiff")) && name.startsWith("image")) {
                final Dimension bandDimensions = getBandDimensions(newRoot, name);
                final InputStream inStream = getInputStream(imgPath);
                if(inStream.available() > 0) {
                    final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);
                    if (imgStream == null)
                        throw new IOException("Unable to open " + imgPath);

                    final ImageIOFile img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 1, ProductData.TYPE_UINT16, productInputFile);
                    bandImageFileMap.put(img.getName(), img);
                }
            }
        }
    }

    private void addShiftFiles(final Product product) {
        addShiftFile(product, "slave_shift_az.flt");
        addShiftFile(product, "slave_shift_rg.flt");
        addShiftFile(product, "slave_shift_az_geo.flt");
        addShiftFile(product, "slave_shift_rg_geo.flt");
    }

    private void addShiftFile(final Product product, final String fileName) {
        try {
            final File level1ProductDir = getBaseDir();
            final File shiftFile = new File(level1ProductDir,
                    "COMMON_AUXRASTER" + File.separator + fileName);
            if (shiftFile.exists()) {
                final SunRasterReader sunRasterReader = new SunRasterReader();
                final Product shiftProduct = sunRasterReader.readProductNodes(shiftFile, null);
                final Band band = shiftProduct.getBandAt(0);
                band.readRasterDataFully(ProgressMonitor.NULL);

                final int gridWidth = band.getRasterWidth();
                final int gridHeight = band.getRasterHeight();
                final float subSamplingX = product.getSceneRasterWidth() / (float) (gridWidth - 1);
                final float subSamplingY = product.getSceneRasterHeight() / (float) (gridHeight - 1);

                final TiePointGrid azShiftGrid = new TiePointGrid(fileName,
                        gridWidth, gridHeight, 0.5f, 0.5f,
                        subSamplingX, subSamplingY, (float[]) band.getData().getElems());
                product.addTiePointGrid(azShiftGrid);
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to add " + fileName + " shift file " + e.getMessage());
        }
    }

    @Override
    protected void addGeoCoding(final Product product) {
        File level1ProductDir = getBaseDir();
        if (getHeaderFileName().startsWith("TDM")) {
            // Using the master product is important here.
            // The slave product is coregistered to the master without its geocoding being updated afterwards.
            // Its geocoding is unusable because of this.
            level1ProductDir = new File(getBaseDir(), masterProductName);
        }
        File georefFile = new File(level1ProductDir, "ANNOTATION" + File.separator + "GEOREF.xml");
        if (georefFile.exists()) {
            try {
                readGeoRef(product, georefFile);
                return;
            } catch (Exception e) {
                //
            }
        }

        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);

        if (isMapProjected() || sampleType.contains("COMPLEX")) {

            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

        } else {

            final boolean isAscending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
            final double[] flippedLatCorners = new double[4];
            final double[] flippedLonCorners = new double[4];
            if (isAscending) { // flip up and down
                flippedLatCorners[0] = latCorners[2];
                flippedLatCorners[1] = latCorners[3];
                flippedLatCorners[2] = latCorners[0];
                flippedLatCorners[3] = latCorners[1];

                flippedLonCorners[0] = lonCorners[2];
                flippedLonCorners[1] = lonCorners[3];
                flippedLonCorners[2] = lonCorners[0];
                flippedLonCorners[3] = lonCorners[1];

            } else { // flip left and right

                flippedLatCorners[0] = latCorners[1];
                flippedLatCorners[1] = latCorners[0];
                flippedLatCorners[2] = latCorners[3];
                flippedLatCorners[3] = latCorners[2];

                flippedLonCorners[0] = lonCorners[1];
                flippedLonCorners[1] = lonCorners[0];
                flippedLonCorners[2] = lonCorners[3];
                flippedLonCorners[3] = lonCorners[2];
            }

            ReaderUtils.addGeoCoding(product, flippedLatCorners, flippedLonCorners);
        }
    }

    private static void readGeoRef(final Product product, final File georefFile) throws IOException {
        final Document xmlDoc = XMLSupport.LoadXML(georefFile.getAbsolutePath());
        final Element root = xmlDoc.getRootElement();
        final Element geoGrid = root.getChild("geolocationGrid");

        final Element numGridPnt = geoGrid.getChild("numberOfGridPoints");
        final Element numAzimuth = numGridPnt.getChild("azimuth");
        final int numAz = Integer.parseInt(numAzimuth.getValue());
        final Element numRange = numGridPnt.getChild("range");
        final int numRg = Integer.parseInt(numRange.getValue());

        final Element gridReferenceTime = geoGrid.getChild("gridReferenceTime");
        final Element tReferenceTimeUTC = gridReferenceTime.getChild("tReferenceTimeUTC");

        final int size = numAz * numRg;
        final double[] latList = new double[size];
        final double[] lonList = new double[size];
        final double[] incList = new double[size];
        final double[] timeList = new double[size];
        final int[] row = new int[size];
        final int[] col = new int[size];

        //final boolean flip = !isSLC();

        int i = 0;
        int r = numRg - 1;
        int c = 0;
        boolean regridNeeded = false;
        final List<Element> grdPntList = geoGrid.getChildren("gridPoint");
        for (Element pnt : grdPntList) {
            int index = i;
            /*
            if(flip) {
                index = (numRg * c) + r;
                --r;
                if(r < 0) {
                    r = numRg-1;
                    ++c;
                }
            }
            */
            final Element tElem = pnt.getChild("tau");
            timeList[index] = Double.parseDouble(tElem.getValue());

            final Element latElem = pnt.getChild("lat");
            latList[index] = Double.parseDouble(latElem.getValue());
            final Element lonElem = pnt.getChild("lon");
            lonList[index] = Double.parseDouble(lonElem.getValue());

            final Element rowElem = pnt.getChild("row");
            if (rowElem != null) {
                row[index] = Integer.parseInt(rowElem.getValue()) - 1;
                regridNeeded = true;
            }
            final Element colElem = pnt.getChild("col");
            if (colElem != null) {
                col[index] = Integer.parseInt(colElem.getValue()) - 1;
            }

            final Element incElem = pnt.getChild("inc");
            incList[index] = Double.parseDouble(incElem.getValue());

            ++i;
        }

        final int gridWidth = numRg;
        final int gridHeight = numAz;
        final int newGridWidth = gridWidth;
        final int newGridHeight = gridHeight;
        float[] newLatList = new float[newGridWidth * newGridHeight];
        float[] newLonList = new float[newGridWidth * newGridHeight];
        float[] newIncList = new float[newGridWidth * newGridHeight];
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        final double subSamplingX = sceneRasterWidth / (double) (newGridWidth - 1);
        final double subSamplingY = sceneRasterHeight / (double) (newGridHeight - 1);

        if (regridNeeded) {
            getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, col, row, latList,
                    newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLatList);

            getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, col, row, lonList,
                    newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLonList);

            getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, col, row, incList,
                    newGridWidth, newGridHeight, subSamplingX, subSamplingY, newIncList);
        } else {
            for (int m = 0; m < newLatList.length; ++m) {
                newLatList[m] = (float) latList[m];
                newLonList[m] = (float) lonList[m];
                newIncList[m] = (float) incList[m];
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLatList);
        latGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(latGrid);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLonList, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(lonGrid);

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newIncList);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);

        // final TiePointGrid timeGrid = new TiePointGrid("Time", gridWidth, gridHeight, 0, 0,
        //          subSamplingX, subSamplingY, timeList);
        //  timeGrid.setUnit(Unit.NANOSECONDS);
        //  product.addTiePointGrid(timeGrid);
    }

    private static void getListInEvenlySpacedGrid(
            final int sceneRasterWidth, final int sceneRasterHeight, final int sourceGridWidth,
            final int sourceGridHeight, final int[] x, final int[] y, final double[] sourcePointList,
            final int targetGridWidth, final int targetGridHeight, final double subSamplingX, final double subSamplingY,
            final float[] targetPointList) {

        if (sourcePointList.length != sourceGridWidth * sourceGridHeight) {
            throw new IllegalArgumentException(
                    "Original tie point array size does not match 'sourceGridWidth' x 'sourceGridHeight'");
        }

        if (targetPointList.length != targetGridWidth * targetGridHeight) {
            throw new IllegalArgumentException(
                    "Target tie point array size does not match 'targetGridWidth' x 'targetGridHeight'");
        }

        int k = 0;
        for (int r = 0; r < targetGridHeight; r++) {

            double newY = r * subSamplingY;
            if (newY > sceneRasterHeight - 1) {
                newY = sceneRasterHeight - 1;
            }
            double oldY0 = 0, oldY1 = 0;
            int j0 = 0, j1 = 0;
            for (int rr = 1; rr < sourceGridHeight; rr++) {
                j0 = rr - 1;
                j1 = rr;
                oldY0 = y[j0 * sourceGridWidth];
                oldY1 = y[j1 * sourceGridWidth];
                if (oldY1 > newY) {
                    break;
                }
            }

            final double wj = (newY - oldY0) / (oldY1 - oldY0);

            for (int c = 0; c < targetGridWidth; c++) {

                double newX = c * subSamplingX;
                if (newX > sceneRasterWidth - 1) {
                    newX = sceneRasterWidth - 1;
                }
                double oldX0 = 0, oldX1 = 0;
                int i0 = 0, i1 = 0;
                for (int cc = 1; cc < sourceGridWidth; cc++) {
                    i0 = cc - 1;
                    i1 = cc;
                    oldX0 = x[i0];
                    oldX1 = x[i1];
                    if (oldX1 > newX) {
                        break;
                    }
                }
                final double wi = (newX - oldX0) / (oldX1 - oldX0);

                targetPointList[k++] = (float) MathUtils.interpolate2D(wi, wj,
                        sourcePointList[i0 + j0 * sourceGridWidth],
                        sourcePointList[i1 + j0 * sourceGridWidth],
                        sourcePointList[i0 + j1 * sourceGridWidth],
                        sourcePointList[i1 + j1 * sourceGridWidth]);
            }
        }
    }

    @Override
    protected void addTiePointGrids(final Product product) {

        final int gridWidth = 4;
        final int gridHeight = 4;
        final double subSamplingX = (double) product.getSceneRasterWidth() / (gridWidth - 1);
        final double subSamplingY = (double) product.getSceneRasterHeight() / (gridHeight - 1);
        if (subSamplingX == 0 || subSamplingY == 0)
            return;

        final float[] flippedSlantRangeCorners = new float[4];
        final float[] flippedIncidenceCorners = new float[4];
        getFlippedCorners(product, flippedSlantRangeCorners, flippedIncidenceCorners);

        if (product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final float[] fineAngles = new float[gridWidth * gridHeight];
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, flippedIncidenceCorners, fineAngles);

            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                    subSamplingX, subSamplingY, fineAngles);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

        final float[] fineSlantRange = new float[gridWidth * gridHeight];
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, flippedSlantRangeCorners, fineSlantRange);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineSlantRange);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);

        addShiftFiles(product);
    }

    private void getFlippedCorners(Product product,
                                   final float[] flippedSlantRangeCorners, final float[] flippedIncidenceCorners) {

        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);

        if (isMapProjected() || sampleType.contains("COMPLEX")) {

            flippedSlantRangeCorners[0] = (float) slantRangeCorners[0];
            flippedSlantRangeCorners[1] = (float) slantRangeCorners[1];
            flippedSlantRangeCorners[2] = (float) slantRangeCorners[2];
            flippedSlantRangeCorners[3] = (float) slantRangeCorners[3];

            flippedIncidenceCorners[0] = (float) incidenceCorners[0];
            flippedIncidenceCorners[1] = (float) incidenceCorners[1];
            flippedIncidenceCorners[2] = (float) incidenceCorners[2];
            flippedIncidenceCorners[3] = (float) incidenceCorners[3];

        } else {

            final boolean isAscending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
            if (isAscending) { // flip up and down
                flippedSlantRangeCorners[0] = (float) slantRangeCorners[2];
                flippedSlantRangeCorners[1] = (float) slantRangeCorners[3];
                flippedSlantRangeCorners[2] = (float) slantRangeCorners[0];
                flippedSlantRangeCorners[3] = (float) slantRangeCorners[1];

                flippedIncidenceCorners[0] = (float) incidenceCorners[2];
                flippedIncidenceCorners[1] = (float) incidenceCorners[3];
                flippedIncidenceCorners[2] = (float) incidenceCorners[0];
                flippedIncidenceCorners[3] = (float) incidenceCorners[1];

            } else { // flip left and right

                flippedSlantRangeCorners[0] = (float) slantRangeCorners[1];
                flippedSlantRangeCorners[1] = (float) slantRangeCorners[0];
                flippedSlantRangeCorners[2] = (float) slantRangeCorners[3];
                flippedSlantRangeCorners[3] = (float) slantRangeCorners[2];

                flippedIncidenceCorners[0] = (float) incidenceCorners[1];
                flippedIncidenceCorners[1] = (float) incidenceCorners[0];
                flippedIncidenceCorners[2] = (float) incidenceCorners[3];
                flippedIncidenceCorners[3] = (float) incidenceCorners[2];
            }
        }
    }

    private static String appendIfMatch(final Band band, final String key, String bands) {
        if (band.getName().contains(key)) {
            bands = bands + band.getName() + ' ';
        }
        return bands;
    }

    @Override
    protected void addBands(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final int width = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int height = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);

        final Set<String> ImageKeys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : ImageKeys) {
            final ImageIOFile img = bandImageFileMap.get(key);

            for (int i = 0; i < img.getNumImages(); ++i) {

                for (int b = 0; b < img.getNumBands(); ++b) {
                    final String pol = SARReader.findPolarizationInBandName(img.getName());
                    final Band band = new Band("Amplitude_" + pol, img.getDataType(), width, height);
                    band.setUnit(Unit.AMPLITUDE);
                    band.setNoDataValue(0);
                    band.setNoDataValueUsed(true);
                    product.addBand(band);

                    SARReader.createVirtualIntensityBand(product, band, '_' + pol);

                    bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));
                }
            }
        }

        if (!cosarFileList.isEmpty()) {

            String masterBands = "";
            String slaveBands = "";

            final boolean polsUnique = arePolarizationsUnique();
            String extraInfo = "";         // if pols not unique add the extra info
            final String mission = getMission();

            for (final File file : cosarFileList) {

                final String fileName = file.getName().toUpperCase();
                final String pol = SARReader.findPolarizationInBandName(fileName);

                if (mission.contains("TDM")) {
                    final String level1ProductDirName = file.getParentFile().getParentFile().getName();
                    if (level1ProductDirName.equals(masterProductName)) {
                        extraInfo = StackUtils.MST;
                    } else if (level1ProductDirName.equals(slaveProductName)) {
                        extraInfo = StackUtils.SLV + '1';
                    }
                    extraInfo += StackUtils.createBandTimeStamp(product);
                } else if (!polsUnique) {
                    final int polIndex = fileName.indexOf(pol);
                    extraInfo = fileName.substring(polIndex + 2, fileName.indexOf('.', polIndex + 3));
                }

                final int bandDataType = (mission.contains("TDM")) ?
                        ProductData.TYPE_FLOAT32 : ProductData.TYPE_INT16;

                final Band realBand = new Band("i_" + pol + extraInfo, bandDataType, width, height);
                realBand.setUnit(Unit.REAL);
                realBand.setNoDataValue(0);
                realBand.setNoDataValueUsed(true);
                product.addBand(realBand);

                masterBands = appendIfMatch(realBand, "mst", masterBands);
                slaveBands = appendIfMatch(realBand, "slv", slaveBands);

                final Band imaginaryBand = new Band("q_" + pol + extraInfo, bandDataType, width, height);
                imaginaryBand.setUnit(Unit.IMAGINARY);
                imaginaryBand.setNoDataValue(0);
                imaginaryBand.setNoDataValueUsed(true);
                product.addBand(imaginaryBand);

                masterBands = appendIfMatch(imaginaryBand, "mst", masterBands);
                slaveBands = appendIfMatch(imaginaryBand, "slv", slaveBands);

                ReaderUtils.createVirtualIntensityBand(product, realBand, imaginaryBand, "");

                try {
                    cosarBandMap.put(realBand.getName(), FileImageInputStreamExtImpl.createInputStream(file));
                    cosarBandMap.put(imaginaryBand.getName(), FileImageInputStreamExtImpl.createInputStream(file));
                } catch (Exception e) {
                    //
                }
            }

            if (mission.contains("TDM")) {
                final MetadataElement slaveMetadata = absRoot.getParentElement().getElement(AbstractMetadata.SLAVE_METADATA_ROOT);

                slaveMetadata.setAttributeString("Master_bands", masterBands);

                final MetadataElement slaveProduct = slaveMetadata.getElement(slaveProductName);
                final MetadataAttribute slaveBandsAttr = new MetadataAttribute("Slave_bands", ProductData.TYPE_ASCII);
                slaveProduct.addAttribute(slaveBandsAttr);
                slaveProduct.setAttributeString(slaveBandsAttr.getName(), slaveBands);
            }
        }

        absRoot.setAttributeInt(AbstractMetadata.num_samples_per_line, width);
        absRoot.setAttributeInt(AbstractMetadata.num_output_lines, height);
    }

    private boolean arePolarizationsUnique() {
        final List<String> pols = new ArrayList<>();
        for (final File file : cosarFileList) {
            pols.add(SARReader.findPolarizationInBandName(file.getName()));
        }
        for (int i = 0; i < pols.size(); ++i) {
            for (int j = i + 1; j < pols.size(); ++j) {
                if (pols.get(i).equals(pols.get(j)))
                    return false;
            }
        }
        return true;
    }

    private void addOrbitStateVectors(MetadataElement absRoot, MetadataElement orbitInformation) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitInformation.getElements();
        for (int i = 1; i < stateVectorElems.length; ++i) {
            // first stateVectorElem is orbitHeader therefore skip it
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i], i);
        }

        // set state vector time
        if (absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.NO_METADATA_UTC).
                equalElems(AbstractMetadata.NO_METADATA_UTC)) {

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                    ReaderUtils.getTime(stateVectorElems[1], "timeUTC", standardDateFormat));
        }
    }

    private void addVector(String name, MetadataElement orbitVectorListElem,
                           MetadataElement srcElem, int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                ReaderUtils.getTime(srcElem, "timeUTC", standardDateFormat));

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                srcElem.getAttributeDouble("posX", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                srcElem.getAttributeDouble("posY", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                srcElem.getAttributeDouble("posZ", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                srcElem.getAttributeDouble("velX", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                srcElem.getAttributeDouble("velY", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                srcElem.getAttributeDouble("velZ", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    private static void addSRGRCoefficients(
            final MetadataElement absRoot, final MetadataElement productSpecific, final MetadataElement productInfo) {

        // get swath begin time and swath end time
        final MetadataElement sceneInfo = productInfo.getElement("sceneInfo");
        if (sceneInfo == null) {
            return;
        }

        final MetadataElement rangeTime = sceneInfo.getElement("rangeTime");
        if (rangeTime == null) {
            return;
        }

        final double firstPixelTime = rangeTime.getAttributeDouble("firstPixel");
        final double lastPixelTime = rangeTime.getAttributeDouble("lastPixel");

        // get slant range time to ground rang conversion coefficients
        final MetadataElement projectedImageInfo = productSpecific.getElement("projectedImageInfo");
        if (projectedImageInfo == null) {
            return;
        }

        final MetadataElement slantToGroundRangeProjection = projectedImageInfo.getElement("slantToGroundRangeProjection");
        if (slantToGroundRangeProjection == null) {
            return;
        }

        // final double validityRangeMin = slantToGroundRangeProjection.getAttributeDouble("validityRangeMin");
        // final double validityRangeMax = slantToGroundRangeProjection.getAttributeDouble("validityRangeMax");
        final double referencePoint = slantToGroundRangeProjection.getAttributeDouble("referencePoint");
        final int polynomialDegree = slantToGroundRangeProjection.getAttributeInt("polynomialDegree");

        final double[] s2gCoef = new double[polynomialDegree + 1];
        int cnt = 0;
        for (MetadataElement elem : slantToGroundRangeProjection.getElements()) {
            s2gCoef[cnt++] = elem.getAttributeDouble("coefficient", 0);
        }

        // compute ground range to slant range conversion coefficients
        final int m = 11; // order of ground to slant polynomial
        double[] sltRgTime = new double[m + 1];
        double[] groundRange = new double[m + 1];
        for (int i = 0; i <= m; i++) {
            sltRgTime[i] = firstPixelTime + (lastPixelTime - firstPixelTime) * i / m;
            groundRange[i] = Maths.computePolynomialValue(sltRgTime[i] - referencePoint, s2gCoef);
        }

        // final double groundRangeRef = (groundRange[0] + groundRange[m]) / 2;
        final double groundRangeRef = 0.0; // set ground range ref to 0 because when g2sCoef are used in computing
        // slant range from ground range, the ground range origin is assumed to be 0
        final double[] deltaGroundRange = new double[m + 1];
        final double deltaMax = groundRange[m] - groundRangeRef;
        for (int i = 0; i <= m; i++) {
            deltaGroundRange[i] = (groundRange[i] - groundRangeRef) / deltaMax;
        }

        final Matrix G = Maths.createVandermondeMatrix(deltaGroundRange, m);
        final Matrix tau = new Matrix(sltRgTime, m + 1);
        final Matrix s = G.solve(tau);
        final double[] g2sCoef = s.getColumnPackedCopy();

        double tmp = 1;
        for (int i = 0; i <= m; i++) {
            g2sCoef[i] *= Constants.halfLightSpeed / tmp;
            tmp *= deltaMax;
        }

        // save ground range to slant range conversion coefficients in abstract metadata
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);
        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

        for (int i = 0; i <= m; i++) {
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            srgrListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, g2sCoef[i]);
        }
    }

    private void addDopplerCentroidCoefficients(
            final MetadataElement absRoot, final MetadataElement dopplerCentroid) {

        final MetadataElement[] dopplerElems = dopplerCentroid.getElements();

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for (MetadataElement dopplerEstimate : dopplerElems) {
            if (dopplerEstimate.getName().equalsIgnoreCase("dopplerEstimate")) {
                final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + '.' + listCnt);
                dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(dopplerEstimate, "timeUTC", standardDateFormat);
                dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

                final MetadataElement combinedDoppler = dopplerEstimate.getElement("combinedDoppler");
                final MetadataElement[] coefficients = combinedDoppler.getElements();

                final double refTime = combinedDoppler.getAttributeDouble("referencePoint", 0) * 1e9; // s to ns
                AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                                                        ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
                AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);

                int cnt = 1;
                for (MetadataElement coefficient : coefficients) {
                    final double coefValue = coefficient.getAttributeDouble("coefficient", 0);
                    final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
                    dopplerListElem.addElement(coefElem);
                    ++cnt;

                    AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                            ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
                    AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
                }
            }
        }
    }

    ImageInputStream getCosarImageInputStream(final Band band) {
        return cosarBandMap.get(band.getName());
    }

    @Override
    public void close() throws IOException {
        super.close();
        final Set<String> keys = cosarBandMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageInputStream img = cosarBandMap.get(key);
            img.close();
        }
    }

    protected String getMission() {
        if (getHeaderFileName().startsWith("TSX")) {
            return "TSX";
        } else if(getHeaderFileName().startsWith("TDX")) {
            return "TDX";
        } else if (getHeaderFileName().startsWith("TDM")) {
            return "TDM";
        }
        return "TSX";
    }

    @Override
    protected String getProductName() {
        return productName;
    }

    @Override
    protected String getProductDescription() {
        return productDescription;
    }

    @Override
    protected String getProductType() {
        return productType;
    }

    private static class CornerCoord {
        final int refRow, refCol;
        final double lat, lon;
        final double rangeTime, incidenceAngle;

        CornerCoord(int row, int col, double lt, double ln, double range, double angle) {
            refRow = row;
            refCol = col;
            lat = lt;
            lon = ln;
            rangeTime = range;
            incidenceAngle = angle;
        }
    }

    private void addDopplerRateAndCentroidSpotlight(MetadataElement elem, MetadataElement origProdRoot) {
        // Compute doppler rate and centroid
        final double[] metadata = getMetadataSpotlight(origProdRoot);

        final double[] dcRangePolyCoeffsFirst = new double[]{metadata[4], metadata[5]};
        final double[] dcRangePolyCoeffsLast = new double[]{metadata[6], metadata[7]};
        final double dcRangePolyAzTimeRawFirst = metadata[1];
        final double dcRangePolyAzTimeRawLast = metadata[8];
        final double fmRangePolyCoeffsFirst = metadata[9];
        final double fmRangePolyCoeffsLast = metadata[10];
        final double firstRangeTime = metadata[2];
        final double lastRangeTime = metadata[13];
        final double refRangeTime = metadata[3];
        final double refAzimuthTimeZp = metadata[0];

        int w = (int) metadata[12];

        final double[] rangeTime = new double[w];
        final double[] dopplerCentroidFirst = new double[w];
        final double[] dopplerCentroidLast = new double[w];
        final double[] dcRangePolyAzTimeZpFirst = new double[w];
        final double[] dcRangePolyAzTimeZpLast = new double[w];
        final double[] dopplerCentroidSpotlight = new double[w];

        final double fmRate = 0.5 * (fmRangePolyCoeffsFirst + fmRangePolyCoeffsLast);

        for (int i = 0; i < w; i++) {
            rangeTime[i] = firstRangeTime + i * (lastRangeTime - firstRangeTime) / (double) (w - 1) - refRangeTime;
        }

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < 2; j++) {
                dopplerCentroidFirst[i] += dcRangePolyCoeffsFirst[j] * Math.pow(rangeTime[i], j);
            }
        }

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < 2; j++) {
                dopplerCentroidLast[i] += dcRangePolyCoeffsLast[j] * Math.pow(rangeTime[i], j);
            }
        }

        for (int i = 0; i < w; i++) {
            dcRangePolyAzTimeZpFirst[i] = dcRangePolyAzTimeRawFirst - dopplerCentroidFirst[i] / fmRate;
        }

        for (int i = 0; i < w; i++) {
            dcRangePolyAzTimeZpLast[i] = dcRangePolyAzTimeRawLast - dopplerCentroidLast[i] / fmRate;
        }

        final double[] dopplerRateSpotlight = new double[w];
        for (int i = 0; i < w; i++) {
            dopplerRateSpotlight[i] = (dopplerCentroidLast[i] - dopplerCentroidFirst[i]) / (dcRangePolyAzTimeZpLast[i] - dcRangePolyAzTimeZpFirst[i]);
        }

        for (int i = 0; i < w; i++) {
            dopplerCentroidSpotlight[i] = dopplerCentroidFirst[i] - dopplerRateSpotlight[i] * (dcRangePolyAzTimeZpFirst[i] - refAzimuthTimeZp);
        }

        // Save in metadata
        String dopplerRateSpotlightStr = Arrays.toString(dopplerRateSpotlight).replace("]", "").replace("[", "");
        String dopplerCentroidSpotlightStr = Arrays.toString(dopplerCentroidSpotlight).replace("]", "").replace("[", "");

        AbstractMetadata.addAbstractedAttribute(elem, "dopplerRateSpotlight",
                                                ProductData.TYPE_ASCII, "", "Doppler Rate Spotlight");
        AbstractMetadata.setAttribute(elem, "dopplerRateSpotlight", dopplerRateSpotlightStr );

        AbstractMetadata.addAbstractedAttribute(elem, "dopplerCentroidSpotlight",
                                                ProductData.TYPE_ASCII, "", "Doppler Centroid Spotlight");
        AbstractMetadata.setAttribute(elem, "dopplerCentroidSpotlight", dopplerCentroidSpotlightStr);
    }

    private void addAzimuthTimeZpSpotlight(MetadataElement elem) {
        // Compute azimuth time
        final double AzimuthTimeZpOffset = 0;

        // Save in metadata
        final MetadataElement azimuthTimeZd = new MetadataElement("azimuthTimeZdSpotlight");
        elem.addElement(azimuthTimeZd);
        AbstractMetadata.addAbstractedAttribute(azimuthTimeZd, "AzimuthTimeZdOffset",
                                                ProductData.TYPE_FLOAT64, "", "Azimuth Time Zero Doppler Offset");
        AbstractMetadata.setAttribute(azimuthTimeZd, "AzimuthTimeZdOffset", AzimuthTimeZpOffset);
    }

    private double[] getMetadataSpotlight(MetadataElement origProdRoot) {

        List<Integer> index;
        final double[] metadata = new double[15];

        MetadataElement rootLevel1ProductElem = origProdRoot.getElement("level1Product");

        MetadataElement productInfoElem = rootLevel1ProductElem.getElement("productInfo");
        MetadataElement imageDataInfoElem = productInfoElem.getElement("imageDataInfo");
        MetadataElement imageRasterElem = imageDataInfoElem.getElement("imageRaster");
        MetadataElement sceneInfoElem = productInfoElem.getElement("sceneInfo");
        MetadataElement startElem = sceneInfoElem.getElement("start");
        MetadataElement stopElem = sceneInfoElem.getElement("stop");
        MetadataElement processingElem = rootLevel1ProductElem.getElement("processing");
        MetadataElement dopplerElem = processingElem.getElement("doppler");
        MetadataElement dopplerCentroidElem = dopplerElem.getElement("dopplerCentroid");
        index = lookForIndex(dopplerCentroidElem, "dopplerEstimate");
        MetadataElement dopplerEstimateFirstElem = dopplerCentroidElem.getElements()[index.get(0)];
        MetadataElement combinedDopplerFirstElem = dopplerEstimateFirstElem.getElement("combinedDoppler");
        MetadataElement coefficientFirst0Elem = combinedDopplerFirstElem.getElements()[0];
        MetadataElement coefficientFirst1Elem = combinedDopplerFirstElem.getElements()[1];
        MetadataElement dopplerEstimateLastElem = dopplerCentroidElem.getElements()[index.get(1)];
        MetadataElement combinedDopplerLastElem = dopplerEstimateLastElem.getElement("combinedDoppler");
        MetadataElement coefficientLast0Elem = combinedDopplerLastElem.getElements()[0];
        MetadataElement coefficientLast1Elem = combinedDopplerLastElem.getElements()[1];
        MetadataElement geometryElem = processingElem.getElement("geometry");
        index = lookForIndex(geometryElem, "dopplerRate");
        MetadataElement dopplerRateFirstElem = geometryElem.getElements()[index.get(0)];
        MetadataElement dopplerRatePolynomialFirst = dopplerRateFirstElem.getElement("dopplerRatePolynomial");
        MetadataElement coefficientFirstElem = dopplerRatePolynomialFirst.getElements()[0];
        MetadataElement dopplerRateLastElem = geometryElem.getElements()[index.get(1)];
        MetadataElement dopplerRatePolynomialLast = dopplerRateLastElem.getElement("dopplerRatePolynomial");
        MetadataElement coefficientLastElem = dopplerRatePolynomialLast.getElements()[0];

        metadata[0] = timeUTCtoSecs(startElem);
        metadata[1] = timeUTCtoSecs(dopplerEstimateFirstElem);
        metadata[2] = combinedDopplerFirstElem.getAttributeDouble("validityRangeMin");
        metadata[3] = combinedDopplerFirstElem.getAttributeDouble("referencePoint");
        metadata[4] = coefficientFirst0Elem.getAttributeDouble("coefficient");
        metadata[5] = coefficientFirst1Elem.getAttributeDouble("coefficient");
        metadata[6] = coefficientLast0Elem.getAttributeDouble("coefficient");
        metadata[7] = coefficientLast1Elem.getAttributeDouble("coefficient");
        metadata[8] = timeUTCtoSecs(dopplerEstimateLastElem);
        metadata[9] = coefficientFirstElem.getAttributeDouble("coefficient");
        metadata[10] = coefficientLastElem.getAttributeDouble("coefficient");
        metadata[11] = imageRasterElem.getAttributeInt("numberOfRows");
        metadata[12] = imageRasterElem.getAttributeInt("numberOfColumns");
        metadata[13] = combinedDopplerFirstElem.getAttributeDouble("validityRangeMax");
        metadata[14] = timeUTCtoSecs(stopElem);

        return metadata;
    }

    private double timeUTCtoSecs(MetadataElement myDate) {

        ProductData.UTC localDateTime = ReaderUtils.getTime(myDate, "timeUTC", standardDateFormat);
        return localDateTime.getMJD() * 24.0 * 3600.0;
    }

    private static List<Integer> lookForIndex(MetadataElement lookForElements, String elemName) {

        String[] indexElem = lookForElements.getElementNames();
        int totalElem = indexElem.length;
        boolean first = false;
        List<Integer> index = new ArrayList<>();

        for (int i = 0; i < totalElem; i++) {

            if (indexElem[i].equals(elemName)) {
                if (!first) {

                    index.add(i);
                    first = true;
                } else if (i == totalElem - 1) {

                    index.add(i);
                }
            }
        }
        return index;
    }
}