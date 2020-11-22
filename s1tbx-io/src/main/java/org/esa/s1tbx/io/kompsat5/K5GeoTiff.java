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
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;

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

        product = geoTiffReader.readProductNodes(inputFile, null);
        product.setFileLocation(inputFile);
        addAuxXML(product);
        addAbstractedMetadataHeader(product, product.getMetadataRoot());

        addExtraBands(inputFile, product);
        product.setStartTime(getStartTime(product));
        product.setEndTime(getEndTime(product));

        return product;
    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement aux = root.getElement("Auxiliary");
        final MetadataElement globalElem = aux.getElement("Root");

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, globalElem.getAttributeString("ProductFilename", defStr));
        final String productType = globalElem.getAttributeString("ProductType", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        final String mode = globalElem.getAttributeString("AcquisitionMode", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, mode);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, mode);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "Kompsat5");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(globalElem, "ProductGenerationUTC", standardDateFormat));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                globalElem.getAttributeString("ProcessingCentre", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,
                globalElem.getAttributeString("LookSide", defStr).toLowerCase());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, globalElem.getAttributeInt("OrbitNumber", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, globalElem.getAttributeString("OrbitDirection", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType(globalElem));


        final ProductData.UTC startTime = ReaderUtils.getTime(globalElem, "SceneSensingStartUTC", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(globalElem, "SceneSensingStopUTC", standardDateFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
        product.setStartTime(startTime);
        product.setEndTime(stopTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                                      ReaderUtils.getLineTimeInterval(startTime, stopTime, product.getSceneRasterHeight()));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                globalElem.getAttributeDouble("RadarFrequency", defInt) / Constants.oneMillion);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                globalElem.getAttributeString("FocusingAlgorithmID", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system,
                globalElem.getAttributeString("EllipsoidDesignator", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                globalElem.getAttributeDouble("RangeProcessingNumberOfLooks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                globalElem.getAttributeDouble("AzimuthProcessingNumberOfLooks", defInt));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                globalElem.getAttributeDouble("GroundRangeGeometricResolution", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                globalElem.getAttributeDouble("AzimuthGeometricResolution", defInt));

        if (productType.contains("GEC")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection,
                    globalElem.getAttributeString("ProjectionID", defStr));
        }

        final String rngSpreadComp = globalElem.getAttributeString(
                "RangeSpreadingLossCompensationGeometry", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, rngSpreadComp.equals("NONE") ? 0 : 1);

        final String incAngComp = globalElem.getAttributeString(
                "IncidenceAngleCompensationGeometry", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, incAngComp.equals("NONE") ? 0 : 1);

        final String antElevComp = globalElem.getAttributeString(
                "RangeAntennaPatternCompensationGeometry", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, antElevComp.equals("NONE") ? 0 : 1);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle,
                globalElem.getAttributeDouble("ReferenceIncidenceAngle", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range,
                globalElem.getAttributeDouble("ReferenceSlantRange", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp,
                globalElem.getAttributeDouble("ReferenceSlantRangeExponent", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor,
                globalElem.getAttributeDouble("RescalingFactor", defInt));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isComplex ? 0 : 1);

        final MetadataElement Subswaths = globalElem.getElement("Subswaths");
        final MetadataElement s01Elem = globalElem.getElement("S01");
        if (s01Elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    s01Elem.getAttributeDouble("PRF", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    s01Elem.getAttributeDouble("SamplingRate", defInt) / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    s01Elem.getAttributeString("Polarisation", defStr));

            // add Range and Azimuth bandwidth
            final double rangeBW = s01Elem.getAttributeDouble("RangeFocusingBandwidth"); // Hz
            final double azimuthBW = s01Elem.getAttributeDouble("AzimuthFocusingBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);

            // Calibration constant read from Global_Metadata during calibration initialization
        } else if(Subswaths != null) {
            final MetadataElement SubSwath = Subswaths.getElement("SubSwath");

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    SubSwath.getAttributeDouble("PRF", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    SubSwath.getAttributeDouble("SamplingRate", defInt) / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    SubSwath.getAttributeString("Polarisation", defStr));

            // add Range and Azimuth bandwidth
            final double rangeBW = SubSwath.getAttributeDouble("RangeFocusingBandwidth"); // Hz
            final double azimuthBW = SubSwath.getAttributeDouble("AzimuthFocusingBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        } else {
            final String prefix = "S01_";
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    globalElem.getAttributeDouble(prefix + "PRF", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    globalElem.getAttributeDouble(prefix + "SamplingRate", defInt) / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    globalElem.getAttributeString(prefix + "Polarisation", defStr));

            // add Range and Azimuth bandwidth
            final double rangeBW = globalElem.getAttributeDouble(prefix + "RangeFocusingBandwidth"); // Hz
            final double azimuthBW = globalElem.getAttributeDouble(prefix + "AzimuthFocusingBandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        }

        addOrbitStateVectors(absRoot, globalElem);
        addSRGRCoefficients(absRoot, globalElem);
        addDopplerCentroidCoefficients(absRoot, globalElem);
    }

    private String getSampleType(final MetadataElement globalElem) {
        if (globalElem.getAttributeInt("Samples_per_Pixel", 0) > 1) {
            isComplex = true;
            return "COMPLEX";
        }
        isComplex = false;
        return "DETECTED";
    }

    private String getTime(final Product product, final String tag){
        final MetadataElement m = product.getMetadataRoot();
        MetadataElement eAux  = m.getElement("Auxiliary");
        MetadataElement eRoot = eAux.getElement("Root");

        return eRoot.getAttributeString(tag);
    }

    private ProductData.UTC getStartTime(final Product product) {
        final String startTime = getTime(product, "DownlinkStartUTC");
        try{
            return ProductData.UTC.parse(startTime, "yyyy-MM-dd HH:mm:ss");

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private ProductData.UTC getEndTime(final Product product){
        final String endTime = getTime(product, "DownlinkStopUTC");
        try{
            return ProductData.UTC.parse(endTime, "yyyy-MM-dd HH:mm:ss");

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private String getPolarization(String fname){
        for (String p: new String [] {"HH", "HV", "VH", "VV"}) {
            if (fname.contains(p)) {
                return p;
            }
        }
        return null;
    }

    private void addExtraBands(final File inputFile, final Product product) throws IOException {
        final String name = inputFile.getName().toUpperCase();
        final Band [] bands = product.getBands();

        final String polarization = getPolarization(name);
        final HashMap<String, Band []> polarizationGroupedBands = new HashMap<>();
        product.setName(name);
        if(name.contains("I_SCS") ) {
            polarizationGroupedBands.put(polarization, new Band[]{bands[0], null});

            bands[0].setName("i_" + polarization);
            bands[0].setNoDataValue(0);

            final File[] files = inputFile.getParentFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    final String fname = file.getName().toUpperCase();
                    if (fname.endsWith(".TIF") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        String polarization2 = getPolarization(fname);
                        Band b = product2.getBands()[0];
                        String bname = "";
                        if (fname.contains("I_SCS")){
                            bname = "i_" + polarization2;
                        } else if (fname.contains("Q_SCS")){
                            bname = "q_" + polarization2;
                        }
                        b.setName(bname);
                        product.addBand(b);
                        if(polarizationGroupedBands.containsKey(polarization2)){
                            Band [] b2 = polarizationGroupedBands.get(polarization2);
                            b2[1] = b;
                            polarizationGroupedBands.put(polarization2, b2);
                        }else{
                            Band [] b2 = new Band[]{b, null};
                            polarizationGroupedBands.put(polarization2, b2);
                        }
                    }
                }
                for (String p : polarizationGroupedBands.keySet()){
                    Band [] bandGroup = polarizationGroupedBands.get(p);
                    bandGroup[0].setNoDataValue(0);
                    bandGroup[1].setNoDataValue(0);

                    ReaderUtils.createVirtualIntensityBand(product, bandGroup[0], bandGroup[1], p);
                }
            }
        } else if (name.contains("Q_SCS")){
            polarizationGroupedBands.put(polarization, new Band[]{bands[0], null});
            bands[0].setName("q_" + polarization);

            final File[] files = inputFile.getParentFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    final String fname = file.getName().toUpperCase();
                    if (fname.endsWith(".TIF") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        String polarization2 = getPolarization(fname);
                        Band b = product2.getBands()[0];
                        b.setNoDataValueUsed(true);
                        b.setNoDataValue(0);

                        if (fname.contains("I_SCS")){
                            b.setName("i_" + polarization2);
                            b.setUnit(Unit.REAL);
                        } else if (fname.contains("Q_SCS")){
                            b.setName("q_" + polarization2);
                            b.setUnit(Unit.IMAGINARY);
                        }

                        product.addBand(b);
                        if(polarizationGroupedBands.containsKey(polarization2)){
                            Band [] b2 = polarizationGroupedBands.get(polarization2);
                            b2[1] = b;
                            polarizationGroupedBands.put(polarization2, b2);
                        }else{
                            Band [] b2 = new Band[]{b, null};
                            polarizationGroupedBands.put(polarization2, b2);
                        }
                    }
                }
                for (String p : polarizationGroupedBands.keySet()){
                    Band [] bandGroup = polarizationGroupedBands.get(p);
                    ReaderUtils.createVirtualIntensityBand(product, bandGroup[0], bandGroup[1], p);
                }
            }
        }
        else if (name.contains("_GEC_") ||name.contains("_GTC_") || name.contains("_WEC_") || name.contains("_WTC_")  ){

            bands[0].setName("Amplitude_" + polarization);
            bands[0].setNoDataValueUsed(true);
            bands[0].setNoDataValue(0);
            bands[0].setUnit(Unit.AMPLITUDE);
            SARReader.createVirtualIntensityBand(product, bands[0],"_" + polarization);

            final File[] files = inputFile.getParentFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    final String fname = file.getName().toUpperCase();
                    if (fname.endsWith(".TIF") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        String polarization2 = getPolarization(fname);
                        Band b = product2.getBands()[0];
                        b.setNoDataValueUsed(true);
                        b.setNoDataValue(0);
                        b.setName("Amplitude_" + polarization2);
                        b.setUnit(Unit.AMPLITUDE);

                        product.addBand(b);
                        SARReader.createVirtualIntensityBand(product, b, "_" + polarization2);
                    }
                }
            }
        }
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
        final ProductData.UTC referenceUTC = ReaderUtils.getTime(globalElem, "ReferenceUTC", standardDateFormat);
        final int numPoints = globalElem.getAttributeInt("NumberOfStateVectors");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, referenceUTC);

        for (int i = 0; i < numPoints; i++) {
            final double stateVectorTime = globalElem.getAttribute("StateVectorsTimes").getData().getElemDoubleAt(i);
            final ProductData.UTC orbitTime =
                    new ProductData.UTC(referenceUTC.getMJD() + stateVectorTime / Constants.secondsInDay);

            final ProductData pos = globalElem.getAttribute("ECEFSatellitePosition").getData();
            final ProductData vel = globalElem.getAttribute("ECEFSatelliteVelocity").getData();

            final double satellitePositionX = pos.getElemDoubleAt(3 * i);
            final double satellitePositionY = pos.getElemDoubleAt(3 * i + 1);
            final double satellitePositionZ = pos.getElemDoubleAt(3 * i + 2);
            final double satelliteVelocityX = vel.getElemDoubleAt(3 * i);
            final double satelliteVelocityY = vel.getElemDoubleAt(3 * i + 1);
            final double satelliteVelocityZ = vel.getElemDoubleAt(3 * i + 2);

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

        for (int i = 0; i < 6; i++) {
            final double coefValue =
                    globalElem.getAttribute("CentroidvsRangeTimePolynomial").getData().getElemDoubleAt(i);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            dopplerListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
        }
    }

    public void close() {
        if (product != null) {
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
