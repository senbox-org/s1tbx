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

package org.esa.beam.dataio.landsat;

import org.esa.beam.dataio.landsat.fast.Landsat5FASTConstants;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

/**
 * The class <code>LandsatHeader</code> is used to store data found in a Landsat TM headerfile
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class LandsatHeader {

    private String productID;
    private String formatVersion;
    private final List<LandsatHeaderStream> headerStreamCollection;

    /**
     * Product Orientation (e.g. Map orientated, Orbit orientated)
     */
    private String _productType;

    /**
     * Product size (e.g. Subscene, Fullscene, Multiscene)
     */
    private String _productSize;

    /**
     * Processing (e.g. SYSTEMATIC) and Resampling (e.g. Nearest Neighbor) Informations
     */
    private String typeOfProcessing;
    private String resampling;

    /**
     * Volume Information
     */
    private String tapeSpanningFlag; //L5 / be careful with the keyword in the specification... the sample image used an other description expression for this field
    private long startLine; //L5 /only use for multi volume image
    private long linesPerVolume;

    /**
     * general Image information
     */
    private int _pixelsPerLine;
    private float pixelSize;
    private long recordLength;
    private int imageHeight;
    private int blockingFactor;
    private int [] bandsPresent;
    private int numberOfBands;

    /**
     * Radiometric information data
     */
    private RadiometricData _radData;

    /**
     * Geometric information data
     */
    private GeometricData _geoData;

    /**
     * Sceneinformation data
     */
    private LandsatLoc loc;
    private String rawDate;
    private UTC acquisitionDate;
    private String instrumentMode;
    private String instrumentType;  // TODO for FAST.REVB parse sensor and instrument type data from a single data field
    private double earthSunDistance;

    private final LandsatTMFile _inputFile;

    /**
     * Constructors
     *
     * @param inputFile
     */
    public LandsatHeader(final LandsatTMFile inputFile) {
        numberOfBands = 0;
        headerStreamCollection = new Vector<LandsatHeaderStream>();
        _inputFile = inputFile;

        if (inputFile.getFormat() == LandsatConstants.FAST_L5) {
            headerStreamCollection.add(inputFile.getLandsatHeaderStream());
        }
    }

    /**
     * @return Returns the _geoData.
     */
    public final GeometricData getGeoData() {
        return _geoData;
    }

    /**
     * @param data
     */
    public final void setGeoData(final GeometricData data) {
        _geoData = data;
    }

    /**
     * @return Returns the pixelSize.
     */
    public final float getPixelSize() {
        return pixelSize;
    }

    /**
     * @return Returns the _pixelsPerLine.
     */
    public final int getImageWidth() {
        return _pixelsPerLine;
    }

    /**
     * @param offset      The beginning byte in the file of the data field
     * @param size        The length of the data field.
     * @param inputStream
     *
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public final void setImageWidth(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                           IllegalArgumentException,
                                                                                                           IOException {
        _pixelsPerLine = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return Returns the product Id.
     */
    public final String getProductID() {
        return productID;
    }

    /**
     * @param offset      The beginning byte in the file of the data field
     * @param size        The length of the data field.
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setProductID(final int offset, final int size, final LandsatImageInputStream inputStream) throws
                                                                                                                IOException {
        productID = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    /**
     * @return Returns the _productSize.
     */
    public final String getProductSize() {
        return _productSize;
    }

    /**
     * @param offset      The beginning byte in the file of the data field
     * @param size        The length of the data field.
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setProductSize(final int offset, final int size, final LandsatImageInputStream inputStream) throws
                                                                                                                  IOException {
        _productSize = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    /**
     * @return Returns the productType.
     */
    public final String getProductType() {
        return _productType;
    }

    /**
     * @param offset      The beginning byte in the file of the data field
     * @param size        The length of the data field.
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setProductType(final int offset, final int size, final LandsatImageInputStream inputStream) throws
                                                                                                                  IOException {
        _productType = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    /**
     * @return Returns the _radData.
     */
    public final RadiometricData getRadData() {
        return _radData;
    }

    /**
     * @param data The _radData to set.
     */
    public final void setRadData(final RadiometricData data) {
        _radData = data;
    }

    /**
     * @return Returns the recordLength.
     */
    public final long getRecordLength() {
        return recordLength;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws IOException
     * @throws NumberFormatException
     */
    public final void setRecordLength(final int offset, final int size,
                                      final LandsatImageInputStream inputStream) throws
                                                                                 NumberFormatException,
                                                                                 IOException {
        recordLength = Long.parseLong(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return Returns the resampling.
     */
    public final String getResampling() {
        return resampling;
    }

    /**
     * The date in the format 'YYYYMMDD'.
     * @return The date.
     */
    public String getRawDate() {
        return rawDate;
    }

    /**
     * @return acquisitionDate
     */
    public final UTC getAcquisitionDate() {
        return acquisitionDate;
    }

    /**
     * @return location object
     */
    public final LandsatLoc getLoc() {
        return loc;
    }

    /**
     * @param offset
     * @param size
     * @param format
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setLoc(final int offset, final int size, final int format,
                             final LandsatImageInputStream inputStream) throws
                                                                        IOException {
        loc = new LandsatLoc(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size), format);
    }

    /**
     * @param loc landsat location object
     */
    public final void setLoc(final LandsatLoc loc) {
        this.loc = loc;
    }

    /**
     * @return instrument Mode
     */
    public final String getInstrumentMode() {
        return instrumentMode;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setInstrumentMode(final int offset, final int size,
                                        final LandsatImageInputStream inputStream) throws
                                                                                   IOException {
        instrumentMode = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    public final void setAcquisitionDate(final int offset, final int size, final LandsatImageInputStream inputStream,
                                         final String format) throws
                                                              IOException {
        rawDate = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
        acquisitionDate = ProductData.UTC.create(convertToUTCString(rawDate, format), 0);
        earthSunDistance = LandsatConstants.EarthSunDistance.getEarthSunDistance(getDays(acquisitionDate.getAsDate()));
    }

    public final void setAcquisitionDate(final ProductData.UTC date) {
        acquisitionDate = date;
    }

    public final String getInstrumentType() {
        return instrumentType;
    }

    public final void setInstrumentTyp(final int offset, final int size,
                                       final LandsatImageInputStream inputStream) throws
                                                                                  IOException {
        instrumentType = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    public final void setResampling(final int offset, final int size, final LandsatImageInputStream inputStream) throws
                                                                                                                 IOException {
        resampling = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    public final String getTapeSpanningFlag() {
        return tapeSpanningFlag;
    }

    /**
     * @param offset      The beginning byte in the file of the data field
     * @param size        The length of the data field.
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setTapeSpanningFlag(final int offset, final int size,
                                          final LandsatImageInputStream inputStream) throws
                                                                                     IOException {
        tapeSpanningFlag = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    public final String getTypeOfProcessing() {
        return typeOfProcessing;
    }

    public final void setTypeOfProcessing(final int offset, final int size,
                                          final LandsatImageInputStream inputStream) throws
                                                                                     IOException {
        typeOfProcessing = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size);
    }

    public final int[] getBandsPresent() {
        return bandsPresent;
    }

    public final void setBandsPresent(final int offset, final int size,
                                      final LandsatImageInputStream inputStream) throws
                                                                                 IOException {
        bandsPresent = parseIncludingBands(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
        setNumberOfBands(bandsPresent.length);
    }

    public final void setBandsPresent(final int [] bands) {
        bandsPresent = bands;
    }

    public final int getBlockingFactor() {
        return blockingFactor;
    }

    public final void setBlockingFactor(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                               NumberFormatException,
                                                                                                               IOException {
        blockingFactor = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return version of Format
     */
    public final String getFormatVersion() {
        return formatVersion;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws IOException
     */
    public final void setFormatVersion(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                              IOException {
        formatVersion = LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim();
    }

    /**
     * @return height of the image
     */
    public final int getImageHeight() {
        return imageHeight;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public final void setImageHeight(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                            IllegalArgumentException,
                                                                                                            IOException {
        imageHeight = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return y value
     */
    public final long getLinesPerVolume() {
        return linesPerVolume;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setLinesPerVolume(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                               NumberFormatException,
                                                                                                               IOException {
        linesPerVolume = Long.parseLong(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return the start line
     */
    public final long getStartLine() {
        return startLine;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setStartLine(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                          NumberFormatException,
                                                                                                          IOException {
        startLine = Long.parseLong(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return number of Bands
     */
    public final int getNumberOfBands() {
        return numberOfBands;
    }

    /**
     * @param numberofBands
     */
    public final void setNumberOfBands(final int numberofBands) {
        numberOfBands = numberofBands;
    }

    /**
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public final void setPixelSize(final int offset, final int size, final LandsatImageInputStream inputStream) throws
                                                                                                                NumberFormatException,
                                                                                                                IOException {
        pixelSize = Float.parseFloat(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * parses the bandnumbers comes with this landsat product
     *
     * @param bands value includes all present bandnumbers
     *
     * @return the numbers of presents bands
     */
    private static int[] parseIncludingBands(final String bands) {
        final int size = bands.length();

        int[] bandCollection = null;
        if (size <= Landsat5FASTConstants.NUMBER_OF_L5_BANDS) {

            bandCollection = new int[size];
            for (int i = 0; i < size; i++) {
                bandCollection[i] = Character.getNumericValue(bands.charAt(i));

            }
        } else {
            Debug.trace("The size of the present band array is wrong");
        }

        return bandCollection;
    }

    /**
     * converts the header date format yyyyMMdd
     * to the UTC format yyyy-MM-dd HH:mm:ss.SSSZ
     *
     * @param date   - header date
     * @param format
     *
     * @return UTC date
     */
    private static Date convertToUTCString(final String date, final String format) {

        try {
            Date dateConversion;
            SimpleDateFormat formatInHeaderFile = new SimpleDateFormat(format);
            SimpleDateFormat formatUsedInBeam = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

            TimeZone tz = TimeZone.getTimeZone("UTM");
            formatInHeaderFile.setTimeZone(tz);
            formatUsedInBeam.setTimeZone(tz);
            dateConversion = formatInHeaderFile.parse(date);

            return dateConversion;
        } catch (ParseException e) {

            Debug.trace(e);
            return null;
        }
    }

    /**
     * @return the distance between the earth and the sun
     */
    public final double getEarthSunDistance() {
        return earthSunDistance;
    }

    /**
     * @param date
     *
     * @return number of days of the year
     */
    private static int getDays(final Date date) {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("D");
        return Integer.parseInt(formatter.format(date));
    }


    /**
     * @param fileName
     *
     * @return the Landsat image Inputstream with a given file Name if there is no such file the function returns <code>null</code>
     */
    public final LandsatImageInputStream getHeaderInputStreamAt(String fileName) {
        fileName = FileUtils.getFilenameWithoutExtension(fileName);
        for (Iterator<LandsatHeaderStream> iter = headerStreamCollection.iterator(); iter.hasNext();) {
            LandsatHeaderStream element = iter.next();
            if (element.getHeaderFileName().equalsIgnoreCase(fileName)) {
                return element.getInputStream();
            }
        }
        return null;
    }

    /**
     * @param file
     *
     * @throws IOException
     */
    public final void addHeaderFile(final File file) throws IOException {
        headerStreamCollection.add(new LandsatHeaderStream(file));
    }

    /**
     * @return the path of the header(s)
     */
    public final String getPath() {
        return _inputFile.getFileLocation();
    }

    /**
     * @param headerFile
     *
     * @return the Landsat image inputstream with a given header File
     */
    public final LandsatImageInputStream getHeaderInputStreamAt(final File headerFile) {
        return getHeaderInputStreamAt(headerFile.getName());
    }

    public void close() {
        for (int i = 0; i < headerStreamCollection.size(); i++) {
            LandsatHeaderStream landsatHeaderStream = headerStreamCollection.get(i);
            landsatHeaderStream.close();
        }
    }

}
