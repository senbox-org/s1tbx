/*
 * Copyright (C) 2020 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.saocom;

import com.bc.ceres.core.VirtualDir;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;

/**
 * This class represents a product directory.
 */
public class SaocomProductDirectory extends XMLProductDirectory {

    private File headerFile;
    private String productName;
    private String productType;
    private String productDescription;
    private String mode, polMode, beamId;
    private int width, height;
    private VirtualDir dataDir;

    private final double[] latCorners = new double[4];
    private final double[] lonCorners = new double[4];
    private final double[] slantRangeCorners = new double[4];
    private final double[] incidenceCorners = new double[4];

    private static final GeoTiffProductReaderPlugIn geotiffPlugIn = new GeoTiffProductReaderPlugIn();

    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DateFormat dateFormat2 = ProductData.UTC.createDateFormat("dd-MMM-yyyy HH:mm:ss");

    public SaocomProductDirectory(final File inputFile) {
        super(inputFile);
    }

    @Override
    public void close() throws IOException {
        if(dataDir != null) {
            dataDir.close();
        }
        super.close();
    }

    @Override
    protected void createProductDir(final File inputFile) {
        headerFile = inputFile;
        productName = headerFile.getName().replace(".xemt", "");

        if (ZipUtils.isZip(inputFile)) {
            baseDir = inputFile;
            productDir = VirtualDir.create(baseDir);
            baseName = baseDir.getName();
            if(baseName.endsWith(".zip")) {
                baseName = baseName.substring(0, baseName.lastIndexOf(".zip"));
            }
        } else {
            if(inputFile.isDirectory()) {
                baseDir = inputFile;
            } else {
                baseDir = inputFile.getParentFile();
            }
            final String imgFolderStr = getRelativePathToImageFolder();
            final File imgFolder = new File(baseDir, imgFolderStr);
            if(!imgFolder.exists()) {
                final File zipFile = new File(baseDir, productName+".zip");
                if(zipFile.exists()) {
                    dataDir = VirtualDir.create(zipFile);
                }
            }

            productDir = VirtualDir.create(baseDir);
            baseName = baseDir.getName();
        }
    }

    @Override
    public void readProductDirectory() throws IOException {
        try (final InputStream is = productDir.getInputStream(getRootFolder() + getHeaderFileName())) {
            xmlDoc = XMLSupport.LoadXML(is);
        }
    }

    @Override
    protected String getRelativePathToImageFolder() {
        if(dataDir == null) {
            final File dataFolder = new File(baseDir, "Data");
            if(dataFolder.exists()) {
                return "Data/";
            }
            return productName + "/Data/";
        }
        return "Data/";
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
    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement.getChild("product"), origProdRoot);

        addMetadataFiles(getRelativePathToImageFolder(), origProdRoot);

        addAbstractedMetadataHeader(root);

        return root;
    }

    @Override
    protected VirtualDir getProductDir() {
        return dataDir != null ? dataDir : productDir;
    }

    private void addMetadataFiles(final String internalPath, final MetadataElement destElem) throws IOException {
        final VirtualDir virtualDir = getProductDir();

        final String[] metaFiles = virtualDir.list(internalPath);
        for (String file : metaFiles) {
            if (file.endsWith(".xml")) {
                try {
                    if (productType == null) {
                        productType = file.substring(0, file.indexOf("-")).toUpperCase();
                        if(productType.equals("SLC")) {
                            setSLC(true);
                        }
                    }

                    final File metaFile = getFile(internalPath + file);
                    final Document xmlDoc = XMLSupport.LoadXML(metaFile.getAbsolutePath());
                    final Element metaFileElement = xmlDoc.getRootElement();

                    AbstractMetadataIO.AddXMLMetadata(metaFileElement.getChild("Channel"), destElem);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Unable to read metadata " + file);
                }
            }
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement product = origProdRoot.getElement("product");
        final MetadataElement features = product.getElement("features");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, productName);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, getMission());
        absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);

        final MetadataElement imageAttributes = features.getElement("imageAttributes");
        final MetadataElement bands = imageAttributes.getElement("bands");
        final MetadataElement[] bandElems = bands.getElements();
        final MetadataElement bandElem = bandElems[0];

        width = bandElem.getAttributeInt("nCols", defInt);
        height = bandElem.getAttributeInt("nRows", defInt);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, width);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, height);

        final MetadataElement acquisition = features.getElement("acquisition");
        final MetadataElement parameters = acquisition.getElement("parameters");
        mode = getAcquisitionMode(parameters.getAttributeString("acqMode"));
        polMode = parameters.getAttributeString("polMode");
        beamId = parameters.getAttributeString("beamID").replace("QP","").replace("DP","").replace("SD","");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS, beamId);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, mode);
        final String antennaPointing = parameters.getAttributeString("sideLooking").toLowerCase();
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, antennaPointing);

        String desc = features.getAttributeString("abstract");
        productDescription = desc.replace("XML Annotated ", "");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, productDescription);

        final MetadataElement production = features.getElement("production");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, production.getAttributeString("facilityID"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType());

        final MetadataElement StateVectorData = features.getElement("StateVectorData");
        final String pass = StateVectorData.getAttributeString("OrbitDirection");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, pass);

        final MetadataElement scene = features.getElement("scene");
        final MetadataElement timeFrame = scene.getElement("timeFrame");
        final MetadataElement timePeriod = timeFrame.getElement("timePeriod");
        setStartStopTime(absRoot, timePeriod, height);

        final MetadataElement frame = scene.getElement("frame");
        final MetadataElement[] vertices = frame.getElements();
        final double[] lat = new double[4];
        final double[] lon = new double[4];
        lat[0] = vertices[0].getAttributeDouble("lat", defInt);
        lon[0] = vertices[0].getAttributeDouble("lon", defInt);
        lat[1] = vertices[1].getAttributeDouble("lat", defInt);
        lon[1] = vertices[1].getAttributeDouble("lon", defInt);
        lat[2] = vertices[2].getAttributeDouble("lat", defInt);
        lon[2] = vertices[2].getAttributeDouble("lon", defInt);
        lat[3] = vertices[3].getAttributeDouble("lat", defInt);
        lon[3] = vertices[3].getAttributeDouble("lon", defInt);

        if (pass.equals("ASCENDING")) {
            if (antennaPointing.equals("right")) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, lat[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lon[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, lat[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lon[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lat[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lon[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lat[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lon[2]);

                latCorners[0] = lat[0]; lonCorners[0] = lon[0];
                latCorners[1] = lat[3]; lonCorners[1] = lon[3];
                latCorners[2] = lat[1]; lonCorners[2] = lon[1];
                latCorners[3] = lat[2]; lonCorners[3] = lon[2];

            } else { // "left"
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, lat[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lon[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, lat[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lon[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lat[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lon[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lat[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lon[1]);

                latCorners[0] = lat[3]; lonCorners[0] = lon[3];
                latCorners[1] = lat[0]; lonCorners[1] = lon[0];
                latCorners[2] = lat[2]; lonCorners[2] = lon[2];
                latCorners[3] = lat[1]; lonCorners[3] = lon[1];
            }

        } else { // "DESCENDING"
            if (antennaPointing.equals("right")) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, lat[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lon[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, lat[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lon[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lat[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lon[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lat[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lon[0]);

                latCorners[0] = lat[2]; lonCorners[0] = lon[2];
                latCorners[1] = lat[1]; lonCorners[1] = lon[1];
                latCorners[2] = lat[3]; lonCorners[2] = lon[3];
                latCorners[3] = lat[0]; lonCorners[3] = lon[0];

            } else { // "left"
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, lat[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lon[1]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, lat[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lon[2]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, lat[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lon[0]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lat[3]);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lon[3]);

                latCorners[0] = lat[1]; lonCorners[0] = lon[1];
                latCorners[1] = lat[2]; lonCorners[1] = lon[2];
                latCorners[2] = lat[0]; lonCorners[2] = lon[0];
                latCorners[3] = lat[3]; lonCorners[3] = lon[3];
            }
        }

        final MetadataElement channel = origProdRoot.getElement("Channel");
        final MetadataElement datasetInfo = channel.getElement("DataSetInfo");
        final MetadataElement swathInfo = channel.getElement("SwathInfo");
        final MetadataElement samplingConstants = channel.getElement("SamplingConstants");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, datasetInfo.getAttributeDouble("fc_hz") / Constants.oneMillion);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, getTime(datasetInfo, "ProcessingDate", dateFormat2));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, swathInfo.getAttributeDouble("AcquisitionPRF"));

        double rangeSpacing = 0.0, azimuthSpacing = 0.0;
        if(samplingConstants != null && samplingConstants.containsElement("PSrg_m") && samplingConstants.containsElement("PSaz_m")) {
            final MetadataElement psrg_m = samplingConstants.getElement("PSrg_m");
            rangeSpacing = psrg_m.getAttributeDouble("PSrg_m");
            final MetadataElement psaz_m = samplingConstants.getElement("PSaz_m");
            azimuthSpacing = psaz_m.getAttributeDouble("PSaz_m");
        }

        if (rangeSpacing > 0.0 && azimuthSpacing > 0.0) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, rangeSpacing);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, azimuthSpacing);
        } else {
            final MetadataElement rasterInfo = channel.getElement("RasterInfo");
            final MetadataElement samplesStep = rasterInfo.getElement("SamplesStep");
//            final MetadataElement linesStep = rasterInfo.getElement("LinesStep");
            rangeSpacing = samplesStep.getAttributeDouble("SamplesStep");
//            azimuthSpacing = linesStep.getAttributeDouble("LinesStep");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, rangeSpacing);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, rangeSpacing);
        }

        double rangeSamplingRateMHz = 0.0;
        if (samplingConstants != null && samplingConstants.containsElement("frg_hz")) {
            final MetadataElement frg_hz = samplingConstants.getElement("frg_hz");
            rangeSamplingRateMHz = frg_hz.getAttributeDouble("frg_hz") / 1e6;
        }

        if (rangeSamplingRateMHz > 0.0) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, rangeSamplingRateMHz);
        } else {
            final MetadataElement rasterInfo = channel.getElement("RasterInfo");
            final MetadataElement samplesStep = rasterInfo.getElement("SamplesStep");
            final String unit = samplesStep.getAttributeString("unit");
            final double step = samplesStep.getAttributeDouble("SamplesStep");
            if (unit != null && unit.equals("s") && step > 0.0) {
                rangeSamplingRateMHz = 1.0 / (step * 1.0e6);
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, rangeSamplingRateMHz);
            }
        }

        final MetadataElement slantToGround = channel.getElement("SlantToGround");
        if(slantToGround != null) {
            double trg0_s = slantToGround.getAttributeDouble("trg0_s", 0);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel, trg0_s * Constants.halfLightSpeed);
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, 1.0);
        if(isSLC()) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 1.0);
        } else {
            if(mode.equals("Stripmap")) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 2.0);
            } else if(mode.equals("TOPSARNarrow")) {
                if(polMode.equals("QP")) {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 5.0);
                } else {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 3.0);
                }
            } else if(mode.equals("TOPSARWide")) {
                if(polMode.equals("QP")) {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 10.0);
                } else {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 5.0);
                }
            }
        }

        if (productType.equals("SLC")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 0);
        } else { // DI
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 1);
        }

        addOrbitStateVectors(absRoot, features.getElement("StateVectorData"));
        addSRGRCoefficients(absRoot, channel.getElement("GroundToSlant"));
        addDopplerCentroidCoefficients(absRoot, channel);
    }

    private static ProductData.UTC getTime(final MetadataElement elem, final String tag, final DateFormat timeFormat) {
        if (elem == null)
            return AbstractMetadata.NO_METADATA_UTC;
        final String timeStr = elem.getAttributeString(tag, " ").toUpperCase();
        return AbstractMetadata.parseUTC(timeStr, timeFormat);
    }

    private void setStartStopTime(final MetadataElement absRoot, final MetadataElement elem, final int height) {
        final ProductData.UTC startTime = ReaderUtils.getTime(elem, "startTime", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(elem, "endTime", standardDateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, stopTime, height));
    }

    private static String getAcquisitionMode(final String mode) {
        if (mode.equalsIgnoreCase("SM"))
            return "Stripmap";
        else if (mode.equalsIgnoreCase("TW"))
            return "TOPSARWide";
        else if (mode.equalsIgnoreCase("TN"))
            return "TOPSARNarrow";
        return " ";
    }

    @Override
    protected void addImageFile(final String imgPath, final MetadataElement newRoot) {
        if (!imgPath.toLowerCase().endsWith(".xml")) {
            try {
                final Dimension bandDimensions = new Dimension(width, height);
                final InputStream inStream = getInputStream(imgPath);
                if (inStream.available() > 0) {
                    final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);

                    final ImageIOFile img = new ImageIOFile(imgPath, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 1, ProductData.TYPE_FLOAT64, productInputFile);
                    bandImageFileMap.put(img.getName(), img);
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(imgPath + " not found");
            }
        }
    }

    private static String getImageName(final String name) {
        String n = name.toLowerCase();
        if(n.endsWith("-v") || n.endsWith("-l") || n.endsWith("-m") || n.endsWith("-h")) {
            // remove resolution
            return name.substring(0, name.length()-2);
        }
        return name;
    }

    @Override
    protected void addBands(final Product product) throws IOException {
        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            int numImages = img.getNumImages();
            final String imgName = getImageName(img.getName());

            final String suffix = SARReader.findPolarizationInBandName(imgName);
            if (isSLC()) {
                numImages *= 2; // real + imaginary
            }

            String bandName;
            boolean real = true;
            Band lastRealBand = null;
            for (int i = 0; i < numImages; ++i) {

                if (isSLC()) {
                    String unit;
                    String swath = "";

                    if(mode.equals("TOPSARNarrow") || mode.equals("TOPSARWide")) {
                        int swathPos = imgName.lastIndexOf("-s");
                        String swathNum = imgName.substring(swathPos+1, swathPos+3).toUpperCase();
                        swath = '_' + swathNum;
                    }

                    for (int b = 0; b < img.getNumBands(); ++b) {
                        if (real) {
                            bandName = "i" + swath + '_' + suffix;
                            unit = Unit.REAL;
                        } else {
                            bandName = "q" + swath + '_' + suffix;
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(unit);
                        band.setNoDataValueUsed(true);
                        band.setNoDataValue(0);

                        product.addBand(band);
                        final ImageIOFile.BandInfo bandInfo = new ImageIOFile.BandInfo(band, img, 0, b);
                        bandMap.put(band, bandInfo);

                        if (real) {
                            lastRealBand = band;
                        } else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, ReaderUtils.createName(band.getName(),
                                    "Intensity"), swath + '_' + suffix);
                            bandInfo.setRealBand(lastRealBand);
                            bandMap.get(lastRealBand).setImaginaryBand(band);
                        }
                        real = !real;
                    }
                } else {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        bandName = "Sigma0" + '_' + suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(Unit.INTENSITY);
                        band.setNoDataValueUsed(true);
                        band.setNoDataValue(0);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

//                        SARReader.createVirtualIntensityBand(product, band, '_' + suffix);
                    }
                }
            }

            if(product.getSceneGeoCoding() == null && (productType.equals("GEC") || productType.equals("GTC"))) {
                final File bandFile = getFile(img.getName());
                final ProductReader reader = geotiffPlugIn.createReaderInstance();
                final Product bandProduct = reader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    if (product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                        product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {

                        ProductUtils.copyGeoCoding(bandProduct, product);
                        Dimension tileSize = bandProduct.getPreferredTileSize();
                        if (tileSize == null) {
                            tileSize = ImageManager.getPreferredTileSize(bandProduct);
                        }
                        product.setPreferredTileSize(tileSize);
                        ImageLayout imageLayout = new ImageLayout();
                        imageLayout.setTileWidth(tileSize.width);
                        imageLayout.setTileHeight(tileSize.height);
                    }
                    bandProduct.closeIO();
                    bandProduct.dispose();
                }
            }
        }
    }

    @Override
    protected void addGeoCoding(final Product product) {

        if(product.getSceneGeoCoding() != null) {
            return;
        }

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

   /*     MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
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
        }*/
    }

    @Override
    protected void addTiePointGrids(final Product product) {

        final int gridWidth = 4;
        final int gridHeight = 4;
        final double subSamplingX = (double) product.getSceneRasterWidth() / (gridWidth - 1);
        final double subSamplingY = (double) product.getSceneRasterHeight() / (gridHeight - 1);
        if (subSamplingX == 0 || subSamplingY == 0)
            return;

        populateIncidenceAngles();

        //final float[] flippedSlantRangeCorners = new float[4];
        //final float[] flippedIncidenceCorners = new float[4];
        //getFlippedCorners(product, flippedSlantRangeCorners, flippedIncidenceCorners);

        if (product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final float[] fineAngles = new float[gridWidth * gridHeight];
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);

            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                    subSamplingX, subSamplingY, fineAngles);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

//        final float[] fineSlantRange = new float[gridWidth * gridHeight];
//        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, flippedSlantRangeCorners, fineSlantRange);
//
//        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
//                subSamplingX, subSamplingY, fineSlantRange);
//        slantRangeGrid.setUnit(Unit.NANOSECONDS);
//        product.addTiePointGrid(slantRangeGrid);
    }

    private void populateIncidenceAngles() {
        switch (mode) {
            case "Stripmap":
                if(polMode.equals("QP")) {
                    if(beamId.equals("S1")) {
                        incidenceCorners[0] = 17.6;
                        incidenceCorners[1] = 19.6;
                    } else if(beamId.equals("S2")) {
                        incidenceCorners[0] = 19.5;
                        incidenceCorners[1] = 21.5;
                    } else if(beamId.equals("S3")) {
                        incidenceCorners[0] = 21.4;
                        incidenceCorners[1] = 23.3;
                    } else if(beamId.equals("S4")) {
                        incidenceCorners[0] = 23.2;
                        incidenceCorners[1] = 25.4;
                    } else if(beamId.equals("S5")) {
                        incidenceCorners[0] = 25.3;
                        incidenceCorners[1] = 27.3;
                    } else if(beamId.equals("S6")) {
                        incidenceCorners[0] = 27.2;
                        incidenceCorners[1] = 29.6;
                    } else if(beamId.equals("S7")) {
                        incidenceCorners[0] = 29.6;
                        incidenceCorners[1] = 31.2;
                    } else if(beamId.equals("S8")) {
                        incidenceCorners[0] = 31.2;
                        incidenceCorners[1] = 33.0;
                    } else if(beamId.equals("S9")) {
                        incidenceCorners[0] = 33.0;
                        incidenceCorners[1] = 34.6;
                    } else if(beamId.equals("S10")) {
                        incidenceCorners[0] = 34.6;
                        incidenceCorners[1] = 35.5;
                    }
                } else {
                    if(beamId.equals("S1")) {
                        incidenceCorners[0] = 20.7;
                        incidenceCorners[1] = 25.0;
                    } else if(beamId.equals("S2")) {
                        incidenceCorners[0] = 24.9;
                        incidenceCorners[1] = 29.2;
                    } else if(beamId.equals("S3")) {
                        incidenceCorners[0] = 29.1;
                        incidenceCorners[1] = 33.8;
                    } else if(beamId.equals("S4")) {
                        incidenceCorners[0] = 33.7;
                        incidenceCorners[1] = 38.3;
                    } else if(beamId.equals("S5")) {
                        incidenceCorners[0] = 38.2;
                        incidenceCorners[1] = 41.3;
                    } else if(beamId.equals("S6")) {
                        incidenceCorners[0] = 41.3;
                        incidenceCorners[1] = 44.5;
                    } else if(beamId.equals("S7")) {
                        incidenceCorners[0] = 44.6;
                        incidenceCorners[1] = 47.1;
                    } else if(beamId.equals("S8")) {
                        incidenceCorners[0] = 47.2;
                        incidenceCorners[1] = 48.7;
                    } else if(beamId.equals("S9")) {
                        incidenceCorners[0] = 48.8;
                        incidenceCorners[1] = 50.2;
                    }
                }
                break;
            case "TOPSARNarrow":
                if(polMode.equals("QP")) {
                    if(beamId.equals("TNA")) {
                        incidenceCorners[0] = 17.6;
                        incidenceCorners[1] = 27.3;
                    } else {
                        incidenceCorners[0] = 27.2;
                        incidenceCorners[1] = 35.5;
                    }
                } else {
                    if(beamId.equals("TNA")) {
                        incidenceCorners[0] = 24.9;
                        incidenceCorners[1] = 38.3;
                    } else {
                        incidenceCorners[0] = 38.2;
                        incidenceCorners[1] = 47.1;
                    }
                }
                break;
            case "TOPSARWide":
                if(polMode.equals("QP")) {
                    incidenceCorners[0] = 17.6;
                    incidenceCorners[1] = 35.5;
                } else {
                    incidenceCorners[0] = 24.9;
                    incidenceCorners[1] = 48.7;
                }
                break;
        }
        incidenceCorners[2] = incidenceCorners[0];
        incidenceCorners[3] = incidenceCorners[1];
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

    private void addOrbitStateVectors(MetadataElement absRoot, MetadataElement orbitInformation) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final ProductData.UTC utc = ReaderUtils.getTime(orbitInformation, "t_ref_Utc", standardDateFormat);
        final Double delta = orbitInformation.getAttributeDouble("dtSV_s");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, utc);

        final MetadataElement pSV_m = orbitInformation.getElement("pSV_m");
        final MetadataElement vSV_mOs = orbitInformation.getElement("vSV_mOs");
        final MetadataAttribute[] pos = pSV_m.getAttributes();
        final MetadataAttribute[] vel = vSV_mOs.getAttributes();

        int cnt = 1;
        ProductData.UTC time = utc;
        for (int i = 0; i < pos.length; i+=3) {
            final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + cnt);

            orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, time);
            addVector(orbitVectorElem, pos, vel, i);

            orbitVectorListElem.addElement(orbitVectorElem);
            time = new ProductData.UTC((int)time.getMJD(), (int)(time.getSecondsFraction() + delta), (int)time.getMicroSecondsFraction());
            ++cnt;
        }
    }

    private void addVector(final MetadataElement orbitVectorElem,
                           final MetadataAttribute[] pos, final MetadataAttribute[] vel, int i) {

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                Double.parseDouble(pos[i].getData().getElemString()));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                Double.parseDouble(pos[i+1].getData().getElemString()));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                Double.parseDouble(pos[i+2].getData().getElemString()));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                Double.parseDouble(vel[i].getData().getElemString()));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                Double.parseDouble(vel[i+1].getData().getElemString()));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                Double.parseDouble(vel[i+2].getData().getElemString()));
    }

    private void addSRGRCoefficients(final MetadataElement absRoot, final MetadataElement GroundToSlant) {

        if(GroundToSlant == null)
            return;

        final MetadataElement pol = GroundToSlant.getElement("pol");

        // save ground range to slant range conversion coefficients in abstract metadata
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);

        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, getTime(GroundToSlant, "taz0_Utc", dateFormat2));
        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

        int cnt = 1;
        for (int i = 0; i < pol.getElements().length; ++i) {
            if (i == 2 || i == 3) {
                continue;
            }
            final MetadataElement val = pol.getElementAt(i);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
            srgrListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef,
                    val.getAttributeDouble("val")*Constants.halfLightSpeed);
            ++cnt;
        }
    }

    private void addDopplerCentroidCoefficients(final MetadataElement absRoot, final MetadataElement channel) {
        final MetadataElement[] channelElements = channel.getElements();

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for (MetadataElement channelElement : channelElements) {
            if (channelElement.getName().equalsIgnoreCase("DopplerCentroid")) {
                final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + '.' + listCnt);
                dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(channelElement, "taz0_Utc", dateFormat2);
                dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

                final double refTime = channelElement.getAttributeDouble("trg0_s", 0)*1e9; // s to ns
                AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                       ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
                AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);

                final MetadataElement pol = channelElement.getElement("pol");
                final MetadataElement[] coefficients = pol.getElements();

                int cnt = 1;
                for (MetadataElement coefficient : coefficients) {
                    final double coefValue = coefficient.getAttributeDouble("val", 0);
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

    private String getSampleType() {
        return isSLC() ? "COMPLEX" : "DETECTED";
    }

    protected String getMission() {
        return "SAOCOM";
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
}
