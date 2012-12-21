/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.radarsat2;

import java.io.File;

/**
 * Several constants used for reading Radarsat2 products.
 */
public class Radarsat2Constants {

    private final static String[] FORMAT_NAMES = new String[]{"RADARSAT-2"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{"xml"};
    private final static String PLUGIN_DESCRIPTION = "RADARSAT-2 Products";      /*I18N*/

    public final static String PRODUCT_HEADER_PREFIX = "PRODUCT";

    final static String PRODUCT_DESCRIPTION_PREFIX = "Radarsat2 product ";

    private final static String INDICATION_KEY = "XML";

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