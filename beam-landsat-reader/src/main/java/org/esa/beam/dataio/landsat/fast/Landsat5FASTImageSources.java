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

import org.esa.beam.dataio.landsat.AbstractLandsatImageSources;
import org.esa.beam.dataio.landsat.LandsatTMFile;
import org.esa.beam.dataio.landsat.LandsatUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * The class <code>LandsatFASTImageSources</code> is used to store the data of the raw data sources of a Landsat 5 TM Fast format product
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class Landsat5FASTImageSources extends AbstractLandsatImageSources {


    /**
     * @param file
     *
     * @throws ZipException
     * @throws IOException
     */
    public Landsat5FASTImageSources(final LandsatTMFile file) throws
                                                              IOException {
        super(file);
        setImageLocations();
    }

    /**
     * finds the image files by searching the file names ...
     */
    @Override
    protected final void setImageFiles() {
        final Pattern bandFilenamePattern = Pattern.compile("band[\\d].dat");

        final File folder = new File(file.getFileLocation());
        final File filesInFolder [] = folder.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.isFile() && bandFilenamePattern.matcher(file.getName().toLowerCase()).matches()) {
                    return true;
                }
                return false;
            }
        });
        Arrays.sort(filesInFolder);
        imageSources = Arrays.asList((Object[])filesInFolder).toArray();
    }

    /**
     * @throws IOException
     */
    @Override
    protected final void setImageZipEntries() throws IOException {
        final File folder = file.getInputFile();
        ZipFile zipFolder = new ZipFile(folder);
        final Enumeration enumeration = zipFolder.entries();
        ZipEntry zipEntry;
        List<ZipEntry> tempSource = new ArrayList<ZipEntry>();
        final Pattern bandFilenamePattern = Pattern.compile("band[\\d]");

        while (enumeration.hasMoreElements()) {
            zipEntry = (ZipEntry) enumeration.nextElement();
            if(bandFilenamePattern.matcher(LandsatUtils.getZipEntryFileName(zipEntry).toLowerCase()).matches()) {
                tempSource.add(zipEntry);
            }
        }
        Collections.sort(tempSource, new Comparator<ZipEntry>() {
            public int compare(ZipEntry entry1, ZipEntry entry2) {
                String entry1FileName = LandsatUtils.getZipEntryFileName(entry1);
                String entry2FileName = LandsatUtils.getZipEntryFileName(entry2);
                return entry1FileName.compareTo(entry2FileName);
            }
        });
        imageSources = tempSource.toArray();
    }
}
