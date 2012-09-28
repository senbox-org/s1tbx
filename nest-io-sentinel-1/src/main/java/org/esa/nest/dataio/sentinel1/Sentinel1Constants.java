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
package org.esa.nest.dataio.sentinel1;

import java.io.File;

/**
 * Several constants used for reading SENTINEL-1 products.
 */
public class Sentinel1Constants {

    private final static String[] FORMAT_NAMES = new String[]{"SENTINEL-1"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{"safe"};
    private final static String PLUGIN_DESCRIPTION = "SENTINEL-1 Products";      /*I18N*/

    public final static String PRODUCT_HEADER_PREFIX = "MANIFEST";

    private final static String INDICATION_KEY = "SAFE";

    final static Class[] VALID_INPUT_TYPES = new Class[]{File.class, String.class};

    public static String getIndicationKey() {
        return INDICATION_KEY;
    }

    public static String getPluginDescription() {
        return PLUGIN_DESCRIPTION;
    }

    public static String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public static String[] getForamtFileExtensions() {
        return FORMAT_FILE_EXTENSIONS;
    }

}