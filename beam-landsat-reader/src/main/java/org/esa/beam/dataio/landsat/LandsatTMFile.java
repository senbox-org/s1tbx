/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// todo - URGENT! add close() method!


/**
 * The class <code>LandsatTMFile</code> is used to store one instance of the input data file and the ability to decode it's data
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class LandsatTMFile {

    private int format = LandsatConstants.INVALID_FORMAT;

    private final File inputFile;
    private boolean header = false;
    private String instrument;
    private String satellite;
    private boolean zipped = false;
    private LandsatHeaderStream headerStream;

    /**
     * @param inputPath creates a new LandsatTMFile Object from the given path
     *
     * @throws IOException
     */
    public LandsatTMFile(final String inputPath) throws
                                                 IOException {
        this(new File(inputPath));
    }

    /**
     * @param inputFile creates a new Landsat TM File Object form the passed File Object
     *
     * @throws IOException
     */
    public LandsatTMFile(final File inputFile) throws
                                               IOException {
        this.inputFile = inputFile;
        init();
    }

    /**
     * inits the LandsatTMFile Object
     *
     * @throws IOException
     */
    private void init() throws
                        IOException {
        analyseInputFile();
    }

    /**
     * @return <code>true</code> if the input file can decode by the reader application <code>false</code> if the input file can not read by the reader application
     */
    public final boolean canDecodeInput() {

        boolean decodable = false;


        if (getLandsatHeaderStream() != null && getHeaderInputStream() != null) {
            if (getFormat() == LandsatConstants.FAST_L5) {
                if (getSatellite().equalsIgnoreCase(
                        LandsatConstants.SATELLITE_NAMES[LandsatConstants.LANDSAT_5])) {
                    if (getInstrument().equalsIgnoreCase(
                            LandsatConstants.TM_INSTRUMENT)) {
                        decodable = true;
                    }
                }
            } else if (getFormat() == LandsatConstants.CEOS) {
                if (getSatellite().equalsIgnoreCase("L5")) {
                    if (getInstrument().equalsIgnoreCase("T")) {
                        decodable = true;
                    }
                }
            }
        }

        return decodable;
    }


    /**
     * @return landsat input file instance
     */

    public final File getFile() {
        return inputFile;
    }

    /**
     * @return isHeader a boolean value checks if the selected
     *         file is also a header File.
     */
    private boolean isHeader() {
        return header;
    }

    /**
     * @return format format coded in a integer value
     */
    public final int getFormat() {
        return format;
    }

    /**
     * returns the inputstream of the header file
     *
     * @return input stream of the input file
     */
    private LandsatImageInputStream getHeaderInputStream() {
        return headerStream.getInputStream();
    }

    /**
     * @return Landsat header stream data
     */
    public final LandsatHeaderStream getLandsatHeaderStream() {
        return headerStream;
    }

    /**
     * @return the location of the Landsat data
     */
    public final String getFileLocation() {
        return getFolderOfFile(inputFile);
    }

    /**
     * @return the name of the instrument
     */
    private String getInstrument() {
        return instrument;
    }

    /**
     * @return the name of the satellite belowed to the image files
     */
    private String getSatellite() {
        return satellite;
    }

    /**
     * @return file object of the selected input file
     */
    public final File getInputFile() {
        return inputFile;
    }

    /**
     * @return shows if the input file is a zipped folder
     */
    public final boolean isZipped() {
        return zipped;
    }

    /**
     * @param header set the boolean value if the selected file is a header or not
     */
    private void setHeader(final boolean header) {
        this.header = header;
    }

    /**
     * @param format set the format typ of the input file
     */
    private void setFormat(final int format) {
//  todo - boolean function searching array for valid formats
        assert format == LandsatConstants.INVALID_FORMAT || format == LandsatConstants.FAST_L5
               || format == LandsatConstants.CEOS || format == LandsatConstants.ZIPED_UNKNOWN_FORMAT;
        this.format = format;
    }

    /**
     * Search for FAST.REV B header files
     *
     * @return File
     *         if a header file exists
     *         <code>false</code> if no header file exists in the given folder
     */
    private File findLandsat5HeaderInFolder() {
        Guardian.assertNotNull("inputFile", inputFile);
        String folderPathName = getFolderOfFile(inputFile);
        File folder = new File(folderPathName);

        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }

        File filesInFolder[] = folder.listFiles();

        for (int i = 0; i < filesInFolder.length; i++) {
            if (filesInFolder[i].length() == Landsat5FASTConstants.FAST_FORMAT_HEADER_SIZE &&
                format == Landsat5FASTConstants.FAST_L5) {
                return filesInFolder[i];
            } else if (isFastCEOSHeader(FileUtils.getFilenameWithoutExtension(
                    filesInFolder[i].getName())) && format == Landsat5FASTConstants.CEOS) {
                return filesInFolder[i];
            }
        }
        return null;
    }

    /**
     * @param format checks if the format type is a valide type and set the attribut variable
     */
    private void setFormat(final String format) {
        Guardian.assertNotNullOrEmpty("format", format);
        int formatValue = LandsatConstants.INVALID_FORMAT;

        for (int i = 0; i < LandsatConstants.LANDSAT_EXTENSIONS.length; i++) {
            if (LandsatConstants.LANDSAT_EXTENSIONS[i].equalsIgnoreCase(format)) {
                formatValue = i;
                break;
            }
        }
        setFormat(formatValue);
    }

    /**
     * @param ipStreamReader inited reader of an inputstream
     * @param offset
     * @param size
     *
     * @throws IOException
     */
    private void setInstrument(final InputStreamReader ipStreamReader,
                               final int offset, final int size) throws
                                                                 IOException {
        instrument = LandsatUtils.getValueFromLandsatFile(ipStreamReader, offset, size);
    }

    /**
     * @param fileNameWithoutExt set the boolean variable isHeader by checking the file depends on its format
     */
    private void setIsHeader(final String fileNameWithoutExt) {
        Guardian.assertNotNullOrEmpty("fileNameWithoutExt", fileNameWithoutExt);
        boolean isHeader = false;

        switch (format) {
            case (LandsatConstants.FAST_L5):
                isHeader = isFastL5Header(fileNameWithoutExt);
                break;
            case (LandsatConstants.CEOS):
                isHeader = isFastCEOSHeader(fileNameWithoutExt);
                break;
            case (LandsatConstants.INVALID_FORMAT):
                break;
        }
        setHeader(isHeader);
    }

    /**
     * @param fileName
     *
     * @return if it is a ceos header
     */
    private static boolean isFastCEOSHeader(final String fileName) {
        Guardian.assertNotNullOrEmpty("fileName", fileName);
        return fileName.equalsIgnoreCase("VDF_DAT");
    }

    /**
     * @param fileNameWithoutExt checks if the file with the given file name (without extension) is also a header file
     *
     * @return if it is a FAST L5 Header
     */
    private boolean isFastL5Header(final String fileNameWithoutExt) {
        Guardian.assertNotNullOrEmpty("fileNameWithoutExt", fileNameWithoutExt);
        if (fileNameWithoutExt.equalsIgnoreCase(Landsat5FASTConstants.L5_HEADER_FILE_NAME)) {
            if (inputFile.length() == Landsat5FASTConstants.FAST_FORMAT_HEADER_SIZE) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks the format type of the input file
     * checks if the chosen file is a header file of this format
     *
     * @throws IOException
     */
    private void analyseInputFile() throws
                                    IOException {
        String extension = FileUtils.getExtension(inputFile);
        if (extension == null) {
            return;
        }
        setFormat(extension);

        if (format == LandsatConstants.ZIPED_UNKNOWN_FORMAT) {
            analyseZipFile();
        } else {
            analyseFileFolder();
        }

        final byte[] headerBegin = getHeaderBegin(format);// minimal data extract to identify the product
        try {
            if (headerBegin != null) {
                extractLandsatIdentifier(headerBegin);
            }
        } catch (IOException e) {
            headerStream.getInputStream().close();
        }
    }

    private static byte[] getHeaderBegin(final int format) {
        switch (format) {
            case (LandsatConstants.FAST_L5):
                return new byte[Landsat5FASTConstants.FASTB_HEADER_DECODE_VALUES];
            case (LandsatConstants.CEOS):
                return new byte[48];
            default:
                return null;
        }
    }

    /**
     * @throws IOException
     */
    private void analyseFileFolder() throws
                                     IOException {
        String fileName = inputFile.getName();
        setIsHeader(FileUtils.getFilenameWithoutExtension(fileName));

        if (isHeader()) {
            initInputStream(inputFile);
        } else {
            File tempFile = findLandsat5HeaderInFolder();
            if (tempFile != null) {
                initInputStream(tempFile);
            }
        }
    }

    /**
     * @throws IOException
     */
    private void analyseZipFile() throws
                                  IOException {
        try {
            ZipFile zipFile = new ZipFile(inputFile, ZipFile.OPEN_READ);
            ZipEntry headerEntry = getZipEntryLandsatHeader(zipFile);
            if (headerEntry != null) {
                initInputStream(zipFile, headerEntry);
            }
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            // IMPORTANT NOTE: ZipFile returns an enumeration which might throw
            // an InternalError for invalid or corrupt ZIP files.
            // We have to handle this case here.
            IOException e = new IOException(t.getMessage());
            e.initCause(t);
            throw e;
        }
    }

    /**
     * @param headerBegin
     *
     * @throws IOException
     */
    private void extractLandsatIdentifier(final byte[] headerBegin) throws
                                                                    IOException {
        headerStream.getInputStream().getImageInputStream().readFully(headerBegin, 0, headerBegin.length);
        InputStreamReader inputReader = new InputStreamReader(
                new ByteArrayInputStream(headerBegin));
        setSatellite(inputReader);
        setInstrument(inputReader);
    }

    /**
     * @param file
     *
     * @throws IOException
     */
    private void initInputStream(final File file) throws
                                                  IOException {
        headerStream = new LandsatHeaderStream(file);
    }

    /**
     * @param zipFile
     * @param headerEntry
     *
     * @throws IOException
     */
    private void initInputStream(final ZipFile zipFile, final ZipEntry headerEntry) throws
                                                                                    IOException {
        this.setHeader(true);
        final InputStream inputStream = zipFile.getInputStream(headerEntry);
        final LandsatImageInputStream input = new LandsatImageInputStream(ImageIO.createImageInputStream(inputStream),
                                                                          headerEntry.getSize());
        headerStream = new LandsatHeaderStream(input, LandsatUtils.getZipEntryFileName(headerEntry));
        zipped = true;
    }

    private void setSatellite(InputStreamReader inputReader) throws
                                                             IOException {
        switch (format) {
            case (LandsatConstants.FAST_L5):
                setSatellite(inputReader, Landsat5FASTConstants.SATELLITE_OFFSET_FASTB,
                             Landsat5FASTConstants.SATELLITE_SIZE_FASTB);
                break;
            case (LandsatConstants.CEOS):
                setSatellite(inputReader, 45, 2);
                break;
            case (LandsatConstants.INVALID_FORMAT):
                break;
        }
    }

    private void setSatellite(InputStreamReader inputReader, int offset,
                              int size) throws
                                        IOException {
        satellite = LandsatUtils.getValueFromLandsatFile(inputReader, offset,
                                                         size);
    }

    private void setInstrument(final InputStreamReader ipStreamReader)
            throws
            IOException {

        switch (format) {
            case (LandsatConstants.FAST_L5):
                setInstrument(ipStreamReader, 14, Landsat5FASTConstants.INSTRUMENT_SIZE_FASTB);
                break;
            case (LandsatConstants.CEOS):
                setInstrument(ipStreamReader, 1, 1);
                break;
            case (LandsatConstants.INVALID_FORMAT):
                break;
        }
    }

    /**
     * extracts the folder value of the given file object
     *
     * @param file input File
     *
     * @return only the file's folder
     */
    private static String getFolderOfFile(File file) {
        String path = file.getParent();
        if (path == null) {
            path = "\".\"";
        }

        return path;
    }

    /**
     * @param dataFile takes the input zip file and identifies the format of the data
     *                 find the right header by searching the header size (1526 byte or 3*1536) for the right
     *                 format compressed in a zip file.
     *
     * @return the ZipEntry of the Header(s) Files
     */
    private ZipEntry getZipEntryLandsatHeader(final ZipFile dataFile) {
        Guardian.assertNotNull("dataFile", dataFile);
        boolean checkFormatFlag = true;   // to ensure that setFormat is invoked only one time.
        final Enumeration enumeration = dataFile.entries();
        ZipEntry zipEntry;
        while (enumeration.hasMoreElements()) {
            zipEntry = (ZipEntry) enumeration.nextElement();
            if (checkFormatFlag) {
                setFormat(FileUtils.getExtension(zipEntry.getName()));
                checkFormatFlag = false;
            }

            if (zipEntry.getSize() == LandsatConstants.FAST_FORMAT_HEADER_SIZE && isFASTFormat(zipEntry.getName())) {
                format = LandsatConstants.FAST_L5;
                return zipEntry;
            }
        }
        return null;
    }

    private static boolean isFASTFormat(final String FileName) {
        String fileExtension = FileUtils.getExtension(FileName);
        return LandsatConstants.LANDSAT_EXTENSIONS[LandsatConstants.FAST_L5].equalsIgnoreCase(fileExtension);
    }


    public void close() {
        if (headerStream != null) {
            headerStream.close();
        }
    }
}
