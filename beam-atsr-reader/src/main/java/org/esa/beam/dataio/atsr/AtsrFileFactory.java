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
package org.esa.beam.dataio.atsr;

import org.esa.beam.util.Debug;
import org.esa.beam.framework.dataio.DecodeQualification;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

class AtsrFileFactory implements AtsrConstants {

    /**
     * Retrieves an instance of the factory. If none exoists, creates one. Singleton interface.
     */
    public static AtsrFileFactory getInstance() {
        return Holder.instance;
    }

    /**
     * Returns whether the path string passed in points to a valid ATSR file.
     *
     * @param path a string containing the path to the file
     */
    public DecodeQualification getDecodeQualification(String path) {
        return getDecodeQualification(new File(path));
    }

    /**
     * Returns whether the file passed is a valid ATSR file.
     *
     * @param file a file
     */
    public DecodeQualification getDecodeQualification(File file) {
        DecodeQualification bRet = DecodeQualification.UNABLE;

        try {
            bRet = getDecodeQualification(new FileImageInputStream(file));
        } catch (IOException e) {
            // ignored
        }
        return bRet;
    }

    /**
     * Returns whether the stream passed is a valid ATSR file stream.
     *
     * @param inStream a stream to an open file
     */
    public DecodeQualification getDecodeQualification(final ImageInputStream inStream) {
        DecodeQualification bRet = DecodeQualification.UNABLE;

        // read the first 68 characters and check for atsr specific stuff
        final byte[] headerBegin = new byte[BYTE_ORDER_SIZE + PRODUCT_FILE_NAME_SIZE + INSTRUMENT_NAME_SIZE];

        try {
            if (inStream.length() < headerBegin.length) {
                bRet = DecodeQualification.UNABLE;
            } else {
                inStream.readFully(headerBegin, 0, headerBegin.length);
                final InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(headerBegin));
                if (checkByteOrderField(reader)) {
                    if (checkProductFileNameField(reader)) {
                        if (checkInstrumentField(reader)) {
                            bRet = DecodeQualification.INTENDED;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Debug.trace(e);
            bRet = DecodeQualification.UNABLE;
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
                Debug.trace(e);
            }
        }

        return bRet;
    }

    /**
     * Opens an ATSR file at the location stored in the string
     */
    public AtsrFile open(String path) throws IOException {
        return open(new File(path));
    }

    /**
     * Opens an ATSR file at the location strored in the file.
     */
    public AtsrFile open(File file) throws IOException {
        return open(new FileImageInputStream(file), file);
    }

    /**
     * Opens an ATSR file from the input stream passed in
     */
    public AtsrFile open(ImageInputStream inStream, File inFile) throws IOException {
        String prodType = readProductType(inStream);
        AtsrFile file = null;

        if (prodType == null) {
            throw new IOException(
                    "Unsupported product.\nOnly ATSR GBT and GSST products are (currently) supported."); /*I18N*/
        }
        if (prodType.equalsIgnoreCase(PRODUCT_TYPES[0])) {
            file = new AtsrGBTFile();
        } else if (prodType.equalsIgnoreCase(PRODUCT_TYPES[1])) {
            file = new AtsrGSSTFile();
        } else {
            throw new IOException(
                    "Unsupported ATSR product.\nOnly GBT and GSST products are (currently) supported."); /*I18N*/
        }

        file.open(inStream, inFile);
        return file;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Checks that the stream passed in contains the byte order characters AB.
     */
    private boolean checkByteOrderField(InputStreamReader reader) throws IOException {
        boolean bRet = false;
        char[] field = new char[BYTE_ORDER_SIZE];

        reader.read(field, 0, BYTE_ORDER_SIZE);
        if ((field[0] == BYTE_ORDER_FIELD[0]) && (field[1] == BYTE_ORDER_FIELD[1])) {
            bRet = true;
        }

        return bRet;
    }

    /**
     * Checks that the stream passed in contains the product file name field and that this field contains one of the
     * valid file type abbreviations (currently GBT and GSST)
     */
    private boolean checkProductFileNameField(InputStreamReader reader) throws IOException {
        boolean bRet = false;
        char[] field = new char[PRODUCT_FILE_NAME_SIZE];

        reader.read(field, 0, PRODUCT_FILE_NAME_SIZE);
        String productName = new String(field);

        for (int n = 0; n < PRODUCT_TYPES.length; n++) {
            if (-1 != productName.indexOf(PRODUCT_TYPES[n])) {
                bRet = true;
                break;
            }
        }

        return bRet;
    }

    /**
     * Checks that the stream passed in contains a valid instrument name field and that this field contains one of the
     * valid instruments
     */
    private boolean checkInstrumentField(InputStreamReader reader) throws IOException {
        boolean bRet = false;
        char[] field = new char[INSTRUMENT_NAME_SIZE];

        reader.read(field, 0, INSTRUMENT_NAME_SIZE);
        String instrument = new String(field);

        for (int n = 0; n < INSTRUMENTS.length; n++) {
            if (-1 != instrument.indexOf(INSTRUMENTS[n])) {
                bRet = true;
                break;
            }
        }

        return bRet;
    }

    /**
     * Returns the product type of the file to be opened. The stream is rewind after the read operation
     */
    private String readProductType(ImageInputStream inStream) throws IOException {
        String strRet = null;

        // read the first 62 characters and check for filename to contain valid abbreviations
        byte[] headerBegin = new byte[PRODUCT_FILE_NAME_SIZE];

        inStream.seek(2);
        inStream.readFully(headerBegin, 0, headerBegin.length);
        inStream.seek(0);

        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(headerBegin));
        char[] fileName = new char[PRODUCT_FILE_NAME_SIZE];
        reader.read(fileName);
        String productName = new String(fileName);

        for (int n = 0; n < PRODUCT_TYPES.length; n++) {
            if (-1 != productName.indexOf(PRODUCT_TYPES[n])) {
                strRet = PRODUCT_TYPES[n];
                break;
            }
        }
        return strRet;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final AtsrFileFactory instance = new AtsrFileFactory();
    }
}
