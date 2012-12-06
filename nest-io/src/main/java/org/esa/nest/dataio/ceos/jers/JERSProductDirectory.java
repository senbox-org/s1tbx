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
package org.esa.nest.dataio.ceos.jers;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Guardian;
import org.esa.nest.dataio.binary.BinaryRecord;
import org.esa.nest.dataio.binary.IllegalBinaryFormatException;
import org.esa.nest.dataio.ceos.CEOSImageFile;
import org.esa.nest.dataio.ceos.CEOSProductDirectory;
import org.esa.nest.dataio.ceos.CeosHelper;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a product directory.
 * <p/>
 * <p>This class is public for the benefit of the implementation of another (internal) class and its API may
 * change in future releases of the software.</p>
 *
 */
class JERSProductDirectory extends CEOSProductDirectory {

    private JERSImageFile[] imageFiles = null;
    private JERSLeaderFile leaderFile = null;
    private JERSTrailerFile trailerFile = null;

    private final transient Map<String, JERSImageFile> bandImageFileMap = new HashMap<String, JERSImageFile>(1);

    public JERSProductDirectory(final File dir) {
        Guardian.assertNotNull("dir", dir);

        constants = new JERSConstants();
        baseDir = dir;
    }

    @Override
    protected void readProductDirectory() throws IOException, IllegalBinaryFormatException {
        readVolumeDirectoryFile();
        leaderFile = new JERSLeaderFile(
                createInputStream(CeosHelper.getCEOSFile(baseDir, constants.getLeaderFilePrefix())));
        
        final String[] imageFileNames = CEOSImageFile.getImageFileNames(baseDir, constants.getImageFilePrefix());
        final List<JERSImageFile> imgArray = new ArrayList<JERSImageFile>(imageFileNames.length);
        for (String fileName : imageFileNames) {
            try {
                final JERSImageFile imgFile = new JERSImageFile(createInputStream(new File(baseDir, fileName)));
                imgArray.add(imgFile);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                // continue
            }
        }
        imageFiles = imgArray.toArray(new JERSImageFile[imgArray.size()]);

        productType = leaderFile.getSceneRecord().getAttributeString("Product type descriptor");
        sceneWidth = imageFiles[0].getRasterWidth();
        sceneHeight = imageFiles[0].getRasterHeight();
        assertSameWidthAndHeightForAllImages(imageFiles, sceneWidth, sceneHeight);
    }

    @Override
    public Product createProduct() throws IOException {
        final Product product = new Product(getProductName(),
                                            productType,
                sceneWidth, sceneHeight);

        if(imageFiles.length > 1) {
            int index = 1;
            for (final JERSImageFile imageFile : imageFiles) {

                if(isProductSLC) {
                    final Band bandI = createBand(product, "i_" + index, Unit.REAL, imageFile);
                    final Band bandQ = createBand(product, "q_" + index, Unit.IMAGINARY, imageFile);
                    ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, "_"+index);
                    ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, "_"+index);
                } else {
                    Band band = createBand(product, "Amplitude_" + index, Unit.AMPLITUDE, imageFile);
                    ReaderUtils.createVirtualIntensityBand(product, band, "_"+index);
                }
                ++index;
            }
        } else {
            final JERSImageFile imageFile = imageFiles[0];
            if(isProductSLC) {
                final Band bandI = createBand(product, "i", Unit.REAL, imageFile);
                final Band bandQ = createBand(product, "q", Unit.IMAGINARY, imageFile);
                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, "");
                ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, "");
            } else {
                Band band = createBand(product, "Amplitude", Unit.AMPLITUDE, imageFile);
                ReaderUtils.createVirtualIntensityBand(product, band, "");
            }
        }

        product.setStartTime(getUTCScanStartTime(leaderFile.getSceneRecord(), null));
        product.setEndTime(getUTCScanStopTime(leaderFile.getSceneRecord(), null));
        product.setDescription(getProductDescription());

        ReaderUtils.addGeoCoding(product, leaderFile.getLatCorners(leaderFile.getMapProjRecord()),
                                          leaderFile.getLonCorners(leaderFile.getMapProjRecord()));
        addTiePointGrids(product, leaderFile.getFacilityRecord(), leaderFile.getSceneRecord());
        addMetaData(product);

        return product;
    }

    public boolean isJERS() throws IOException {
        final String volumeId = getVolumeId().toUpperCase();
        final String logicalVolumeId = getLogicalVolumeId().toUpperCase();
        return (volumeId.contains("JERS") || volumeId.contains("J1") || volumeId.contains("JE1") ||
                logicalVolumeId.contains("JERS") || logicalVolumeId.contains("J1") || logicalVolumeId.contains("JE1"));
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

    private Band createBand(final Product product, final String name, final String unit, final JERSImageFile imageFile) {

        final Band band = createBand(product, name, unit, imageFile.getBitsPerSample());
        bandImageFileMap.put(name, imageFile);

        return band;
    }

    private void addMetaData(final Product product) throws IOException {
        final MetadataElement root = product.getMetadataRoot();

        final MetadataElement leadMetadata = new MetadataElement("Leader");
        leaderFile.addMetadata(leadMetadata);
        root.addElement(leadMetadata);

        if(trailerFile != null) {
            final MetadataElement trailMetadata = new MetadataElement("Trailer");
            trailerFile.addMetadata(trailMetadata);
            root.addElement(trailMetadata);
        }

        final MetadataElement volMetadata = new MetadataElement("Volume");
        volumeDirectoryFile.assignMetadataTo(volMetadata);
        root.addElement(volMetadata);

        int c = 1;
        for (final JERSImageFile imageFile : imageFiles) {
            imageFile.assignMetadataTo(root, c++);
        }

        addSummaryMetadata(new File(baseDir, JERSConstants.SUMMARY_FILE_NAME), "Summary Information", root);
        addAbstractedMetadataHeader(product, root);
    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        final BinaryRecord sceneRec = leaderFile.getSceneRecord();
        final BinaryRecord mapProjRec = leaderFile.getMapProjRecord();
        final BinaryRecord facilityRec = leaderFile.getFacilityRecord();

        //mph
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                sceneRec.getAttributeString("Product type descriptor"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "JERS1");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, "Stripmap");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, "right");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                getProcTime(volumeDirectoryFile.getVolumeDescriptorRecord()));

        final ProductData.UTC startTime = getUTCScanStartTime(sceneRec, null);
        final ProductData.UTC endTime = getUTCScanStopTime(sceneRec, null);
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
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                sceneRec.getAttributeString("Processing system identifier").trim() );
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                    sceneRec.getAttributeString("Processing algorithm identifier"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT,
                Integer.parseInt(sceneRec.getAttributeString("Orbit number").trim()));

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
            double radarFreq = sceneRec.getAttributeDouble("Radar frequency") * 1000.0;
            if(radarFreq == 0) {
                radarFreq = getRadarFrequency(sceneRec);
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, radarFreq);
            Double slantRangeTime = sceneRec.getAttributeDouble("Zero-doppler range time of first range pixel");
            if(slantRangeTime != null) {
                slantRangeTime *= 0.001; //s
                AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                    slantRangeTime* Constants.halfLightSpeed);
            }

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    sceneRec.getAttributeDouble("Range sampling rate"));

            // add Range and Azimuth bandwidth
            final double rangeBW = sceneRec.getAttributeDouble("Total processor bandwidth in range"); // MHz
            final double azimuthBW = sceneRec.getAttributeDouble("Total processor bandwidth in azimuth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW);
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
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor,
                    facilityRec.getAttributeDouble("Absolute calibration constant K"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);
        }

        addOrbitStateVectors(absRoot, leaderFile.getPlatformPositionRecord());
        //addSRGRCoefficients(absRoot, facilityRec);
    }

    private String getMapProjection(BinaryRecord mapProjRec) {
        if(productType.contains("IMG") || productType.contains("GEC")) {
            return mapProjRec.getAttributeString("Map projection descriptor");
        }
        return " ";
    }

    private String getProductName() {
        return volumeDirectoryFile.getProductName();
    }

    private String getProductDescription() {
        BinaryRecord sceneRecord = leaderFile.getSceneRecord();
        
        String level = "";
        if(sceneRecord != null) {
            level = sceneRecord.getAttributeString("Scene reference number").trim();
        }
        return JERSConstants.PRODUCT_DESCRIPTION_PREFIX + level;
    }
}