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

import org.esa.beam.dataio.landsat.ceos.Landsat5CEOSConstants.DataType;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;

/**
 * The class <code>LandsatUtils</code> is used to provide static functions used in the complete landsat reader package
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class LandsatUtils {

    /**
     * @param ipStreamReader reader for getting access to a LANDSAT TM inputstream
     * @param offset         the entrypoint of the reading process
     * @param size           number of bytes needed to read
     *
     * @return data value
     *
     * @throws IOException
     */
    public static String getValueFromLandsatFile(final InputStreamReader ipStreamReader, final int offset,
                                                 final int size) throws
                                                                 IOException {
        assertParameters(ipStreamReader, offset, size);

        final int offsetInArray = offset - 1;
        final char [] charDataValue = new char[size + offsetInArray];
        ipStreamReader.read(charDataValue, 0, charDataValue.length);
        return new String(charDataValue).substring(offsetInArray, size + offsetInArray);
    }

    /**
     * @param landsatInputStream data inputstream where the data should be extracted
     * @param offset             the beginning of the reading process (first index of inputstream is 1)
     * @param size               the size of the dataset
     *                           <p/>
     *                           "1" was choosen for the inputstream's entrypoint because the data in the landsat specifications uses also the first byte as entry point.
     *
     * @return data value
     *
     * @throws IOException
     */
    public static String getValueFromLandsatFile(final LandsatImageInputStream landsatInputStream, final int offset,
                                                 final int size) throws
                                                                 IOException {
        assertParameters(landsatInputStream, offset, size);
        String stringDataValue = null;
        byte [] byteDataValue = new byte[size];
        final ImageInputStream inputStream = landsatInputStream.getImageInputStream();

        if (landsatInputStream.length() > byteDataValue.length) {
            inputStream.seek(offset - 1);
            inputStream.read(byteDataValue, 0, size);
            inputStream.seek(0);
            stringDataValue = new String(byteDataValue);
        }

        return stringDataValue;
    }

    /**
     * @param io     - LandsatImageInputStream or LandsatStreamReader value
     * @param offset - the entrypoint of the inputstream
     * @param size   - the size of the needed data
     *               checks if the io object parameter value  is not null
     *               checks if the offset is greater then 0
     *               checks if the sizei is greater then 0
     */
    private static void assertParameters(final Object io, final int offset, final int size) {
        Guardian.assertNotNull("io", io);
        Guardian.assertTrue("size > 0", size > 0);
        Guardian.assertTrue("offset > 0", offset > 0);
    }

    /**
     * @param inputStream
     * @param offset
     * @param type
     *
     * @return data value
     *
     * @throws IOException
     */
    public static String getValueFromLandsatFile(final LandsatImageInputStream inputStream, final int offset,
                                                 DataType type) throws
                                                                IOException {
        return getValueFromLandsatFile(inputStream, offset, type.toInt());
    }

    /**
     * @param entry
     *
     * @return the file name of a ZipEntry
     */
    public static String getZipEntryFileName(final ZipEntry entry) {
        Guardian.assertNotNull("entry", entry);
        return getZipEntryFileName(entry.getName());
    }

    /**
     * @param entryName
     *
     * @return the fileName of a ZipEntry
     */
    private static String getZipEntryFileName(final String entryName) {
        Guardian.assertNotNullOrEmpty("entryName", entryName);
        if (entryName.contains("/")) {
            return FileUtils.getFilenameWithoutExtension(entryName.substring(entryName.lastIndexOf("/") + 1));
        }
        return (FileUtils.getFilenameWithoutExtension(entryName));
    }
}
