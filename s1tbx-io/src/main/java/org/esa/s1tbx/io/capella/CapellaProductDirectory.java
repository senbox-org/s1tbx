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
package org.esa.s1tbx.io.capella;

import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.JSONProductDirectory;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Map;

/**
 * This class represents a product directory.
 */
public class CapellaProductDirectory extends JSONProductDirectory {

    private int width, height;
    private final String productName;
    private String pol;
    private double scaleFactor;
    private Product bandProduct;

    private static final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static Double NoDataValue = 65535.0;

    public CapellaProductDirectory(final File inputFile) {
        super(inputFile);

        productName = inputFile.getName().replace("_extended.json", "");
    }

    @Override
    public void close() throws IOException  {
        super.close();
        if(bandProduct != null) {
            bandProduct.dispose();
        }
    }

    protected void addAbstractedMetadataHeader(final MetadataElement root) {
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        final MetadataElement productMetadata = origProdRoot.getElement("ProductMetadata");
        final MetadataElement collect = productMetadata.getElement("collect");
        setSLC(getProductType().equals("SLC"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "Capella");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, getProductDescription());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getProductType().equals("SLC") ? "COMPLEX" : "DETECTED");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME, ReaderUtils.getTime(productMetadata, "processing_time", standardDateFormat));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, collect.getAttributeString("mode"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, "--");

        final ProductData.UTC startTime = ReaderUtils.getTime(collect, "start_timestamp", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(collect, "stop_timestamp", standardDateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, stopTime, height));

        final MetadataElement image = collect.getElement("image");

        width = image.getAttributeInt("columns");
        height = image.getAttributeInt("rows");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, width);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, height);
        //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, image.getAttributeDouble("pixel_spacing_column"));
        //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, image.getAttributeDouble("pixel_spacing_row"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, image.getAttributeDouble("range_resolution"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, image.getAttributeDouble("azimuth_resolution"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, image.getAttributeDouble("range_looks"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, image.getAttributeDouble("azimuth_looks"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm, image.getAttributeString("algorithm"));

        final String radiometry = image.getAttributeString("radiometry");
        scaleFactor = image.getAttributeDouble("scale_factor");
        if (radiometry.equals("sigma_nought")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 1);
        }

        final MetadataElement radar = collect.getElement("radar");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, radar.getAttributeDouble("center_frequency") / Constants.oneMillion);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, radar.getAttributeString("pointing"));

        final MetadataElement prfElem = radar.getElement("prf");
        final MetadataElement prf = prfElem.getElement("prf");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, prf.getAttributeDouble("prf"));

        final String transmit_polarization = radar.getAttributeString("transmit_polarization");
        final String receive_polarization = radar.getAttributeString("receive_polarization");
        pol = (transmit_polarization + receive_polarization).toUpperCase();
    }

    @Override
    protected String getProductName() {
        return productName;
    }

    @Override
    protected String getProductType() {
        return productName.contains("SLC") ? "SLC" : "GEO";
    }

    @Override
    protected String getProductDescription() {
        return getProductType().equals("SLC") ? "Single Look Complex" : "GEO";
    }

    double getScaleFactor() {
        return scaleFactor;
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) {
        final String name = getBandFileNameFromImage(imgPath);
        if ((name.endsWith("tif")) && name.startsWith(productName) && !name.contains("preview")) {
            try {
                final Dimension bandDimensions = new Dimension(width, height);
                final InputStream inStream = getInputStream(imgPath);
                if (inStream.available() > 0) {
                    final ImageInputStream imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);

                    final ImageIOFile img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 1, ProductData.TYPE_INT32, productInputFile);
                    bandImageFileMap.put(img.getName(), img);

                    ProductReader reader = geoTiffPlugIn.createReaderInstance();
                    bandProduct = reader.readProductNodes(productDir.getFile(imgPath), null);
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(imgPath + " not found");
            }
        }
    }

    @Override
    protected void addBands(final Product product) {

        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            int numImages = img.getNumImages();

            String suffix = pol;
            if (isSLC()) {
                numImages *= 2; // real + imaginary
            }

            String bandName;
            boolean real = true;
            Band lastRealBand = null;
            for (int i = 0; i < numImages; ++i) {

                if (isSLC()) {
                    String unit;

                    for (int b = 0; b < img.getNumBands(); ++b) {
                        if (real) {
                            bandName = "i" + '_' + suffix;
                            unit = Unit.REAL;
                        } else {
                            bandName = "q" + '_' + suffix;
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(unit);
                        band.setNoDataValueUsed(true);
                        band.setNoDataValue(NoDataValue);

                        product.addBand(band);
                        final ImageIOFile.BandInfo bandInfo = new ImageIOFile.BandInfo(band, img, 0, b);
                        bandMap.put(band, bandInfo);

                        if (real) {
                            lastRealBand = band;
                        } else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_' + suffix);
                            bandInfo.setRealBand(lastRealBand);
                            bandMap.get(lastRealBand).setImaginaryBand(band);
                        }
                        real = !real;

                        // reset to null so it doesn't adopt a geocoding from the bands
                        product.setSceneGeoCoding(null);
                    }
                } else {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude" + '_' + suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_INT32, width, height);
                        band.setUnit(Unit.AMPLITUDE);
                        band.setNoDataValueUsed(true);
                        band.setNoDataValue(NoDataValue);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        SARReader.createVirtualIntensityBand(product, band, '_' + suffix);

                        // reset to null so it doesn't adopt a geocoding from the bands
                        product.setSceneGeoCoding(null);
                    }
                }
            }
        }
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement orbitList) {
        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);

        final MetadataElement[] stateVectorElems = orbitList.getElements();
        for (int i = 1; i <= stateVectorElems.length; ++i) {
            addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, stateVectorElems[i - 1], i);
        }

        // set state vector time
        if (absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.NO_METADATA_UTC).
                equalElems(AbstractMetadata.NO_METADATA_UTC)) {

            //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
            //                              ReaderUtils.getTime(stateVectorElems[0], "time", sentinelDateFormat));
        }
    }

    private void addVector(final String name, final MetadataElement orbitVectorListElem,
                           final MetadataElement orbitElem, final int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        final MetadataElement positionElem = orbitElem.getElement("position");
        final MetadataElement velocityElem = orbitElem.getElement("velocity");

        //orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
        //                                ReaderUtils.getTime(orbitElem, "time", sentinelDateFormat));

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                positionElem.getAttributeDouble("x", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                positionElem.getAttributeDouble("y", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                positionElem.getAttributeDouble("z", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                velocityElem.getAttributeDouble("x", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                velocityElem.getAttributeDouble("y", 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                velocityElem.getAttributeDouble("z", 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    @Override
    protected void addGeoCoding(final Product product) {
        ProductUtils.copyGeoCoding(bandProduct, product);
    }

    @Override
    protected void addTiePointGrids(final Product product) {
        // replaced by call to addTiePointGrids(band)
    }

    private static void setLatLongMetadata(Product product, TiePointGrid latGrid, TiePointGrid lonGrid) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latGrid.getPixelDouble(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonGrid.getPixelDouble(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latGrid.getPixelDouble(w, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonGrid.getPixelDouble(w, 0));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latGrid.getPixelDouble(0, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonGrid.getPixelDouble(0, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latGrid.getPixelDouble(w, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonGrid.getPixelDouble(w, h));
    }

    @Override
    public Product createProduct() throws IOException {

        final MetadataElement newRoot = addMetaData();
        findImages(newRoot);

        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        final int sceneWidth = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int sceneHeight = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);

        final Product product = new Product(getProductName(), getProductType(), sceneWidth, sceneHeight);
        updateProduct(product, newRoot);

        addBands(product);
        addGeoCoding(product);

        ReaderUtils.addMetadataIncidenceAngles(product);
        ReaderUtils.addMetadataProductSize(product);

        return product;
    }
}
