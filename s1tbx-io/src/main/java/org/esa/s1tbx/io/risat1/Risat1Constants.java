
package org.esa.s1tbx.io.risat1;

import java.io.File;

/**
 * Several constants used for reading Risat-1 products.
 */
public class Risat1Constants {

    private final static String[] FORMAT_NAMES = new String[]{"RISAT-1"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{"txt", "zip"};
    private final static String PLUGIN_DESCRIPTION = "RISAT-1 Products";
    public final static String BAND_HEADER_NAME = "BAND_META.txt";
    public final static String PRODUCT_HEADER_NAME = "product.xml";
    public final static String PRODUCT_HEADER_PREFIX = "PRODUCT";

    private final static String INDICATION_KEY = "txt";

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
