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
package org.esa.s1tbx.io.seasat;

import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Set;

/**
 * This class represents a product directory.
 */
public class SeaSatProductDirectory extends XMLProductDirectory {

    private String productType = "SeaSat";
    private String productDescription = "SeaSat Product";
    private File imageFile;

    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public SeaSatProductDirectory(final File headerFile) {
        super(headerFile);
    }

    protected String getHeaderFileName() {
        if (ZipUtils.isZip(productInputFile)) {
            try {
                String[] files = listFiles("");
                for (String file : files) {
                    if (file.startsWith("SS_") && file.endsWith(".xml") && !file.endsWith(".iso.xml")) {
                        return file;
                    }
                }
            } catch (Exception e) {

            }
        }
        return productInputFile.getName();
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) throws IOException {
        final String name = getBandFileNameFromImage(imgPath);
        if (name.endsWith("tif")) {
            final Dimension bandDimensions = getBandDimensions(newRoot, name);
            final InputStream inStream = getInputStream(imgPath);
            if(inStream.available() > 0) {
                final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);
                if (imgStream == null)
                    throw new IOException("Unable to open " + imgPath);

                final ImageIOFile img;
                if (isSLC()) {
                    img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 2, ProductData.TYPE_FLOAT32, productInputFile);
                } else {
                    img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream), productInputFile);
                }
                bandImageFileMap.put(img.getName(), img);
                imageFile = getFile(imgPath);
            }
        }
    }

    @Override
    protected void addBands(final Product product) {

        String bandName;
        boolean real = true;
        Band lastRealBand = null;
        String unit;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final int width = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int height = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);

        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);

            for (int i = 0; i < img.getNumImages(); ++i) {

                if (isSLC()) {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        if (real) {
                            bandName = "i_" + getPol();
                            unit = Unit.REAL;
                        } else {
                            bandName = "q_" + getPol();
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, img.getDataType(), width, height);
                        band.setUnit(unit);
                        band.setNoDataValue(0);
                        band.setNoDataValueUsed(true);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        if (real) {
                            lastRealBand = band;
                        } else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_' + getPol());
                        }
                        real = !real;
                    }
                } else {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude_" + getPol();
                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(Unit.AMPLITUDE);
                        band.setNoDataValue(0);
                        band.setNoDataValueUsed(true);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        SARReader.createVirtualIntensityBand(product, band, '_' + getPol());
                    }
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

        final MetadataElement level1Product = origProdRoot.getElement("level1Product");
        final MetadataElement generalHeader = level1Product.getElement("generalHeader");
        productDescription = generalHeader.getAttributeString("itemName");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "SeaSat");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getBandFileNameFromImage(getHeaderFileName()));

        final MetadataElement productInfo = level1Product.getElement("productInfo");

        final MetadataElement generationInfo = productInfo.getElement("generationInfo");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, generationInfo.getAttributeString("level1ProcessingFacility"));

        final MetadataElement missionInfo = productInfo.getElement("missionInfo");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, missionInfo.getAttributeInt("absOrbit"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT, missionInfo.getAttributeInt("relOrbit"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.CYCLE, missionInfo.getAttributeInt("orbitCycle"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, missionInfo.getAttributeString("orbitDirection"));

        final MetadataElement acquisitionInfo = productInfo.getElement("acquisitionInfo");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, productDescription);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, acquisitionInfo.getAttributeString("imagingMode"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, acquisitionInfo.getAttributeString("lookDirection").toLowerCase());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, getPol());

        final MetadataElement productVariantInfo = productInfo.getElement("productVariantInfo");
        productType = productVariantInfo.getAttributeString("productVariant");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);

        final MetadataElement imageDataInfo = productInfo.getElement("imageDataInfo");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, imageDataInfo.getAttributeString("imageDataType"));

        final MetadataElement imageRaster = imageDataInfo.getElement("imageRaster");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, imageRaster.getAttributeInt("numberOfRows"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, imageRaster.getAttributeInt("numberOfColumns"));

        final MetadataElement rowSpacing = imageRaster.getElement("rowSpacing");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, rowSpacing.getAttributeDouble("rowSpacing"));

        final MetadataElement groundRangeResolution = imageRaster.getElement("groundRangeResolution");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, groundRangeResolution.getAttributeDouble("groundRangeResolution"));
        final MetadataElement azimuthResolution = imageRaster.getElement("azimuthResolution");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, azimuthResolution.getAttributeDouble("azimuthResolution"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, imageRaster.getAttributeDouble("azimuthLooks"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, imageRaster.getAttributeDouble("rangeLooks"));

        final MetadataElement sceneInfo = productInfo.getElement("sceneInfo");
        final MetadataElement start = sceneInfo.getElement("start");
        final MetadataElement stop = sceneInfo.getElement("stop");
        final ProductData.UTC startTime = ReaderUtils.getTime(start, "timeUTC", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(stop, "timeUTC", standardDateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

        final MetadataElement setup = level1Product.getElement("setup");
        final MetadataElement orderInfo = setup.getElement("orderInfo");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.orbit_state_vector_file, orderInfo.getAttributeString("orbitAccuracy"));

        final MetadataElement instrument = level1Product.getElement("instrument");
        final MetadataElement radarParameters = instrument.getElement("radarParameters");
        final MetadataElement centerFrequency = radarParameters.getElement("centerFrequency");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, centerFrequency.getAttributeDouble("centerFrequency"));

        final MetadataElement productSpecific = level1Product.getElement("productSpecific");
        final MetadataElement complexImageInfo = productSpecific.getElement("complexImageInfo");
        if(complexImageInfo != null) {
            final MetadataElement commonPRF = complexImageInfo.getElement("commonPRF");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, commonPRF.getAttributeDouble("commonPRF"));
        }

        final MetadataElement platform = level1Product.getElement("platform");
        final MetadataElement orbit = platform.getElement("orbit");
        addOrbitStateVectors(absRoot, orbit);

        final MetadataElement processing = level1Product.getElement("processing");
        final MetadataElement doppler = processing.getElement("doppler");
        addDopplerCentroidCoefficients(absRoot, doppler.getElement("dopplerCentroid"));
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement orbit) {
        final MetadataElement orbitHeader = orbit.getElement("orbitHeader");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                                      ReaderUtils.getTime(orbitHeader, "firstStateTimeUTC", standardDateFormat));

        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        int i = 1;
        final MetadataElement[] elems = orbit.getElements();
        for(MetadataElement stateVec : elems) {
            if(stateVec.getName().equals("stateVec")) {
                addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVec, i++);
            }
        }
    }

    private void addVector(String name, MetadataElement orbitVectorListElem,
                                  MetadataElement srcElem, int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                                        ReaderUtils.getTime(srcElem, "timeUTC", standardDateFormat));

        final MetadataElement xpos = srcElem.getElement("posX");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                                           xpos.getAttributeDouble("posX", 0));
        final MetadataElement ypos = srcElem.getElement("posY");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                                           ypos.getAttributeDouble("posY", 0));
        final MetadataElement zpos = srcElem.getElement("posZ");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                                           zpos.getAttributeDouble("posZ", 0));
        final MetadataElement xvel = srcElem.getElement("velX");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                                           xvel.getAttributeDouble("velX", 0));
        final MetadataElement yvel = srcElem.getElement("velY");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                                           yvel.getAttributeDouble("velY", 0));
        final MetadataElement zvel = srcElem.getElement("velZ");
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                                           zvel.getAttributeDouble("velZ", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    private void addDopplerCentroidCoefficients(final MetadataElement absRoot, final MetadataElement dopplerCentroid) {
        if (dopplerCentroid == null) return;

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

        int listCnt = 1;
        for (MetadataElement elem : dopplerCentroid.getElements()) {
            final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + '.' + listCnt);
            dopplerCentroidCoefficientsElem.addElement(dopplerListElem);
            ++listCnt;

            final ProductData.UTC utcTime = ReaderUtils.getTime(elem, "timeUTC", dateFormat);
            dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

//            final double refTime = elem.getAttributeDouble("t0", 0) * 1e9; // s to ns
//            AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
//                    ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
//            AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, refTime);

            final MetadataElement basebandDoppler = elem.getElement("basebandDoppler");
            int cnt = 1;
            for (MetadataElement coef : basebandDoppler.getElements()) {
                if(coef.getName().equals("coefficient")) {

                    final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
                    dopplerListElem.addElement(coefElem);
                    ++cnt;
                    AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                                ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
                    AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coef.getAttributeDouble("coefficient"));
                }
            }
        }
    }

    @Override
    protected void addGeoCoding(final Product product) {
        try {
            Product imageProduct = ProductIO.readProduct(imageFile);
            if (imageProduct != null) {
                product.setSceneGeoCoding(imageProduct.getSceneGeoCoding());
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    @Override
    protected void addTiePointGrids(final Product product) {

    }

    private static String getMission() {
        return "SeaSat";
    }

    @Override
    protected String getProductName() {
        return getHeaderFileName();
    }

    @Override
    protected String getProductDescription() {
        return productDescription;
    }

    @Override
    protected String getProductType() {
        return productType;
    }

    private static String getPol() {
        return "HH";
    }
}
