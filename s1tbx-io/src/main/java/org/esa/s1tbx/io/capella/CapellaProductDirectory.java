/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Map;

import static org.esa.s1tbx.io.sentinel1.Sentinel1Directory.sentinelDateFormat;

/**
 * This class represents a product directory.
 */
public class CapellaProductDirectory extends JSONProductDirectory {

    private int width, height;
    private final String productName;
    private String pol;
    private double scaleFactor;
    private String calibration = null;
    private Product bandProduct;

    private static final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

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
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, productMetadata.getAttributeString("software_version"));

        final MetadataElement image = collect.getElement("image");
        final MetadataElement imageGeometry = image.getElement("image_geometry");

        width = image.getAttributeInt("columns");
        height = image.getAttributeInt("rows");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, width);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, height);
        if (imageGeometry.getAttribute("delta_range_sample") != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, imageGeometry.getAttributeDouble("delta_range_sample"));
        } else {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, image.getAttributeDouble("pixel_spacing_column"));
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, image.getAttributeDouble("pixel_spacing_row"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, image.getAttributeDouble("range_looks"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, image.getAttributeDouble("azimuth_looks"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm, image.getAttributeString("algorithm"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.bistatic_correction_applied, 1);

        final String radiometry = image.getAttributeString("radiometry");
        scaleFactor = image.getAttributeDouble("scale_factor");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor, scaleFactor);
        if (radiometry.contains("nought")) {
            calibration = radiometry.contains("gamma") ? "Gamma0" : radiometry.contains("sigma") ? "Sigma0" : "Beta0";
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 1);
        }

        if(isSLC()) {
            if(imageGeometry.containsAttribute("range_to_first_sample")) {
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                        imageGeometry.getAttributeDouble("range_to_first_sample"));
            }
        }

        final ProductData.UTC firstLineTime;
        if(imageGeometry.containsAttribute("first_line_time")) {
            firstLineTime = ReaderUtils.getTime(imageGeometry, "first_line_time", standardDateFormat);
        } else {
            firstLineTime = ReaderUtils.getTime(collect, "start_timestamp", standardDateFormat);
        }

        //double delta_line_time = imageGeometry.getAttributeDouble("delta_line_time");

        final MetadataElement centerPixel = image.getElement("center_pixel");
        final ProductData.UTC centerTime = ReaderUtils.getTime(centerPixel, "center_time", standardDateFormat);

        final double firstTime = firstLineTime.getMJD() * 24.0 * 3600.0;
        final double midTime = centerTime.getMJD() * 24.0 * 3600.0;
        final double imageDuration = (midTime - firstTime) * 2.0;
        //final double deltaTime = (height - 1) * delta_line_time;
        //final double lastTime = firstTime + deltaTime;
        final double lastTime = firstTime + imageDuration;
        final ProductData.UTC lastLineTime = new ProductData.UTC(lastTime / 3600.0 / 24.0);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, firstLineTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, lastLineTime);
        //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, delta_line_time);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(firstLineTime, lastLineTime, height));

        final MetadataElement state = collect.getElement("state");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, state.getAttributeString("direction", "unknown").toUpperCase());

        final MetadataElement radar = collect.getElement("radar");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, radar.getAttributeDouble("center_frequency") / Constants.oneMillion);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, radar.getAttributeString("pointing"));

        final MetadataElement prfElem = radar.getElement("prf");
        final MetadataElement prf = prfElem.getElement("prf");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, prf.getAttributeDouble("prf"));

        final String transmit_polarization = radar.getAttributeString("transmit_polarization");
        final String receive_polarization = radar.getAttributeString("receive_polarization");
        pol = (transmit_polarization + receive_polarization).toUpperCase();
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, pol);

        addOrbitStateVectors(absRoot, state.getElement("state_vectors"));
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
                ImageInputStream imgStream = null;
                if(!productDir.isCompressed()) {
                    File file = productDir.getFile(imgPath);
                    imgStream = new FileImageInputStream(file);
                } else {
                    final Dimension bandDimensions = new Dimension(width, height);
                    final InputStream inStream = getInputStream(imgPath);
                    if (inStream.available() > 0) {
                        imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);
                    }
                }
                if(imgStream != null) {
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

    private static String getProcessorVersion(final MetadataElement absRoot) {
        String processingVersion = absRoot.getAttributeString(AbstractMetadata.ProcessingSystemIdentifier);
        if(processingVersion != null) {
            processingVersion =  processingVersion.replace("v","");
        }
        return processingVersion;
    }

    @Override
    protected void addBands(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        double NoDataValue = 0;
        if(!isSLC() && "1.6.20".compareToIgnoreCase(getProcessorVersion(absRoot)) > 0) {
            NoDataValue = bandProduct.getBandAt(0).getSampleFloat(0,0);
        }

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
                        bandName = calibration != null ? calibration : "Amplitude";
                        bandName += '_' + suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                        band.setUnit(calibration != null ? Unit.INTENSITY : Unit.AMPLITUDE);
                        band.setNoDataValueUsed(true);
                        band.setNoDataValue(NoDataValue);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));

                        if(calibration == null) {
                            SARReader.createVirtualIntensityBand(product, band, '_' + suffix);
                        }

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

            DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                                          ReaderUtils.getTime(stateVectorElems[0], "time", dateFormat));
        }
    }

    private void addVector(final String name, final MetadataElement orbitVectorListElem,
                           final MetadataElement orbitElem, final int num) {
        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        final MetadataElement positionElem = orbitElem.getElement("position");
        final MetadataElement velocityElem = orbitElem.getElement("velocity");

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time,
                                        ReaderUtils.getTime(orbitElem, "time", sentinelDateFormat));

        String xPosName = positionElem.containsAttribute("x") ? "x" : "position1";
        String yPosName = positionElem.containsAttribute("y") ? "y" : "position2";
        String zPosName = positionElem.containsAttribute("z") ? "z" : "position3";
        String xVelName = positionElem.containsAttribute("x") ? "x" : "velocity1";
        String yVelName = positionElem.containsAttribute("y") ? "y" : "velocity2";
        String zVelName = positionElem.containsAttribute("z") ? "z" : "velocity3";

        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos,
                positionElem.getAttributeDouble(xPosName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos,
                positionElem.getAttributeDouble(yPosName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos,
                positionElem.getAttributeDouble(zPosName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel,
                velocityElem.getAttributeDouble(xVelName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel,
                velocityElem.getAttributeDouble(yVelName, 0));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel,
                velocityElem.getAttributeDouble(zVelName, 0));

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    @Override
    protected void addGeoCoding(final Product product) {
        if(bandProduct != null) {
            ProductUtils.copyGeoCoding(bandProduct, product);
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

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement productMetadata = origProdRoot.getElement("ProductMetadata");
        final MetadataElement collect = productMetadata.getElement("collect");
        final MetadataElement image = collect.getElement("image");
        final MetadataElement centerPixel = image.getElement("center_pixel");
        final double incidenceAngle = centerPixel.getAttributeDouble("incidence_angle");

        final double[] incidenceCorners = new double[] { incidenceAngle,incidenceAngle,incidenceAngle,incidenceAngle};

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
}
