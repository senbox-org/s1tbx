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
package org.esa.nest.dataio.ceos.alos;

import org.esa.nest.dataio.ceos.CEOSConstants;

/**
 * Several constants used for reading Palsar products.
 */
public class AlosPalsarConstants implements CEOSConstants {

    private final static String[] FORMAT_NAMES = new String[]{"ALOS PALSAR CEOS"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{""};
    private final static String PLUGIN_DESCRIPTION = "ALOS PALSAR CEOS Products";      /*I18N*/

    private final static String[] VOLUME_FILE_PREFIX = { "VOL" };
    private static final String[] LEADER_FILE_PREFIX = { "LE" };
    private static final String[] IMAGE_FILE_PREFIX = { "IMG" };
    private static final String[] TRAILER_FILE_PREFIX = { "TR" };

    final static String MISSION = "alos";

    final static String PRODUCT_DESCRIPTION_PREFIX = "ALOS PALSAR product ";

    final static String SUMMARY_FILE_NAME = "summary.txt";
    final static String WORKREPORT_FILE_NAME = "workreport";

    final static int LEVEL1_0 = 0;
    final static int LEVEL1_1 = 1;
    final static int LEVEL1_5 = 3;
    final static int LEVEL4_1 = 4;
    final static int LEVEL4_2 = 5;

    private final static int MINIMUM_FILES = 4;    // 4 image files + leader file + volume file + trailer file

    public String[] getVolumeFilePrefix() {
        return VOLUME_FILE_PREFIX;
    }

    public String[] getLeaderFilePrefix() {
        return LEADER_FILE_PREFIX;
    }

    public String[] getImageFilePrefix() {
        return IMAGE_FILE_PREFIX;
    }

    public String[] getTrailerFilePrefix() {
        return TRAILER_FILE_PREFIX;
    }

    public int getMinimumNumFiles() {
        return MINIMUM_FILES;
    }

    public String getPluginDescription() {
        return PLUGIN_DESCRIPTION;
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getForamtFileExtensions() {
        return FORMAT_FILE_EXTENSIONS;
    }

    public String getMission() {
        return MISSION;
    }
}