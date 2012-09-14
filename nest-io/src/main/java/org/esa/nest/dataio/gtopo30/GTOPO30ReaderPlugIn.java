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
package org.esa.nest.dataio.gtopo30;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.nest.dataio.netcdf.NetCDFReaderPlugIn;

/**
 * The ReaderPlugIn for GTOPO30 tiles.
 */
public class GTOPO30ReaderPlugIn extends NetCDFReaderPlugIn {

    private final static String[] GTOPO30_FORMAT_NAMES = {"GTOPO30"};
    private final static String[] GTOPO30_FORMAT_FILE_EXTENSIONS = {"dem"};
    private final static String GTOPO30_PLUGIN_DESCRIPTION = "GTOPO30 DEM Tiles";

    public GTOPO30ReaderPlugIn() {
        FORMAT_NAMES = GTOPO30_FORMAT_NAMES;
        FORMAT_FILE_EXTENSIONS = GTOPO30_FORMAT_FILE_EXTENSIONS;
        PLUGIN_DESCRIPTION = GTOPO30_PLUGIN_DESCRIPTION;
    }

    protected DecodeQualification isIntended(final String extension) {
        return DecodeQualification.SUITABLE;
    }
}