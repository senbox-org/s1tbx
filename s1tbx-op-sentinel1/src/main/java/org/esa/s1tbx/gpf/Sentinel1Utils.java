/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.gpf;

import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.gpf.geometric.SARGeocoding;
import org.esa.s1tbx.gpf.geometric.SARUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.eo.Constants;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public final class Sentinel1Utils {

    private Product sourceProduct = null;
    private MetadataElement absRoot = null;
    private MetadataElement origProdRoot = null;
    private int numOfSubSwath = 0;
    private String acquisitionMode = null;
    private SubSwathInfo[] subSwath = null;
    private SARGeocoding.Orbit orbit = null;
    private String[] polarizations = null;
    private String[] subSwathNames = null;
    private boolean isDopplerCentroidAvailable = false;
    private boolean isRangeDependDopplerRateAvailable = false;

    public double firstLineUTC = 0.0; // in days
    public double lastLineUTC = 0.0; // in days
    public double lineTimeInterval = 0.0; // in days
    //public double prf = 0.0; // in Hz
    //public double samplingRate = 0.0; // Hz
    public double nearEdgeSlantRange = 0.0; // in m
    public double wavelength = 0.0; // in m
    public double rangeSpacing = 0.0;
    public double azimuthSpacing = 0.0;
    public int sourceImageWidth = 0;
    public int sourceImageHeight = 0;
    public boolean nearRangeOnLeft = true;
    public boolean srgrFlag = false;
    public AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
    public final static DateFormat sentinelDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd_HH:mm:ss");

    public Sentinel1Utils(final Product sourceProduct) throws Exception {

        this.sourceProduct = sourceProduct;

        getMetadataRoot();

        getAbstractedMetadata();

        getProductAcquisitionMode();

        getProductPolarizations();

        getProductSubSwathNames();

        getSubSwathParameters();

        this.nearRangeOnLeft = (subSwath[0].incidenceAngle[0][0] < subSwath[0].incidenceAngle[0][1]);
    }

    private void getMetadataRoot() {

        final MetadataElement root = sourceProduct.getMetadataRoot();
        if (root == null) {
            throw new OperatorException("Root Metadata not found");
        }

        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (absRoot == root) {
            throw new OperatorException(AbstractMetadata.ABSTRACT_METADATA_ROOT + " not found.");
        }

        origProdRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        if (origProdRoot == root) {
            throw new OperatorException("Original_Product_Metadata not found.");
        }

        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            throw new OperatorException(mission + " is not a valid mission for Sentinel1 product.");
        }
    }

    private void getAbstractedMetadata() throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        this.srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
        this.wavelength = SARUtils.getRadarFrequency(absRoot);
        this.rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
        this.azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
        this.firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
        this.lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD(); // in days
        this.lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) /
                Constants.secondsInDay; // s to day
        //this.prf = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.pulse_repetition_frequency); //Hz
        //this.samplingRate = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_sampling_rate)*
        //        1000000; // MHz to Hz
        this.sourceImageWidth = sourceProduct.getSceneRasterWidth();
        this.sourceImageHeight = sourceProduct.getSceneRasterHeight();
        OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        this.orbit = new SARGeocoding.Orbit(orbitStateVectors, firstLineUTC, lineTimeInterval, sourceImageHeight);

        if (this.srgrFlag) {
            this.srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
        } else {
            this.nearEdgeSlantRange = AbstractMetadata.getAttributeDouble(absRoot,
                    AbstractMetadata.slant_range_to_first_pixel);
        }

        //final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        //this.nearRangeOnLeft = SARGeocoding.isNearRangeOnLeft(incidenceAngle, sourceImageWidth);
    }

    /**
     * Get acquisition mode from abstracted metadata.
     */
    private void getProductAcquisitionMode() {

        acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
    }

    /**
     * Get source product polarizations.
     */
    private void getProductPolarizations() {

        final MetadataElement[] elems = absRoot.getElements();
        final List<String> polList = new ArrayList<>(4);
        for (MetadataElement elem : elems) {
            if (elem.getName().contains("Band_")) {
                final String pol = elem.getAttributeString("polarization");
                if (!polList.contains(pol)) {
                    polList.add(pol);
                }
            }
        }

        if (polList.size() > 0) {
            polarizations =  polList.toArray(new String[polList.size()]);
            return;
        }

        final String[] sourceBandNames = sourceProduct.getBandNames();
        for (String bandName:sourceBandNames) {
            if (bandName.contains("HH")) {
                if (!polList.contains("HH")) {
                    polList.add("HH");
                }
            } else if (bandName.contains("HV")) {
                if (!polList.contains("HV")) {
                    polList.add("HV");
                }
            } else if (bandName.contains("VH")) {
                if (!polList.contains("VH")) {
                    polList.add("VH");
                }
            } else if (bandName.contains("VV")) {
                if (!polList.contains("VV")) {
                    polList.add("VV");
                }
            }
        }
        polarizations = polList.toArray(new String[polList.size()]);
    }

    /**
     * Get source product subSwath names.
     */
    private void getProductSubSwathNames() {

        final MetadataElement[] elems = absRoot.getElements();
        final List<String> subSwathNameList = new ArrayList<>(4);
        for (MetadataElement elem : elems) {
            if (elem.getName().contains(acquisitionMode)) {
                final String swath = elem.getAttributeString("swath");
                if (!subSwathNameList.contains(swath)) {
                    subSwathNameList.add(swath);
                }
            }
        }

        if (subSwathNameList.size() < 1) {
            final String[] sourceBandNames = sourceProduct.getBandNames();
            for (String bandName : sourceBandNames) {
                if (bandName.contains(acquisitionMode)) {
                    final int idx = bandName.indexOf(acquisitionMode);
                    final String subSwathName = bandName.substring(idx, idx + 3);
                    if (!subSwathNameList.contains(subSwathName)) {
                        subSwathNameList.add(subSwathName);
                    }
                }
            }
        }
        subSwathNames =  subSwathNameList.toArray(new String[subSwathNameList.size()]);
        numOfSubSwath = subSwathNames.length;
    }

    /**
     * Get parameters for all sub-swaths.
     */
    private void getSubSwathParameters() {

        subSwath = new SubSwathInfo[numOfSubSwath];
        for (int i = 0; i < numOfSubSwath; i++) {
            subSwath[i] = new SubSwathInfo();
			subSwath[i].subSwathName = subSwathNames[i];
            final MetadataElement subSwathMetadata = getSubSwathMetadata(subSwath[i].subSwathName);
            getSubSwathParameters(subSwathMetadata, subSwath[i]);
        }
    }

    /**
     * Get root metadata element of given sub-swath.
     *
     * @param subSwathName Sub-swath name string.
     * @return The root metadata element.
     */
    private MetadataElement getSubSwathMetadata(final String subSwathName) {

        MetadataElement annotation = origProdRoot.getElement("annotation");
        if (annotation == null) {
            throw new OperatorException("Annotation Metadata not found");
        }

        final MetadataElement[] elems = annotation.getElements();
        for (MetadataElement elem : elems) {
            if (elem.getName().contains(subSwathName.toLowerCase())) {
                return elem;
            }
        }

        return null;
    }

    /**
     * Get sub-swath parameters and save them in SubSwathInfo object.
     *
     * @param subSwathMetadata The root metadata element of a given sub-swath.
     * @param subSwath         The SubSwathInfo object.
     */
    private static void getSubSwathParameters(final MetadataElement subSwathMetadata, final SubSwathInfo subSwath) {

        final MetadataElement product = subSwathMetadata.getElement("product");
        final MetadataElement imageAnnotation = product.getElement("imageAnnotation");
        final MetadataElement imageInformation = imageAnnotation.getElement("imageInformation");
        final MetadataElement swathTiming = product.getElement("swathTiming");
        final MetadataElement burstList = swathTiming.getElement("burstList");
        final MetadataElement generalAnnotation = product.getElement("generalAnnotation");
        final MetadataElement productInformation = generalAnnotation.getElement("productInformation");

        subSwath.firstLineTime = getTime(imageInformation, "productFirstLineUtcTime").getMJD()*Constants.secondsInDay;
        subSwath.lastLineTime = getTime(imageInformation, "productLastLineUtcTime").getMJD()*Constants.secondsInDay;
        subSwath.numOfSamples = Integer.parseInt(imageInformation.getAttributeString("numberOfSamples"));
        subSwath.numOfLines = Integer.parseInt(imageInformation.getAttributeString("numberOfLines"));
        subSwath.azimuthTimeInterval = Double.parseDouble(imageInformation.getAttributeString("azimuthTimeInterval"));
        subSwath.rangePixelSpacing = Double.parseDouble(imageInformation.getAttributeString("rangePixelSpacing"));
        subSwath.azimuthPixelSpacing = Double.parseDouble(imageInformation.getAttributeString("azimuthPixelSpacing"));
        subSwath.slrTimeToFirstPixel = Double.parseDouble(imageInformation.getAttributeString("slantRangeTime")) / 2.0; // 2-way to 1-way
        subSwath.slrTimeToLastPixel = subSwath.slrTimeToFirstPixel +
                (subSwath.numOfSamples - 1) * subSwath.rangePixelSpacing / Constants.lightSpeed;

        subSwath.numOfBursts = Integer.parseInt(burstList.getAttributeString("count"));
        subSwath.linesPerBurst = Integer.parseInt(swathTiming.getAttributeString("linesPerBurst"));
        subSwath.samplesPerBurst = Integer.parseInt(swathTiming.getAttributeString("samplesPerBurst"));
        subSwath.radarFrequency = Double.parseDouble(productInformation.getAttributeString("radarFrequency"));
        subSwath.azimuthSteeringRate = Double.parseDouble(productInformation.getAttributeString("azimuthSteeringRate"));

        subSwath.burstFirstLineTime = new double[subSwath.numOfBursts];
        subSwath.burstLastLineTime = new double[subSwath.numOfBursts];
        subSwath.burstFirstValidLineTime = new double[subSwath.numOfBursts];
        subSwath.burstLastValidLineTime = new double[subSwath.numOfBursts];
        subSwath.firstValidSample = new int[subSwath.numOfBursts][];
        subSwath.lastValidSample = new int[subSwath.numOfBursts][];

        subSwath.firstValidPixel = 0;
        subSwath.lastValidPixel = subSwath.numOfSamples;

        int k = 0;
        if (subSwath.numOfBursts > 0) {
            int firstValidPixel = subSwath.numOfSamples;
            int lastValidPixel = 0;
            final MetadataElement[] burstListElem = burstList.getElements();
            for (MetadataElement listElem : burstListElem) {

                subSwath.burstFirstLineTime[k] =
                        Sentinel1Utils.getTime(listElem, "azimuthTime").getMJD()*Constants.secondsInDay;

                subSwath.burstLastLineTime[k] = subSwath.burstFirstLineTime[k] +
                        (subSwath.linesPerBurst - 1) * subSwath.azimuthTimeInterval;

                final MetadataElement firstValidSampleElem = listElem.getElement("firstValidSample");
                final MetadataElement lastValidSampleElem = listElem.getElement("lastValidSample");
                subSwath.firstValidSample[k] = Sentinel1Utils.getIntArray(firstValidSampleElem, "firstValidSample");
                subSwath.lastValidSample[k] = Sentinel1Utils.getIntArray(lastValidSampleElem, "lastValidSample");

                int firstValidLineIdx = -1;
                int lastValidLineIdx = -1;
                for (int lineIdx = 0; lineIdx < subSwath.firstValidSample[k].length; lineIdx++) {
                    if (subSwath.firstValidSample[k][lineIdx] != -1 &&
                            subSwath.firstValidSample[k][lineIdx] < firstValidPixel) {
                        firstValidPixel = subSwath.firstValidSample[k][lineIdx];

                        if (firstValidLineIdx == -1) {
                            firstValidLineIdx = lineIdx;
                            lastValidLineIdx = lineIdx;
                        } else {
                            lastValidLineIdx++;
                        }
                    }
                }

                for (int lineIdx = 0; lineIdx < subSwath.lastValidSample[k].length; lineIdx++) {
                    if (subSwath.lastValidSample[k][lineIdx] != -1 &&
                            subSwath.lastValidSample[k][lineIdx] > lastValidPixel) {
                        lastValidPixel = subSwath.lastValidSample[k][lineIdx];
                    }
                }

                subSwath.burstFirstValidLineTime[k] = subSwath.burstFirstLineTime[k] +
                        firstValidLineIdx * subSwath.azimuthTimeInterval;

                subSwath.burstLastValidLineTime[k] = subSwath.burstFirstLineTime[k] +
                        lastValidLineIdx * subSwath.azimuthTimeInterval;

                k++;
            }
            subSwath.firstValidPixel = firstValidPixel;
            subSwath.lastValidPixel = lastValidPixel;
        }

        subSwath.slrTimeToFirstValidPixel = subSwath.slrTimeToFirstPixel +
                subSwath.firstValidPixel * subSwath.rangePixelSpacing / Constants.lightSpeed;

        subSwath.slrTimeToLastValidPixel = subSwath.slrTimeToFirstPixel +
                subSwath.lastValidPixel * subSwath.rangePixelSpacing / Constants.lightSpeed;

        // get geolocation grid points
        final MetadataElement geolocationGrid = product.getElement("geolocationGrid");
        final MetadataElement geolocationGridPointList = geolocationGrid.getElement("geolocationGridPointList");
        final int numOfGeoLocationGridPoints = Integer.parseInt(geolocationGridPointList.getAttributeString("count"));
        final MetadataElement[] geolocationGridPointListElem = geolocationGridPointList.getElements();
        int numOfGeoPointsPerLine = 0;
        int line = 0;
        for (MetadataElement listElem : geolocationGridPointListElem) {
            if (numOfGeoPointsPerLine == 0) {
                line = Integer.parseInt(listElem.getAttributeString("line"));
                numOfGeoPointsPerLine++;
            } else if (line == Integer.parseInt(listElem.getAttributeString("line"))) {
                numOfGeoPointsPerLine++;
            } else {
                break;
            }
        }

        final int numOfGeoLines = numOfGeoLocationGridPoints / numOfGeoPointsPerLine;
        subSwath.numOfGeoLines = numOfGeoLines;
        subSwath.numOfGeoPointsPerLine = numOfGeoPointsPerLine;
        subSwath.azimuthTime = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.slantRangeTime = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.latitude = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.longitude = new double[numOfGeoLines][numOfGeoPointsPerLine];
        subSwath.incidenceAngle = new double[numOfGeoLines][numOfGeoPointsPerLine];
        k = 0;
        for (MetadataElement listElem : geolocationGridPointListElem) {
            final int i = k / numOfGeoPointsPerLine;
            final int j = k - i * numOfGeoPointsPerLine;
            subSwath.azimuthTime[i][j] = Sentinel1Utils.getTime(listElem, "azimuthTime").getMJD()*Constants.secondsInDay;
            subSwath.slantRangeTime[i][j] = Double.parseDouble(listElem.getAttributeString("slantRangeTime")) / 2.0;
            subSwath.latitude[i][j] = Double.parseDouble(listElem.getAttributeString("latitude"));
            subSwath.longitude[i][j] = Double.parseDouble(listElem.getAttributeString("longitude"));
            subSwath.incidenceAngle[i][j] = Double.parseDouble(listElem.getAttributeString("incidenceAngle"));
            k++;
        }
    }

    private void getProductOrbit() {

        final OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        this.orbit = new SARGeocoding.Orbit(orbitStateVectors);
    }

    public SARGeocoding.Orbit getOrbit() {

        if (this.orbit == null) {
            getProductOrbit();
        }
        return orbit;
    }

    /**
     * Get noise vectors for each sub-swath and each polarization.
     */
    private void getSubSwathNoiseVectors() {

        for (int i = 0; i < numOfSubSwath; i++) {
            for (String pol:polarizations) {
                if (pol != null) {
                    final Band srcBand = getSourceBand(subSwath[i].subSwathName, pol);
                    final NoiseVector[] noiseVectors = getNoiseVector(srcBand);
                    subSwath[i].noise.put(pol, noiseVectors);
                }
            }
        }
    }

    private Band getSourceBand(final String subSwathName, final String polarization) {

        final Band[] sourceBands = sourceProduct.getBands();
        for (Band band:sourceBands) {
            if (band.getName().contains(subSwathName + '_' + polarization)) {
                return band;
            }
        }
        return null;
    }

    private NoiseVector[] getNoiseVector(final Band band) {

        final MetadataElement bandAbsMetadata = AbstractMetadata.getBandAbsMetadata(absRoot, band);
        final String annotation = bandAbsMetadata.getAttributeString(AbstractMetadata.annotation);
        final MetadataElement noiseElem = origProdRoot.getElement("noise");
        final MetadataElement bandNoise = noiseElem.getElement(annotation);
        final MetadataElement noise = bandNoise.getElement("noise");
        final MetadataElement noiseVectorListElem = noise.getElement("noiseVectorList");
        final MetadataElement[] list = noiseVectorListElem.getElements();

        final List<NoiseVector> noiseVectorList = new ArrayList<>(5);
        for (MetadataElement noiseVectorElem : list) {
            final ProductData.UTC time = getTime(noiseVectorElem, "azimuthTime");
            final int line = Integer.parseInt(noiseVectorElem.getAttributeString("line"));

            final MetadataElement pixelElem = noiseVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
            final MetadataElement noiseLutElem = noiseVectorElem.getElement("noiseLut");
            final String noiseLUT = noiseLutElem.getAttributeString("noiseLut");

            final int[] pixelArray = new int[count];
            final float[] noiseLUTArray = new float[count];
            addToArray(pixelArray, 0, pixel, " ");
            addToArray(noiseLUTArray, 0, noiseLUT, " ");

            noiseVectorList.add(new NoiseVector(time, line, pixelArray, noiseLUTArray));
        }
        return noiseVectorList.toArray(new NoiseVector[noiseVectorList.size()]);
    }

    /**
     * Get noise vectors for each sub-swath and each polarization.
     */
    private void getSubSwathCalibrationVectors(final boolean outputSigmaBand,
                                               final boolean outputBetaBand,
                                               final boolean outputGammaBand,
                                               final boolean outputDNBand) {

        for (int i = 0; i < numOfSubSwath; i++) {
            for (String pol:polarizations) {
                if (pol != null) {
                    final CalibrationVector[] calibrationVectors = getCalibrationVector(
                            i+1, pol, outputSigmaBand, outputBetaBand, outputGammaBand, outputDNBand);

                    subSwath[i].calibration.put(pol, calibrationVectors);
                }
            }
        }
    }

    private CalibrationVector[] getCalibrationVector(final int subSwathIndex,
                                                     final String polarization,
                                                     final boolean outputSigmaBand,
                                                     final boolean outputBetaBand,
                                                     final boolean outputGammaBand,
                                                     final boolean outputDNBand) {

        final MetadataElement calibrationVectorListElem = getCalibrationVectorList(subSwathIndex, polarization);
        final MetadataElement[] list = calibrationVectorListElem.getElements();

        final List<CalibrationVector> calibrationVectorList = new ArrayList<>(5);
        for (MetadataElement calibrationVectorElem : list) {
            final ProductData.UTC time = getTime(calibrationVectorElem, "azimuthTime");
            final int line = Integer.parseInt(calibrationVectorElem.getAttributeString("line"));

            final MetadataElement pixelElem = calibrationVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
            final int[] pixelArray = new int[count];
            addToArray(pixelArray, 0, pixel, " ");

            float[] sigmaNoughtArray = null;
            if (outputSigmaBand) {
                final MetadataElement sigmaNoughtElem = calibrationVectorElem.getElement("sigmaNought");
                final String sigmaNought = sigmaNoughtElem.getAttributeString("sigmaNought");
                sigmaNoughtArray = new float[count];
                addToArray(sigmaNoughtArray, 0, sigmaNought, " ");
            }

            float[] betaNoughtArray = null;
            if (outputBetaBand) {
                final MetadataElement betaNoughtElem = calibrationVectorElem.getElement("betaNought");
                final String betaNought = betaNoughtElem.getAttributeString("betaNought");
                betaNoughtArray = new float[count];
                addToArray(betaNoughtArray, 0, betaNought, " ");
            }

            float[] gammaArray = null;
            if (outputGammaBand) {
                final MetadataElement gammaElem = calibrationVectorElem.getElement("gamma");
                final String gamma = gammaElem.getAttributeString("gamma");
                gammaArray = new float[count];
                addToArray(gammaArray, 0, gamma, " ");
            }

            float[] dnArray = null;
            if (outputDNBand) {
                final MetadataElement dnElem = calibrationVectorElem.getElement("dn");
                final String dn = dnElem.getAttributeString("dn");
                dnArray = new float[count];
                addToArray(dnArray, 0, dn, " ");
            }

            calibrationVectorList.add(new CalibrationVector(
                    time, line, pixelArray, sigmaNoughtArray, betaNoughtArray, gammaArray, dnArray));
        }
        return calibrationVectorList.toArray(new CalibrationVector[calibrationVectorList.size()]);
    }

    /**
     * Compute range-dependent Doppler rate Ka(r) for each burst.
     */
    private void computeRangeDependentDopplerRate() {

        for (int s = 0; s < numOfSubSwath; s++) {
            final AzimuthFmRate[] azFmRateList = getAzimuthFmRateList(subSwath[s].subSwathName);
            subSwath[s].rangeDependDopplerRate = new double[subSwath[s].numOfBursts][subSwath[s].samplesPerBurst];
            for (int b = 0; b < subSwath[s].numOfBursts; b++) {
                for (int x = 0; x < subSwath[s].samplesPerBurst; x++) {
                    final double slrt = getSlantRangeTime(x, s+1)*2; // 1-way to 2-way
                    final double dt = slrt - azFmRateList[b].t0;
                    subSwath[s].rangeDependDopplerRate[b][x] =
                            azFmRateList[b].c0 + azFmRateList[b].c1*dt + azFmRateList[b].c2*dt*dt;
                }
            }
        }
        isRangeDependDopplerRateAvailable = true;
    }

    private AzimuthFmRate[] getAzimuthFmRateList(final String subSwathName) {

        final MetadataElement subSwathMetadata = getSubSwathMetadata(subSwathName);
        final MetadataElement product = subSwathMetadata.getElement("product");
        final MetadataElement generalAnnotation = product.getElement("generalAnnotation");
        final MetadataElement azimuthFmRateList = generalAnnotation.getElement("azimuthFmRateList");
        final int count = Integer.parseInt(azimuthFmRateList.getAttributeString("count"));
        AzimuthFmRate[] azFmRateList = null;
        int k = 0;
        if (count > 0) {
            azFmRateList = new AzimuthFmRate[count];
            final MetadataElement[] azFmRateListElem = azimuthFmRateList.getElements();
            for (MetadataElement listElem : azFmRateListElem) {
                azFmRateList[k] = new AzimuthFmRate();
                azFmRateList[k].time = Sentinel1Utils.getTime(listElem, "azimuthTime").getMJD()*Constants.secondsInDay;
                azFmRateList[k].t0 = Double.parseDouble(listElem.getAttributeString("t0"));
                azFmRateList[k].c0 = Double.parseDouble(listElem.getAttributeString("c0"));
                azFmRateList[k].c1 = Double.parseDouble(listElem.getAttributeString("c1"));
                azFmRateList[k].c2 = Double.parseDouble(listElem.getAttributeString("c2"));
                k++;
            }
        }
        return azFmRateList;
    }

    /**
     * Compute Doppler rate Kt(r) for each burst.
     */
    public void computeDopplerRate() {

        if (orbit == null) {
            getProductOrbit();
        }

        if (!isRangeDependDopplerRateAvailable) {
            computeRangeDependentDopplerRate();
        }

        final double waveLength = Constants.lightSpeed / subSwath[0].radarFrequency;
        for (int s = 0; s < numOfSubSwath; s++) {
            final double azTime = (subSwath[s].firstLineTime + subSwath[s].lastLineTime)/2.0;
            subSwath[s].dopplerRate = new double[subSwath[s].numOfBursts][subSwath[s].samplesPerBurst];
            for (int b = 0; b < subSwath[s].numOfBursts; b++) {
                final double v = orbit.getVelocity(azTime/Constants.secondsInDay); // DLR: 7594.0232
                final double steeringRate = subSwath[s].azimuthSteeringRate * Constants.DTOR;
                final double krot = 2*v*steeringRate/waveLength; // doppler rate by antenna steering
                for (int x = 0; x < subSwath[s].samplesPerBurst; x++) {
                    subSwath[s].dopplerRate[b][x] = subSwath[s].rangeDependDopplerRate[b][x] * krot
                            / (subSwath[s].rangeDependDopplerRate[b][x] - krot);
                }
            }
        }
    }

    /**
     * Compute range-dependent reference time t_ref for each burst.
     */
    public void computeReferenceTime() {

        if (!isDopplerCentroidAvailable) {
            computeDopplerCentroid();
        }

        if (!isRangeDependDopplerRateAvailable) {
            computeRangeDependentDopplerRate();
        }

        for (int s = 0; s < numOfSubSwath; s++) {
            subSwath[s].referenceTime = new double[subSwath[s].numOfBursts][subSwath[s].samplesPerBurst];
            final double tmp1 = subSwath[s].linesPerBurst * subSwath[s].azimuthTimeInterval / 2.0;

            for (int b = 0; b < subSwath[s].numOfBursts; b++) {
                final double tmp2 = tmp1 + subSwath[s].dopplerCentroid[b][subSwath[s].firstValidPixel] /
                        subSwath[s].rangeDependDopplerRate[b][subSwath[s].firstValidPixel];

                for (int x = 0; x < subSwath[s].samplesPerBurst; x++) {
                    subSwath[s].referenceTime[b][x] = tmp2 -
                            subSwath[s].dopplerCentroid[b][x] / subSwath[s].rangeDependDopplerRate[b][x];
                }
            }
        }
    }

    /**
     * Compute range-dependent Doppler centroid for each burst.
     */
    private void computeDopplerCentroid() {

        for (int s = 0; s < numOfSubSwath; s++) {
            final DCPolynomial[] dcEstimateList = getDCEstimateList(subSwath[s].subSwathName);
            final DCPolynomial[] dcBurstList = computeDCForBurstCenters(dcEstimateList, s+1);
            subSwath[s].dopplerCentroid = new double[subSwath[s].numOfBursts][subSwath[s].samplesPerBurst];
            for (int b = 0; b < subSwath[s].numOfBursts; b++) {
                for (int x = 0; x < subSwath[s].samplesPerBurst; x++) {
                    final double slrt = getSlantRangeTime(x, s+1)*2; // 1-way to 2-way
                    final double dt = slrt - dcBurstList[b].t0;
                    double dcValue = 0.0;
                    for (int i = 0; i < dcBurstList[b].dataDcPolynomial.length; i++) {
                        dcValue += dcBurstList[b].dataDcPolynomial[i] * FastMath.pow(dt, i);
                    }
                    subSwath[s].dopplerCentroid[b][x] = dcValue;
                }
            }
        }

        isDopplerCentroidAvailable = true;
    }

    private DCPolynomial[] getDCEstimateList(final String subSwathName) {

        final MetadataElement subSwathMetadata = getSubSwathMetadata(subSwathName);
        final MetadataElement product = subSwathMetadata.getElement("product");
        final MetadataElement imageAnnotation = product.getElement("imageAnnotation");
        final MetadataElement processingInformation = imageAnnotation.getElement("processingInformation");
        final String dcMethod = processingInformation.getAttributeString("dcMethod");
        final MetadataElement dopplerCentroid = product.getElement("dopplerCentroid");
        final MetadataElement dcEstimateList = dopplerCentroid.getElement("dcEstimateList");
        final int count = Integer.parseInt(dcEstimateList.getAttributeString("count"));
        DCPolynomial[] dcPolynomial = null;
        int k = 0;
        if (count > 0) {
            dcPolynomial = new DCPolynomial[count];
            final MetadataElement[] dcEstimateListElem = dcEstimateList.getElements();
            for (MetadataElement listElem : dcEstimateListElem) {
                dcPolynomial[k] = new DCPolynomial();
                dcPolynomial[k].time = Sentinel1Utils.getTime(listElem, "azimuthTime").getMJD()*Constants.secondsInDay;
                dcPolynomial[k].t0 = listElem.getAttributeDouble("t0");

                if (dcMethod.contains("Data Analysis")) {
                    final MetadataElement dataDcPolynomialElem = listElem.getElement("dataDcPolynomial");
                    dcPolynomial[k].dataDcPolynomial =
                            Sentinel1Utils.getDoubleArray(dataDcPolynomialElem, "dataDcPolynomial");
                } else {
                    final MetadataElement geometryDcPolynomialElem = listElem.getElement("geometryDcPolynomial");
                    dcPolynomial[k].dataDcPolynomial =
                            Sentinel1Utils.getDoubleArray(geometryDcPolynomialElem, "geometryDcPolynomial");
                }

                k++;
            }
        }
        return dcPolynomial;
    }

    private DCPolynomial[] computeDCForBurstCenters(final DCPolynomial[] dcEstimateList, final int subSwathIndex) {

        if (dcEstimateList.length >= subSwath[subSwathIndex - 1].numOfBursts) {
            return dcEstimateList;
        }

        final DCPolynomial[] dcBurstList = new DCPolynomial[subSwath[subSwathIndex - 1].numOfBursts];
        for (int b = 0; b < subSwath[subSwathIndex - 1].numOfBursts; b++) {
            if (b < dcEstimateList.length) {
                dcBurstList[b] = dcEstimateList[b];
            } else {
                final double centerTime = 0.5*(subSwath[subSwathIndex - 1].burstFirstLineTime[b] +
                        subSwath[subSwathIndex - 1].burstLastLineTime[b]);

                dcBurstList[b] = computeDC(centerTime, dcEstimateList);
            }
        }

        return dcBurstList;
    }

    private DCPolynomial computeDC(final double centerTime, final DCPolynomial[] dcEstimateList) {

        int i0 = 0, i1 = 0;
        if (centerTime < dcEstimateList[0].time) {
            i0 = 0;
            i1 = 1;
        } else if (centerTime > dcEstimateList[dcEstimateList.length - 1].time) {
            i0 = dcEstimateList.length - 2;
            i1 = dcEstimateList.length - 1;
        } else {
            for (int i = 0; i < dcEstimateList.length - 1; i++) {
                if (centerTime >= dcEstimateList[i].time && centerTime < dcEstimateList[i+1].time) {
                    i0 = i;
                    i1 = i + 1;
                    break;
                }
            }
        }

        final DCPolynomial dcPolynomial = new DCPolynomial();
        dcPolynomial.time = centerTime;
        dcPolynomial.t0 = dcEstimateList[i0].t0;
        dcPolynomial.dataDcPolynomial = new double[dcEstimateList[i0].dataDcPolynomial.length];
        final double mu = (centerTime - dcEstimateList[i0].time) / (dcEstimateList[i1].time - dcEstimateList[i0].time);
        for (int j = 0; j < dcEstimateList[i0].dataDcPolynomial.length; j++) {
            dcPolynomial.dataDcPolynomial[j] = (1 - mu)*dcEstimateList[i0].dataDcPolynomial[j] +
                    mu*dcEstimateList[i1].dataDcPolynomial[j];
        }

        return dcPolynomial;
    }


    // =================================================================================
    private MetadataElement getCalibrationVectorList(final int subSwathIndex, final String polarization) {

        final Band srcBand = getSourceBand(subSwath[subSwathIndex - 1].subSwathName, polarization);
        final MetadataElement bandAbsMetadata = AbstractMetadata.getBandAbsMetadata(absRoot, srcBand);
        final String annotation = bandAbsMetadata.getAttributeString(AbstractMetadata.annotation);
        final MetadataElement calibrationElem = origProdRoot.getElement("calibration");
        final MetadataElement bandCalibration = calibrationElem.getElement(annotation);
        final MetadataElement calibration = bandCalibration.getElement("calibration");
        return calibration.getElement("calibrationVectorList");
    }

    public float[] getCalibrationVector(
            final int subSwathIndex, final String polarization, final int vectorIndex, final String vectorName) {

        final MetadataElement calibrationVectorListElem = getCalibrationVectorList(subSwathIndex, polarization);
        final MetadataElement[] list = calibrationVectorListElem.getElements();
        final MetadataElement vectorElem = list[vectorIndex].getElement(vectorName);
        final String vectorStr = vectorElem.getAttributeString(vectorName);
        final int count = Integer.parseInt(vectorElem.getAttributeString("count"));
        float[] vectorArray = new float[count];
        addToArray(vectorArray, 0, vectorStr, " ");

        return vectorArray;
    }

    public int[] getCalibrationPixel(
            final int subSwathIndex, final String polarization, final int vectorIndex) {

        final MetadataElement calibrationVectorListElem = getCalibrationVectorList(subSwathIndex, polarization);
        final MetadataElement[] list = calibrationVectorListElem.getElements();
        final MetadataElement pixelElem = list[vectorIndex].getElement("pixel");
        final String pixel = pixelElem.getAttributeString("pixel");
        final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
        final int[] pixelArray = new int[count];
        addToArray(pixelArray, 0, pixel, " ");

        return pixelArray;
    }

    //todo: This function is currently used by Sentinel1RemoveThermalNoiseOp and should be replaced later by the function above.
    public static NoiseVector[] getNoiseVector(final MetadataElement noiseVectorListElem) {

        final MetadataElement[] list = noiseVectorListElem.getElements();

        final List<NoiseVector> noiseVectorList = new ArrayList<>(5);
        for (MetadataElement noiseVectorElem : list) {
            final ProductData.UTC time = getTime(noiseVectorElem, "azimuthTime");
            final int line = Integer.parseInt(noiseVectorElem.getAttributeString("line"));

            final MetadataElement pixelElem = noiseVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
            final MetadataElement noiseLutElem = noiseVectorElem.getElement("noiseLut");
            final String noiseLUT = noiseLutElem.getAttributeString("noiseLut");

            final int[] pixelArray = new int[count];
            final float[] noiseLUTArray = new float[count];
            addToArray(pixelArray, 0, pixel, " ");
            addToArray(noiseLUTArray, 0, noiseLUT, " ");

            noiseVectorList.add(new NoiseVector(time, line, pixelArray, noiseLUTArray));
        }
        return noiseVectorList.toArray(new NoiseVector[noiseVectorList.size()]);
    }

    //todo: This function is currently used by Sentinel1CalibratorOp and should be replaced later by the function above.
    public static CalibrationVector[] getCalibrationVector(final MetadataElement calibrationVectorListElem,
                                                           final boolean outputSigmaBand,
                                                           final boolean outputBetaBand,
                                                           final boolean outputGammaBand,
                                                           final boolean outputDNBand) {

        final MetadataElement[] list = calibrationVectorListElem.getElements();

        final List<CalibrationVector> calibrationVectorList = new ArrayList<>(5);
        for (MetadataElement calibrationVectorElem : list) {
            final ProductData.UTC time = getTime(calibrationVectorElem, "azimuthTime");
            final int line = Integer.parseInt(calibrationVectorElem.getAttributeString("line"));

            final MetadataElement pixelElem = calibrationVectorElem.getElement("pixel");
            final String pixel = pixelElem.getAttributeString("pixel");
            final int count = Integer.parseInt(pixelElem.getAttributeString("count"));
            final int[] pixelArray = new int[count];
            addToArray(pixelArray, 0, pixel, " ");

            float[] sigmaNoughtArray = null;
            if (outputSigmaBand) {
                final MetadataElement sigmaNoughtElem = calibrationVectorElem.getElement("sigmaNought");
                final String sigmaNought = sigmaNoughtElem.getAttributeString("sigmaNought");
                sigmaNoughtArray = new float[count];
                addToArray(sigmaNoughtArray, 0, sigmaNought, " ");
            }

            float[] betaNoughtArray = null;
            if (outputBetaBand) {
                final MetadataElement betaNoughtElem = calibrationVectorElem.getElement("betaNought");
                final String betaNought = betaNoughtElem.getAttributeString("betaNought");
                betaNoughtArray = new float[count];
                addToArray(betaNoughtArray, 0, betaNought, " ");
            }

            float[] gammaArray = null;
            if (outputGammaBand) {
                final MetadataElement gammaElem = calibrationVectorElem.getElement("gamma");
                final String gamma = gammaElem.getAttributeString("gamma");
                gammaArray = new float[count];
                addToArray(gammaArray, 0, gamma, " ");
            }

            float[] dnArray = null;
            if (outputDNBand) {
                final MetadataElement dnElem = calibrationVectorElem.getElement("dn");
                final String dn = dnElem.getAttributeString("dn");
                dnArray = new float[count];
                addToArray(dnArray, 0, dn, " ");
            }

            calibrationVectorList.add(new CalibrationVector(
                    time, line, pixelArray, sigmaNoughtArray, betaNoughtArray, gammaArray, dnArray));
        }
        return calibrationVectorList.toArray(new CalibrationVector[calibrationVectorList.size()]);
    }

    //todo: This function is used by Sentinel1CalibratorOp and should be replaced by getPolarizations() function later.
    public static String[] getProductPolarizations(final MetadataElement absRoot) {

        final MetadataElement[] elems = absRoot.getElements();
        final List<String> polList = new ArrayList<>(4);
        for (MetadataElement elem : elems) {
            if (elem.getName().contains("Band_")) {
                final String pol = elem.getAttributeString("polarization", null);
                if (pol != null && !polList.contains(pol)) {
                    polList.add(pol);
                }
            }
        }

        if (polList.size() > 0) {
            return  polList.toArray(new String[polList.size()]);
        }

        final Product sourceProduct = absRoot.getProduct();
        final String[] sourceBandNames = sourceProduct.getBandNames();
        for (String bandName:sourceBandNames) {
            if (bandName.contains("HH")) {
                if (!polList.contains("HH")) {
                    polList.add("HH");
                }
            } else if (bandName.contains("HV")) {
                if (!polList.contains("HV")) {
                    polList.add("HV");
                }
            } else if (bandName.contains("VH")) {
                if (!polList.contains("VH")) {
                    polList.add("VH");
                }
            } else if (bandName.contains("VV")) {
                if (!polList.contains("VV")) {
                    polList.add("VV");
                }
            }
        }

        return polList.toArray(new String[polList.size()]);
    }

    public static String[] getProductSubswaths(final MetadataElement absRoot) {

        final MetadataElement[] elems = absRoot.getElements();
        final List<String> swathList = new ArrayList<>(4);
        for (MetadataElement elem : elems) {
            if (elem.getName().contains("Band_")) {
                final String swath = elem.getAttributeString("swath", null);
                if (swath != null && !swathList.contains(swath)) {
                    swathList.add(swath);
                }
            }
        }

        if (swathList.size() > 0) {
            return swathList.toArray(new String[swathList.size()]);
        }

        final Product sourceProduct = absRoot.getProduct();
        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        final String[] sourceBandNames = sourceProduct.getBandNames();
        for (String bandName:sourceBandNames) {
            if (bandName.contains(acquisitionMode)) {
                final int idx = bandName.indexOf(acquisitionMode);
                final String subSwathName = bandName.substring(idx, idx + 3);
                if (!swathList.contains(subSwathName)) {
                    swathList.add(subSwathName);
                }
            }
        }
        return swathList.toArray(new String[swathList.size()]);
    }

    public static ProductData.UTC getTime(final MetadataElement elem, final String tag) {

        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, sentinelDateFormat);
    }

    public String getAcquisitionMode() {
        return acquisitionMode;
    }

    /**
     * Get source product polarizations.
     *
     * @return The polarization array.
     */
    public String[] getPolarizations() {
        return polarizations;
    }

    /**
     * Get source product subSwath names.
     *
     * @return The subSwath name array.
     */
    public String[] getSubSwathNames() {
        return subSwathNames;
    }

    public SubSwathInfo[] getSubSwath() {
        return subSwath;
    }

    public int getNumOfSubSwath() {
        return numOfSubSwath;
    }

    public static boolean checkIfWideSwathProduct(final Product product) {
        final String[] bandNames = product.getBandNames();
        return (contains(bandNames, "IW1") && contains(bandNames, "IW2") ||
                contains(bandNames, "EW1") && contains(bandNames, "EW2"));
    }

    private static boolean contains(final String[] list, final String tag) {
        for(String s : list) {
            if(s.contains(tag))
                return true;
        }
        return false;
    }

    /**
     * Get azimuth time for given line index in given sub-swath.
     * @param y Line index in given sub-swath.
     * @param subSwathIndex Sub-swath index (start from 1).
     * @return The azimuth time.
     */
    public double getAzimuthTime(final int y, final int subSwathIndex) {

        final int burstIdx = y / subSwath[subSwathIndex - 1].linesPerBurst;
        final int lineIdxInBurst = y - burstIdx * subSwath[subSwathIndex - 1].linesPerBurst;
        return subSwath[subSwathIndex - 1].burstFirstLineTime[burstIdx] +
                lineIdxInBurst * subSwath[subSwathIndex - 1].azimuthTimeInterval;
    }

    /**
     * Get slant range time for given pixel index in given sub-swath.
     * @param x Pixel index in given sub-swath.
     * @param subSwathIndex Sub-swath index (start from 1).
     * @return The slant range time.
     */
    public double getSlantRangeTime(final int x, final int subSwathIndex) {

        return subSwath[subSwathIndex - 1].slrTimeToFirstPixel +
                x * subSwath[subSwathIndex - 1].rangePixelSpacing / Constants.lightSpeed;
    }

    /**
     * Get sub-swath index for given slant range time.
     * @param slrTime The given slant range time.
     * @return The sub-swath index (start from 1).
     */
    public int getSubSwathIndex(final double slrTime) {
        double startTime, endTime;
        for (int i = 0; i < numOfSubSwath; i++) {

            if (i == 0) {
                startTime = subSwath[i].slrTimeToFirstPixel;
            } else {
                startTime = 0.5 * (subSwath[i].slrTimeToFirstPixel + subSwath[i - 1].slrTimeToLastPixel);
            }

            if (i == numOfSubSwath - 1) {
                endTime = subSwath[i].slrTimeToLastPixel;
            } else {
                endTime = 0.5 * (subSwath[i].slrTimeToLastPixel + subSwath[i + 1].slrTimeToFirstPixel);
            }

            if (slrTime >= startTime && slrTime < endTime) {
                return i + 1; // sub-swath index start from 1
            }
        }

        return 0;
    }

    public void computeIndex(final double azTime, final double slrTime, final int subSwathIndex, Index index) {

        int j0 = -1, j1 = -1;
        double muX = 0;
        if (slrTime < subSwath[subSwathIndex - 1].slantRangeTime[0][0]) {
            j0 = 0;
            j1 = 1;
        } else if (slrTime >
                subSwath[subSwathIndex - 1].slantRangeTime[0][subSwath[subSwathIndex - 1].numOfGeoPointsPerLine - 1]) {
            j0 = subSwath[subSwathIndex - 1].numOfGeoPointsPerLine - 2;
            j1 = subSwath[subSwathIndex - 1].numOfGeoPointsPerLine - 1;
        } else {
            for (int j = 0; j < subSwath[subSwathIndex - 1].numOfGeoPointsPerLine - 1; j++) {
                if (subSwath[subSwathIndex - 1].slantRangeTime[0][j] <= slrTime &&
                        subSwath[subSwathIndex - 1].slantRangeTime[0][j + 1] > slrTime) {
                    j0 = j;
                    j1 = j + 1;
                    break;
                }
            }
        }

        muX = (slrTime - subSwath[subSwathIndex - 1].slantRangeTime[0][j0]) /
                (subSwath[subSwathIndex - 1].slantRangeTime[0][j1] -
                        subSwath[subSwathIndex - 1].slantRangeTime[0][j0]);

        int i0 = -1, i1 = -1;
        double muY = 0;
        for (int i = 0; i < subSwath[subSwathIndex - 1].numOfGeoLines - 1; i++) {
            final double i0AzTime = (1 - muX) * subSwath[subSwathIndex - 1].azimuthTime[i][j0] +
                    muX * subSwath[subSwathIndex - 1].azimuthTime[i][j1];

            final double i1AzTime = (1 - muX) * subSwath[subSwathIndex - 1].azimuthTime[i + 1][j0] +
                    muX * subSwath[subSwathIndex - 1].azimuthTime[i + 1][j1];

            if ((i == 0 && azTime < i0AzTime) ||
                    (i == subSwath[subSwathIndex - 1].numOfGeoLines - 2 && azTime >= i1AzTime) ||
                    (i0AzTime <= azTime && i1AzTime > azTime)) {

                i0 = i;
                i1 = i + 1;
                muY = (azTime - i0AzTime) / (i1AzTime - i0AzTime);
                break;
            }
        }

        index.i0 = i0;
        index.i1 = i1;
        index.j0 = j0;
        index.j1 = j1;
        index.muX = muX;
        index.muY = muY;
    }

    public double getLatitude(final double azimuthTime, final double slantRangeTime) {
        Index index = new Index();
        final int subSwathIndex = getSubSwathIndex(slantRangeTime);
        computeIndex(azimuthTime, slantRangeTime, subSwathIndex, index);
        return getLatitudeValue(index, subSwathIndex);
    }

    public double getLatitude(final double azimuthTime, final double slantRangeTime, final int subSwathIndex) {
        Index index = new Index();
        computeIndex(azimuthTime, slantRangeTime, subSwathIndex, index);
        return getLatitudeValue(index, subSwathIndex);
    }

    public double getLongitude(final double azimuthTime, final double slantRangeTime) {
        Index index = new Index();
        final int subSwathIndex = getSubSwathIndex(slantRangeTime);
        computeIndex(azimuthTime, slantRangeTime, subSwathIndex, index);
        return getLongitudeValue(index, subSwathIndex);
    }

    public double getLongitude(final double azimuthTime, final double slantRangeTime, final int subSwathIndex) {
        Index index = new Index();
        computeIndex(azimuthTime, slantRangeTime, subSwathIndex, index);
        return getLongitudeValue(index, subSwathIndex);
    }

    public double getSlantRangeTime(final double azimuthTime, final double slantRangeTime) {
        Index index = new Index();
        final int subSwathIndex = getSubSwathIndex(slantRangeTime);
        computeIndex(azimuthTime, slantRangeTime, subSwathIndex, index);
        return getSlantRangeTimeValue(index, subSwathIndex);
    }

    public double getIncidenceAngle(final double azimuthTime, final double slantRangeTime) {
        Index index = new Index();
        final int subSwathIndex = getSubSwathIndex(slantRangeTime);
        computeIndex(azimuthTime, slantRangeTime, subSwathIndex, index);
        return getIncidenceAngleValue(index, subSwathIndex);
    }

    private double getLatitudeValue(final Index index, final int subSwathIndex) {
        final double lat00 = subSwath[subSwathIndex - 1].latitude[index.i0][index.j0];
        final double lat01 = subSwath[subSwathIndex - 1].latitude[index.i0][index.j1];
        final double lat10 = subSwath[subSwathIndex - 1].latitude[index.i1][index.j0];
        final double lat11 = subSwath[subSwathIndex - 1].latitude[index.i1][index.j1];

        return (1 - index.muY) * ((1 - index.muX) * lat00 + index.muX * lat01) +
                index.muY * ((1 - index.muX) * lat10 + index.muX * lat11);
    }

    private double getLongitudeValue(final Index index, final int subSwathIndex) {
        final double lon00 = subSwath[subSwathIndex - 1].longitude[index.i0][index.j0];
        final double lon01 = subSwath[subSwathIndex - 1].longitude[index.i0][index.j1];
        final double lon10 = subSwath[subSwathIndex - 1].longitude[index.i1][index.j0];
        final double lon11 = subSwath[subSwathIndex - 1].longitude[index.i1][index.j1];

        return (1 - index.muY) * ((1 - index.muX) * lon00 + index.muX * lon01) +
                index.muY * ((1 - index.muX) * lon10 + index.muX * lon11);
    }

    private double getSlantRangeTimeValue(final Index index, final int subSwathIndex) {
        final double slrt00 = subSwath[subSwathIndex - 1].slantRangeTime[index.i0][index.j0];
        final double slrt01 = subSwath[subSwathIndex - 1].slantRangeTime[index.i0][index.j1];
        final double slrt10 = subSwath[subSwathIndex - 1].slantRangeTime[index.i1][index.j0];
        final double slrt11 = subSwath[subSwathIndex - 1].slantRangeTime[index.i1][index.j1];

        return (1 - index.muY) * ((1 - index.muX) * slrt00 + index.muX * slrt01) +
                index.muY * ((1 - index.muX) * slrt10 + index.muX * slrt11);
    }

    private double getIncidenceAngleValue(final Index index, final int subSwathIndex) {
        final double inc00 = subSwath[subSwathIndex - 1].incidenceAngle[index.i0][index.j0];
        final double inc01 = subSwath[subSwathIndex - 1].incidenceAngle[index.i0][index.j1];
        final double inc10 = subSwath[subSwathIndex - 1].incidenceAngle[index.i1][index.j0];
        final double inc11 = subSwath[subSwathIndex - 1].incidenceAngle[index.i1][index.j1];

        return (1 - index.muY) * ((1 - index.muX) * inc00 + index.muX * inc01) +
                index.muY * ((1 - index.muX) * inc10 + index.muX * inc11);
    }

    public static void updateBandNames(
            final MetadataElement absRoot, final java.util.List<String> selectedPolList, final String[] bandNames) {

        final MetadataElement[] children = absRoot.getElements();
        for (MetadataElement child : children) {
            final String childName = child.getName();
            if (childName.startsWith(AbstractMetadata.BAND_PREFIX)) {
                final String pol = childName.substring(childName.lastIndexOf("_") + 1);
                final String sw_pol = childName.substring(childName.indexOf("_") + 1);
                if (selectedPolList.contains(pol)) {
                    String bandNameArray = "";
                    for (String bandName : bandNames) {
                        if (bandName.contains(sw_pol)) {
                            bandNameArray += bandName + " ";
                        } else if (bandName.contains(pol)) {
                            bandNameArray += bandName + " ";
                        }
                    }
                    if(!bandNameArray.isEmpty()) {
                        child.setAttributeString(AbstractMetadata.band_names, bandNameArray);
                    }
                } else {
                    absRoot.removeElement(child);
                }
            }
        }
    }

    private static int[] getIntArray(final MetadataElement elem, final String tag) {

        final MetadataAttribute attribute = elem.getAttribute(tag);
        if (attribute == null) {
            throw new OperatorException(tag + " attribute not found");
        }

        int[] array = null;
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            final String dataStr = attribute.getData().getElemString();
            final String[] items = dataStr.split(" ");
            array = new int[items.length];
            for (int i = 0; i < items.length; i++) {
                try {
                    array[i] = Integer.parseInt(items[i]);
                } catch (NumberFormatException e) {
                    throw new OperatorException("Failed in getting" + tag + " array");
                }
            }
        }

        return array;
    }

    private static double[] getDoubleArray(final MetadataElement elem, final String tag) {

        final MetadataAttribute attribute = elem.getAttribute(tag);
        if (attribute == null) {
            throw new OperatorException(tag + " attribute not found");
        }

        double[] array = null;
        if (attribute.getData() instanceof ProductData.ASCII) {
            final String dataStr = attribute.getData().getElemString();
            final String[] items = dataStr.split(" ");
            array = new double[items.length];
            for (int i = 0; i < items.length; i++) {
                try {
                    array[i] = Double.parseDouble(items[i]);
                } catch (NumberFormatException e) {
                    throw new OperatorException("Failed in getting" + tag + " array");
                }
            }
        }

        return array;
    }

    private static int addToArray(final int[] array, int index, final String csvString, final String delim) {
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        while (tokenizer.hasMoreTokens()) {
            array[index++] = Integer.parseInt(tokenizer.nextToken());
        }
        return index;
    }

    private static int addToArray(final float[] array, int index, final String csvString, final String delim) {
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        while (tokenizer.hasMoreTokens()) {
            array[index++] = Float.parseFloat(tokenizer.nextToken());
        }
        return index;
    }


    public static class SubSwathInfo {

        // subswath info
        public String subSwathName;
        public int numOfLines;
        public int numOfSamples;
        public double firstLineTime;
        public double lastLineTime;
        public double slrTimeToFirstPixel;
        public double slrTimeToLastPixel;
        public double slrTimeToFirstValidPixel;
        public double slrTimeToLastValidPixel;
        public double azimuthTimeInterval;
        public double rangePixelSpacing;
        public double azimuthPixelSpacing;
        public double radarFrequency;
        public double azimuthSteeringRate;
        public int firstValidPixel;
        public int lastValidPixel;

        // bursts info
        public int numOfBursts;
        public int linesPerBurst;
        public int samplesPerBurst;
        public double[] burstFirstLineTime;
        public double[] burstLastLineTime;
        public double[] burstFirstValidLineTime;
        public double[] burstLastValidLineTime;
        public int[][] firstValidSample;
        public int[][] lastValidSample;
        public double[][] rangeDependDopplerRate;
        public double[][] dopplerRate;
        public double[][] referenceTime;
        public double[][] dopplerCentroid;

        // GeoLocationGridPoint
        public int numOfGeoLines;
        public int numOfGeoPointsPerLine;
        public double[][] azimuthTime;
        public double[][] slantRangeTime;
        public double[][] latitude;
        public double[][] longitude;
        public double[][] incidenceAngle;

        // Noise vectors
        public Map<String, NoiseVector[]> noise = new HashMap<>();

        // Calibration vectors
        public Map<String, CalibrationVector[]> calibration = new HashMap<>();

    }

    public static class AzimuthFmRate {
        public double time;
        public double t0;
        public double c0;
        public double c1;
        public double c2;
    }

    public static class DCPolynomial {
        public double time;
        public double t0;
        public double[] dataDcPolynomial;
    }

    public static class NoiseVector {
        public final double timeMJD;
        public final int line;
        public final int[] pixels;
        public final float[] noiseLUT;

        public NoiseVector(final ProductData.UTC time, final int line, final int[] pixels, final float[] noiseLUT) {
            this.timeMJD = time.getMJD();
            this.line = line;
            this.pixels = pixels;
            this.noiseLUT = noiseLUT;
        }
    }

    public static class CalibrationVector {
        public final double timeMJD;
        public final int line;
        public final int[] pixels;
        public final float[] sigmaNought;
        public final float[] betaNought;
        public final float[] gamma;
        public final float[] dn;

        public CalibrationVector(final ProductData.UTC time,
                                 final int line,
                                 final int[] pixels,
                                 final float[] sigmaNought,
                                 final float[] betaNought,
                                 final float[] gamma,
                                 final float[] dn) {
            this.timeMJD = time.getMJD();
            this.line = line;
            this.pixels = pixels;
            this.sigmaNought = sigmaNought;
            this.betaNought = betaNought;
            this.gamma = gamma;
            this.dn = dn;
        }
    }

    private static class Index {
        public int i0;
        public int i1;
        public int j0;
        public int j1;
        public double muX;
        public double muY;

        public Index() {
        }
    }

}
