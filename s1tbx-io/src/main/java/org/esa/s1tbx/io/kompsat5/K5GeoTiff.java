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
package org.esa.s1tbx.io.kompsat5;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by luis on 12/08/2016.
 */
public class K5GeoTiff implements K5Format {
    private final Kompsat5Reader reader;
    private final ProductReaderPlugIn readerPlugIn;
    private Product product;
    private boolean isComplex = false;
    private final ProductReader geoTiffReader;
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public K5GeoTiff(final ProductReaderPlugIn readerPlugIn, final Kompsat5Reader reader) {
        this.readerPlugIn = readerPlugIn;
        this.reader = reader;

        geoTiffReader = ProductIO.getProductReader("GeoTiff");
    }

    public Product open(final File inputFile) throws IOException {
        // change addExtraBands to setProduct
//      addExtraBands(inputFile, product);
        setProduct(inputFile);
        product.setFileLocation(inputFile);
//      addAuxXML(product);
        // add addMetadataToProduct
        addMetadataToProduct();
        addAbstractedMetadataHeader(product, product.getMetadataRoot());
        // add Tie-Point Grids
        addGeocodingFromMetadata(product);
        addIncidenceAnglesSlantRangeTime(product);
        product.setStartTime(getStartTime(product));
        product.setEndTime(getEndTime(product));

        return product;
    }

    // change method : addExtraBands to setProduct
    private void setProduct(final File inputFile) throws IOException {
        final HashMap<String, Band[]> polarizationGroupedBands = new HashMap<>();
        final File[] files = inputFile.getParentFile().listFiles();
        if (files != null) {
            for (final File file : files) {
                final String name = file.getName().toUpperCase();
                if (name.endsWith(".XML")) {
                    product.setName(name.toUpperCase().replace("_AUX.XML", ""));
                }
                if (name.contains("I_SCS") || name.contains("I_01_SCS")) {
                    final String polarization = getPolarization(name);
                    product = geoTiffReader.readProductNodes(file, null);

                    final Band bandI = product.getBandAt(0);
                    final String bname = "i_" + polarization;

                    polarizationGroupedBands.put(polarization, new Band[]{bandI, null, null});

                    bandI.setName(bname);
                    bandI.setNoDataValue(0);
                    bandI.setUnit(Unit.REAL);

                    for (File f : files) {
                        final String fname = f.getName().toUpperCase();
                        if (fname.contains("Q_SCS") || fname.contains("Q_01_SCS")) {
                            final Product productQ = ProductIO.readProduct(f, "GeoTiff");
                            final Band bandQ = productQ.getBandAt(0);
                            String bnameQ = "q_" + polarization;
                            bandQ.setName(bnameQ);
                            bandQ.setNoDataValue(0);
                            bandQ.setUnit(Unit.IMAGINARY);
                            product.addBand(bandQ);

                            Band[] band = polarizationGroupedBands.get(polarization);
                            band[1] = bandQ;
                            polarizationGroupedBands.put(polarization, band);
                        }
                    }
                    for (String p : polarizationGroupedBands.keySet()) {
                        Band[] bandGroup = polarizationGroupedBands.get(p);
                        bandGroup[0].setNoDataValue(0);
                        bandGroup[1].setNoDataValue(0);

                        ReaderUtils.createVirtualIntensityBand(product, bandGroup[0], bandGroup[1], p);
                    }
                } else if (name.endsWith("TIF") & !name.contains("GIM") & (name.contains("_GEC_") || name.contains("_GTC_") || name.contains("_WEC_") || name.contains("_WTC_"))) {
                    final String polarization = getPolarization(name);
                    product = ProductIO.readProduct(file, "GeoTiff");
                    final Band band = product.getBandAt(0);

                    band.setName("Amplitude_" + polarization);
                    band.setNoDataValueUsed(true);
                    band.setNoDataValue(0);
                    band.setUnit(Unit.AMPLITUDE);
                    SARReader.createVirtualIntensityBand(product, band, "_" + polarization);
                }
            }
        }
    }

    private void addMetadataToProduct() {
        try {
            //addAuxXML(product);

            final File folder = product.getFileLocation().getParentFile();
            File dnFile = null;
            final File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    final String name = f.getName().toLowerCase();
                    if (name.endsWith("_aux.xml")) {
                        dnFile = f;
                        break;
                    }
                }
            }
            if (dnFile != null) {
                final Document xmlDoc = XMLSupport.LoadXML(dnFile.getAbsolutePath());
                final Element rootElement = xmlDoc.getRootElement();

                // Add Original_Product_Metadata
//              AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.getOriginalProductMetadata(product));
                AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot()));
            }

//          final MetadataElement origMetadataRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
            final MetadataElement origMetadataRoot = AbstractMetadata.getOriginalProductMetadata(product);
            final MetadataElement bandElem = getBandElement();

            origMetadataRoot.addElement(bandElem);
//          origMetadataRoot.addElement(gim);

            // delete tiff metadata
            for (MetadataElement meta : product.getMetadataRoot().getElements()) {
                final String metadata = meta.getName().toUpperCase();
                if (metadata.contains("TIFF")) {
                    product.getMetadataRoot().removeElement(meta);
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Error reading metadata for " + product.getName() + ": " + e.getMessage());
        }

    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement aux = AbstractMetadata.getOriginalProductMetadata(product).getElement("Auxiliary");
        final MetadataElement globalElem = aux.getElement("Root");
        final MetadataElement subswathElem = globalElem.getElement("SubSwaths").getElement("SubSwath");
        // add SBI or MBI Element
        final MetadataElement bandElem = getBandElement();

//      final String defStr = AbstractMetadata.NO_METADATA_STRING;
//      final int defInt = AbstractMetadata.NO_METADATA;

        // del default value
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, globalElem.getAttributeString("ProductFilename"));
        final String productType = globalElem.getAttributeString("ProductType");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        final String mode = globalElem.getAttributeString("AcquisitionMode");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, mode);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, mode);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "Kompsat5");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(globalElem, "ProductGenerationUTC", standardDateFormat));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                globalElem.getAttributeString("ProcessingCentre"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,
                globalElem.getAttributeString("LookSide").toLowerCase());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, globalElem.getAttributeInt("OrbitNumber"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, globalElem.getAttributeString("OrbitDirection"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType(globalElem));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                globalElem.getAttributeDouble("RadarFrequency") / Constants.oneMillion);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                globalElem.getAttributeString("FocusingAlgorithmID"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system,
                globalElem.getAttributeString("EllipsoidDesignator"));

        // mod range_spacing, azimuth_spacing value
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                bandElem.getAttributeDouble("ColumnSpacing"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                bandElem.getAttributeDouble("LineSpacing"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                subswathElem.getAttributeDouble("GroundRangeInstrumentGeometricResolution"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                subswathElem.getAttributeDouble("AzimuthInstrumentGeometricResolution"));

        if (productType.contains("GEC")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection,
                    globalElem.getAttributeString("ProjectionID"));
        }

        final String rngSpreadComp = globalElem.getAttributeString(
                "RangeSpreadingLossCompensationGeometry");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, rngSpreadComp.equals("NONE") ? 0 : 1);

        final String incAngComp = globalElem.getAttributeString(
                "IncidenceAngleCompensationGeometry");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, incAngComp.equals("NONE") ? 0 : 1);

        final String antElevComp = globalElem.getAttributeString(
                "RangeAntennaPatternCompensationGeometry");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, antElevComp.equals("NONE") ? 0 : 1);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle,
                globalElem.getAttributeDouble("ReferenceIncidenceAngle"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range,
                globalElem.getAttributeDouble("ReferenceSlantRange"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp,
                globalElem.getAttributeDouble("ReferenceSlantRangeExponent"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor,
                globalElem.getAttributeDouble("RescalingFactor"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isComplex ? 0 : 1);

        final MetadataElement Subswaths = globalElem.getElement("Subswaths");
        final MetadataElement s01Elem = globalElem.getElement("S01");
        if (s01Elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    s01Elem.getAttributeDouble("PRF"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    s01Elem.getAttributeDouble("SamplingRate") / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    s01Elem.getAttributeString("Polarisation"));

            // add Range and Azimuth bandwidth
            final double rangeBW = s01Elem.getAttributeDouble("RangeFocusingBandwidth"); // Hz
            final double azimuthBW = s01Elem.getAttributeDouble("AzimuthFocusingBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);

            // Calibration constant read from Global_Metadata during calibration initialization
        } else if (Subswaths != null) {
            final MetadataElement SubSwath = Subswaths.getElement("SubSwath");

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    SubSwath.getAttributeDouble("PRF"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    SubSwath.getAttributeDouble("SamplingRate") / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    SubSwath.getAttributeString("Polarisation"));

            // add Range and Azimuth bandwidth
            final double rangeBW = SubSwath.getAttributeDouble("RangeFocusingBandwidth"); // Hz
            final double azimuthBW = SubSwath.getAttributeDouble("AzimuthFocusingBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        } else {
            final String prefix = "S01_";
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    globalElem.getAttributeDouble(prefix + "PRF"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    globalElem.getAttributeDouble(prefix + "SamplingRate") / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    globalElem.getAttributeString(prefix + "Polarisation"));

            // add Range and Azimuth bandwidth
            final double rangeBW = globalElem.getAttributeDouble(prefix + "RangeFocusingBandwidth"); // Hz
            final double azimuthBW = globalElem.getAttributeDouble(prefix + "AzimuthFocusingBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        }

        addSlantRangeToFirstPixel();
        addFirstLastLineTimes(product);
        addOrbitStateVectors(absRoot, globalElem);
        addSRGRCoefficients(absRoot, globalElem);
        addDopplerCentroidCoefficients(absRoot, globalElem);
    }

    private String getSampleType(final MetadataElement globalElem) {
        if (globalElem.getAttributeInt("SamplesperPixel", 0) > 1) {
            isComplex = true;
            return "COMPLEX";
        }
        isComplex = false;
        return "DETECTED";
    }

    private String getTime(final Product product, final String tag) {
        final MetadataElement m = AbstractMetadata.getOriginalProductMetadata(product);
        MetadataElement eAux = m.getElement("Auxiliary");
        MetadataElement eRoot = eAux.getElement("Root");

        return eRoot.getAttributeString(tag);
    }

    private ProductData.UTC getStartTime(final Product product) {
        final String startTime = getTime(product, "DownlinkStartUTC");
        try {
            return ProductData.UTC.parse(startTime, "yyyy-MM-dd HH:mm:ss");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ProductData.UTC getEndTime(final Product product) {
        final String endTime = getTime(product, "DownlinkStopUTC");
        try {
            return ProductData.UTC.parse(endTime, "yyyy-MM-dd HH:mm:ss");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getPolarization(String fname) {
        for (String p : new String[]{"HH", "HV", "VH", "VV"}) {
            if (fname.contains(p)) {
                return p;
            }
        }
        return null;
    }

    private void addSlantRangeToFirstPixel() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement bandElem = AbstractMetadata.getOriginalProductMetadata(product).getElement("SBI");
        if (bandElem != null) {
            final double slantRangeTime = bandElem.getAttributeDouble("ZeroDopplerRangeFirstTime"); //s
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                    slantRangeTime * Constants.halfLightSpeed);
        }
    }

    private void addFirstLastLineTimes(final Product product) {
        final int rasterHeight = product.getSceneRasterHeight();
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement globalElem = root.getElement("Auxiliary").getElement("root");
        final MetadataElement bandElem = root.getElement("SBI");

        if (bandElem != null) {
            final double referenceUTC = ReaderUtils.getTime(globalElem, "ReferenceUTC", standardDateFormat).getMJD(); // in days
            double firstLineTime = bandElem.getAttributeDouble("ZeroDopplerAzimuthFirstTime") / (24 * 3600); // in days
            if (firstLineTime == 0) {
                final MetadataElement s01Elem = globalElem.getElement("S01");
                if (s01Elem != null) {
                    firstLineTime = s01Elem.getElement("B001").getAttributeDouble("Azimuth_First_Time") / (24 * 3600); // in days
                } else {
                    firstLineTime = globalElem.getAttributeDouble("S01_B001_Azimuth_First_Time") / (24 * 3600); // in days
                }
            }
            double lastLineTime = bandElem.getAttributeDouble("ZeroDopplerAzimuthLastTime") / (24 * 3600); // in days
            if (lastLineTime == 0) {
                final MetadataElement s01Elem = globalElem.getElement("S01");
                if (s01Elem != null) {
                    lastLineTime = s01Elem.getElement("B001").getAttributeDouble("Azimuth_Last_Time") / (24 * 3600); // in days
                } else {
                    lastLineTime = globalElem.getAttributeDouble("S01_B001_Azimuth_Last_Time") / (24 * 3600); // in days
                }
            }
            double lineTimeInterval = bandElem.getAttributeDouble("LineTimeInterval", AbstractMetadata.NO_METADATA); // in s
            final ProductData.UTC startTime = new ProductData.UTC(referenceUTC + firstLineTime);
            final ProductData.UTC stopTime = new ProductData.UTC(referenceUTC + lastLineTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
            product.setStartTime(startTime);
            product.setEndTime(stopTime);
            if (lineTimeInterval == 0 || lineTimeInterval == AbstractMetadata.NO_METADATA) {
                lineTimeInterval = ReaderUtils.getLineTimeInterval(startTime, stopTime, rasterHeight);
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, lineTimeInterval);
        }
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
        final ProductData.UTC referenceUTC = ReaderUtils.getTime(globalElem, "ReferenceUTC", standardDateFormat);
        final int numPoints = globalElem.getAttributeInt("NumberOfStateVectors");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, referenceUTC);

        final List<String> pos = Arrays.asList(globalElem.getAttribute("ECEFSatellitePosition").getData().toString().replace(" ", "").split(","));
        final List<String> vel = Arrays.asList(globalElem.getAttribute("ECEFSatelliteVelocity").getData().toString().replace(" ", "").split(","));
        final List<String> stateVectorTime = Arrays.asList(globalElem.getAttribute("StateVectorsTimes").getData().toString().replace(" ", "").split(","));

        for (int i = 0; i < numPoints; i++) {
            final ProductData.UTC orbitTime =
                    new ProductData.UTC(referenceUTC.getMJD() + Double.parseDouble(stateVectorTime.get(i)) / Constants.secondsInDay);
            final double satellitePositionX = Double.parseDouble(pos.get(3 * i));
            final double satellitePositionY = Double.parseDouble(pos.get(3 * i + 1));
            final double satellitePositionZ = Double.parseDouble(pos.get(3 * i + 2));

            final double satelliteVelocityX = Double.parseDouble(vel.get(3 * i));
            final double satelliteVelocityY = Double.parseDouble(vel.get(3 * i + 1));
            final double satelliteVelocityZ = Double.parseDouble(vel.get(3 * i + 2));

            final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + (i + 1));

            orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, orbitTime);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos, satellitePositionX);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos, satellitePositionY);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos, satellitePositionZ);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel, satelliteVelocityX);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel, satelliteVelocityY);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel, satelliteVelocityZ);

            orbitVectorListElem.addElement(orbitVectorElem);
        }
    }

    private void addSRGRCoefficients(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataAttribute attribute = globalElem.getAttribute("GroundProjectionPolynomialReferenceRange");
        if (attribute == null) {
            return;
        }


//        final double referenceRange = attribute.getData().getElemDouble();
//
//        final MetadataElement bandElem = getBandElement(getNonGIMBand());
//        final double rangeSpacing = bandElem.getAttributeDouble("ColumnSpacing", AbstractMetadata.NO_METADATA);
//
//        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
//        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
//        srgrCoefficientsElem.addElement(srgrListElem);
//
//        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
//        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
//        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
//                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
//        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);
//
//        final int numCoeffs = 6;
//        for (int i = 0; i < numCoeffs; i++) {
//            double srgrCoeff = globalElem.getAttribute("GroundtoSlantPolynomial").getData().getElemDoubleAt(i);
//            if (i == 0) {
//                srgrCoeff += referenceRange;
//            } else {
//                srgrCoeff /= FastMath.pow(rangeSpacing, i);
//            }
//
//            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
//            srgrListElem.addElement(coefElem);
//
//            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
//                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");
//
//            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, srgrCoeff);
//        }
//    }
    }

    private static void addDopplerCentroidCoefficients(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

        AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, 0.0);

        final List<String> coefValueList = Arrays.asList(globalElem.getAttribute("CentroidvsRangeTimePolynomial").getData().toString().replace(" ", "").split(","));
        for (int i = 0; i < 6; i++) {
            final double coefValue = Double.parseDouble(coefValueList.get(i));
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            dopplerListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
        }
    }

    private void addIncidenceAnglesSlantRangeTime(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement aux = AbstractMetadata.getOriginalProductMetadata(product).getElement("Auxiliary");

        final MetadataElement bandElem = getBandElement();

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float subSamplingX = product.getSceneRasterWidth() / (float) (gridWidth - 1);
        final float subSamplingY = product.getSceneRasterHeight() / (float) (gridHeight - 1);

        final double nearRangeAngle = Double.parseDouble(bandElem.getAttributeString("NearIncidenceAngle"));
        final double farRangeAngle = Double.parseDouble(bandElem.getAttributeString("FarIncidenceAngle"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, nearRangeAngle);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, farRangeAngle);

        final double firstRangeTime = Double.parseDouble(bandElem.getAttributeString("ZeroDopplerRangeFirstTime")) * Constants.oneBillion;
        final double lastRangeTime = Double.parseDouble(bandElem.getAttributeString("ZeroDopplerRangeLastTime")) * Constants.oneBillion;

        final float[] incidenceCorners = new float[]{(float) nearRangeAngle, (float) farRangeAngle, (float) nearRangeAngle, (float) farRangeAngle};
        final float[] slantRange = new float[]{(float) firstRangeTime, (float) lastRangeTime, (float) firstRangeTime, (float) lastRangeTime};

        final float[] fineAngles = new float[gridWidth * gridHeight];
        final float[] fineTimes = new float[gridWidth * gridHeight];

        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, slantRange, fineTimes);

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineAngles);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineTimes);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);
    }

    private void addGeocodingFromMetadata(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement bandElem = getBandElement();

        try {
            String str = bandElem.getAttributeString("TopLeftGeodeticCoordinates");
            final float latUL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("TopRightGeodeticCoordinates");
            final float latUR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("BottomLeftGeodeticCoordinates");
            final float latLL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("BottomRightGeodeticCoordinates");
            final float latLR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));

            absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

            final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};
            final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};

            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // continue
        }
    }

    // select SBI or MBI
    private MetadataElement getBandElement() {
        final MetadataElement originalProductMetadata = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement auxElem = originalProductMetadata.getElement("Auxiliary");
        final MetadataElement rootElem = auxElem.getElement("Root");
        final MetadataElement subSwathElem = rootElem.getElement("SubSwaths").getElement("SubSwath");

        MetadataElement bandElem;
        if (rootElem.getAttributeString("AcquisitionMode").equals("WIDE SWATH")
                && (rootElem.getAttributeString("ProductType").contains("GEC") || rootElem.getAttributeString("ProductType").contains("GTC"))) {
            bandElem = rootElem.getElement("MBI");
        } else {
            bandElem = subSwathElem.getElement("SBI");
        }

        return bandElem;
    }

    public void close() {
        if (product != null) {
            product.dispose();
            product = null;
        }
    }

    public void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                       ProgressMonitor pm) throws IOException {

        geoTiffReader.readBandRasterData(destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }
}
