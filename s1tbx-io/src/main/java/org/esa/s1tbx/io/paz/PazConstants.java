
package org.esa.s1tbx.io.paz;

import java.io.File;

/**
 * Several constants used for reading TerraSarX products.
 */
class PazConstants {

    private final static String[] FORMAT_NAMES = new String[]{"PAZ"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{"xml"};
    private final static String PLUGIN_DESCRIPTION = "Paz Products";
    final static String PRODUCT_TYPE_PREFIX = "";
    final static String[] HEADER_PREFIX = {"PAZ"};

    final static String PRODUCT_DESCRIPTION_PREFIX = "Paz product ";

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

    public static String[] getFormatFileExtensions() {
        return FORMAT_FILE_EXTENSIONS;
    }

}
