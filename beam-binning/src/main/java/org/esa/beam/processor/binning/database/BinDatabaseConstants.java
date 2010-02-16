/*
 * $Id: BinDatabaseConstants.java,v 1.1 2006/09/11 10:47:31 norman Exp $
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
package org.esa.beam.processor.binning.database;

/**
 * Thic class stores all constants needed within this package.
 */
public class BinDatabaseConstants {

    // algorithm properties file name
    public static final String ALGORITHM_PROPERTIES_FILE = "algorithm.props";
    // database properties file name
    public static final String CONTEXT_PROPERTIES_FILE = "context.props";

    // name of the temporal database
    public static final String TEMP_DB_NAME = "temp.dat";
    // name of the interpreted database
    public static final String FINAL_DB_NAME = "final.dat";

    // database directory extension
    public static final String DIRECTORY_EXTENSION = ".binData";
    // database file extension
    public static final String FILE_EXTENSION = ".bindb";
    public static final String FILE_EXTENSION_DESCRIPTION = "Bin Database Files";

    // property keys
    // -------------
    public static final String CELL_SIZE_KEY = "cell_size";
    public static final String RESAMPLING_TYPE_KEY = "resampling";
    public static final String PRODUCT_COUNT_KEY = "product_count";
    public static final String PROCESSED_PRODUCT_BASE_KEY = "product";
    public static final String STORAGE_TYPE_KEY = "storage_type";
    public static final String LAT_MIN_KEY = "lat_min";
    public static final String LAT_MAX_KEY = "lat_max";
    public static final String LON_MIN_KEY = "lon_min";
    public static final String LON_MAX_KEY = "lon_max";

    // resampled pixel (Rixel) type added T Lankester 26/04/05
    public static final String DATABASE_SIMPLE_VALUE    = "simple";
    public static final String DATABASE_RIXEL_VALUE     = "rixel";
    public static final String DATABASE_QUAD_TREE_VALUE = "quad";

    // pi times the earth radius - corrected TLankester 10/05/05
    public static final float PI_EARTH_RADIUS = (float) (Math.PI * 6378.137);
   // public static final float PI_EARTH_RADIUS = (float) (Math.PI * 6378.145);
    // SeaWiFS original cell size
    public static final float SEA_WIFS_CELL_SIZE = 9.28f;
}
