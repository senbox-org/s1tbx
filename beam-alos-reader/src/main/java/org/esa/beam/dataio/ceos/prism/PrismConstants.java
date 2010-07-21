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
package org.esa.beam.dataio.ceos.prism;

import java.io.File;

/**
 * Several constants used for reading Prism products.
 */
public interface PrismConstants {

    String[] FORMAT_NAMES = new String[]{"PRISM"};
    String PLUGIN_DESCRIPTION = "PRISM Products";   /*I18N*/

    Class[] VALID_INPUT_TYPES = new Class[]{File.class, String.class};
    String[] FORMAT_FILE_EXTENSIONS = new String[]{""};
    String PRODUCT_LEVEL_1B2 = "1B2";
    String VOLUME_FILE_PREFIX = "VOL-ALPSM";
}
