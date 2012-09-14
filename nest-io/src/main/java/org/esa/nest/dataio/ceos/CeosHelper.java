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
package org.esa.nest.dataio.ceos;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;

public class CeosHelper {

    public static File getVolumeFile(final File baseDir, final CEOSConstants constants) throws IOException {
        final File[] files = baseDir.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String fileName) {
                final String name = fileName.toUpperCase();
                for(String prefix : constants.getVolumeFilePrefix()) {
                    if(name.startsWith(prefix) || name.endsWith('.'+prefix))
                        return true;
                }
                return false;
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

    public static FilePointerRecord[] readFilePointers(final BinaryRecord vdr, final org.jdom.Document filePointerXML,
                                                       final String recName) throws IOException {
        final int numFilePointers = vdr.getAttributeInt("Number of filepointer records");
        final BinaryFileReader reader = vdr.getReader();
        reader.seek(vdr.getRecordLength());
        final FilePointerRecord[] filePointers = new FilePointerRecord[numFilePointers];
        for (int i = 0; i < numFilePointers; i++) {
            filePointers[i] = new FilePointerRecord(reader, filePointerXML, recName);
        }
        return filePointers;
    }

    public static File getCEOSFile(final File baseDir, final String[] prefixList) throws IOException {
        final File[] fileList = baseDir.listFiles();
        for (File file : fileList) {
            final String name = file.getName().toUpperCase();
            for(String prefix : prefixList) {
                if (name.startsWith(prefix) || name.endsWith('.'+prefix))
                    return file;
            }
        }
        return null;
    }

    public static String getProductName(final BinaryRecord textRecord) {
        if(textRecord == null) return "unknown";
        final String name = textRecord.getAttributeString("Product type specifier").trim().replace("PRODUCT:", "")
                + '-' + textRecord.getAttributeString("Scene identification").trim();
        return StringUtils.createValidName(name.trim(), new char[]{'_', '-'}, '_');
    }

    public static String getProductType(final BinaryRecord textRecord) {
        if(textRecord == null) return "unknown";
        String type = textRecord.getAttributeString("Product type specifier").trim();
        type = type.replace("PRODUCT:", "");
        type = type.replace("JERS-1", "JERS1");
        type = type.replace("JERS_1", "JERS1");
        type = type.replace("ERS-1", "ERS1");
        type = type.replace("ERS_1", "ERS1");
        type = type.replace("ERS-2", "ERS2");
        type = type.replace("ERS_2", "ERS2");
        return type.trim();
    }

    public static ProductData.UTC createUTCDate(final int year, final int dayOfYear, final int millisInDay) {
        final Calendar calendar = ProductData.UTC.createCalendar();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.add(Calendar.MILLISECOND, millisInDay);

        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    public static void addMetadata(MetadataElement sphElem, BinaryRecord rec, String name) {
        if(rec != null) {
            final MetadataElement metadata = new MetadataElement(name);
            rec.assignMetadataTo(metadata);
            sphElem.addElement(metadata);
        }
    }

}
