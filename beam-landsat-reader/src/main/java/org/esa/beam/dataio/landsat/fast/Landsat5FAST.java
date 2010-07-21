/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.landsat.fast;

import org.esa.beam.dataio.landsat.GeoPoint;
import org.esa.beam.dataio.landsat.GeometricData;
import org.esa.beam.dataio.landsat.Landsat5TMBand;
import org.esa.beam.dataio.landsat.LandsatBandReader;
import org.esa.beam.dataio.landsat.LandsatByteBandReader;
import org.esa.beam.dataio.landsat.LandsatConstants;
import org.esa.beam.dataio.landsat.LandsatHeader;
import org.esa.beam.dataio.landsat.LandsatImageInputStream;
import org.esa.beam.dataio.landsat.LandsatTMBand;
import org.esa.beam.dataio.landsat.LandsatTMData;
import org.esa.beam.dataio.landsat.LandsatTMFile;
import org.esa.beam.dataio.landsat.LandsatUtils;
import org.esa.beam.dataio.landsat.RadiometricData;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.Debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class <code>Landsat5FAST</code> is used to store amd init the data of a landsat 5 FAST product
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class Landsat5FAST extends Landsat5FASTConstants implements LandsatTMData {

    private static final int DEFAULT_NUMBER = 7;
    private static final int SEPERATOR = 1;
    private static final int BLANK = 1;

    private static final String PRODUCTNAME = Landsat5FASTConstants.L5_PRODUCT_NAME;
    private static final int FORMAT = LandsatConstants.FAST_L5;

    private LandsatHeader landsatHeader;
    private final LandsatTMFile inputFile;
    private LandsatTMBand[] landsatBands;
    private Landsat5FASTMetadata metadata;
    private Map<LandsatTMBand, LandsatBandReader> bandReaders;


    /**
     * creates a Landsat5Fast format object
     *
     * @param inputFile the input file
     *
     * @throws java.io.IOException if an IO error occurs
     */
    public Landsat5FAST(final LandsatTMFile inputFile) throws IOException {
        this.inputFile = inputFile;
        init();
    }

    /**
     * delivers the name of the landsatproduct
     *
     * @return name of the product
     */
    @Override
    public String getProductName() {
        return PRODUCTNAME;
    }

    /**
     * delivers the landsat Header object
     *
     * @return landsat Header
     */
    @Override
    public LandsatHeader getHeader() {
        return landsatHeader;
    }

    /**
     * @param band the band to get the {@link LandsatBandReader reader} for.
     *
     * @return bandreader of the passed band
     */
    @Override
    public LandsatBandReader getBandReader(final ConstBand band) {
        LandsatTMBand landsatBand = extractLandsatTMBand(band);
        return bandReaders.get(landsatBand);
    }


    /**
     * Adds the reader to the list of available readers.
     *
     * @param reader the reader
     */
    private void addBandReader(final LandsatBandReader reader) {

        LandsatTMBand band = getBand(reader.getBandName());

        if (band != null) {
            bandReaders.put(band, reader);
        } else {
            Debug.trace("band = null");
        }
    }

    /**
     * @return Landsat 5 FAST format metadata
     */
    @Override
    public List getMetadata() {
        return metadata.getLandsatMetadataElements();
    }

    @Override
    public LandsatTMBand getBandAt(final int idx) {
        return landsatBands[idx];
    }

    @Override
    public int getFormat() {
        return FORMAT;
    }


    /**
     * close all open inputstreams of the open band readers
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (bandReaders != null) {
            for (LandsatBandReader element : bandReaders.values()) {
                element.close();
            }
        }
        if (landsatHeader != null) {
            landsatHeader.close();
        }
        if (inputFile != null) {
            inputFile.close();
        }
    }

    /**
     * @param band the band to get the {@link LandsatBandReader reader} for.
     *
     * @return LandsatBandReader the apropriate LandsatBandReader object
     */
    @Override
    public LandsatBandReader getBandReader(final Band band) {
        return getBandReader(ConstBand.getConstBand(band));
    }

    private LandsatTMBand getBand(final String BandName) {
        for (LandsatTMBand landsatBand : landsatBands) {
            if (landsatBand.getBandName().equals(BandName)) {
                return landsatBand;
            }
        }
        return null;
    }

    /**
     * Builds the landsat FAST object with LANDSAT 5 TM specific data
     *
     * @throws java.io.IOException if an IO error occurs
     */
    private void init() throws IOException {
        bandReaders = new HashMap<LandsatTMBand, LandsatBandReader>();

        readHeaderData();

        final int[] presentBands = landsatHeader.getBandsPresent();
        landsatBands = new Landsat5TMBand[presentBands.length];
        Landsat5FASTImageSources sources = new Landsat5FASTImageSources(inputFile);

        if (presentBands.length > sources.getSize()) {
            throw new IOException("Not able to read product. At least one data file is missing.");
        }

        for (int i = 0; i < presentBands.length; i++) {
            landsatBands[i] = new Landsat5TMBand(i, presentBands[i], sources.getLandsatImageSourceAt(i),
                                                 RadiometricData.getRadiometricData(), inputFile);
        }

        metadata = new Landsat5FASTMetadata(landsatHeader, landsatBands);
        createBandReaders();
    }

    private void readHeaderData() throws
                                  IOException {
        LandsatImageInputStream headerInputStream = null;
        try {
            landsatHeader = new LandsatHeader(inputFile);
            headerInputStream = landsatHeader.getHeaderInputStreamAt("header"); //todo
            if (headerInputStream == null) {
                throw new IOException("Failed to read LANDSAT-5 header data.");
            }

            landsatHeader.setAcquisitionDate(DATE_OF_AQUISITION_OFFSET_FASTB, DATE_OF_AQUISITION_SIZE_FASTB,
                                             headerInputStream, Landsat5FASTConstants.YYYY_MMDD);
            landsatHeader.setLoc(PATH_ROW_FRACTION_OFFSET_FASTB, PATH_ROW_FRACTION_SIZE_FASTB, FAST_L5,
                                 headerInputStream);
            landsatHeader.setInstrumentTyp(INSTRUMENT_OFFSET_FASTB, INSTRUMENT_SIZE_FASTB, headerInputStream);
            landsatHeader.setInstrumentMode(INSTRUMENT_MODE_OFFSET_FASTB, INSTRUMENT_MODE_SIZE_FASTB,
                                            headerInputStream);

            readAdministrationData(headerInputStream);
            readGeometricData(headerInputStream);
            readRadiometricData(headerInputStream);

        } finally {
            if (headerInputStream != null) {
                try {
                    headerInputStream.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    private void readAdministrationData(final LandsatImageInputStream headerInputStream) throws IOException {

        landsatHeader.setBandsPresent(BANDS_PRESENT_OFFSET_FASTB, BANDS_PRESENT_SIZE_FASTB, headerInputStream);
        landsatHeader.setBlockingFactor(BLOCKING_FACTOR_OFFSET_FASTB, BLOCKING_FACTOR_SIZE_FASTB, headerInputStream);
        landsatHeader.setFormatVersion(FORMAT_VERSION_OFFSET_FASTB, FORMAT_VERSION_SIZE_FASTB, headerInputStream);
        landsatHeader.setImageHeight(LINES_PER_IMAGE_OFFSET_FASTB, LINES_PER_IMAGE_SIZE_FASTB, headerInputStream);
        landsatHeader.setLinesPerVolume(LINES_PER_VOLUMES_OFFSET_FASTB, LINES_PER_VOLUMES_SIZE_FASTB,
                                        headerInputStream);
        landsatHeader.setPixelSize(PIXEL_SIZE_OFFSET_FASTB, PIXEL_SIZE_SIZE_FASTB, headerInputStream);
        landsatHeader.setImageWidth(PIXELS_PER_LINE_OFFSET_FASTB, PIXELS_PER_LINE_SIZE_FASTB, headerInputStream);
        landsatHeader.setProductID(PRODUCT_ID_OFFSET_FASTB, PRODUCT_ID_SIZE_FASTB, headerInputStream);
        landsatHeader.setProductSize(SIZE_OF_PRODUCT_OFFSET_FASTB, SIZE_OF_PRODUCT_SIZE_FASTB, headerInputStream);
        landsatHeader.setProductType(TYPE_OF_PRODUCT_OFFSET_FASTB, TYPE_OF_PRODUCT_SIZE_FASTB, headerInputStream);
        landsatHeader.setRecordLength(RECORD_LENGTH_OFFSET_FASTB, RECORD_LENGTH_SIZE_FASTB, headerInputStream);
        landsatHeader.setResampling(RESAMPLING_OFFSET_FASTB, RESAMPLING_SIZE_FASTB, headerInputStream);
        landsatHeader.setStartLine(START_LINE_OFFSET_FASTB, START_LINE_SIZE_FASTB, headerInputStream);
        landsatHeader.setTapeSpanningFlag(TAPE_SPANNING_FLAG_OFFSET_FASTB, TAPE_SPANNING_FLAG_SIZE_FASTB,
                                          headerInputStream);
        landsatHeader.setTypeOfProcessing(TYPE_OF_PROCESSING_OFFSET_FASTB, TYPE_OF_PROCESSING_SIZE_FASTB,
                                          headerInputStream);
    }

    private void readGeometricData(final LandsatImageInputStream headerInputStream) throws
                                                                                    IOException {

        GeometricData geoData = new GeometricData();
        geoData.setLookAngle(LOOK_ANGLE_OFFSET_FASTB, LOOK_ANGLE_SIZE_FASTB, headerInputStream);
        geoData.setSunElevationAngles(SUN_ELEVATION_OFFSET_FASTB, SUN_ELEVATION_SIZE_FASTB, headerInputStream);
        geoData.setSunAzimuthAngles(SUN_AZIMUTH_OFFSET_FASTB, SUN_AZIMUTH_SIZE_FASTB, headerInputStream);
        geoData.setHorizontalOffset(HORIZONTAL_OFFSET_OFFSET_FASTB, HORIZONTAL_OFFSET_SIZE_FASTB, headerInputStream);
        geoData.setMapProjection(PROJECTION_OFFSET_FASTB, PROJECTION_SIZE_FASTB, headerInputStream);

        geoData.setEllipsoid(ELLIPSOID_OFFSET_FASTB, ELLIPSOID_SIZE_FASTB, headerInputStream);
        geoData.setSemiMajorAxis(SEMI_MAJOR_AXIS_OFFSET_FASTB, SEMI_MAJOR_AXIS_SIZE_FASTB, headerInputStream);
        geoData.setSemiMinorAxis(SEMI_MINOR_AXIS_OFFSET_FASTB, SEMI_MINOR_AXIS_SIZE_FASTB, headerInputStream);
        geoData.setMapZoneNumber(MAP_ZONE_OFFSET_FASTB, MAP_ZONE_SIZE_FASTB, headerInputStream);
        geoData.setProjectionNumber(PROJECTION_NUMBER_OFFSET_FASTB, PROJECTION_NUMBER_SIZE_FASTB, headerInputStream);
        geoData.setProjectionParameter(PROJECTION_PARAMETERS_OFFSET_FASTB, PROJECTION_PARAMETERS_SIZE_FASTB,
                                       headerInputStream);

        GeoPoint ul = new GeoPoint(Points.UPPER_LEFT);
        GeoPoint ll = new GeoPoint(Points.LOWER_LEFT);
        GeoPoint ur = new GeoPoint(Points.UPPER_RIGHT);
        GeoPoint lr = new GeoPoint(Points.LOWER_RIGHT);
        GeoPoint center = new GeoPoint(Points.CENTER);

        ul.setEasting(UPPER_LEFT_EASTING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        ul.setNorthing(UPPER_LEFT_NORTHING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        ul.setGeodicLatitude(UPPER_LEFT_CORNER_LATITUDE_OFFSET_FASTB, LATITUDE_SIZE_FASTB, headerInputStream);
        ul.setGeodicLongitude(UPPER_LEFT_CORNER_LONGITUDE_OFFSET_FASTB, LONGITUDE_SIZE_FASTB, headerInputStream);
        ul.setPixelX(0);
        ul.sePixelY(0);

        ll.setEasting(LOWER_LEFT_EASTING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        ll.setNorthing(LOWER_LEFT_NORTHING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        ll.setGeodicLatitude(LOWER_LEFT_CORNER_LATITUDE_OFFSET_FASTB, LATITUDE_SIZE_FASTB, headerInputStream);
        ll.setGeodicLongitude(LOWER_LEFT_CORNER_LONGITUDE_OFFSET_FASTB, LONGITUDE_SIZE_FASTB, headerInputStream);
        ll.setPixelX(0);
        ll.sePixelY(landsatHeader.getImageHeight() - 1);

        ur.setEasting(UPPER_RIGHT_EASTING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        ur.setNorthing(UPPER_RIGHT_NORTHING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        ur.setGeodicLatitude(UPPER_RIGHT_CORNER_LATITUDE_OFFSET_FASTB, LATITUDE_SIZE_FASTB, headerInputStream);
        ur.setGeodicLongitude(UPPER_RIGHT_CORNER_LONGITUDE_OFFSET_FASTB, LONGITUDE_SIZE_FASTB, headerInputStream);
        ur.setPixelX(landsatHeader.getImageWidth() - 1);
        ur.sePixelY(0);

        lr.setEasting(LOWER_RIGHT_EASTING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        lr.setNorthing(LOWER_RIGHT_NORTHING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        lr.setGeodicLatitude(LOWER_RIGHT_CORNER_LATITUDE_OFFSET_FASTB, LATITUDE_SIZE_FASTB, headerInputStream);
        lr.setGeodicLongitude(LOWER_RIGHT_CORNER_LONGITUDE_OFFSET_FASTB, LONGITUDE_SIZE_FASTB, headerInputStream);
        ur.setPixelX(landsatHeader.getImageWidth() - 1);
        ur.sePixelY(landsatHeader.getImageHeight() - 1);

        center.setEasting(CENTER_EASTING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        center.setNorthing(CENTER_NORTHING_OFFSET_FASTB, EASTING_NORTHING_SIZE_FASTB, headerInputStream);
        center.setGeodicLatitude(CENTER_LATITUDE_OFFSET_FASTB, LATITUDE_SIZE_FASTB, headerInputStream);
        center.setGeodicLongitude(CENTER_LONGITUDE_OFFSET_FASTB, LONGITUDE_SIZE_FASTB, headerInputStream);
        // for the center point the pixel location is given
        String yString = LandsatUtils.getValueFromLandsatFile(headerInputStream, CENTER_LINE_NUMBER_OFFSET_FASTB,
                                                              CENTER_NUMBERS_SIZE_FASTB);
        center.sePixelY(Integer.parseInt(yString.trim()));
        String xString = LandsatUtils.getValueFromLandsatFile(headerInputStream, CENTER_PIXEL_NUMBER_OFFSET_FASTB,
                                                              CENTER_NUMBERS_SIZE_FASTB);
        center.setPixelX(Integer.parseInt(xString.trim()));

        geoData.addGeoPoint(ul);
        geoData.addGeoPoint(ur);
        geoData.addGeoPoint(ll);
        geoData.addGeoPoint(lr);
        geoData.addGeoPoint(center);

        landsatHeader.setGeoData(geoData);
    }

    private void readRadiometricData(final LandsatImageInputStream headerInputStream) throws IOException {
        final int[] bandsPresent = landsatHeader.getBandsPresent();
        final int number = landsatHeader.getNumberOfBands();

        int size = Landsat5FASTConstants.RADIANCE_VALUE_DATA_SIZE;
        final int offset = Landsat5FASTConstants.RADIOMETRIC_DATA_FASTB_OFFSET;

        final int[] maxOffsets = generateMaxRadianceOffsets(size, offset, (number == 0) ? DEFAULT_NUMBER : number);
        final int[] minOffsets = generateMinRadianceOffsets(size, offset + size,
                                                            (number == 0) ? DEFAULT_NUMBER : number);

        landsatHeader.setRadData(
                RadiometricData.createRadiometricData(maxOffsets, minOffsets, size, headerInputStream, bandsPresent));
    }

    private static int[] generateMaxRadianceOffsets(final int sizeOfData, final int startOffset,
                                                    final int numberOfBands) {
        int[] maxRad = new int[numberOfBands];
        for (int i = 0; i < numberOfBands; i++) {
            if (i == 0) {
                maxRad[i] = startOffset;
            } else {
                maxRad[i] = maxRad[i - 1] + 2 * sizeOfData + BLANK;
            }
        }
        return maxRad;
    }

    private static int[] generateMinRadianceOffsets(final int sizeOfData, final int startOffset,
                                                    final int numberOfBands) {
        int[] minRad = new int[numberOfBands];
        for (int i = 0; i < numberOfBands; i++) {
            if (i == 0) {
                minRad[i] = startOffset + SEPERATOR;
            } else {
                minRad[i] = minRad[i - 1] + 2 * sizeOfData + SEPERATOR;
            }
        }
        return minRad;
    }


    private void createBandReaders() throws IOException {

        if (landsatHeader != null) {
            final int width = landsatHeader.getImageWidth();

            if (landsatBands != null) {
                for (LandsatTMBand tempBand : landsatBands) {
                    addBandReader(new LandsatByteBandReader(width, tempBand.getBandName(), tempBand.createStream()));
                }
            }
        } else {
            Debug.trace("no landsat header object available!");
        }
    }

    private LandsatTMBand extractLandsatTMBand(final ConstBand band) {
        for (LandsatTMBand element : landsatBands) {
            if (element.toString().equalsIgnoreCase(band.toString())) {
                return element;
            }
        }
        return null;
    }
}
