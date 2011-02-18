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
package org.esa.beam.dataio.avhrr;



/**
 * Defines generally useful constants for AVHRR/3 Level-1b data products
 */
public interface AvhrrConstants {

    public static final String PRODUCT_TYPE = "AVHRR_3_L1B";
    public static final String PRODUCT_DESCRIPTION = "AVHRR/3 Level-1b Data Product";

    public static final String RADIANCE_BAND_NAME_PREFIX = "radiance_";
    public static final String TEMPERATURE_BAND_NAME_PREFIX = "temp_";
    public static final String REFLECTANCE_BAND_NAME_PREFIX = "reflec_";
    public static final String COUNTS_BAND_NAME_PREFIX = "counts_";

    public static final String VIS_RADIANCE_UNIT = "mW / (m^2 sr nm)";
    public static final String IR_RADIANCE_UNIT = "mW / (m^2 sr cm^-1)";
    public static final String TEMPERATURE_UNIT = "K";
    public static final String REFLECTANCE_UNIT = "%";
    public static final String COUNTS_UNIT = "DL"; // dimensionless

    public static final String REFLECTANCE_FACTOR_DESCRIPTION = "Reflectance factor for channel {0}";
    public static final String RADIANCE_DESCRIPTION_VIS = "Spectral radiance for channel {0}";
    public static final String RADIANCE_DESCRIPTION_IR = "Earth scene radiance for channel {0}";
    public static final String TEMPERATURE_DESCRIPTION = "Blackbody temperature for channel {0}";
    public static final String COUNTS_DESCRIPTION = "Raw counts for channel {0}";

    public static final String SZA_DS_NAME = "sun_zenith";
    public static final String VZA_DS_NAME = "view_zenith";
    public static final String SAA_DS_NAME = "sun_azimuth";
    public static final String VAA_DS_NAME = "view_azimuth";
    public static final String DAA_DS_NAME = "delta_azimuth";
    public static final String LAT_DS_NAME = "latitude";
    public static final String LON_DS_NAME = "longitude";

    public static final String FLAGS_DS_NAME = "flags";

    public static final String FLAG_QS = "QUALITY_ERROR";
    public static final String FLAG_SCANLINE = "SCANLINE_ERROR";
    public static final String FLAG_3B = "CALIB_CH_3B_ERROR";
    public static final String FLAG_4 = "CALIB_CH_4_ERROR";
    public static final String FLAG_5 = "CALIB_CH_5_ERROR";
    public static final String FLAG_SYNC = "FRAME_SYNC_ERROR";

    public static final String FLAG_QS_DESC = "Set if any of the bits from the quality indicator bit-field are set";
    public static final String FLAG_SCANLINE_DESC = "Set if any of the bits from the scanline quality flags are set";
    public static final String FLAG_CALIB_CH_DESC = "Set if any of the bits from the calibration quality flags are set";
    public static final String FLAG_SYNC_DESC = "Set if there are bit errors in the frame sync";

    public static final String UNIT_DEG = "deg";
    public static final String UNIT_M = "m";
    public static final String UNIT_MM = "mm";
    public static final String UNIT_KM = "km";
    public static final String UNIT_M_PER_S = "m/s";
    public static final String UNIT_KM_PER_S = "km/s";
    public static final String UNIT_PER_CM = "1/cm";
    public static final String UNIT_YEARS = "year";
    public static final String UNIT_DAYS = "day";
    public static final String UNIT_MINUTES = "min";
    public static final String UNIT_MS = "ms";
    public static final String UNIT_DATE = "date";
    public static final String UNIT_BYTES = "bytes";
    public static final String UNIT_BITS = "bits";

    public static final int CH_1 = 0;
    public static final int CH_2 = 1;
    public static final int CH_3A = 2;
    public static final int CH_3B = 3;
    public static final int CH_4 = 4;
    public static final int CH_5 = 5;

    public static final String[] CH_STRINGS = {
        "1", //CH_1
        "2", //CH_2
        "3a", //CH_3A
        "3b", //CH_3B
        "4", //CH_4
        "5", //CH_5
    };

    public static final int[] CH_DATASET_INDEXES = {
        0, // CH_1
        1, // CH_2
        2, // CH_3A
        2, // CH_3B
        3, // CH_4
        4, // CH_5
    };

    public static final float[] CH_WAVELENGTHS = {
        630.0F, // CH_1
        862.5F, // CH_2
        1610.0F, // CH_3A
        3740.0F, // CH_3B
        10800.0F, // CH_4
        12000.0F, // CH_5
    };

    public static final float[] CH_BANDWIDTHS = {
        100.0F, // CH_1
        275.0F, // CH_2
        60.0F, // CH_3A
        38.0F, // CH_3B
        1000.0F, // CH_4
        1000.0F, // CH_5
    };

    String _BASELINE_VALID_MASK_EXPRESSION =
            "NOT " + FLAGS_DS_NAME + "." + FLAG_QS
            + " AND NOT " + FLAGS_DS_NAME + "." + FLAG_SCANLINE
            + " AND NOT " + FLAGS_DS_NAME + "." + FLAG_SYNC;

    public static final String[] CH_VALID_MASK_EXPRESSIONS = {
        _BASELINE_VALID_MASK_EXPRESSION, // CH_1
        _BASELINE_VALID_MASK_EXPRESSION, // CH_2
        _BASELINE_VALID_MASK_EXPRESSION, // CH_3A
        _BASELINE_VALID_MASK_EXPRESSION + " AND NOT " + FLAGS_DS_NAME + "." + FLAG_3B, // CH_3B
        _BASELINE_VALID_MASK_EXPRESSION + " AND NOT " + FLAGS_DS_NAME + "." + FLAG_4, // CH_4
        _BASELINE_VALID_MASK_EXPRESSION + " AND NOT " + FLAGS_DS_NAME + "." + FLAG_5, // CH_5
    };

    public static final int RAW_SCENE_RASTER_WIDTH = 2048;
    public static final int SCENE_RASTER_WIDTH = 2001;
    public static final byte NO_DATA_VALUE = 0;

    public static final int[] HEADER_LENGTHS = new int[]{12288, 15872, 22528};

    public static final int ARS_LENGTH = 512;

    public static final int GI_OFFSET = 0;
    public static final int GI_LENGTH = 108;

    public static final int DSQI_OFFSET = 116;
    public static final int DSQI_LENGTH = 60;

    public static final int RC_OFFSET = 256;
    public static final int RC_LENGTH = 18 * 4;

    public static final int NAV_OFFSET = 328;
    public static final int NAV_LENGTH = 80;

    public static final int CALIB_COEFF_OFFSET = 48;
    public static final int CALIB_COEFF_LENGTH = 5 * 3 * 3 + 3 * 2 * 3;


    public static final float TP_OFFSET_X = 0.5f;
    public static final float TP_OFFSET_Y = 0.5f;
    public static final int TP_SUB_SAMPLING_X = 40;
    public static final int TP_SUB_SAMPLING_Y = 40;
    public static final int TP_GRID_WIDTH = 51;
    public static final int TP_TRIM_X = 25;
}
