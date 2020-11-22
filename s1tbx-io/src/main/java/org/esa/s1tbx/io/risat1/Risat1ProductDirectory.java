/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.risat1;

import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.PropertyMapProductDirectory;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.ceos.risat.RisatCeosProductReader;
import org.esa.s1tbx.io.ceos.risat.RisatCeosProductReaderPlugIn;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

/**
 * This class represents a product directory.
 */
public class Risat1ProductDirectory extends PropertyMapProductDirectory {

    private String productName = "Risat1";
    private String productType = "Risat1";
    private final String productDescription = "";
    private boolean compactPolMode = false;

    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("dd-MMM-yyyy HH:mm:ss");

    private static final boolean flipToSARGeometry = System.getProperty(SystemUtils.getApplicationContextId() +
            ".flip.to.sar.geometry", "false").equals("true");

    private static final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
    private static final RisatCeosProductReaderPlugIn ceosPlugIn = new RisatCeosProductReaderPlugIn();
    private List<Product> bandProductList = new ArrayList<>();

    public Risat1ProductDirectory(final File headerFile) {
        super(headerFile);
    }

    protected String getHeaderFileName() {
        return Risat1Constants.BAND_HEADER_NAME;
    }

    @Override
    protected void findImages(final MetadataElement newRoot) throws IOException {
        final String parentPath = getRelativePathToImageFolder();
        findImages(parentPath + "scene_HH//", newRoot);
        findImages(parentPath + "scene_HV//", newRoot);
        findImages(parentPath + "scene_VV//", newRoot);
        findImages(parentPath + "scene_VH//", newRoot);
        findImages(parentPath + "scene_RH//", newRoot);
        findImages(parentPath + "scene_RV//", newRoot);
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) throws IOException {
        final String name = getBandFileNameFromImage(imgPath);
        if (((name.endsWith("tif") || name.endsWith("tiff"))) && name.contains("imagery")) {
            final InputStream inStream = getInputStream(imgPath);
            if (inStream.available() > 0) {
                final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, getBandDimensions(newRoot, name));
                if (imgStream == null)
                    throw new IOException("Unable to open " + imgPath);

                if (!isCompressed()) {
                    final ProductReader geoTiffReader = geoTiffPlugIn.createReaderInstance();
                    Product bProduct = geoTiffReader.readProductNodes(new File(getBaseDir(), imgPath), null);
                    bandProductList.add(bProduct);
                }

                final ImageIOFile img;
                if (isSLC()) {
                    img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 2, ProductData.TYPE_INT32, productInputFile);
                } else {
                    img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream), productInputFile);
                }
                bandImageFileMap.put(img.getName(), img);
            }
        } else if (name.endsWith(".001") && name.contains("vdf_")) {
            final ProductReader ceosReader = ceosPlugIn.createReaderInstance();
            Product bProduct = ceosReader.readProductNodes(new File(getBaseDir(), imgPath), null);
            int idx = imgPath.indexOf("scene_") + 6;
            String pol = imgPath.substring(idx, idx + 2);
            bProduct.setName(bProduct.getName() + "_" + pol);
            bandProductList.add(bProduct);
        }
    }

    private String getPol(String imgName) {
        imgName = imgName.toUpperCase();
        if (imgName.contains("RH")) {
            compactPolMode = true;
            return "RCH";
        } else if (imgName.contains("RV")) {
            compactPolMode = true;
            return "RCV";
        } else if (imgName.contains("HH")) {
            return "HH";
        } else if (imgName.contains("HV")) {
            return "HV";
        } else if (imgName.contains("VV")) {
            return "VV";
        } else if (imgName.contains("VH")) {
            return "VH";
        }
        return null;
    }

    @Override

    protected void addBands(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final int width, height;
        if (!bandProductList.isEmpty()) {
            final Product bandProduct = bandProductList.get(0);
            width = bandProduct.getSceneRasterWidth();
            height = bandProduct.getSceneRasterHeight();

            if (bandImageFileMap.isEmpty()) {
                if (isSLC()) {
                    for (Product bProduct : bandProductList) {
                        final String pol = getPol(bProduct.getName());

                        Band iBand = bProduct.getBandAt(0);
                        Band iTrgBand = ProductUtils.copyBand(iBand.getName(), bProduct,
                                iBand.getName() + "_" + pol, product, true);
                        iTrgBand.setUnit(Unit.REAL);
                        iTrgBand.setNoDataValue(0);
                        iTrgBand.setNoDataValueUsed(true);

                        Band qBand = bProduct.getBandAt(1);
                        Band qTrgBand = ProductUtils.copyBand(qBand.getName(), bProduct,
                                qBand.getName() + "_" + pol, product, true);
                        qTrgBand.setUnit(Unit.IMAGINARY);
                        qTrgBand.setNoDataValue(0);
                        qTrgBand.setNoDataValueUsed(true);

                        ReaderUtils.createVirtualIntensityBand(product, iTrgBand, qTrgBand, '_' + pol);
                    }
                } else {
                    for (Product bProduct : bandProductList) {
                        final String pol = getPol(bProduct.getName());
                        String bandName = "Amplitude_" + pol;
                        Band trgBand = ProductUtils.copyBand(bProduct.getBandAt(0).getName(), bProduct,
                                bandName, product, true);
                        trgBand.setUnit(Unit.AMPLITUDE);
                        trgBand.setNoDataValue(0);
                        trgBand.setNoDataValueUsed(true);

                        SARReader.createVirtualIntensityBand(product, trgBand, '_' + pol);
                    }
                }
            }

            // add metadata
            updateMetadataFromBandProduct(product, bandProduct);

            if (product.getSceneGeoCoding() == null && bandProduct.getSceneGeoCoding() != null &&
                    product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                bandProduct.transferGeoCodingTo(product, null);
                Dimension tileSize = bandProduct.getPreferredTileSize();
                if (tileSize == null) {
                    tileSize = ImageManager.getPreferredTileSize(bandProduct);
                }
                product.setPreferredTileSize(tileSize);
            }
        } else {
            width = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
            height = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);
        }

        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);

            for (int i = 0; i < img.getNumImages(); ++i) {

                if (isSLC()) {
                    boolean real = false;
                    String bandName;
                    String unit;
                    Band lastRealBand = null;

                    for (int b = 0; b < img.getNumBands(); ++b) {
                        final String pol = getPol(img.getName());
                        if (real) {
                            bandName = "i_" + pol;
                            unit = Unit.REAL;
                        } else {
                            bandName = "q_" + pol;
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, img.getDataType(), width, height);
                        band.setUnit(unit);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        if (real) {
                            lastRealBand = band;
                        } else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_' + pol);
                        }
                        real = !real;
                    }
                } else {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        final String pol = getPol(img.getName());
                        String bandName = "Amplitude_" + pol;
                        final Band band = new Band(bandName, ProductData.TYPE_UINT32, width, height);
                        band.setUnit(Unit.AMPLITUDE);
                        band.setNoDataValue(0);
                        band.setNoDataValueUsed(true);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        SARReader.createVirtualIntensityBand(product, band, '_' + pol);
                    }
                }
            }
        }
    }

    private void updateMetadataFromBandProduct(final Product product, final Product bandProduct) {
        final MetadataElement trgAbsMeta = AbstractMetadata.getAbstractedMetadata(product);
        if (compactPolMode) {
            trgAbsMeta.setAttributeInt(AbstractMetadata.polsarData, 1);
            trgAbsMeta.setAttributeString(AbstractMetadata.compact_mode, "Right Circular Hybrid Mode");
        }

        if(bandProduct.getProductReader() instanceof RisatCeosProductReader) {
            final MetadataElement trgOrigProdElem = AbstractMetadata.getOriginalProductMetadata(product);

            for (Product bProduct : bandProductList) {
                final String pol = getPol(bProduct.getName());
                MetadataElement polElem = AbstractMetadata.getOriginalProductMetadata(bProduct);
                polElem.setName(pol + "_Metadata");

                trgOrigProdElem.addElement(polElem);
            }

            final MetadataElement bandAbsMeta = AbstractMetadata.getAbstractedMetadata(bandProduct);

            MetadataElement osv = bandAbsMeta.getElement(AbstractMetadata.orbit_state_vectors).createDeepClone();
            trgAbsMeta.removeElement(trgAbsMeta.getElement(AbstractMetadata.orbit_state_vectors));
            trgAbsMeta.addElement(osv);

            MetadataElement srgr = bandAbsMeta.getElement(AbstractMetadata.srgr_coefficients).createDeepClone();
            trgAbsMeta.removeElement(trgAbsMeta.getElement(AbstractMetadata.srgr_coefficients));
            trgAbsMeta.addElement(srgr);

            trgAbsMeta.setAttributeDouble(AbstractMetadata.slant_range_to_first_pixel,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel));
            trgAbsMeta.setAttributeString(AbstractMetadata.SPH_DESCRIPTOR,
                    bandAbsMeta.getAttributeString(AbstractMetadata.SPH_DESCRIPTOR));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.PROC_TIME,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.PROC_TIME));
            trgAbsMeta.setAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME,
                    bandAbsMeta.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME));
            trgAbsMeta.setAttributeString(AbstractMetadata.algorithm,
                    bandAbsMeta.getAttributeString(AbstractMetadata.algorithm));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.pulse_repetition_frequency,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.pulse_repetition_frequency));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.radar_frequency,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.radar_frequency));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.range_sampling_rate,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.range_sampling_rate));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.range_bandwidth,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.range_bandwidth));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.azimuth_bandwidth,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.azimuth_bandwidth));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.range_looks,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.range_looks));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.azimuth_looks,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.azimuth_looks));


            trgAbsMeta.setAttributeDouble(AbstractMetadata.first_near_lat,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.first_near_lat));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.first_near_long,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.first_near_long));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.first_far_lat,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.first_far_lat));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.first_far_long,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.first_far_long));

            trgAbsMeta.setAttributeDouble(AbstractMetadata.last_near_lat,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.last_near_lat));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.last_near_long,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.last_near_long));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.last_far_lat,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.last_far_lat));
            trgAbsMeta.setAttributeDouble(AbstractMetadata.last_far_long,
                    bandAbsMeta.getAttributeDouble(AbstractMetadata.last_far_long));


            final MetadataElement origMeta = AbstractMetadata.getOriginalProductMetadata(bandProduct);
            //final MetadataElement imageGenerationParameters = origMeta.getElement("wah");
            //addSRGRCoefficients(trgAbsMeta, imageGenerationParameters);

            // for debugging
            //trgOrigProdElem.addElement(bandAbsMeta);
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement productElem = origProdRoot.getElement("ProductMetadata");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                productElem.getAttributeString("ProductType", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE,
                productElem.getAttributeString("ImagingMode", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,
                productElem.getAttributeString("SensorOrientation", defStr).toLowerCase());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.BEAMS,
                productElem.getAttributeString("NumberOfBeams", defStr));

//        final MetadataElement radarCenterFrequency = productElem.getElement("radarCenterFrequency");
//        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
//                                      radarCenterFrequency.getAttributeDouble("radarCenterFrequency", defInt) / Constants.oneMillion);

        final String pass = productElem.getAttributeString("Node", defStr).toUpperCase();
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, pass);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, productElem.getAttributeDouble("ImagingOrbitNo", defInt));

        productType = productElem.getAttributeString("ProductType", defStr);
        if (productType.contains("SLANT")) {
            setSLC(true);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, "COMPLEX");
        } else {
            setSLC(false);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, "DETECTED");
        }

        final String productId = productElem.getAttributeString("productId", defStr);
        final String beamMode = productElem.getAttributeString("beamModeMnemonic", defStr);
        final String passStr;
        if (pass.equals("ASCENDING")) {
            passStr = "ASC";
        } else {
            passStr = "DSC";
        }

        ProductData.UTC startTime, stopTime;
        if (flipToSARGeometry && pass.equals("ASCENDING")) {
            stopTime = getTime(productElem, "SceneStartTime", standardDateFormat);
            startTime = getTime(productElem, "SceneEndTime", standardDateFormat);
        } else {
            startTime = getTime(productElem, "SceneStartTime", standardDateFormat);
            stopTime = getTime(productElem, "SceneEndTime", standardDateFormat);
        }

        final DateFormat dateFormat = ProductData.UTC.createDateFormat("dd-MMM-yyyy_HH.mm");
        final Date date = startTime.getAsDate();
        final String dateString = dateFormat.format(date);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, 5350);

        productName = getMission() + '-' + productType + '-' + beamMode + '-' + passStr + '-' + dateString + '-' + productId;
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, productName);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, getMission());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                productElem.getAttributeString("processingFacility", defStr) + '-' +
                        productElem.getAttributeString("softwareVersion", defStr));

        //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
         //       ReaderUtils.getTime(productElem, "processingTime", standardDateFormat));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag,
                getFlag(productElem, "elevationPatternCorrection"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag,
                getFlag(productElem, "rangeSpreadingLossCorrection"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isSLC() ? 0 : 1);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                productElem.getAttributeDouble("RangeLooks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                productElem.getAttributeDouble("AzimuthLooks", defInt));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                productElem.getAttributeInt("NoScans", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                productElem.getAttributeInt("NoPixels", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                                      ReaderUtils.getLineTimeInterval(startTime, stopTime,
                                                                      absRoot.getAttributeInt(AbstractMetadata.num_output_lines)));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                productElem.getAttributeDouble("OutputPixelSpacing", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                productElem.getAttributeDouble("OutputLineSpacing", defInt));

        // polarizations
        getPolarizations(absRoot, productElem);
    }

    private static ProductData.UTC getTime(final MetadataElement elem, final String tag, final DateFormat timeFormat) {
        if (elem == null)
            return AbstractMetadata.NO_METADATA_UTC;
        final String timeStr = elem.getAttributeString(tag, " ").toUpperCase();
        return AbstractMetadata.parseUTC(timeStr, timeFormat);
    }

    private static int getFlag(final MetadataElement elem, String tag) {
        String valStr = elem.getAttributeString(tag, " ").toUpperCase();
        if (valStr.equals("FALSE") || valStr.equals("0"))
            return 0;
        else if (valStr.equals("TRUE") || valStr.equals("1"))
            return 1;
        return -1;
    }

    private String[] getPolarizations(final MetadataElement absRoot, final MetadataElement prodElem) {
        final List<String> pols = new ArrayList<>();
        int i = 0;
        String pol = prodElem.getAttributeString("TxRxPol1", null);
        if (pol != null) {
            pol = pol.toUpperCase();
            pols.add(getPol(pol));
            absRoot.setAttributeString(AbstractMetadata.polarTags[i], pol);
            ++i;
        }
        pol = prodElem.getAttributeString("TxRxPol2", null);
        if (pol != null) {
            pol = pol.toUpperCase();
            pols.add(getPol(pol));
            absRoot.setAttributeString(AbstractMetadata.polarTags[i], pol);
            ++i;
        }
        return pols.toArray(new String[0]);
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
    }

    @Override
    protected void addTiePointGrids(final Product product) {
        Angles angles = null;
        try {
            final String parentPath = getRelativePathToImageFolder();
            System.out.println(parentPath);

            final String[] listing = productDir.list(parentPath);
            for(String path : listing) {
                if(path.endsWith("_grid.txt")) {
                    angles = readGridFile(getFile(path));
                    break;
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to read tie point grids " + e.getMessage());
        }

        if(angles == null)
            return;


        final int sourceImageWidth = product.getSceneRasterWidth();
        final int sourceImageHeight = product.getSceneRasterHeight();
        final int gridWidth = angles.gridWidth;
        final int gridHeight = angles.gridHeight;
        final int subSamplingX = (int) ((float) sourceImageWidth / (float) (gridWidth - 1));
        final int subSamplingY = (int) ((float) sourceImageHeight / (float) (gridHeight - 1));

        final float[] incidenceAngleList = new float[gridWidth * gridHeight];
        int j = 0;
        for (float f : angles.incidenceAngle) {
            incidenceAngleList[j] = f;
            ++j;
        }

        final TiePointGrid incidentAngleGrid = new TiePointGrid(
                OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, incidenceAngleList);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);


        final float[] rangeTime = new float[gridWidth * gridHeight];
        j = 0;
        for (float f : angles.slantRange) {
            rangeTime[j] = f;
            ++j;
        }

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, rangeTime);

        product.addTiePointGrid(slantRangeGrid);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
    }

    private Angles readGridFile(final File file) throws Exception {
        final List<String> fileContent = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        final Angles angles = new Angles();

        for (final String line : fileContent) {
            if (line.startsWith("#")) {
                if (line.contains("Records in Grid")) {
                    angles.gridHeight = Integer.valueOf(line.substring(line.lastIndexOf(":") + 1).trim());
                }
                if (line.contains("Samples in Grid")) {
                    angles.gridWidth = Integer.valueOf(line.substring(line.lastIndexOf(":") + 1).trim());
                }
                if (line.contains("Scan Direction")) {
                    angles.yInterval = Integer.valueOf(line.substring(line.lastIndexOf(":") + 1).trim());
                }
                if (line.contains("Pix Direction")) {
                    angles.xInterval = Integer.valueOf(line.substring(line.lastIndexOf(":") + 1).trim());
                }
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line);
            String lat = tokenizer.nextToken();
            String lon = tokenizer.nextToken();
            String range = tokenizer.nextToken();
            String incidence = tokenizer.nextToken();

            angles.slantRange.add(Float.valueOf(range) * 10f);
            angles.incidenceAngle.add(Float.valueOf(incidence));
        }

        return angles;
    }

    private static class Angles {
        int gridWidth, gridHeight;
        int xInterval, yInterval;
        final List<Float> slantRange = new ArrayList<>();
        final List<Float> incidenceAngle = new ArrayList<>();
    }

    private static String getMission() {
        return "RISAT1";
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
