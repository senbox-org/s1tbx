/*
 * $Id: AtsrConstants.java,v 1.4 2007/01/25 10:26:57 marcop Exp $
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
package org.esa.beam.dataio.atsr;

/**
 * Defines some frequently used constants for ERS ATSR products.
 */
public interface AtsrConstants {

    // The atsr format name as shown in the application
    String ATSR_FORMAT_NAME = "ATSR";
    // the format description
    String DESCRIPTION = "ATSR1/ATSR2 products";       /*I18N*/

    // ATSR header constants
    // ---------------------
    // sizes of header fields in bytes - to be read from ascii header and names of
    // the fields as shown in the metadata plus (eventually) a physical unit
    int SADIST_2_HEAER_SIZE = 4096;
    int BYTE_ORDER_SIZE = 2;
    short LITTLE_ENDIAN_TAG = 16961;
    int BT_PIXEL_SIZE = 2;
    int REF_PIXEL_SIZE = 2;
    int SST_PIXEL_SIZE = 2;
    int SST_CONF_PIXEL_SIZE = 2;
    int LATLON_PIXEL_SIZE = 4;
    int OFFSET_PIXEL_SIZE = 1;
    int FLAGS_PIXEL_SIZE = 2;

    int PRODUCT_FILE_NAME_SIZE = 60;
    String PRODUCT_FILE_NAME_FIELD_NAME = "PRODUCT_FILE_NAME";

    int INSTRUMENT_NAME_SIZE = 6;
    String INSTRUMENT_NAME_FIELD_NAME = "INSTRUMENT_NAME";

    int STATE_VECTOR_TYPE_SIZE = 5;
    String STATE_VECTOR_FIELD_NAME = "ERS_STATE_VECTOR_TYPE";

    int ASCENDING_NODE_TIME_SIZE = 16;
    String ASCENDING_NODE_TIME_FIELD_NAME = "ASCENDING_NODE_TIME";
    String ASCENDING_NODE_TIME_DESCRIPTION = "Days since January 1st, 1950";  /*I18N*/
    String ASCENDING_NODE_TIME_UNIT = "d";

    int ASCENDING_NODE_UT_SIZE = 25;
    String ASCENDING_NODE_UT_FIELD_NAME = "ASCENDING_NODE_UT";
    String ASCENDING_NODE_UT_DESCRIPTION = "Universal time at ascending node"; /*I18N*/

    int ASCENDING_NODE_STATE_VECTOR_POSITION_SIZE = 13;
    String ASCENDING_NODE_STATE_VECTOR_POSITION_UNIT = "km";
    String ASCENDING_NODE_STATE_VECTOR_POSITION_X_NAME = "STATE_VECTOR_POSITION_X";
    String ASCENDING_NODE_STATE_VECTOR_POSITION_X_DESCRIPTION = "Ascending node state vector position x"; /*I18N*/
    String ASCENDING_NODE_STATE_VECTOR_POSITION_Y_NAME = "STATE_VECTOR_POSITION_Y";
    String ASCENDING_NODE_STATE_VECTOR_POSITION_Y_DESCRIPTION = "Ascending node state vector position y"; /*I18N*/
    String ASCENDING_NODE_STATE_VECTOR_POSITION_Z_NAME = "STATE_VECTOR_POSITION_Z";
    String ASCENDING_NODE_STATE_VECTOR_POSITION_Z_DESCRIPTION = "Ascending node state vector position z"; /*I18N*/

    int ASCENDING_NODE_STATE_VECTOR_VELOCITY_SIZE = 9;
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_UNIT = "km/s";
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_X_NAME = "STATE_VECTOR_VELOCITY_X";
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_X_DESCRIPTION = "Ascending node state vector velocity x"; /*I18N*/
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_Y_NAME = "STATE_VECTOR_VELOCITY_Y";
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_Y_DESCRIPTION = "Ascending node state vector velocity y"; /*I18N*/
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_Z_NAME = "STATE_VECTOR_VELOCITY_Z";
    String ASCENDING_NODE_STATE_VECTOR_VELOCITY_Z_DESCRIPTION = "Ascending node state vector velocity z"; /*I18N*/

    int ASCENDING_NODE_LON_SIZE = 11;
    String ASCENDING_NODE_LON_NAME = "ASCENDING_NODE_LONGITUDE";
    String ASCENDING_NODE_LON_DESCRIPTION = "Longitude of ascending node"; /*I18N*/
    String ASCENDING_NODE_LON_UNIT = "dec. deg.";

    int REFERENCE_UT_SIZE = 16;
    String REFERENCE_UT_FIELD_NAME = "REFERENCE_UT";
    String REFERENCE_UT_DESCRIPTION = "Reference universal time (days since January 1st, 1950)"; /*I18N*/
    String REFERENCE_UT_UNIT = "d";

    int REFERENCE_ERS_CLOCK_SIZE = 13;
    String REFERENCE_ERS_CLOCK_UNIT = "ns";
    String REFERENCE_ERS_CLOCK_TIME_NAME = "ERS_CLOCK_TIME";
    String REFERENCE_ERS_CLOCK_TIME_DESCRIPTION = "Reference ERS satellite clock time"; /*I18N*/
    String REFERENCE_ERS_CLOCK_PERIOD_NAME = "ERS_CLOCK_PERIOD";
    String REFERENCE_ERS_CLOCK_PERIOD_DESCRIPTION = "Period of ERS satellite clock"; /*I18N*/

    int RECORD_CONTENTS_SIZE = 2;
    String NADIR_ONLY_PRESENT_NAME = "NADIR_ONLY_PRESENT";
    String NADIR_ONLY_DESCRIPTION = "Nadir-only records present"; /*I18N*/
    String THERMAL_PRESENT_NAME = "THERMAL_RECORDS_PRESENT";
    String THERMAL_PRESENT_DESCRIPTION = "Thermal infra-red detector records present"; /*I18N*/
    String VISIBLE_PRESENT_NAME = "VISIBLE_RECORDS_PRESENT";
    String VISIBLE_PRESENT_DESCRIPTION = "Visible/near-infra-red detector records present"; /*I18N*/
    String LAT_LON_PRESENT_NAME = "LAT_LON_RECORDS_PRESENT";
    String LAT_LON_PRESENT_DESCRIPTION = "Latitude/longitude records present"; /*I18N*/
    String X_Y_PRESENT_NAME = "X_Y_RECORDS_PRESENT";
    String X_Y_PRESENT_DESCRIPTION = "X/Y coordinate records present"; /*I18N*/
    String FLAGS_PRESENT_NAME = "FLAG_RECORDS_PRESENT";
    String FLAGS_PRESENT_DESCRIPTION = "Cloud-clearing/land-flagging records present"; /*I18N*/

    int TRACK_DISTANCE_SIZE = 6;
    String TRACK_DISTANCE_UNIT = "km"; /*I18N*/
    String TRACK_DISTANCE_START_NAME = "ALONG_TRACK_START_DISTANCE";
    String TRACK_DISTANCE_START_DESCRIPTION = "Along-track distance of product start"; /*I18N*/
    String TRACK_DISTANCE_END_NAME = "ALONG_TRACK_END_DISTANCE";
    String TRACK_DISTANCE_END_DESCRIPTION = "Along-track distance of product end"; /*I18N*/

    int PRODUCT_TIME_SIZE = 25;
    String UT_PRODUCT_START_NAME = "PRODUCT_START_TIME";
    String UT_PRODUCT_START_DESCRIPTION = "Universal time of data acquisition at product start"; /*I18N*/
    String UT_PRODUCT_END_NAME = "PRODUCT_END_TIME";
    String UT_PRODUCT_END_DESCRIPTION = "Universal time of data acquisition at product end"; /*I18N*/

    int CORNER_LAT_SIZE = 8;
    String CORNER_LAT_LON_UNITS = "dec. deg."; /*I18N*/
    String CORNER_LAT_LHS_START_NAME = "CORNER_LAT_LHS_START";
    String CORNER_LAT_LHS_START_DESCRIPTION = "Latitude of product LHS corner point at start"; /*I18N*/
    String CORNER_LAT_RHS_START_NAME = "CORNER_LAT_RHS_START";
    String CORNER_LAT_RHS_START_DESCRIPTION = "Latitude of product RHS corner point at start"; /*I18N*/
    String CORNER_LAT_LHS_END_NAME = "CORNER_LAT_LHS_END";
    String CORNER_LAT_LHS_END_DESCRIPTION = "Latitude of product LHS corner point at end"; /*I18N*/
    String CORNER_LAT_RHS_END_NAME = "CORNER_LAT_RHS_END";
    String CORNER_LAT_RHS_END_DESCRIPTION = "Latitude of product RHS corner point at end"; /*I18N*/

    int CORNER_LON_SIZE = 9;
    String CORNER_LON_LHS_START_NAME = "CORNER_LON_LHS_START";
    String CORNER_LON_LHS_START_DESCRIPTION = "Longitude of product LHS corner point at start"; /*I18N*/
    String CORNER_LON_RHS_START_NAME = "CORNER_LON_RHS_START";
    String CORNER_LON_RHS_START_DESCRIPTION = "Longitude of product RHS corner point at start"; /*I18N*/
    String CORNER_LON_LHS_END_NAME = "CORNER_LON_LHS_END";
    String CORNER_LON_LHS_END_DESCRIPTION = "Longitude of product LHS corner point at end"; /*I18N*/
    String CORNER_LON_RHS_END_NAME = "CORNER_LON_RHS_END";
    String CORNER_LON_RHS_END_DESCRIPTION = "Longitude of product RHS corner point at end"; /*I18N*/

    int PIXEL_SELECTION_MAP_SIZE = 3;
    String PIXEL_SELECTION_MAP_NADIR_NAME = "PIXEL_SELECTION_MAPS_NADIR";
    String PIXEL_SELECTION_MAP_NADIR_DESCRIPTION = "1st and 2nd ATSR-2 Pixel Selection Maps in nadir-view"; /*I18N*/
    String PIXEL_SELECTION_MAP_FORWARD_NAME = "PIXEL_SELECTION_MAPS_FORWARD";
    String PIXEL_SELECTION_MAP_FORWARD_DESCRIPTION = "1st and 2nd ATSR-2 Pixel Selection Maps in forward-view"; /*I18N*/

    int PSM_CHANGE_SIZE = 6;
    String PSM_CHANGE_UNIT = "km"; /*I18N*/
    String PSM_CHANGE_NADIR_NAME = "PSM_CHANGE_DISTANCE_NADIR";
    String PSM_CHANGE_NADIR_DESCRIPTION = "Along-track distance of 1st PSM change in nadir-view"; /*I18N*/
    String PSM_CHANGE_FORWARD_NAME = "PSM_CHANGE_DISTANCE_FORWARD";
    String PSM_CHANGE_FORWARD_DESCRIPTION = "Along-track distance of 1st PSM change in forward-view"; /*I18N*/

    int ATSR2_DATA_RATE_SIZE = 2;
    String ATSR2_DATA_RATE_NADIR_NAME = "ATSR2_DATA_RATE_NADIR_VIEW";
    String ATSR2_DATA_RATE_NADIR_DESCRIPTION = "ATSR-2 data-rate at start of nadir-view"; /*I18N*/
    String ATSR2_DATA_RATE_FORWARD_NAME = "ATSR2_DATA_RATE_FORWARD_VIEW";
    String ATSR2_DATA_RATE_FORWARD_DESCRIPTION = "ATSR-2 data-rate at start of forward-view"; /*I18N*/

    int ATSR2_DATA_RATE_CHANGE_SIZE = 6;
    String ATSR2_DATA_RATE_CHANGE_UNIT = "km"; /*I18N*/
    String ATSR2_DATA_RATE_CHANGE_NADIR_NAME = "ATSR2_DATA_RATE_CHANGE_NADIR_VIEW";
    String ATSR2_DATA_RATE_CHANGE_NADIR_DESCRIPTION = "Along-track distance of 1st ATSR-2 data-rate change in nadir-view"; /*I18N*/
    String ATSR2_DATA_RATE_CHANGE_FORWARD_NAME = "ATSR2_DATA_RATE_CHANGE_FORWARD_VIEW";
    String ATSR2_DATA_RATE_CHANGE_FORWARD_DESCRIPTION = "Along-track distance of 1st ATSR-2 data-rate change in forward-view"; /*I18N*/

    int TEMPERATURES_SIZE = 8;
    String TEMPERATURES_UNIT = "K";
    String MIN_SCC_TEMPERATURE_NAME = "MINIMUM_SCC_TEMPERATURE";
    String MIN_SCC_TEMPERATURE_DESCRIPTION = "Minimum Stirling Cycle Cooler cold-tip temperature";  /*I18N*/
    String MIN_INSTRUMENT_TEMPERATURE_1200_NM_NAME = "MINIMUM_INSTRUMENT_TEMPERATURE_1200_NM";
    String MIN_INSTRUMENT_TEMPERATURE_1200_NM_DESCRIPTION = "Minimum instrument detector temperature at 1200 nm"; /*I18N*/
    String MIN_INSTRUMENT_TEMPERATURE_1100_NM_NAME = "MINIMUM_INSTRUMENT_TEMPERATURE_1100_NM";
    String MIN_INSTRUMENT_TEMPERATURE_1100_NM_DESCRIPTION = "Minimum instrument detector temperature at 1100 nm"; /*I18N*/
    String MIN_INSTRUMENT_TEMPERATURE_370_NM_NAME = "MINIMUM_INSTRUMENT_TEMPERATURE_370_NM";
    String MIN_INSTRUMENT_TEMPERATURE_370_NM_DESCRIPTION = "Minimum instrument detector temperature at 370 nm"; /*I18N*/
    String MIN_INSTRUMENT_TEMPERATURE_160_NM_NAME = "MINIMUM_INSTRUMENT_TEMPERATURE_160_NM";
    String MIN_INSTRUMENT_TEMPERATURE_160_NM_DESCRIPTION = "Minimum instrument detector temperature at 160 nm"; /*I18N*/
    String MIN_INSTRUMENT_TEMPERATURE_87_NM_NAME = "MINIMUM_INSTRUMENT_TEMPERATURE_87_NM";
    String MIN_INSTRUMENT_TEMPERATURE_87_NM_DESCRIPTION = "Minimum instrument detector temperature at 87 nm"; /*I18N*/
    String MAX_SCC_TEMPERATURE_NAME = "MAXIMUM_SCC_TEMPERATURE";
    String MAX_SCC_TEMPERATURE_DESCRIPTION = "Maximum Stirling Cycle Cooler cold-tip temperature"; /*I18N*/
    String MAX_INSTRUMENT_TEMPERATURE_1200_NM_NAME = "MAXIMUM_INSTRUMENT_TEMPERATURE_1200_NM";
    String MAX_INSTRUMENT_TEMPERATURE_1200_NM_DESCRIPTION = "Maximum instrument detector temperature at 1200 nm"; /*I18N*/
    String MAX_INSTRUMENT_TEMPERATURE_1100_NM_NAME = "MAXIMUM_INSTRUMENT_TEMPERATURE_1100_NM";
    String MAX_INSTRUMENT_TEMPERATURE_1100_NM_DESCRIPTION = "Maximum instrument detector temperature at 1100 nm"; /*I18N*/
    String MAX_INSTRUMENT_TEMPERATURE_370_NM_NAME = "MAXIMUM_INSTRUMENT_TEMPERATURE_370_NM";
    String MAX_INSTRUMENT_TEMPERATURE_370_NM_DESCRIPTION = "Maximum instrument detector temperature at 370 nm"; /*I18N*/
    String MAX_INSTRUMENT_TEMPERATURE_160_NM_NAME = "MAXIMUM_INSTRUMENT_TEMPERATURE_160_NM";
    String MAX_INSTRUMENT_TEMPERATURE_160_NM_DESCRIPTION = "Maximum instrument detector temperature at 160 nm"; /*I18N*/
    String MAX_INSTRUMENT_TEMPERATURE_87_NM_NAME = "MAXIMUM_INSTRUMENT_TEMPERATURE_87_NM";
    String MAX_INSTRUMENT_TEMPERATURE_87_NM_DESCRIPTION = "Maximum instrument detector temperature at 87 nm"; /*I18N*/

    int ANGLE_PARAMETER_SIZE = 9;
    String ANGLE_UNIT = "dec. deg.";
    String SUN_ELEVATION_NADIR_NAME = "sun_elev_nadir";
    String SUN_ELEVATION_NADIR_DESCRIPTION = "Nadir-view solar elevation"; /*I18N*/
    String VIEW_ELEVATION_NADIR_NAME = "view_elev_nadir";
    String VIEW_ELEVATION_NADIR_DESCRIPTION = "Nadir-view satellite elevation"; /*I18N*/
    String SUN_AZIMUTH_NADIR_NAME = "sun_azimuth_nadir";
    String SUN_AZIMUTH_NADIR_DESCRIPTION = "Nadir-view solar azimuth"; /*I18N*/
    String VIEW_AZIMUTH_NADIR_NAME = "view_azimuth_nadir";
    String VIEW_AZIMUTH_NADIR_DESCRIPTION = "Nadir-view satellite azimuth"; /*I18N*/
    String SUN_ELEVATION_FORWARD_NAME = "sun_elev_forward";
    String SUN_ELEVATION_FORWARD_DESCRIPTION = "Forward-view solar elevation"; /*I18N*/
    String VIEW_ELEVATION_FORWARD_NAME = "view_elev_forward";
    String VIEW_ELEVATION_FORWARD_DESCRIPTION = "Forward-view satellite elevation"; /*I18N*/
    String SUN_AZIMUTH_FORWARD_NAME = "sun_azimuth_forward";
    String SUN_AZIMUTH_FORWARD_DESCRIPTION = "Forward-view solar azimuth"; /*I18N*/
    String VIEW_AZIMUTH_FORWARD_NAME = "view_azimuth_forward";
    String VIEW_AZIMUTH_FORWARD_DESCRIPTION = "Forward-view satellite azimuth"; /*I18N*/
    String LATITUDE_NAME = "latitude";
    String LATITUDE_DESCRIPTION = "Latitudes of image pixels"; /*I18N*/
    String LONGITUDE_NAME = "longitude";
    String LONGITUDE_DESCRIPTION = "Longitudes of image pixels"; /*I18N*/

    int CONFIDENCE_SIZE = 6;
    String ERS_MODE_YSM_NADIR_NAME = "ERS_PLATFORM_MODE_YSM_NADIR";
    String ERS_MODE_YSM_NADIR_DESCRIPTION = "ERS platform modes during nadir view as # of scans in YSM"; /*I18N*/
    String ERS_MODE_FCM_NADIR_NAME = "ERS_PLATFORM_MODE_FCM_NADIR";
    String ERS_MODE_FCM_NADIR_DESCRIPTION = "ERS platform modes during nadir view as # of scans in FCM"; /*I18N*/
    String ERS_MODE_OCM_NADIR_NAME = "ERS_PLATFORM_MODE_OCM_NADIR";
    String ERS_MODE_OCM_NADIR_DESCRIPTION = "ERS platform modes during nadir view as # of scans in OCM"; /*I18N*/
    String ERS_MODE_FPM_NADIR_NAME = "ERS_PLATFORM_MODE_FPM_NADIR";
    String ERS_MODE_FPM_NADIR_DESCRIPTION = "ERS platform modes during nadir view as # of scans in FPM"; /*I18N*/
    String ERS_MODE_RTMM_NADIR_NAME = "ERS_PLATFORM_MODE_RTMM_NADIR";
    String ERS_MODE_RTMM_NADIR_DESCRIPTION = "ERS platform modes during nadir view as # of scans in RTMM"; /*I18N*/
    String ERS_MODE_RTMC_NADIR_NAME = "ERS_PLATFORM_MODE_RTMC_NADIR";
    String ERS_MODE_RTMC_NADIR_DESCRIPTION = "ERS platform modes during nadir view as # of scans in RTMC"; /*I18N*/

    String ERS_MODE_YSM_FORWARD_NAME = "ERS_PLATFORM_MODE_YSM_FORWARD";
    String ERS_MODE_YSM_FORWARD_DESCRIPTION = "ERS platform modes during forward view as # of scans in YSM"; /*I18N*/
    String ERS_MODE_FCM_FORWARD_NAME = "ERS_PLATFORM_MODE_FCM_FORWARD";
    String ERS_MODE_FCM_FORWARD_DESCRIPTION = "ERS platform modes during forward view as # of scans in FCM"; /*I18N*/
    String ERS_MODE_OCM_FORWARD_NAME = "ERS_PLATFORM_MODE_OCM_FORWARD";
    String ERS_MODE_OCM_FORWARD_DESCRIPTION = "ERS platform modes during forward view as # of scans in OCM"; /*I18N*/
    String ERS_MODE_FPM_FORWARD_NAME = "ERS_PLATFORM_MODE_FPM_FORWARD";
    String ERS_MODE_FPM_FORWARD_DESCRIPTION = "ERS platform modes during forward view as # of scans in FPM"; /*I18N*/
    String ERS_MODE_RTMM_FORWARD_NAME = "ERS_PLATFORM_MODE_RTMM_FORWARD";
    String ERS_MODE_RTMM_FORWARD_DESCRIPTION = "ERS platform modes during forward view as # of scans in RTMM"; /*I18N*/
    String ERS_MODE_RTMC_FORWARD_NAME = "ERS_PLATFORM_MODE_RTMC_FORWARD";
    String ERS_MODE_RTMC_FORWARD_DESCRIPTION = "ERS platform modes during forward view as # of scans in RTMC"; /*I18N*/

    int NUM_PCD_SETS = 8;
    String PCD_INFO_NADIR_NAME = "PCD_INFORMATION_NADIR";
    String PCD_INFO_NADIR_DESCRIPTION = "Acquisition of PCD information during nadir-view as # of scans for each condition"; /*I18N*/
    String PCD_INFO_FORWARD_NAME = "PCD_INFORMATION_FORWARD";
    String PCD_INFO_FORWARD_DESCRIPTION = "Acquisition of PCD information during forward-view as # of scans for each condition"; /*I18N*/

    int NUM_PACKET_SETS = 10;
    String PACKET_INFO_NADIR_NAME = "PACKET_INFORMATION_NADIR";
    String PACKET_INFO_NADIR_DESCRIPTION = "SADIST-2 packet validation during nadir-view as # of scans for each condition"; /*I18N*/
    String PACKET_INFO_FORWARD_NAME = "PACKET_INFORMATION_FORWARD";
    String PACKET_INFO_FORWARD_DESCRIPTION = "SADIST-2 packet validation during forward-view as # of scans for each condition"; /*I18N*/

    int PIXEL_ERROR_SIZE = 4;
    String MAX_PIXEL_ERROR_CODE_NAME = "MAX_SINGLE_PIXEL_ERROR_CODE";
    String MAX_PIXEL_ERROR_CODE_DESCRIPTION = "Maximum single-pixel error code"; /*I18N*/

    // ranges of parameters
    char[] BYTE_ORDER_FIELD = new char[]{'A', 'B'};
    String[] PRODUCT_TYPES = new String[]{"GBT", "GSST"};
    String[] INSTRUMENTS = new String[]{"ATSR1", "ATSR2"};

    // metadata information
    // --------------------
    String MPH_NAME = "MPH";
    String SPH_NAME = "SPH";
    String QADS_NAME = "QUALITY_ADS";

    // coordinate and product size parameter
    int ATSR_SCENE_RASTER_WIDTH = 512;
    int ATSR_SCENE_RASTER_HEIGHT = 512;
    int ATSR_TIE_PT_GRID_WIDTH = 11;
    int ATSR_TIE_PT_GRID_HEIGHT = 2;
    int ATSR_TIE_PT_SUBS_X = 50;
    int ATSR_TIE_PT_SUBS_Y = ATSR_SCENE_RASTER_HEIGHT;
    float ATSR_TIE_PT_OFFS_X = 0.5f * ATSR_SCENE_RASTER_WIDTH - 0.5F * (ATSR_TIE_PT_GRID_WIDTH - 1) * ATSR_TIE_PT_SUBS_X;
    int LAT_LON_SUBS_X = 16;
    int LAT_LON_SUBS_Y = 16;

    // conversion constants
    float LAT_LON_CONVERSION = 0.001f;

    // flag names and codings
    // ----------------------
    String NADIR_FLAGS_NAME = "cloud_flags_nadir";
    String NADIR_FLAGS_DESCRIPTION = "Nadir-view cloud-clearing/land-flagging results";  /*I18N*/
    String FORWARD_FLAGS_NAME = "cloud_flags_fward";
    String FORWARD_FLAGS_DESCRIPTION = "Forward-view cloud-clearing/land-flagging results"; /*I18N*/

    String LAND_FLAG_NAME = "LAND";
    int LAND_FLAG_MASK = 0x1;
    String LAND_FLAG_DESCRIPTION = "Pixel is over land";  /*I18N*/
    String CLOUD_FLAG_NAME = "CLOUDY";
    int CLOUD_FLAG_MASK = 0x2;
    String CLOUD_FLAG_DESCRIPTION = "Pixel is cloudy (result of all cloud tests)"; /*I18N*/
    String SUNGLINT_FLAG_NAME = "SUN_GLINT";
    int SUNGLINT_FLAG_MASK = 0x4;
    String SUNGLINT_FLAG_DESCRIPTION = "Sunglint detected in pixel"; /*I18N*/

    String REFL_HIST_FLAG_NAME = "CLOUDY_REFL_HIST";
    int REFL_HIST_FLAG_MASK = 0x8;
    String REFL_HIST_FLAG_DESCRIPTION = "1.6 um reflectance histogram test (day-time only)"; /*I18N*/
    String SPAT_COHER_16_FLAG_NAME = "CLOUDY_SPAT_COHER_16";
    int SPAT_COHER_16_FLAG_MASK = 0x10;
    String SPAT_COHER_16_FLAG_DESCRIPTION = "1.6 um spatial coherence test (day-time only)"; /*I18N*/
    String SPAT_COHER_11_FLAG_NAME = "CLOUDY_SPAT_COHER_11";
    int SPAT_COHER_11_FLAG_MASK = 0x20;
    String SPAT_COHER_11_FLAG_DESCRIPTION = "11 um spatial coherence test"; /*I18N*/
    String GROSS_12_FLAG_NAME = "CLOUDY_GROSS_12";
    int GROSS_12_FLAG_MASK = 0x40;
    String GROSS_12_FLAG_DESCRIPTION = "12 um gross cloud test"; /*I18N*/
    String CIRRUS_11_12_FLAG_NAME = "CLOUDY_CIRRUS_11_12";
    int CIRRUS_11_12_FLAG_MASK = 0x80;
    String CIRRUS_11_12_FLAG_DESCRIPTION = "11/12 um thin cirrus test"; /*I18N*/
    String MED_HI_37_12_FLAG_NAME = "CLOUDY_MED_HI_LEVEL_37_12";
    int MED_HI_37_12_FLAG_MASK = 0x100;
    String MED_HI_37_12_FLAG_DESCRIPTION = "3.7/12 um medium/high level test (night-time only)"; /*I18N*/
    String FOG_LOW_STRATUS_11_37_FLAG_NAME = "CLOUDY_FOG_LOW_STRATUS_11_37";
    int FOG_LOW_STRATUS_11_37_FLAG_MASK = 0x200;
    String FOG_LOW_STRATUS_11_37_FLAG_DESCRIPTION = "11/3.7 um fog/low stratus test (night-time only)"; /*I18N*/
    String VW_DIFF_11_12_FLAG_NAME = "CLOUDY_VW_DIFF_11_12";
    int VW_DIFF_11_12_FLAG_MASK = 0x400;
    String VW_DIFF_11_12_FLAG_DESCRIPTION = "11/12 um view difference test"; /*I18N*/
    String VW_DIFF_37_11_FLAG_NAME = "CLOUDY_VW_DIFF_37_11";
    int VW_DIFF_37_11_FLAG_MASK = 0x800;
    String VW_DIFF_37_11_FLAG_DESCRIPTION = "3.7/11 um view difference test (night-time only)"; /*I18N*/
    String THERM_HIST_11_12_FLAG_NAME = "CLOUDY_THERM_HIST_11_12";
    int THERM_HIST_11_12_FLAG_MASK = 0x1000;
    String THERM_HIST_11_12_FLAG_DESCRIPTION = "11/12 um thermal histogram test"; /*I18N*/

    // center wavelengths and bandwidths
    int BAND_12_WAVELENGTH = 12000;
    int BAND_12_WIDTH = 1000;
    int BAND_11_WAVELENGTH = 10800;
    int BAND_11_WIDTH = 1000;
    int BAND_37_WAVELENGTH = 3700;
    int BAND_37_WIDTH = 300;
    int BAND_16_WAVELENGTH = 1600;
    int BAND_16_WIDTH = 300;
    int BAND_87_WAVELENGTH = 870;
    int BAND_87_WIDTH = 20;
    int BAND_65_WAVELENGTH = 670;
    int BAND_65_WIDTH = 20;
    int BAND_55_WAVELENGTH = 550;
    int BAND_55_WIDTH = 20;
}
