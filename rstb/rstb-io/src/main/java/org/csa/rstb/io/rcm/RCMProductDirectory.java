/*
 * Copyright (C) 2018 Skywatch. https://www.skywatch.co
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
package org.csa.rstb.io.rcm;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

/**
 * This class represents a product directory.
 */
public class RCMProductDirectory extends XMLProductDirectory {

    private String productName = RCMConstants.MISSION;
    private String productType = RCMConstants.MISSION;
    private final String productDescription = "";

    private static final GeoTiffProductReaderPlugIn geotiffPlugIn = new GeoTiffProductReaderPlugIn();
    private final List<Product> bandProducts = new ArrayList<>();
    private final Map<String, String> polarizationMap = new HashMap<>(4);

    private static final boolean flipToSARGeometry = System.getProperty(SystemUtils.getApplicationContextId() +
            ".flip.to.sar.geometry", "false").equals("true");

    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public RCMProductDirectory(final File headerFile) {
        super(headerFile);
    }

    protected String getHeaderFileName() {
        String[] files = findFilesContaining(getRootFolder(), RCMConstants.PRODUCT_MANIFEST);
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    @Override
    protected String findRootFolder() {
        String rootFolder = "";
        try {
            if (isCompressed()) {
                rootFolder = ZipUtils.getRootFolder(getBaseDir(), "manifest.safe");
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to get root path from zip file " + e.getMessage());
        }
        return rootFolder;
    }

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
            bandProduct.dispose();
        }
        bandProducts.clear();
        super.close();
    }

    @Override
    protected String getRelativePathToImageFolder() {
        return getRootFolder() + "imagery" + '/';
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) throws IOException {
        final String name = getBandFileNameFromImage(imgPath);
        if (name.endsWith("tif")) {
            int dataType = ProductData.TYPE_INT32;
            final Dimension bandDimensions = getBandDimension(newRoot, name);
            final InputStream inStream = getInputStream(imgPath);
            if (inStream.available() > 0) {
                final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);
                if (imgStream == null)
                    throw new IOException("Unable to open " + imgPath);

                final ImageIOFile img;
                if(productType.equals("GRD")) {
                    img = new ImageIOFile(imgPath, imgStream, getTiffIIOReader(imgStream),
                            1, 1, dataType, productInputFile);
                } else {
                    img = new ImageIOFile(imgPath, imgStream, getTiffIIOReader(imgStream), productInputFile);
                }
                bandImageFileMap.put(img.getName(), img);
            }
        }
    }

    private static ImageReader getTiffIIOReader(final ImageInputStream stream) throws IOException {
        ImageReader reader = null;
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while (imageReaders.hasNext()) {
            final ImageReader iioReader = imageReaders.next();
            if (iioReader instanceof TIFFImageReader) {
                reader = iioReader;
                break;
            }
        }
        if (reader == null)
            throw new IOException("Unable to open " + stream.toString());
        reader.setInput(stream, true, true);
        return reader;
    }

    private static Dimension getBandDimension(final MetadataElement newRoot, final String name) {
        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        final int width = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int height = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);
        return new Dimension(width, height);
    }

    @Override
    protected void addBands(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final int width = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int height = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);

        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);
            final String name = img.getName().toLowerCase();

            for (int i = 0; i < img.getNumImages(); ++i) {
                try {
                    if (name.contains("hh")) {
                        addBand(product, img, width, height, "HH");
                    } else if (name.contains("hv")) {
                        addBand(product, img, width, height, "HV");
                    } else if (name.contains("vv")) {
                        addBand(product, img, width, height, "VV");
                    } else if (name.contains("vh")) {
                        addBand(product, img, width, height, "VH");
                    } else if (name.contains("ch")) {
                        addBand(product, img, width, height, "CH");
                    } else if (name.contains("cv")) {
                        addBand(product, img, width, height, "CV");
                    }
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Unable to read band " + name);
                }
            }
        }

        ImageLayout imageLayout = new ImageLayout();
        for (Product bandProduct : bandProducts) {
            if (product.getSceneGeoCoding() == null &&
                    product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                ProductUtils.copyGeoCoding(bandProduct, product);
                Dimension tileSize = bandProduct.getPreferredTileSize();
                if (tileSize == null) {
                    tileSize = ImageManager.getPreferredTileSize(bandProduct);
                }
                product.setPreferredTileSize(tileSize);
                imageLayout.setTileWidth(tileSize.width);
                imageLayout.setTileHeight(tileSize.height);
                break;
            }
        }
    }

    private void addBand(final Product product, final ImageIOFile img, final int width, final int height,
                         final String pol) throws IOException {
        if(isSLC()) {
            Band iBand = addBand(product, "i_"+pol, width, height, img, 0, Unit.REAL);
            Band qBand = addBand(product, "q_"+pol, width, height, img, 1, Unit.IMAGINARY);
            ReaderUtils.createVirtualIntensityBand(product, iBand, qBand, '_' + pol);
        } else {
            Band ampBand = addBand(product, "Amplitude_" + pol, width, height, img, 0, Unit.AMPLITUDE);
            SARReader.createVirtualIntensityBand(product, ampBand, '_' + pol);
        }
    }

    private Band addBand(final Product product, String name, final int w, final int h,
                         final ImageIOFile img, final int bandIndex, final String unit) throws IOException {
        File bandFile = getFile(img.getName());
        ProductReader reader = geotiffPlugIn.createReaderInstance();
        Product bandProduct = reader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(bandIndex);
            if (product.containsBand(name)) {
                name += "2";
            }
            Band band = new Band(name, srcBand.getDataType(), w, h);
            band.setNoDataValue(0);
            band.setNoDataValueUsed(true);
            band.setUnit(unit);
            band.setSourceImage(srcBand.getSourceImage());
            product.addBand(band);

            return band;
        }
        return null;
    }

    @Override
    protected MetadataElement addMetaData() throws IOException {

        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);
        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement, origProdRoot);

        addMetadataFiles(getRootFolder() + "metadata", origProdRoot);

        final MetadataElement calibrationElem = new MetadataElement("calibration");
        origProdRoot.addElement(calibrationElem);
        addMetadataFiles(getRootFolder() + "metadata/calibration", calibrationElem);

        addAbstractedMetadataHeader(root);

        return root;
    }

    private void addMetadataFiles(final String internalPath, final MetadataElement destElem) throws IOException {
        String[] metaFiles = productDir.list(internalPath);
        for (String file : metaFiles) {
            if (file.endsWith(".xml")) {
                try {
                    final File metaFile = getFile(internalPath + "/" + file);
                    final Document xmlDoc = XMLSupport.LoadXML(metaFile.getAbsolutePath());
                    final Element metaFileElement = xmlDoc.getRootElement();

                    if (metaFileElement.getName().equals("lut")) {
                        metaFileElement.setName(file.substring(0, file.lastIndexOf(".xml")));
                    }

                    AbstractMetadataIO.AddXMLMetadata(metaFileElement, destElem);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("Unable to read metadata " + file);
                }
            }
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final MetadataElement root) throws IOException {

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);
        final MetadataElement productElem = origProdRoot.getElement("product");

        productName = getBaseName();
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, productName);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, getMission());

        final MetadataElement imageGenerationParameters = productElem.getElement("imageGenerationParameters");
        final MetadataElement generalProcessingInformation = imageGenerationParameters.getElement("generalProcessingInformation");
        final MetadataElement sarProcessingInformation = imageGenerationParameters.getElement("sarProcessingInformation");

        productType = generalProcessingInformation.getAttributeString("productType");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        if(productType.equals("SLC")) {
            setSLC(true);
        }

        final MetadataElement imageReferenceAttributes = productElem.getElement("imageReferenceAttributes");
        final MetadataElement sceneAttributes = productElem.getElement("sceneAttributes");
        final MetadataElement imageAttributes = sceneAttributes.getElement("imageAttributes");
        final MetadataElement sourceAttributes = productElem.getElement("sourceAttributes");
        final MetadataElement radarParameters = sourceAttributes.getElement("radarParameters");
        final MetadataElement orbitAndAttitude = sourceAttributes.getElement("orbitAndAttitude");
        final MetadataElement orbitInformation = orbitAndAttitude.getElement("orbitInformation");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, imageAttributes.getAttributeInt("samplesPerLine"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, imageAttributes.getAttributeInt("numLines"));

        final MetadataElement rasterAttributes = imageReferenceAttributes.getElement("rasterAttributes");
        final MetadataElement sampledPixelSpacing = rasterAttributes.getElement("sampledPixelSpacing");
        final MetadataElement sampledLineSpacing = rasterAttributes.getElement("sampledLineSpacing");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, sampledPixelSpacing.getAttributeDouble("sampledPixelSpacing"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, sampledLineSpacing.getAttributeDouble("sampledLineSpacing"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, sarProcessingInformation.getAttributeDouble("numberOfRangeLooks"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, sarProcessingInformation.getAttributeDouble("numberOfAzimuthLooks"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getDataType(rasterAttributes));
        final String aquisitionMode = radarParameters.getAttributeString("acquisitionType", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, aquisitionMode);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS, radarParameters.getAttributeString("beams", defStr));
        final MetadataElement radarCenterFrequency = radarParameters.getElement("radarCenterFrequency");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                radarCenterFrequency.getAttributeDouble("radarCenterFrequency", defInt) / Constants.oneMillion);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, radarParameters.getAttributeString("antennaPointing", defStr).toLowerCase());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, getPass(orbitInformation.getAttributeString("passDirection", defStr)));
        final String orbitFile = orbitInformation.getAttributeString("orbitDataFileName", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.orbit_state_vector_file, orbitFile);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, Integer.parseInt(orbitFile.substring(0, orbitFile.indexOf('_')).trim()));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                generalProcessingInformation.getAttributeString("processingFacility", defStr) + '-' +
                        generalProcessingInformation.getAttributeString("softwareVersion", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(generalProcessingInformation, "processingTime", standardDateFormat));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag,
                getFlag(sarProcessingInformation, "elevationPatternCorrection"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag,
                getFlag(sarProcessingInformation, "rangeSpreadingLossCorrection"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isSLC() ? 0 : 1);

        final MetadataElement slantRangeToGroundRange = imageGenerationParameters.getElement("slantRangeToGroundRange");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                slantRangeToGroundRange.getElement("slantRangeTimeToFirstRangeSample").getAttributeDouble("slantRangeTimeToFirstRangeSample"));

        // add Range and Azimuth bandwidth
        final MetadataElement totalProcessedRangeBandwidth = sarProcessingInformation.getElement("totalProcessedRangeBandwidth");
        final MetadataElement totalProcessedAzimuthBandwidth = sarProcessingInformation.getElement("totalProcessedAzimuthBandwidth");
        final double rangeBW = totalProcessedRangeBandwidth.getAttributeDouble("totalProcessedRangeBandwidth"); // Hz
        final double azimuthBW = totalProcessedAzimuthBandwidth.getAttributeDouble("totalProcessedAzimuthBandwidth"); // Hz

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);

        final MetadataElement prfInformation = radarParameters.getElement("prfInformation");
        final MetadataElement pulseRepetitionFrequency = prfInformation.getElement("pulseRepetitionFrequency");
        double prf = pulseRepetitionFrequency.getAttributeDouble("pulseRepetitionFrequency", defInt);
        final MetadataElement adcSamplingRate = radarParameters.getElement("adcSamplingRate");
        double rangeSamplingRate = adcSamplingRate.getAttributeDouble("adcSamplingRate", defInt) / Constants.oneMillion;

        if (aquisitionMode.equalsIgnoreCase("UltraFine")) {
            prf *= 2.0;
            rangeSamplingRate *= 2.0;
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, prf);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, rangeSamplingRate);

        final MetadataElement geographicInformation = imageReferenceAttributes.getElement("geographicInformation");
        if (geographicInformation != null) {
            final MetadataElement ellipsoidParameters = geographicInformation.getElement("ellipsoidParameters");
            if (ellipsoidParameters != null) {
                final MetadataElement geodeticTerrainHeight = ellipsoidParameters.getElement("geodeticTerrainHeight");
                if (geodeticTerrainHeight != null) {
                    AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height,
                            geodeticTerrainHeight.getAttributeDouble("geodeticTerrainHeight", defInt));
                }
            }
        }

        try {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, getTime(generalProcessingInformation, "processingTime"));

            ProductData.UTC startTime = getTime(sarProcessingInformation, "zeroDopplerTimeFirstLine");
            ProductData.UTC stopTime = getTime(sarProcessingInformation, "zeroDopplerTimeLastLine");

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                                          ReaderUtils.getLineTimeInterval(startTime, stopTime,
                                                                          absRoot.getAttributeInt(AbstractMetadata.num_output_lines)));

        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to get product times");
        }

        // polarizations
        getPolarizations(absRoot, imageAttributes);

        addOrbitStateVectors(absRoot, orbitInformation);
        addSRGRCoefficients(absRoot, imageGenerationParameters);
        addDopplerCentroidCoefficients(absRoot, imageGenerationParameters);
    }

    private ProductData.UTC getTime(final MetadataElement elem, final String tag) {
        String timeStr = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        timeStr = timeStr.replace("T"," ");
        return AbstractMetadata.parseUTC(timeStr, standardDateFormat);
    }

    private static String getPass(final String pass) {
        if (pass.toUpperCase().contains("ASCENDING"))
            return "ASCENDING";
        else if (pass.toUpperCase().contains("DESCENDING"))
            return "DESCENDING";
        return pass;
    }

    private static int getFlag(final MetadataElement elem, String tag) {
        String valStr = elem.getAttributeString(tag, " ").toUpperCase();
        if (valStr.equals("FALSE") || valStr.equals("0"))
            return 0;
        else if (valStr.equals("TRUE") || valStr.equals("1"))
            return 1;
        return -1;
    }

    private void getPolarizations(final MetadataElement absRoot, final MetadataElement imageAttributes) {
        final MetadataElement[] imageAttribElems = imageAttributes.getElements();
        int i = 0;
        for (MetadataElement elem : imageAttribElems) {
            if (elem.getName().equals("ipdf")) {

                final String pol = elem.getAttributeString("pole", "").toUpperCase();
                polarizationMap.put(elem.getAttributeString("ipdf", "").toLowerCase(), pol);
                absRoot.setAttributeString(AbstractMetadata.polarTags[i], pol);
                ++i;
            }
        }
    }

    private static String getDataType(final MetadataElement rasterAttributes) {
        final String dataType = rasterAttributes.getAttributeString("dataType", AbstractMetadata.NO_METADATA_STRING).toUpperCase();
        if (dataType.contains("COMPLEX"))
            return "COMPLEX";
        return "DETECTED";
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement orbitInformation) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitInformation.getElements();
        for (int i = 1; i <= stateVectorElems.length; ++i) {
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i - 1], i);
        }

        // set state vector time
        if (absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.NO_METADATA_UTC).
                equalElems(AbstractMetadata.NO_METADATA_UTC)) {

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                    ReaderUtils.getTime(stateVectorElems[0], "timeStamp", standardDateFormat));
        }
    }

    private void addVector(String name, MetadataElement orbitVectorListElem,
                           MetadataElement srcElem, int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                ReaderUtils.getTime(srcElem, "timeStamp", standardDateFormat));

        final MetadataElement xpos = srcElem.getElement("xPosition");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                xpos.getAttributeDouble("xPosition", 0));
        final MetadataElement ypos = srcElem.getElement("yPosition");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                ypos.getAttributeDouble("yPosition", 0));
        final MetadataElement zpos = srcElem.getElement("zPosition");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                zpos.getAttributeDouble("zPosition", 0));
        final MetadataElement xvel = srcElem.getElement("xVelocity");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                xvel.getAttributeDouble("xVelocity", 0));
        final MetadataElement yvel = srcElem.getElement("yVelocity");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                yvel.getAttributeDouble("yVelocity", 0));
        final MetadataElement zvel = srcElem.getElement("zVelocity");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                zvel.getAttributeDouble("zVelocity", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    private void addSRGRCoefficients(final MetadataElement absRoot, final MetadataElement imageGenerationParameters) {
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);

        int listCnt = 1;
        for (MetadataElement elem : imageGenerationParameters.getElements()) {
            if (elem.getName().equalsIgnoreCase("slantRangeToGroundRange")) {
                final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list + '.' + listCnt);
                srgrCoefficientsElem.addElement(srgrListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "zeroDopplerAzimuthTime", standardDateFormat);
                srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);

                final double grOrigin = elem.getElement("groundRangeOrigin").getAttributeDouble("groundRangeOrigin", 0);
                AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                        ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
                AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, grOrigin);

                final String coeffStr = elem.getAttributeString("groundToSlantRangeCoefficients", "");
                if (!coeffStr.isEmpty()) {
                    final StringTokenizer st = new StringTokenizer(coeffStr);
                    int cnt = 1;
                    while (st.hasMoreTokens()) {
                        final double coefValue = Double.parseDouble(st.nextToken());

                        final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
                        srgrListElem.addElement(coefElem);
                        ++cnt;
                        AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                                ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
                        AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, coefValue);
                    }
                }
            }
        }
    }

    private void addDopplerCentroidCoefficients(
            final MetadataElement absRoot, final MetadataElement imageGenerationParameters) {

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);

        int listCnt = 1;
        for (MetadataElement elem : imageGenerationParameters.getElements()) {
            if (elem.getName().equalsIgnoreCase("dopplerCentroid")) {
                final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + '.' + listCnt);
                dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
                ++listCnt;

                final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "timeOfDopplerCentroidEstimate", standardDateFormat);
                dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

                final double refTime = elem.getElement("dopplerCentroidReferenceTime").
                        getAttributeDouble("dopplerCentroidReferenceTime", 0) * 1e9; // s to ns
                AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                        ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
                AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);

                final String coeffStr = elem.getAttributeString("dopplerCentroidCoefficients", "");
                if (!coeffStr.isEmpty()) {
                    final StringTokenizer st = new StringTokenizer(coeffStr);
                    int cnt = 1;
                    while (st.hasMoreTokens()) {
                        final double coefValue = Double.parseDouble(st.nextToken());

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
    }

    @Override
    protected void addGeoCoding(final Product product) {

        if(product.getSceneGeoCoding() != null) {
            return;
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final boolean isAscending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
        final boolean isAntennaPointingRight = absRoot.getAttributeString(AbstractMetadata.antenna_pointing).equals("right");

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement productElem = origProdRoot.getElement("product");
        final MetadataElement imageAttributes = productElem.getElement("imageReferenceAttributes");
        final MetadataElement geographicInformation = imageAttributes.getElement("geographicInformation");
        final MetadataElement geolocationGrid = geographicInformation.getElement("geolocationGrid");

        final MetadataElement[] geoGrid = geolocationGrid.getElements();

        float[] latList = new float[geoGrid.length];
        float[] lngList = new float[geoGrid.length];

        int gridWidth = 0, gridHeight = 0;
        int i = 0;
        for (MetadataElement imageTiePoint : geoGrid) {
            final MetadataElement geodeticCoordinate = imageTiePoint.getElement("geodeticCoordinate");
            final MetadataElement latitude = geodeticCoordinate.getElement("latitude");
            final MetadataElement longitude = geodeticCoordinate.getElement("longitude");
            latList[i] = (float) latitude.getAttributeDouble("latitude", 0);
            lngList[i] = (float) longitude.getAttributeDouble("longitude", 0);

            final MetadataElement imageCoordinate = imageTiePoint.getElement("imageCoordinate");
            final double pix = imageCoordinate.getAttributeDouble("pixel", 0);
            if (pix == 0) {
                if (gridWidth == 0)
                    gridWidth = i;
                ++gridHeight;
            }

            ++i;
        }

        if (flipToSARGeometry) {
            float[] flippedLatList = new float[geoGrid.length];
            float[] flippedLonList = new float[geoGrid.length];
            int is, id;
            if (isAscending) {
                if (isAntennaPointingRight) { // flip upside down
                    for (int r = 0; r < gridHeight; r++) {
                        is = r * gridWidth;
                        id = (gridHeight - r - 1) * gridWidth;
                        for (int c = 0; c < gridWidth; c++) {
                            flippedLatList[id + c] = latList[is + c];
                            flippedLonList[id + c] = lngList[is + c];
                        }
                    }
                } else { // flip upside down then left to right
                    for (int r = 0; r < gridHeight; r++) {
                        is = r * gridWidth;
                        id = (gridHeight - r) * gridWidth;
                        for (int c = 0; c < gridWidth; c++) {
                            flippedLatList[id - c - 1] = latList[is + c];
                            flippedLonList[id - c - 1] = lngList[is + c];
                        }
                    }
                }

            } else { // descending

                if (isAntennaPointingRight) {  // flip left to right
                    for (int r = 0; r < gridHeight; r++) {
                        is = r * gridWidth;
                        id = r * gridWidth + gridWidth;
                        for (int c = 0; c < gridWidth; c++) {
                            flippedLatList[id - c - 1] = latList[is + c];
                            flippedLonList[id - c - 1] = lngList[is + c];
                        }
                    }
                } else { // no flipping is needed
                    flippedLatList = latList;
                    flippedLonList = lngList;
                }
            }

            latList = flippedLatList;
            lngList = flippedLonList;
        }

        double subSamplingX = (double) (product.getSceneRasterWidth() - 1) / (gridWidth - 1);
        double subSamplingY = (double) (product.getSceneRasterHeight() - 1) / (gridHeight - 1);

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, latList);
        latGrid.setUnit(Unit.DEGREES);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight, 0.5f, 0.5f,
                subSamplingX, subSamplingY, lngList, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        if(product.getTiePointGrid(OperatorUtils.TPG_LATITUDE) == null) {
            product.addTiePointGrid(latGrid);
            product.addTiePointGrid(lonGrid);
        }
        setLatLongMetadata(product, latGrid, lonGrid);

        if (product.getSceneGeoCoding() == null) {
            product.setSceneGeoCoding(tpGeoCoding);
        }
    }

    private static void setLatLongMetadata(Product product, TiePointGrid latGrid, TiePointGrid lonGrid) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latGrid.getPixelDouble(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonGrid.getPixelDouble(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latGrid.getPixelDouble(w - 1, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonGrid.getPixelDouble(w - 1, 0));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latGrid.getPixelDouble(0, h - 1));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonGrid.getPixelDouble(0, h - 1));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latGrid.getPixelDouble(w - 1, h - 1));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonGrid.getPixelDouble(w - 1, h - 1));
    }

    @Override
    protected void addTiePointGrids(final Product product) {

        final int sourceImageWidth = product.getSceneRasterWidth();
        final int sourceImageHeight = product.getSceneRasterHeight();
        final int gridWidth = 11;
        final int gridHeight = 11;
        final int subSamplingX = (int) ((float) sourceImageWidth / (float) (gridWidth - 1));
        final int subSamplingY = (int) ((float) sourceImageHeight / (float) (gridHeight - 1));

        double a = Constants.semiMajorAxis; // WGS 84: equatorial Earth radius in m
        double b = Constants.semiMinorAxis; // WGS 84: polar Earth radius in m

        // get slant range to first pixel and pixel spacing
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final double slantRangeToFirstPixel = absRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel, 0); // in m
        final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 0); // in m
        final boolean srgrFlag = absRoot.getAttributeInt(AbstractMetadata.srgr_flag) != 0;
        final boolean isDescending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("DESCENDING");
        final boolean isAntennaPointingRight = absRoot.getAttributeString(AbstractMetadata.antenna_pointing).equals("right");

        // get scene center latitude
        final GeoPos sceneCenterPos =
                product.getSceneGeoCoding().getGeoPos(new PixelPos(sourceImageWidth / 2.0f, sourceImageHeight / 2.0f), null);
        double sceneCenterLatitude = sceneCenterPos.lat; // in deg

        // get near range incidence angle
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement productElem = origProdRoot.getElement("product");
        final MetadataElement sceneAttributes = productElem.getElement("sceneAttributes");
        final MetadataElement imageAttributes = sceneAttributes.getElement("imageAttributes");
        final MetadataElement incAngNearRng = imageAttributes.getElement("incAngNearRng");
        final double nearRangeIncidenceAngle = (float) incAngNearRng.getAttributeDouble("incAngNearRng", 0);

        final double alpha1 = nearRangeIncidenceAngle * Constants.DTOR;
        final double lambda = sceneCenterLatitude * Constants.DTOR;
        final double cos2 = FastMath.cos(lambda) * FastMath.cos(lambda);
        final double sin2 = FastMath.sin(lambda) * FastMath.sin(lambda);
        final double e2 = (b * b) / (a * a);
        final double rt = a * Math.sqrt((cos2 + e2 * e2 * sin2) / (cos2 + e2 * sin2));
        final double rt2 = rt * rt;

        double groundRangeSpacing;
        if (srgrFlag) { // detected
            groundRangeSpacing = rangeSpacing;
        } else {
            groundRangeSpacing = rangeSpacing / FastMath.sin(alpha1);
        }

        double deltaPsi = groundRangeSpacing / rt; // in radian
        final double r1 = slantRangeToFirstPixel;
        final double rtPlusH = Math.sqrt(rt2 + r1 * r1 + 2.0 * rt * r1 * FastMath.cos(alpha1));
        final double rtPlusH2 = rtPlusH * rtPlusH;
        final double theta1 = FastMath.acos((r1 + rt * FastMath.cos(alpha1)) / rtPlusH);
        final double psi1 = alpha1 - theta1;
        double psi = psi1;
        float[] incidenceAngles = new float[gridWidth];
        final int n = gridWidth * subSamplingX;
        int k = 0;
        for (int i = 0; i < n; i++) {
            final double ri = Math.sqrt(rt2 + rtPlusH2 - 2.0 * rt * rtPlusH * FastMath.cos(psi));
            final double alpha = FastMath.acos((rtPlusH2 - ri * ri - rt2) / (2.0 * ri * rt));
            if (i % subSamplingX == 0) {
                int index = k++;

                if (!flipToSARGeometry && (isDescending && isAntennaPointingRight || (!isDescending && !isAntennaPointingRight))) {// flip
                    index = gridWidth - 1 - index;
                }

                incidenceAngles[index] = (float) (alpha * Constants.RTOD);
            }

            if (!srgrFlag) { // complex
                groundRangeSpacing = rangeSpacing / FastMath.sin(alpha);
                deltaPsi = groundRangeSpacing / rt;
            }
            psi = psi + deltaPsi;
        }

        float[] incidenceAngleList = new float[gridWidth * gridHeight];
        for (int j = 0; j < gridHeight; j++) {
            System.arraycopy(incidenceAngles, 0, incidenceAngleList, j * gridWidth, gridWidth);
        }

        final TiePointGrid incidentAngleGrid = new TiePointGrid(
                OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, incidenceAngleList);

        incidentAngleGrid.setUnit(Unit.DEGREES);

        product.addTiePointGrid(incidentAngleGrid);

        //addSlantRangeTime(product, imageGenerationParameters);
    }

    private void addSlantRangeTime(final Product product, final MetadataElement imageGenerationParameters) {

        class coefList {
            double utcSeconds = 0.0;
            double grOrigin = 0.0;
            final List<Double> coefficients = new ArrayList<>();
        }

        final List<coefList> segmentsArray = new ArrayList<>();

        for (MetadataElement elem : imageGenerationParameters.getElements()) {
            if (elem.getName().equalsIgnoreCase("slantRangeToGroundRange")) {
                final coefList coef = new coefList();
                segmentsArray.add(coef);
                coef.utcSeconds = ReaderUtils.getTime(elem, "zeroDopplerAzimuthTime", standardDateFormat).getMJD() * 24 * 3600;
                coef.grOrigin = elem.getElement("groundRangeOrigin").getAttributeDouble("groundRangeOrigin", 0);

                final String coeffStr = elem.getAttributeString("groundToSlantRangeCoefficients", "");
                if (!coeffStr.isEmpty()) {
                    final StringTokenizer st = new StringTokenizer(coeffStr);
                    while (st.hasMoreTokens()) {
                        coef.coefficients.add(Double.parseDouble(st.nextToken()));
                    }
                }
            }
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final double lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval, 0);
        final ProductData.UTC startTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        final double startSeconds = startTime.getMJD() * 24 * 3600;
        final double pixelSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 0);
        final boolean isDescending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("DESCENDING");
        final boolean isAntennaPointingRight = absRoot.getAttributeString(AbstractMetadata.antenna_pointing).equals("right");

        final int gridWidth = 11;
        final int gridHeight = 11;
        final int sceneWidth = product.getSceneRasterWidth();
        final int sceneHeight = product.getSceneRasterHeight();
        final int subSamplingX = sceneWidth / (gridWidth - 1);
        final int subSamplingY = sceneHeight / (gridHeight - 1);
        final float[] rangeDist = new float[gridWidth * gridHeight];
        final float[] rangeTime = new float[gridWidth * gridHeight];

        final coefList[] segments = segmentsArray.toArray(new coefList[segmentsArray.size()]);

        int k = 0;
        int c = 0;
        for (int j = 0; j < gridHeight; j++) {
            final double time = startSeconds + (j * lineTimeInterval);
            while (c < segments.length && segments[c].utcSeconds < time)
                ++c;
            if (c >= segments.length)
                c = segments.length - 1;

            final coefList coef = segments[c];
            final double GR0 = coef.grOrigin;
            final double s0 = coef.coefficients.get(0);
            final double s1 = coef.coefficients.get(1);
            final double s2 = coef.coefficients.get(2);
            final double s3 = coef.coefficients.get(3);
            final double s4 = coef.coefficients.get(4);

            for (int i = 0; i < gridWidth; i++) {
                int x = i * subSamplingX;
                final double GR = x * pixelSpacing;
                final double g = GR - GR0;
                final double g2 = g * g;

                //SlantRange = s0 + s1(GR - GR0) + s2(GR-GR0)^2 + s3(GRGR0)^3 + s4(GR-GR0)^4;
                rangeDist[k++] = (float) (s0 + s1 * g + s2 * g2 + s3 * g2 * g + s4 * g2 * g2);
            }
        }

        // get slant range time in nanoseconds from range distance in meters
        for (int i = 0; i < rangeDist.length; i++) {
            int index = i;
            if (!flipToSARGeometry && (isDescending && isAntennaPointingRight || !isDescending && !isAntennaPointingRight)) // flip for descending RS2
                index = rangeDist.length - 1 - i;

            rangeTime[index] = (float) (rangeDist[i] / Constants.halfLightSpeed * Constants.oneBillion); // in ns
        }

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, rangeTime);

        product.addTiePointGrid(slantRangeGrid);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
    }

    private static String getMission() {
        return RCMConstants.MISSION;
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
