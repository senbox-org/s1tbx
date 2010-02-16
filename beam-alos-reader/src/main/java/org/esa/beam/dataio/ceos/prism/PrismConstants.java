/*
 * $Id: PrismConstants.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
