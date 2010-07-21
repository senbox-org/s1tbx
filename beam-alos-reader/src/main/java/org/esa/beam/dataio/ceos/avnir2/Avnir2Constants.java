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

package org.esa.beam.dataio.ceos.avnir2;

import java.io.File;

/**
 * Several constants used for reading Avnir-2 products.
 */
public interface Avnir2Constants {

    Class[] VALID_INPUT_TYPES = new Class[]{File.class, String.class};
    String[] FORMAT_NAMES = new String[]{"AVNIR-2"};
    String[] FORMAT_FILE_EXTENSIONS = new String[]{""};
    String PLUGIN_DESCRIPTION = "AVNIR-2 Products";      /*I18N*/
    String PRODUCT_TYPE_PREFIX = "AV2_";
    String PRODUCT_LEVEL_1B2 = "1B2";
    String VOLUME_FILE_PREFIX = "VOL-ALAV2";

    String SUMMARY_FILE_NAME = "summary.txt";


    /**
     * Taken from <a href="http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm">http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm</a>
     */
    float WAVELENGTH_BAND_1 = 420.0F;
    /**
     * Taken from <a href="http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm">http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm</a>
     */
    float WAVELENGTH_BAND_2 = 520.0F;
    /**
     * Taken from <a href="http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm">http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm</a>
     */
    float WAVELENGTH_BAND_3 = 610.0F;
    /**
     * Taken from <a href="http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm">http://www.eorc.jaxa.jp/ALOS/about/avnir2.htm</a>
     */
    float WAVELENGTH_BAND_4 = 760.0F;

    float BANDWIDTH_BAND_1 = 80.0F;
    float BANDWIDTH_BAND_2 = BANDWIDTH_BAND_1;
    float BANDWIDTH_BAND_3 = BANDWIDTH_BAND_1;
    float BANDWIDTH_BAND_4 = 130.0F;

    String GEOPHYSICAL_UNIT = "mw / (m^2*sr*nm)";
    String BANDNAME_PREFIX = "radiance_";
    String BAND_DESCRIPTION_FORMAT_STRING = "Radiance, Band %d";    /*I18N*/
    String PRODUCT_DESCRIPTION_PREFIX = "AVNIR-2 product Level ";

    String MAP_PROJECTION_RAW = "NNNNN";
    String MAP_PROJECTION_UTM = "YNNNN";
    String MAP_PROJECTION_PS = "NNNNY";
}
