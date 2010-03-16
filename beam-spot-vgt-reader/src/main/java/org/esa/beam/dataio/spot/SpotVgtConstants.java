/*
 * Copyright (C) 2010  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.spot;

import java.io.File;
import java.io.FilenameFilter;

public class SpotVgtConstants {

    public static final String PHYS_VOL_FILENAME = "PHYS_VOL.TXT";
    public static final String FILE_EXTENSION = "TXT";
    public static final String READER_DESCRIPTION = "SPOT VGT Data Products";
    public static final String FORMAT_NAME = "SPOT-VGT";
    public static final HdfFilenameFilter HDF_FILTER = new HdfFilenameFilter();

    private static class HdfFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".hdf") || name.endsWith(".HDF");
        }
    }
}
