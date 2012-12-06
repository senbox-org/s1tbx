/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos.basic;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dataio.binary.BinaryRecord;
import org.esa.nest.dataio.binary.IllegalBinaryFormatException;
import org.esa.nest.dataio.ceos.CEOSImageFile;
import org.esa.nest.dataio.ceos.CEOSProductDirectory;
import org.esa.nest.dataio.ceos.CeosHelper;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Orbits;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.Constants;
import org.esa.nest.util.GeoUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a product directory.
 *
 */
class BasicCeosProductDirectory extends CEOSProductDirectory {

    private BasicCeosImageFile[] imageFiles = null;
    private BasicCeosLeaderFile leaderFile = null;
    private BasicCeosTrailerFile trailerFile = null;

    private final transient Map<String, BasicCeosImageFile> bandImageFileMap = new HashMap<String, BasicCeosImageFile>(1);

    public BasicCeosProductDirectory(final File dir) {
        Guardian.assertNotNull("dir", dir);

        constants = new BasicCeosConstants();
        baseDir = dir;
    }

    @Override
    protected void readProductDirectory() throws IOException, IllegalBinaryFormatException {
        readVolumeDirectoryFile();

        leaderFile = new BasicCeosLeaderFile(
                createInputStream(CeosHelper.getCEOSFile(baseDir, constants.getLeaderFilePrefix())));
        final File trlFile = CeosHelper.getCEOSFile(baseDir, constants.getTrailerFilePrefix());
        if(trlFile != null) {
            trailerFile = new BasicCeosTrailerFile(createInputStream(trlFile));
        }

        BinaryRecord histogramRec = leaderFile.getHistogramRecord();
        if(histogramRec == null)
            histogramRec = trailerFile.getHistogramRecord();

        final String[] imageFileNames = CEOSImageFile.getImageFileNames(baseDir, constants.getImageFilePrefix());
        final List<BasicCeosImageFile> imgArray = new ArrayList<BasicCeosImageFile>(imageFileNames.length);
        for (String fileName : imageFileNames) {
            try {
                final BasicCeosImageFile imgFile = new BasicCeosImageFile(createInputStream(new File(baseDir, fileName)), histogramRec);
                imgArray.add(imgFile);
            } catch (Exception e) {
                e.printStackTrace();
                // continue
            }
        }
        imageFiles = imgArray.toArray(new BasicCeosImageFile[imgArray.size()]);

        sceneWidth = imageFiles[0].getRasterWidth();
        sceneHeight = imageFiles[0].getRasterHeight();
        assertSameWidthAndHeightForAllImages(imageFiles, sceneWidth, sceneHeight);
    }

    @Override
    public Product createProduct() throws IOException {
        assert(productType != null);
        productType = extractProductType(productType);

        final Product product = new Product(getProductName(), productType, sceneWidth, sceneHeight);

        if(imageFiles.length > 1) {
            int index = 1;
            for (final BasicCeosImageFile imageFile : imageFiles) {

                if(isProductSLC) {
                    final Band bandI = createBand(product, "i_" + index, Unit.REAL, imageFile);
                    final Band bandQ = createBand(product, "q_" + index, Unit.IMAGINARY, imageFile);
                    ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, "_"+index);
                    ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, "_"+index);
                } else {
                    final Band band = createBand(product, "Amplitude_" + index, Unit.AMPLITUDE, imageFile);
                    ReaderUtils.createVirtualIntensityBand(product, band, "_"+index);
                }
                ++index;
            }
        } else {
            final BasicCeosImageFile imageFile = imageFiles[0];
            if(isProductSLC) {
                final Band bandI = createBand(product, "i", Unit.REAL, imageFile);
                final Band bandQ = createBand(product, "q", Unit.IMAGINARY, imageFile);
                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, "");
                ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, "");
            } else {
                final Band band = createBand(product, "Amplitude", Unit.AMPLITUDE, imageFile);
                ReaderUtils.createVirtualIntensityBand(product, band, "");
            }
        }

        BinaryRecord facilityRec = leaderFile.getFacilityRecord();
        if(facilityRec == null)
            facilityRec = trailerFile.getFacilityRecord();
        BinaryRecord sceneRec = leaderFile.getSceneRecord();
        if(sceneRec == null)
            sceneRec = trailerFile.getSceneRecord();
        BinaryRecord detProcRec = leaderFile.getDetailedProcessingRecord();
        if(detProcRec == null)
            detProcRec = trailerFile.getDetailedProcessingRecord();
        BinaryRecord mapProjRec = leaderFile.getMapProjRecord();
        if(mapProjRec == null)
            mapProjRec = trailerFile.getMapProjRecord();

        product.setStartTime(getUTCScanStartTime(sceneRec, detProcRec));
        product.setEndTime(getUTCScanStopTime(sceneRec, detProcRec));
        product.setDescription(getProductDescription());

        addMetaData(product);

        // set slant_range_to_first_pixel in metadata
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final TiePointGrid slantRangeTimeTPG = product.getTiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME);
        if(slantRangeTimeTPG != null) {
            final int numOutputLines = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);
            final double slantRangeTime = slantRangeTimeTPG.getPixelFloat(numOutputLines/2, 0) / 1000000000.0; //s
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                    slantRangeTime*Constants.halfLightSpeed);
        }

        float[] latCorners = leaderFile.getLatCorners(leaderFile.getMapProjRecord());
        float[] lonCorners = leaderFile.getLonCorners(leaderFile.getMapProjRecord());
        if(latCorners == null || lonCorners == null) {
            latCorners = imageFiles[0].getLatCorners();
            lonCorners = imageFiles[0].getLonCorners();
        }
        if(latCorners != null && lonCorners != null) {
            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        }

        if(product.getGeoCoding() == null) {
            addGeoCodingFromSceneLabel(product);
        }

        if(product.getGeoCoding() == null) {
            addTPGGeoCoding(product, sceneRec);
        }

        if(mapProjRec == null) {
            setLatLonMetadata(product, absRoot);
        }

        if(VisatApp.getApp() != null) {
            VisatApp.getApp().showWarningDialog("This product is for an unknown mission in CEOS format.\n" +
                    "Some functionality may not be supported.\n" +
                    "Please contact nest_pr@array.ca for further support.");
        }

        return product;
    }

    private static String extractProductType(final String productType) {

        if(productType.contains("SLC"))
            return "SLC";
        else if(productType.contains("SGF"))
            return "SGF";
        else if(productType.contains("SGX"))
            return "SGX";
        else if(productType.contains("SSG"))
            return "SSG";
        else if(productType.contains("SCN"))
            return "SCN";
        else if(productType.contains("SCW"))
            return "SCW";
        else if(productType.trim().isEmpty())
            return "unknown";
        return productType;
    }

    public boolean isCeos() throws IOException {
        return true;
    }

    @Override
    public CEOSImageFile getImageFile(final Band band) {
        return bandImageFileMap.get(band.getName());
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < imageFiles.length; i++) {
            imageFiles[i].close();
            imageFiles[i] = null;
        }
        imageFiles = null;
    }

    private Band createBand(final Product product, final String name, final String unit, final BasicCeosImageFile imageFile) {

        final Band band = createBand(product, name, unit, imageFile.getBitsPerSample());
        bandImageFileMap.put(name, imageFile);

        return band;
    }

    private void addMetaData(final Product product) throws IOException {

        final MetadataElement root = product.getMetadataRoot();

        final MetadataElement leadMetadata = new MetadataElement("Leader");
        leaderFile.addMetadata(leadMetadata);
        root.addElement(leadMetadata);

        final MetadataElement trailMetadata = new MetadataElement("Trailer");
        trailerFile.addMetadata(trailMetadata);
        root.addElement(trailMetadata);

        final MetadataElement volMetadata = new MetadataElement("Volume");
        volumeDirectoryFile.assignMetadataTo(volMetadata);
        root.addElement(volMetadata);

        int c = 1;
        for (final BasicCeosImageFile imageFile : imageFiles) {
            imageFile.assignMetadataTo(root, c++);
        }

        addSummaryMetadata(new File(baseDir, BasicCeosConstants.SUMMARY_FILE_NAME), "Summary Information", root);
        addSummaryMetadata(new File(baseDir, BasicCeosConstants.SCENE_LABEL_FILE_NAME), "Scene Label", root);
        addSummaryMetadata(new File(baseDir.getParentFile(), BasicCeosConstants.SCENE_LABEL_FILE_NAME), "Scene Label", root);

        // try txt summary file
        // removed because it is not in the property name value format
        //final File volFile = CeosHelper.getCEOSFile(baseDir, constants.getVolumeFilePrefix());
        //final File txtFile = FileUtils.exchangeExtension(volFile, ".txt");
        //addSummaryMetadata(txtFile, "Scene Description", root);

        addAbstractedMetadataHeader(product, root);
    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        BinaryRecord mapProjRec = leaderFile.getMapProjRecord();
        if(mapProjRec == null)
            mapProjRec = trailerFile.getMapProjRecord();
        BinaryRecord sceneRec = leaderFile.getSceneRecord();
        if(sceneRec == null)
            sceneRec = trailerFile.getSceneRecord();
        final BinaryRecord radiometricRec = leaderFile.getRadiometricRecord();
        BinaryRecord facilityRec = leaderFile.getFacilityRecord();
        if(facilityRec == null)
            facilityRec = trailerFile.getFacilityRecord();
        BinaryRecord detProcRec = leaderFile.getDetailedProcessingRecord();
        if(detProcRec == null)
            detProcRec = trailerFile.getDetailedProcessingRecord();

        //mph
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                sceneRec.getAttributeString("Product type descriptor"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "RS1");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                getProcTime(volumeDirectoryFile.getVolumeDescriptorRecord()));

        final ProductData.UTC startTime = getUTCScanStartTime(sceneRec, detProcRec);
        final ProductData.UTC endTime = getUTCScanStopTime(sceneRec, detProcRec);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, endTime);

        if(mapProjRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat,
                    mapProjRec.getAttributeDouble("1st line 1st pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long,
                    mapProjRec.getAttributeDouble("1st line 1st pixel geodetic longitude"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat,
                    mapProjRec.getAttributeDouble("1st line last valid pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long,
                    mapProjRec.getAttributeDouble("1st line last valid pixel geodetic longitude"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat,
                    mapProjRec.getAttributeDouble("Last line 1st pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long,
                    mapProjRec.getAttributeDouble("Last line 1st pixel geodetic longitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat,
                    mapProjRec.getAttributeDouble("Last line last valid pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long,
                    mapProjRec.getAttributeDouble("Last line last valid pixel geodetic longitude"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, getPass(mapProjRec, sceneRec));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                mapProjRec.getAttributeDouble("Nominal inter-pixel distance in output scene"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                mapProjRec.getAttributeDouble("Nominal inter-line distance in output scene"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isGroundRange(mapProjRec));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection, getMapProjection(mapProjRec));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system,
                mapProjRec.getAttributeString("Name of reference ellipsoid"));

        } else if(sceneRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                sceneRec.getAttributeDouble("Pixel spacing"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                sceneRec.getAttributeDouble("Line spacing"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, getPass(mapProjRec, sceneRec));
        }

        //sph
        if(sceneRec != null) {
            final String absOrbit = sceneRec.getAttributeString("Orbit number").trim();
            if(absOrbit != null && !absOrbit.isEmpty()){
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, Integer.parseInt(absOrbit));
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    ReaderUtils.findPolarizationInBandName(
                            sceneRec.getAttributeString("Sensor ID and mode of operation for this channel")));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                    sceneRec.getAttributeString("Processing algorithm identifier"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                    sceneRec.getAttributeDouble("Nominal number of looks processed in azimuth"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                    sceneRec.getAttributeDouble("Nominal number of looks processed in range"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    sceneRec.getAttributeDouble("Pulse Repetition Frequency"));
            double radarFreq = sceneRec.getAttributeDouble("Radar frequency");
            if (Double.compare(radarFreq, 0.0) == 0) {
                final double radarWaveLength = sceneRec.getAttributeDouble("Radar wavelength"); // in m
                if (Double.compare(radarWaveLength, 0.0) != 0) {
                    radarFreq = Constants.lightSpeed / radarWaveLength / Constants.oneMillion; // in MHz
                }
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, radarFreq);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                sceneRec.getAttributeDouble("Range sampling rate"));

            // add Range and Azimuth bandwidth
            final double rangeBW = sceneRec.getAttributeDouble("Total processor bandwidth in range"); // Hz
            final double azimuthBW = sceneRec.getAttributeDouble("Total processor bandwidth in azimuth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, endTime, sceneHeight));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        if(facilityRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, AbstractMetadata.parseUTC(
                facilityRec.getAttributeString("Time of input state vector used to processed the image")));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag,
                    facilityRec.getAttributeInt("Antenna pattern correction flag"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag,
                    facilityRec.getAttributeInt("Range spreading loss compensation flag"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.replica_power_corr_flag, 0);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 0);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor,
                    facilityRec.getAttributeDouble("Absolute calibration constant K"));
        }

        if(radiometricRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor,
                radiometricRec.getAttributeDouble("Calibration constant"));
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.replica_power_corr_flag, 0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 0);

        addOrbitStateVectors(absRoot, leaderFile.getPlatformPositionRecord());
        if(facilityRec != null)
            addSRGRCoefficients(absRoot, facilityRec);
        else
            addSRGRCoefficients(absRoot, detProcRec);
    }

    private String getMapProjection(final BinaryRecord mapProjRec) {
        if(productType.contains("IMG") || productType.contains("GEC") || productType.contains("SSG")) {
            return mapProjRec.getAttributeString("Map projection descriptor");
        }
        return " ";
    }

    private String getProductName() {
        return volumeDirectoryFile.getProductName();
    }

    private String getProductDescription() {
        BinaryRecord sceneRecord = leaderFile.getSceneRecord();
        if(sceneRecord == null)
            sceneRecord = trailerFile.getSceneRecord();

        String level = "";
        if(sceneRecord != null) {
            level = sceneRecord.getAttributeString("Scene reference number").trim();
        }
        return BasicCeosConstants.PRODUCT_DESCRIPTION_PREFIX + level;
    }

    private static void addGeoCodingFromSceneLabel(Product product) {

        final MetadataElement sceneLabelElem = product.getMetadataRoot().getElement("Scene Label");
        if (sceneLabelElem != null) {

            try {
                final String ulLatLon = sceneLabelElem.getAttributeString("UL_CORNER_LAT_LON");
                final String urLatLon = sceneLabelElem.getAttributeString("UR_CORNER_LAT_LON");
                final String llLatLon = sceneLabelElem.getAttributeString("LL_CORNER_LAT_LON");
                final String lrLatLon = sceneLabelElem.getAttributeString("LR_CORNER_LAT_LON");

                final float latUL = Float.parseFloat(ulLatLon.substring(0, ulLatLon.indexOf(',')));
                final float latUR = Float.parseFloat(urLatLon.substring(0, urLatLon.indexOf(',')));
                final float latLL = Float.parseFloat(llLatLon.substring(0, llLatLon.indexOf(',')));
                final float latLR = Float.parseFloat(lrLatLon.substring(0, lrLatLon.indexOf(',')));
                final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};

                final float lonUL = Float.parseFloat(ulLatLon.substring(ulLatLon.indexOf(',')+1, ulLatLon.length()-1));
                final float lonUR = Float.parseFloat(urLatLon.substring(urLatLon.indexOf(',')+1, urLatLon.length()-1));
                final float lonLL = Float.parseFloat(llLatLon.substring(llLatLon.indexOf(',')+1, llLatLon.length()-1));
                final float lonLR = Float.parseFloat(lrLatLon.substring(lrLatLon.indexOf(',')+1, lrLatLon.length()-1));
                final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};

                ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
            } catch (Exception e) {
                Debug.trace(e.toString());
            }
        }
    }

    protected static void addOrbitStateVectors(final MetadataElement absRoot, final BinaryRecord platformPosRec) {
        if(platformPosRec == null) return;

        try {
            final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
            final int numPoints = platformPosRec.getAttributeInt("Number of data points");
            final double theta = platformPosRec.getAttributeDouble("Greenwich mean hour angle");
            /*
            final double firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD();
            final double lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD();
            int startIdx = 0;
            int endIdx = 0;
            for (int i = 1; i <= numPoints; i++) {
                double time = getOrbitTime(platformPosRec, i).getMJD();
                if (time < firstLineUTC) {
                    startIdx = i;
                }

                if (time < lastLineUTC) {
                    endIdx = i;
                }
            }
            startIdx = Math.max(startIdx - 1, 1);
            endIdx = Math.min(endIdx+1, numPoints);
            */
            for(int i = 1; i <= numPoints; ++i) {
                addVector(AbstractMetadata.orbit_vector, orbitVectorListElem, platformPosRec, theta, i);
            }

            if(absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME, new ProductData.UTC(0)).
                    equalElems(new ProductData.UTC(0))) {

                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME,
                    getOrbitTime(platformPosRec, 1));
            }
        } catch(Exception e) {
            // continue without state vectors
        }
    }

    private static void addVector(String name, MetadataElement orbitVectorListElem,
                                  BinaryRecord platformPosRec, double theta, int num) {

        final MetadataElement orbitVectorElem = new MetadataElement(name + num);

        final double xPosECI = platformPosRec.getAttributeDouble("Position vector X " + num);
        final double yPosECI = platformPosRec.getAttributeDouble("Position vector Y " + num);
        final double zPosECI = platformPosRec.getAttributeDouble("Position vector Z " + num);

        final double xVelECI = platformPosRec.getAttributeDouble("Velocity vector X' " + num)/1000.0; // mm to m
        final double yVelECI = platformPosRec.getAttributeDouble("Velocity vector Y' " + num)/1000.0;
        final double zVelECI = platformPosRec.getAttributeDouble("Velocity vector Z' " + num)/1000.0;

        final double thetaInRd = theta*MathUtils.DTOR;
        final double cosTheta = Math.cos(thetaInRd);
        final double sinTheta = Math.sin(thetaInRd);

        final double xPosECEF =  cosTheta*xPosECI + sinTheta*yPosECI;
        final double yPosECEF = -sinTheta*xPosECI + cosTheta*yPosECI;
        final double zPosECEF = zPosECI;

        double t = getOrbitTime(platformPosRec, num).getMJD() / 36525.0; // Julian centuries
        double a1 = 876600.0*3600.0 + 8640184.812866;
        double a2 = 0.093104;
        double a3 = -6.2e-6;
        double thp = ((a1 + 2.0*a2*t + 3.0*a3*t*t)/240.0*Math.PI/180.0)/(36525.0*86400.0);

        final double xVelECEF = -sinTheta*thp*xPosECI + cosTheta*thp*yPosECI + cosTheta*xVelECI + sinTheta*yVelECI;
        final double yVelECEF = -cosTheta*thp*xPosECI - sinTheta*thp*yPosECI - sinTheta*xVelECI + cosTheta*yVelECI;
        final double zVelECEF = zVelECI;

        orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, getOrbitTime(platformPosRec, num));
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos, xPosECEF);
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos, yPosECEF);
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos, zPosECEF);
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel, xVelECEF);
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel, yVelECEF);
        orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel, zVelECEF);

        orbitVectorListElem.addElement(orbitVectorElem);
    }

    protected static void addSRGRCoefficients(final MetadataElement absRoot, final BinaryRecord detailedProcRec) {
        if(detailedProcRec == null) return;

        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final Integer numSRGRCoefSets = detailedProcRec.getAttributeInt("Number of SRGR coefficient sets");
        if(numSRGRCoefSets == null) return;

        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-DDD-HH:mm:ss");

        for(int i=1; i <= numSRGRCoefSets; ++i) {

            final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list+"."+i);
            srgrCoefficientsElem.addElement(srgrListElem);

            final String updateTimeStr = detailedProcRec.getAttributeString("SRGR update date/time "+i);
            final ProductData.UTC utcTime = AbstractMetadata.parseUTC(updateTimeStr, dateFormat);
            srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
            AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                    ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
            AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

            addSRGRCoef(srgrListElem, detailedProcRec, "SRGR coefficients1 "+i, 1);
            addSRGRCoef(srgrListElem, detailedProcRec, "SRGR coefficients2 "+i, 2);
            addSRGRCoef(srgrListElem, detailedProcRec, "SRGR coefficients3 "+i, 3);
            addSRGRCoef(srgrListElem, detailedProcRec, "SRGR coefficients4 "+i, 4);
            addSRGRCoef(srgrListElem, detailedProcRec, "SRGR coefficients5 "+i, 5);
            addSRGRCoef(srgrListElem, detailedProcRec, "SRGR coefficients6 "+i, 6);
        }
    }

    private void addRSATTiePointGrids(final Product product, final BinaryRecord sceneRec, final BinaryRecord detProcRec) {

        final int gridWidth = 11;
        final int gridHeight = 11;
        final int sceneWidth = product.getSceneRasterWidth();
        final int sceneHeight = product.getSceneRasterHeight();
        final int subSamplingX = sceneWidth / (gridWidth - 1);
        final int subSamplingY = sceneHeight / (gridHeight - 1);
        final float[] rangeDist = new float[gridWidth*gridHeight];
        final float[] rangeTime = new float[gridWidth*gridHeight];

        int k = 0;
        for (int j = 0; j < gridHeight; j++) {
            final int y = Math.min(j*subSamplingY, sceneHeight-1);
            final double slantRangeToFirstPixel = imageFiles[0].getSlantRangeToFirstPixel(y); // meters
            final double slantRangeToMidPixel = imageFiles[0].getSlantRangeToMidPixel(y);
            final double slantRangeToLastPixel = imageFiles[0].getSlantRangeToLastPixel(y);
            final double[] polyCoef = computePolynomialCoefficients(slantRangeToFirstPixel,
                    slantRangeToMidPixel,
                    slantRangeToLastPixel,
                    sceneWidth);

            for(int i = 0; i < gridWidth; i++) {
                final int x = i*subSamplingX;
                rangeDist[k++] = (float)(polyCoef[0] + polyCoef[1]*x + polyCoef[2]*x*x);
            }
        }

        // get slant range time in nanoseconds from range distance in meters
        for(k = 0; k < rangeDist.length; k++) {
            rangeTime[k] = (float)(rangeDist[k] / Constants.halfLightSpeed)*1000000000;// in ns
        }

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, rangeTime);

        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);

        if(detProcRec == null)
            return;

        final double r = calculateEarthRadius(sceneRec);    // earth radius
        final double eph_orb_data = detProcRec.getAttributeDouble("Ephemeris orbit data1");
        final double h = eph_orb_data - r;                  // orbital altitude

        // incidence angle
        final float[] angles = new float[gridWidth*gridHeight];

        k = 0;
        for(int j = 0; j < gridHeight; j++) {
            for (int i = 0; i < gridWidth; i++) {
                final double RS = rangeDist[k];
                final double a = ( (h*h) - (RS*RS) + (2.0*r*h) ) / (2.0*RS*r);
                angles[k] = (float)(Math.acos( a ) * MathUtils.RTOD);
                k++;
            }
        }

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, angles);

        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);
    }

    private static double[] computePolynomialCoefficients(
                    double slantRangeToFirstPixel, double slantRangeToMidPixel, double slantRangeToLastPixel, int imageWidth) {

        final int firstPixel = 0;
        final int midPixel = imageWidth/2;
        final int lastPixel = imageWidth - 1;
        final double[] idxArray = {firstPixel, midPixel, lastPixel};
        final double[] rangeArray = {slantRangeToFirstPixel, slantRangeToMidPixel, slantRangeToLastPixel};
        final Matrix A = org.esa.nest.util.MathUtils.createVandermondeMatrix(idxArray, 2);
        final Matrix b = new Matrix(rangeArray, 3);
        final Matrix x = A.solve(b);
        return x.getColumnPackedCopy();
    }

    private static double calculateEarthRadius(BinaryRecord sceneRec) {

        final double platLat = sceneRec.getAttributeDouble("Sensor platform geodetic latitude at nadir");
        final double a = Math.tan(platLat * MathUtils.DTOR);
        final double a2 = a*a;
        final double ellipmin = Constants.semiMinorAxis;
        final double ellipmin2 = ellipmin * ellipmin;
        final double ellipmaj = Constants.semiMajorAxis;
        final double ellipmaj2 = ellipmaj * ellipmaj;

        return Constants.semiMinorAxis * (Math.sqrt(1+a2) / Math.sqrt((ellipmin2/ellipmaj2) + a2));
    }

    /**
     * Update target product GEOCoding. A new tie point grid is generated.
     * @param product The product.
     * @param sceneRec The scene record.
     * @throws java.io.IOException The exceptions.
     */
    private static void addTPGGeoCoding(final Product product, final BinaryRecord sceneRec) throws IOException {

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float[] targetLatTiePoints = new float[gridWidth*gridHeight];
        final float[] targetLonTiePoints = new float[gridWidth*gridHeight];
        final int sourceImageWidth = product.getSceneRasterWidth();
        final int sourceImageHeight = product.getSceneRasterHeight();

        final float subSamplingX = sourceImageWidth / (float)(gridWidth - 1);
        final float subSamplingY = sourceImageHeight / (float)(gridHeight - 1);

        final TiePointGrid slantRangeTime = product.getTiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME);
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final double firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD();
        final double lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD();
        final double lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day

        final double latMid = sceneRec.getAttributeDouble("scene centre geodetic latitude");
        final double lonMid = sceneRec.getAttributeDouble("scene centre geodetic longitude");

        AbstractMetadata.OrbitStateVector[] orbitStateVectors;
        try {
            orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        if(!checkStateVectorValidity(orbitStateVectors))
            return;

        final int numVectors = orbitStateVectors.length;
        int startIdx = 0;
        int endIdx = 0;
        final double t1 = Math.min(firstLineUTC, lastLineUTC);
        final double t2 = Math.max(firstLineUTC, lastLineUTC);
        for (int i = 0; i < numVectors; i++) {
            double time = orbitStateVectors[i].time_mjd;
            if (time < t1) {
                startIdx = i;
            }

            if (time < t2) {
                endIdx = i;
            }
        }

        while (endIdx - startIdx + 1 < Math.min(5, numVectors)) {
            startIdx = Math.max(startIdx - 1, 0);
            endIdx = Math.min(endIdx + 1, numVectors - 1);
        }
        final int numVectorsUsed = endIdx - startIdx + 1;

        final double[] timeArray = new double[numVectorsUsed];
        final double[] xPosArray = new double[numVectorsUsed];
        final double[] yPosArray = new double[numVectorsUsed];
        final double[] zPosArray = new double[numVectorsUsed];
        final double[] xVelArray = new double[numVectorsUsed];
        final double[] yVelArray = new double[numVectorsUsed];
        final double[] zVelArray = new double[numVectorsUsed];

        for (int i = startIdx; i <= endIdx; i++) {
            timeArray[i - startIdx] = orbitStateVectors[i].time_mjd;
            xPosArray[i - startIdx] = orbitStateVectors[i].x_pos; // m
            yPosArray[i - startIdx] = orbitStateVectors[i].y_pos; // m
            zPosArray[i - startIdx] = orbitStateVectors[i].z_pos; // m
            xVelArray[i - startIdx] = orbitStateVectors[i].x_vel; // m/s
            yVelArray[i - startIdx] = orbitStateVectors[i].y_vel; // m/s
            zVelArray[i - startIdx] = orbitStateVectors[i].z_vel; // m/s
        }

        // Create new tie point grid
        int k = 0;
        for (int r = 0; r < gridHeight; r++) {
            // get the zero Doppler time for the rth line
            int y;
            if (r == gridHeight - 1) { // last row
                y = sourceImageHeight - 1;
            } else { // other rows
                y = (int)(r * subSamplingY);
            }
            final double curLineUTC = firstLineUTC + y*lineTimeInterval;
            //System.out.println((new ProductData.UTC(curLineUTC)).toString());

            // compute the satellite position and velocity for the zero Doppler time using cubic interpolation
            final Orbits.OrbitData data = getOrbitData(curLineUTC, timeArray, xPosArray, yPosArray, zPosArray,
                                                xVelArray, yVelArray, zVelArray);

            for (int c = 0; c < gridWidth; c++) {
                int x;
                if (c == gridWidth - 1) { // last column
                    x = sourceImageWidth - 1;
                } else { // other columns
                    x = (int)(c * subSamplingX);
                }

                final double slrgTime = slantRangeTime.getPixelFloat((float)x, (float)y) / 1000000000.0; // ns to s;
                final GeoPos geoPos = computeLatLon(latMid, lonMid, slrgTime, data);
                targetLatTiePoints[k] = geoPos.lat;
                targetLonTiePoints[k] = geoPos.lon; 
                ++k;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetLatTiePoints);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetLonTiePoints, TiePointGrid.DISCONT_AT_180);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(tpGeoCoding);
    }

    private static boolean checkStateVectorValidity(AbstractMetadata.OrbitStateVector[] orbitStateVectors) {

        if(orbitStateVectors == null) {
            return false;
        }
        if(orbitStateVectors.length <= 1) {
            return false;
        }

        for (int i = 1; i < orbitStateVectors.length; i++) {
            if (orbitStateVectors[i].time_mjd == orbitStateVectors[0].time_mjd) {
                return false;
            }
        }

        return true;
    }

    private static void setLatLonMetadata(final Product product, final MetadataElement absRoot) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if(geoCoding == null) return;

        final GeoPos geoPosFirstNear = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null);
        final GeoPos geoPosFirstFar = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth()-1,
                                                                                   0), null);
        final GeoPos geoPosLastNear = product.getGeoCoding().getGeoPos(new PixelPos(0,
                                                                                   product.getSceneRasterHeight()-1), null);
        final GeoPos geoPosLastFar = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth()-1,
                                                                                   product.getSceneRasterHeight()-1), null);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, geoPosFirstNear.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, geoPosFirstNear.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, geoPosFirstFar.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, geoPosFirstFar.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, geoPosLastNear.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, geoPosLastNear.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, geoPosLastFar.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, geoPosLastFar.getLon());
    }

    /**
     * Compute accurate target geo position.
     * @param latMid The scene latitude.
     * @param lonMid The scene longitude.
     * @param slrgTime The slant range time of the given pixel.
     * @param data The orbit data.
     * @return The geo position of the target.
     */
    private static GeoPos computeLatLon(final double latMid, final double lonMid, double slrgTime, Orbits.OrbitData data) {

        final double[] xyz = new double[3];
        final GeoPos geoPos = new GeoPos((float)latMid, (float)lonMid);

        // compute initial (x,y,z) coordinate from lat/lon
        GeoUtils.geo2xyz(geoPos, xyz);

        // compute accurate (x,y,z) coordinate using Newton's method
        GeoUtils.computeAccurateXYZ(data, xyz, slrgTime);

        // compute (lat, lon, alt) from accurate (x,y,z) coordinate
        GeoUtils.xyz2geo(xyz, geoPos);

        return geoPos;
    }

    /**
     * Get orbit information for given time.
     * @param utc The UTC in days.
     * @param timeArray Array holding zeros Doppler times for all state vectors.
     * @param xPosArray Array holding x coordinates for sensor positions in all state vectors.
     * @param yPosArray Array holding y coordinates for sensor positions in all state vectors.
     * @param zPosArray Array holding z coordinates for sensor positions in all state vectors.
     * @param xVelArray Array holding x velocities for sensor positions in all state vectors.
     * @param yVelArray Array holding y velocities for sensor positions in all state vectors.
     * @param zVelArray Array holding z velocities for sensor positions in all state vectors.
     * @return The orbit information.
     */
    private static Orbits.OrbitData getOrbitData(final double utc, final double[] timeArray,
                                         final double[] xPosArray, final double[] yPosArray, final double[] zPosArray,
                                         final double[] xVelArray, final double[] yVelArray, final double[] zVelArray) {

        // Lagrange polynomial interpolation
        final Orbits.OrbitData orbitData = new Orbits.OrbitData();
        orbitData.xPos = org.esa.nest.util.MathUtils.lagrangeInterpolatingPolynomial(timeArray, xPosArray, utc);
        orbitData.yPos = org.esa.nest.util.MathUtils.lagrangeInterpolatingPolynomial(timeArray, yPosArray, utc);
        orbitData.zPos = org.esa.nest.util.MathUtils.lagrangeInterpolatingPolynomial(timeArray, zPosArray, utc);
        orbitData.xVel = org.esa.nest.util.MathUtils.lagrangeInterpolatingPolynomial(timeArray, xVelArray, utc);
        orbitData.yVel = org.esa.nest.util.MathUtils.lagrangeInterpolatingPolynomial(timeArray, yVelArray, utc);
        orbitData.zVel = org.esa.nest.util.MathUtils.lagrangeInterpolatingPolynomial(timeArray, zVelArray, utc);

        return orbitData;
    }
}
