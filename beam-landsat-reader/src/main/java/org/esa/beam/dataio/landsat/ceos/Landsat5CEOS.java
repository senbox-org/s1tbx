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

package org.esa.beam.dataio.landsat.ceos;

import org.esa.beam.dataio.landsat.*;
import org.esa.beam.dataio.landsat.LandsatConstants.ConstBand;
import org.esa.beam.dataio.landsat.fast.Landsat5FASTConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * @author cberwang
 */
public final class Landsat5CEOS extends Landsat5CEOSConstants implements LandsatTMData {

    private LandsatHeader landsatHeader;

    private LandsatTMBand [] landsatBands;

    private static final int FORMAT = LandsatConstants.CEOS;

    private static final String PRODUCT_NAME = Landsat5FASTConstants.L5_PRODUCT_NAME;

    private LandsatTMFile inputFile = null;

    public Landsat5CEOS(final LandsatTMFile inputFile) throws IOException {
        this.inputFile = inputFile;
        init();
    }

    public final String getProductName() {
        return PRODUCT_NAME;
    }

    public final LandsatHeader getHeader() {
        return landsatHeader;
    }

    public final List getMetadata() {
        return new ArrayList();
    }

    public final LandsatTMBand getBandAt(final int idx) {
        return landsatBands[idx];
    }

    public final int getFormat() {
        return FORMAT;
    }

    private void init() throws
                        IOException {
        landsatHeader = new LandsatHeader(inputFile);
        final List<File> headerFiles = getHeaderFiles(landsatHeader.getPath());
        final File aLeaderFile = getaLeaderFile(headerFiles);
        if (aLeaderFile == null) {
            Debug.trace("No leader files could be found");
        }
        setHeaderFiles(headerFiles);

        LandsatImageInputStream leaderStream = landsatHeader.getHeaderInputStreamAt(aLeaderFile);
        if (leaderStream == null) {
            Debug.trace("the stream of the leader file is not valid");
        }

        setLocation(leaderStream);
        setImageSize(leaderStream);
        setAdministrationData(leaderStream);
        setBandInformation(leaderStream);
        setGeoInformation(leaderStream);
    }

    private void setGeoInformation(LandsatImageInputStream inputStream) throws
                                                                        NumberFormatException,
                                                                        IOException {
        GeometricData geoData = new GeometricData();
        final int RECORD_OFFSET = 2 * SIZE_OF_FILE_RECORD;
        geoData.setSunElevationAngles(RECORD_OFFSET + 605, DataType.FLOAT16.toInt(), inputStream);
        geoData.setSunAzimuthAngles(RECORD_OFFSET + 621, DataType.FLOAT16.toInt(), inputStream);
        geoData.setLookAngle(RECORD_OFFSET + 205, DataType.DOUBLE16.toInt(), inputStream);
        //geoData.setMapProjection(RECORD_OFFSET, DataType.FLOAT16.toInt(), inputStream)
        landsatHeader.setGeoData(geoData);
    }

    private void setBandInformation(LandsatImageInputStream leaderStream) throws
                                                                          NumberFormatException,
                                                                          IOException {
        landsatHeader.setNumberOfBands(Integer.parseInt(
                LandsatUtils.getValueFromLandsatFile(leaderStream, 1413 + SIZE_OF_FILE_RECORD, 16).trim()));
        final int number = landsatHeader.getNumberOfBands();
        if (number == 7) {
            landsatHeader.setBandsPresent(DEFAULT_BANDS);
        }
        final Landsat5CEOSImageSource ceosImageSource = new Landsat5CEOSImageSource(inputFile);
        final int presentBands[] = landsatHeader.getBandsPresent();
        landsatBands = new Landsat5TMBand [number];

        for (int i = 0; i < number; i++) {
            ceosImageSource.getLandsatImageSourceAt(i);
            landsatBands[i] = new Landsat5TMBand(i, presentBands[i], ceosImageSource.getLandsatImageSourceAt(i), null,
                                                 inputFile);
        }
    }

    private void setAdministrationData(LandsatImageInputStream leaderStream) throws
                                                                             NumberFormatException,
                                                                             IOException {
        landsatHeader.setAcquisitionDate(SIZE_OF_FILE_RECORD + 117, DataType.DATETIME14.toInt(), leaderStream,
                                         "yyyyMMddHHmmss");
        landsatHeader.setProductID(SIZE_OF_FILE_RECORD + 21, 21, leaderStream);
    }

    private void setImageSize(final LandsatImageInputStream leaderStream) throws
                                                                          IllegalArgumentException,
                                                                          IOException {
        landsatHeader.setImageWidth(1429 + SIZE_OF_FILE_RECORD, DataType.INT16.toInt(), leaderStream);
        landsatHeader.setImageHeight(1445 + SIZE_OF_FILE_RECORD, DataType.INT16.toInt(), leaderStream);
    }

    private void setLocation(LandsatImageInputStream leaderStream) throws
                                                                   IOException {
        final String path = LandsatUtils.getValueFromLandsatFile(leaderStream, SIZE_OF_FILE_RECORD + CEOS_PATH_OFFSET,
                                                                 DataType.STRING3);
        final String row = LandsatUtils.getValueFromLandsatFile(leaderStream, SIZE_OF_FILE_RECORD + CEOS_ROW_OFFSET,
                                                                DataType.STRING3);
        landsatHeader.setLoc(new LandsatLoc(path, row));
    }

    private void setHeaderFiles(List<File> headerFiles) throws
                                                        FileNotFoundException,
                                                        IOException {
        for (Iterator<File> iter = headerFiles.iterator(); iter.hasNext();) {
            File element = iter.next();
            landsatHeader.addHeaderFile(element);
        }
    }

    private static File getaLeaderFile(final List<File> headerFiles) {
        String leaderFileString = Landsat5CEOSConstants.CEOS_LEADFILE_IDENTIFIER;
        for (Iterator<File> iter = headerFiles.iterator(); iter.hasNext();) {
            File element = iter.next();
            String fileName = element.getName();
            if (fileName.contains(leaderFileString) || fileName.contains(leaderFileString.toUpperCase())) {
                return element;
            }
        }
        return null;
    }

    private static List<File> getHeaderFiles(final String path) {
        List<File> headerFiles = new Vector<File>();
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }
        File filesInFolder[] = folder.listFiles();
        String [] headerNames = Landsat5CEOSConstants.CEOS_HEADER_NAMES;
        for (int i = 0; i < headerNames.length; i++) {
            for (int j = 0; j < filesInFolder.length; j++) {
                String fileName = FileUtils.getFilenameWithoutExtension(filesInFolder[j].getName());
                if (fileName.equalsIgnoreCase(headerNames[i])) {
                    headerFiles.add(filesInFolder[j]);
                }
            }
        }
        return headerFiles;
    }

    public final void close() {
        if (landsatHeader != null) {
            landsatHeader.close();
        }
    }

    public final LandsatBandReader getBandReader(Band band) {
        // TODO Auto-generated method stub
        return null;
    }

    public final LandsatBandReader getBandReader(ConstBand band) {
        // TODO Auto-generated method stub
        return null;
    }
}
