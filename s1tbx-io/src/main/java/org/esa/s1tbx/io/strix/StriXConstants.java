package org.esa.s1tbx.io.strix;

import org.esa.s1tbx.io.ceos.CEOSConstants;

/**
 * Several constants used for reading StriX products.
 */

public class StriXConstants implements CEOSConstants {

    private static final String[] FORMAT_NAMES = {"StriX"};
    private static final String[] FORMAT_FILE_EXTENSIONS = {""};
    private static final String PLUGIN_DESCRIPTION = "StriX CEOS Products";      /*I18N*/

    private static final String[] VOLUME_FILE_PREFIX = {"VOL-STRIX"};
    private static final String[] LEADER_FILE_PREFIX = {"LED-STRIX"};
    private static final String[] IMAGE_FILE_PREFIX = {"IMG-"};
    private static final String[] TRAILER_FILE_PREFIX = {"TRL-STRIX"};

    static final String MISSION = "StriX";

    static final String PRODUCT_DESCRIPTION_PREFIX = "StriX product ";
    final static String SUMMARY_FILE_NAME = "summary.txt";

    public final static int LEVEL1_0 = 0;
    public final static int LEVEL1_1 = 1;
    public final static int LEVEL1_5 = 3;
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

    public String getPluginDescription() {
        return PLUGIN_DESCRIPTION;
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getFormatFileExtensions() {
        return FORMAT_FILE_EXTENSIONS;
    }

    public String getMission() {
        return MISSION;
    }

    public int getMinimumNumFiles() { return MINIMUM_FILES; }
}
