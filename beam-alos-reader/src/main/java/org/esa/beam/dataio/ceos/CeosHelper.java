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
package org.esa.beam.dataio.ceos;

import org.esa.beam.dataio.ceos.records.FilePointerRecord;
import org.esa.beam.dataio.ceos.records.TextRecord;
import org.esa.beam.dataio.ceos.records.VolumeDescriptorRecord;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;

public class CeosHelper {

    private static final String VOLUME_FILE_PREFIX = "VOL-";
    private static final String LEADER_FILE_PREFIX = "LED-";
    private static final String IMAGE_FILE_PREFIX = "IMG-";
    private static final String TRAILER_FILE_PREFIX = "TRL-";
    private static final String SUPPLEMENTAL_FILE_PREFIX = "SUP-";

    public static File getVolumeFile(final File baseDir) throws IOException {
        final File[] files = baseDir.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.startsWith(VOLUME_FILE_PREFIX) &&
                        (name.indexOf('.') == -1);
            }
        });
        if (files == null || files.length < 1) {
            throw new IOException("No volume descriptor file found in directory:\n"
                                  + baseDir.getPath());
        }
        if (files.length > 1) {
            throw new IOException("Multiple volume descriptor files found in directory:\n"
                                  + baseDir.getPath());
        }
        return files[0];
    }

    public static FilePointerRecord[] readFilePointers(final VolumeDescriptorRecord vdr) throws
                                                                                         IllegalCeosFormatException,
                                                                                         IOException {
        final int numFilePointers = vdr.getNumberOfFilepointerRecords();
        final CeosFileReader reader = vdr.getReader();
        reader.seek(vdr.getRecordLength());
        final FilePointerRecord[] filePointers = new FilePointerRecord[numFilePointers];
        for (int i = 0; i < numFilePointers; i++) {
            filePointers[i] = new FilePointerRecord(reader);
        }
        return filePointers;
    }

    public static String getLeaderFileName(final TextRecord textRecord) {
        return LEADER_FILE_PREFIX + getProductName(textRecord);
    }

    public static String getTrailerFileName(final TextRecord textRecord) {
        return TRAILER_FILE_PREFIX + getProductName(textRecord);
    }

    public static String getSupplementalFileName(final TextRecord textRecord) {
        return SUPPLEMENTAL_FILE_PREFIX + getProductName(textRecord);
    }

    public static String getImageFileName(final TextRecord textRecord, final String ccd) {
        if (ccd != null && ccd.trim().length() > 0) {
            return IMAGE_FILE_PREFIX + "0" + ccd + "-" + getProductName(textRecord);
        } else {
            return IMAGE_FILE_PREFIX + getProductName(textRecord);
        }
    }

    public static String getProductName(final TextRecord textRecord) {
        return textRecord.getSceneID() + "-" + textRecord.getProductID();
    }

    public static ProductData.UTC createUTCDate(final int year, final int dayOfYear, final int millisInDay) {
        final Calendar calendar = ProductData.UTC.createCalendar();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.add(Calendar.MILLISECOND, millisInDay);

        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    public static double[] sortToFXYSumOrder(final double[] coeffs) {
        final double[] newOrder = new double[coeffs.length];
        newOrder[0] = coeffs[0];
        newOrder[1] = coeffs[1];
        newOrder[2] = coeffs[2];
        newOrder[3] = coeffs[4];
        newOrder[4] = coeffs[3];
        newOrder[5] = coeffs[5];
        newOrder[6] = coeffs[8];
        newOrder[7] = coeffs[6];
        newOrder[8] = coeffs[7];
        newOrder[9] = coeffs[9];

        return newOrder;
    }

    public static double[] convertLongToDouble(final long[] longs) {
        final double[] doubles = new double[longs.length];
        for (int i = 0; i < longs.length; i++) {
            doubles[i] = Double.longBitsToDouble(longs[i]);
        }
        return doubles;
    }

    /**
     * Returns a <code>File</code> if the given input is a <code>String</code> or <code>File</code>,
     * otherwise it returns null;
     *
     * @param input an input object of unknown type
     *
     * @return a <code>File</code> or <code>null</code> it the input can not be resolved to a <code>File</code>.
     */
    public static File getFileFromInput(final Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }
}
