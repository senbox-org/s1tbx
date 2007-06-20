/*
 * $Id: AtsrConstantsTest.java,v 1.3 2007/01/25 10:26:58 marcop Exp $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AtsrConstantsTest extends TestCase {

    public AtsrConstantsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AtsrConstantsTest.class);
    }

    public void testBasicConstants() {
        assertEquals("ATSR", AtsrConstants.ATSR_FORMAT_NAME);
        assertEquals("ATSR1/ATSR2 products", AtsrConstants.DESCRIPTION);
    }

    public void testHeaderByteSizeConstants() {
        assertEquals(4096, AtsrConstants.SADIST_2_HEAER_SIZE);
        assertEquals(2, AtsrConstants.BYTE_ORDER_SIZE);
        assertEquals(60, AtsrConstants.PRODUCT_FILE_NAME_SIZE);
        assertEquals(6, AtsrConstants.INSTRUMENT_NAME_SIZE);
        assertEquals(5, AtsrConstants.STATE_VECTOR_TYPE_SIZE);
        assertEquals(16, AtsrConstants.ASCENDING_NODE_TIME_SIZE);
        assertEquals(25, AtsrConstants.ASCENDING_NODE_UT_SIZE);
        assertEquals(13, AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_SIZE);
        assertEquals(9, AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_SIZE);
        assertEquals(11, AtsrConstants.ASCENDING_NODE_LON_SIZE);
        assertEquals(16, AtsrConstants.REFERENCE_UT_SIZE);
        assertEquals(13, AtsrConstants.REFERENCE_ERS_CLOCK_SIZE);
        assertEquals(2, AtsrConstants.RECORD_CONTENTS_SIZE);
        assertEquals(6, AtsrConstants.TRACK_DISTANCE_SIZE);
        assertEquals(25, AtsrConstants.PRODUCT_TIME_SIZE);
        assertEquals(8, AtsrConstants.CORNER_LAT_SIZE);
        assertEquals(9, AtsrConstants.CORNER_LON_SIZE);
        assertEquals(3, AtsrConstants.PIXEL_SELECTION_MAP_SIZE);
        assertEquals(6, AtsrConstants.PSM_CHANGE_SIZE);
        assertEquals(2, AtsrConstants.ATSR2_DATA_RATE_SIZE);
        assertEquals(6, AtsrConstants.ATSR2_DATA_RATE_CHANGE_SIZE);
        assertEquals(8, AtsrConstants.TEMPERATURES_SIZE);
        assertEquals(9, AtsrConstants.ANGLE_PARAMETER_SIZE);
        assertEquals(6, AtsrConstants.CONFIDENCE_SIZE);
        assertEquals(8, AtsrConstants.NUM_PCD_SETS);
        assertEquals(10, AtsrConstants.NUM_PACKET_SETS);
        assertEquals(4, AtsrConstants.PIXEL_ERROR_SIZE);
    }

    public void testHeaderConstants() {
        char[] expByteOrder = new char[]{'A', 'B'};
        String[] expProductTypes = new String[]{"GBT", "GSST"};
        String[] expInstruments = new String[]{"ATSR1", "ATSR2"};

        for (int n = 0; n < expByteOrder.length; n++) {
            assertEquals(expByteOrder[n], AtsrConstants.BYTE_ORDER_FIELD[n]);
        }

        for (int n = 0; n < expProductTypes.length; n++) {
            assertEquals(expProductTypes[n], AtsrConstants.PRODUCT_TYPES[n]);
        }

        for (int n = 0; n < expInstruments.length; n++) {
            assertEquals(expInstruments[n], AtsrConstants.INSTRUMENTS[n]);
        }
    }

    public void testHeaderFieldNameConstants() {
        assertEquals("PRODUCT_FILE_NAME", AtsrConstants.PRODUCT_FILE_NAME_FIELD_NAME);
        assertEquals("INSTRUMENT_NAME", AtsrConstants.INSTRUMENT_NAME_FIELD_NAME);
        assertEquals("ERS_STATE_VECTOR_TYPE", AtsrConstants.STATE_VECTOR_FIELD_NAME);
        assertEquals("ASCENDING_NODE_TIME", AtsrConstants.ASCENDING_NODE_TIME_FIELD_NAME);
        assertEquals("STATE_VECTOR_POSITION_X", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_X_NAME);
        assertEquals("STATE_VECTOR_POSITION_Y", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_Y_NAME);
        assertEquals("STATE_VECTOR_POSITION_Z", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_Z_NAME);
        assertEquals("STATE_VECTOR_VELOCITY_X", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_X_NAME);
        assertEquals("STATE_VECTOR_VELOCITY_Y", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_Y_NAME);
        assertEquals("STATE_VECTOR_VELOCITY_Z", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_Z_NAME);
        assertEquals("ASCENDING_NODE_LONGITUDE", AtsrConstants.ASCENDING_NODE_LON_NAME);
        assertEquals("REFERENCE_UT", AtsrConstants.REFERENCE_UT_FIELD_NAME);
        assertEquals("ERS_CLOCK_TIME", AtsrConstants.REFERENCE_ERS_CLOCK_TIME_NAME);
        assertEquals("ERS_CLOCK_PERIOD", AtsrConstants.REFERENCE_ERS_CLOCK_PERIOD_NAME);
        assertEquals("NADIR_ONLY_PRESENT", AtsrConstants.NADIR_ONLY_PRESENT_NAME);
        assertEquals("THERMAL_RECORDS_PRESENT", AtsrConstants.THERMAL_PRESENT_NAME);
        assertEquals("VISIBLE_RECORDS_PRESENT", AtsrConstants.VISIBLE_PRESENT_NAME);
        assertEquals("LAT_LON_RECORDS_PRESENT", AtsrConstants.LAT_LON_PRESENT_NAME);
        assertEquals("X_Y_RECORDS_PRESENT", AtsrConstants.X_Y_PRESENT_NAME);
        assertEquals("FLAG_RECORDS_PRESENT", AtsrConstants.FLAGS_PRESENT_NAME);
        assertEquals("ALONG_TRACK_START_DISTANCE", AtsrConstants.TRACK_DISTANCE_START_NAME);
        assertEquals("ALONG_TRACK_END_DISTANCE", AtsrConstants.TRACK_DISTANCE_END_NAME);
        assertEquals("PRODUCT_START_TIME", AtsrConstants.UT_PRODUCT_START_NAME);
        assertEquals("PRODUCT_END_TIME", AtsrConstants.UT_PRODUCT_END_NAME);
        assertEquals("CORNER_LAT_LHS_START", AtsrConstants.CORNER_LAT_LHS_START_NAME);
        assertEquals("CORNER_LAT_RHS_START", AtsrConstants.CORNER_LAT_RHS_START_NAME);
        assertEquals("CORNER_LAT_LHS_END", AtsrConstants.CORNER_LAT_LHS_END_NAME);
        assertEquals("CORNER_LAT_RHS_END", AtsrConstants.CORNER_LAT_RHS_END_NAME);
        assertEquals("CORNER_LON_LHS_START", AtsrConstants.CORNER_LON_LHS_START_NAME);
        assertEquals("CORNER_LON_RHS_START", AtsrConstants.CORNER_LON_RHS_START_NAME);
        assertEquals("CORNER_LON_LHS_END", AtsrConstants.CORNER_LON_LHS_END_NAME);
        assertEquals("CORNER_LON_RHS_END", AtsrConstants.CORNER_LON_RHS_END_NAME);
        assertEquals("PIXEL_SELECTION_MAPS_NADIR", AtsrConstants.PIXEL_SELECTION_MAP_NADIR_NAME);
        assertEquals("PSM_CHANGE_DISTANCE_NADIR", AtsrConstants.PSM_CHANGE_NADIR_NAME);
        assertEquals("PIXEL_SELECTION_MAPS_FORWARD", AtsrConstants.PIXEL_SELECTION_MAP_FORWARD_NAME);
        assertEquals("PSM_CHANGE_DISTANCE_FORWARD", AtsrConstants.PSM_CHANGE_FORWARD_NAME);
        assertEquals("ATSR2_DATA_RATE_NADIR_VIEW", AtsrConstants.ATSR2_DATA_RATE_NADIR_NAME);
        assertEquals("ATSR2_DATA_RATE_CHANGE_NADIR_VIEW", AtsrConstants.ATSR2_DATA_RATE_CHANGE_NADIR_NAME);
        assertEquals("ATSR2_DATA_RATE_FORWARD_VIEW", AtsrConstants.ATSR2_DATA_RATE_FORWARD_NAME);
        assertEquals("ATSR2_DATA_RATE_CHANGE_FORWARD_VIEW", AtsrConstants.ATSR2_DATA_RATE_CHANGE_FORWARD_NAME);
        assertEquals("MINIMUM_SCC_TEMPERATURE", AtsrConstants.MIN_SCC_TEMPERATURE_NAME);
        assertEquals("MINIMUM_INSTRUMENT_TEMPERATURE_1200_NM", AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_1200_NM_NAME);
        assertEquals("MINIMUM_INSTRUMENT_TEMPERATURE_1100_NM", AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_1100_NM_NAME);
        assertEquals("MINIMUM_INSTRUMENT_TEMPERATURE_370_NM", AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_370_NM_NAME);
        assertEquals("MINIMUM_INSTRUMENT_TEMPERATURE_160_NM", AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_160_NM_NAME);
        assertEquals("MINIMUM_INSTRUMENT_TEMPERATURE_87_NM", AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_87_NM_NAME);
        assertEquals("MAXIMUM_SCC_TEMPERATURE", AtsrConstants.MAX_SCC_TEMPERATURE_NAME);
        assertEquals("MAXIMUM_INSTRUMENT_TEMPERATURE_1200_NM", AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_1200_NM_NAME);
        assertEquals("MAXIMUM_INSTRUMENT_TEMPERATURE_1100_NM", AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_1100_NM_NAME);
        assertEquals("MAXIMUM_INSTRUMENT_TEMPERATURE_370_NM", AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_370_NM_NAME);
        assertEquals("MAXIMUM_INSTRUMENT_TEMPERATURE_160_NM", AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_160_NM_NAME);
        assertEquals("MAXIMUM_INSTRUMENT_TEMPERATURE_87_NM", AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_87_NM_NAME);
        assertEquals("sun_elev_nadir", AtsrConstants.SUN_ELEVATION_NADIR_NAME);
        assertEquals("view_elev_nadir", AtsrConstants.VIEW_ELEVATION_NADIR_NAME);
        assertEquals("sun_azimuth_nadir", AtsrConstants.SUN_AZIMUTH_NADIR_NAME);
        assertEquals("view_azimuth_nadir", AtsrConstants.VIEW_AZIMUTH_NADIR_NAME);
        assertEquals("sun_elev_forward", AtsrConstants.SUN_ELEVATION_FORWARD_NAME);
        assertEquals("view_elev_forward", AtsrConstants.VIEW_ELEVATION_FORWARD_NAME);
        assertEquals("sun_azimuth_forward", AtsrConstants.SUN_AZIMUTH_FORWARD_NAME);
        assertEquals("view_azimuth_forward", AtsrConstants.VIEW_AZIMUTH_FORWARD_NAME);
        assertEquals("ERS_PLATFORM_MODE_YSM_NADIR", AtsrConstants.ERS_MODE_YSM_NADIR_NAME);
        assertEquals("ERS_PLATFORM_MODE_FCM_NADIR", AtsrConstants.ERS_MODE_FCM_NADIR_NAME);
        assertEquals("ERS_PLATFORM_MODE_OCM_NADIR", AtsrConstants.ERS_MODE_OCM_NADIR_NAME);
        assertEquals("ERS_PLATFORM_MODE_FPM_NADIR", AtsrConstants.ERS_MODE_FPM_NADIR_NAME);
        assertEquals("ERS_PLATFORM_MODE_RTMM_NADIR", AtsrConstants.ERS_MODE_RTMM_NADIR_NAME);
        assertEquals("ERS_PLATFORM_MODE_RTMC_NADIR", AtsrConstants.ERS_MODE_RTMC_NADIR_NAME);
        assertEquals("ERS_PLATFORM_MODE_YSM_FORWARD", AtsrConstants.ERS_MODE_YSM_FORWARD_NAME);
        assertEquals("ERS_PLATFORM_MODE_FCM_FORWARD", AtsrConstants.ERS_MODE_FCM_FORWARD_NAME);
        assertEquals("ERS_PLATFORM_MODE_OCM_FORWARD", AtsrConstants.ERS_MODE_OCM_FORWARD_NAME);
        assertEquals("ERS_PLATFORM_MODE_FPM_FORWARD", AtsrConstants.ERS_MODE_FPM_FORWARD_NAME);
        assertEquals("ERS_PLATFORM_MODE_RTMM_FORWARD", AtsrConstants.ERS_MODE_RTMM_FORWARD_NAME);
        assertEquals("ERS_PLATFORM_MODE_RTMC_FORWARD", AtsrConstants.ERS_MODE_RTMC_FORWARD_NAME);
        assertEquals("PCD_INFORMATION_NADIR", AtsrConstants.PCD_INFO_NADIR_NAME);
        assertEquals("PCD_INFORMATION_FORWARD", AtsrConstants.PCD_INFO_FORWARD_NAME);
        assertEquals("PACKET_INFORMATION_NADIR", AtsrConstants.PACKET_INFO_NADIR_NAME);
        assertEquals("PACKET_INFORMATION_FORWARD", AtsrConstants.PACKET_INFO_FORWARD_NAME);
        assertEquals("MAX_SINGLE_PIXEL_ERROR_CODE", AtsrConstants.MAX_PIXEL_ERROR_CODE_NAME);
        assertEquals("latitude", AtsrConstants.LATITUDE_NAME);
        assertEquals("longitude", AtsrConstants.LONGITUDE_NAME);
    }

    public void testHeaderFieldDescriptionsandUnits() {
        // @todo 3 nf/tb - remove and avoid tests checking values not used in the system's logic
        assertEquals("Days since January 1st, 1950", AtsrConstants.ASCENDING_NODE_TIME_DESCRIPTION);
        assertEquals("d", AtsrConstants.ASCENDING_NODE_TIME_UNIT);
        assertEquals("Universal time at ascending node", AtsrConstants.ASCENDING_NODE_UT_DESCRIPTION);
        assertEquals("km", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_UNIT);
        assertEquals("Ascending node state vector position x",
                     AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_X_DESCRIPTION);
        assertEquals("Ascending node state vector position y",
                     AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_Y_DESCRIPTION);
        assertEquals("Ascending node state vector position z",
                     AtsrConstants.ASCENDING_NODE_STATE_VECTOR_POSITION_Z_DESCRIPTION);
        assertEquals("km/s", AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_UNIT);
        assertEquals("Ascending node state vector velocity x",
                     AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_X_DESCRIPTION);
        assertEquals("Ascending node state vector velocity y",
                     AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_Y_DESCRIPTION);
        assertEquals("Ascending node state vector velocity z",
                     AtsrConstants.ASCENDING_NODE_STATE_VECTOR_VELOCITY_Z_DESCRIPTION);
        assertEquals("Longitude of ascending node", AtsrConstants.ASCENDING_NODE_LON_DESCRIPTION);
        assertEquals("dec. deg.", AtsrConstants.ASCENDING_NODE_LON_UNIT);
        assertEquals("Reference universal time (days since January 1st, 1950)", AtsrConstants.REFERENCE_UT_DESCRIPTION);
        assertEquals("d", AtsrConstants.REFERENCE_UT_UNIT);
        assertEquals("ns", AtsrConstants.REFERENCE_ERS_CLOCK_UNIT);
        assertEquals("Reference ERS satellite clock time", AtsrConstants.REFERENCE_ERS_CLOCK_TIME_DESCRIPTION);
        assertEquals("Period of ERS satellite clock", AtsrConstants.REFERENCE_ERS_CLOCK_PERIOD_DESCRIPTION);
        assertEquals("Nadir-only records present", AtsrConstants.NADIR_ONLY_DESCRIPTION);
        assertEquals("Thermal infra-red detector records present", AtsrConstants.THERMAL_PRESENT_DESCRIPTION);
        assertEquals("Visible/near-infra-red detector records present", AtsrConstants.VISIBLE_PRESENT_DESCRIPTION);
        assertEquals("Latitude/longitude records present", AtsrConstants.LAT_LON_PRESENT_DESCRIPTION);
        assertEquals("X/Y coordinate records present", AtsrConstants.X_Y_PRESENT_DESCRIPTION);
        assertEquals("Cloud-clearing/land-flagging records present", AtsrConstants.FLAGS_PRESENT_DESCRIPTION);
        assertEquals("km", AtsrConstants.TRACK_DISTANCE_UNIT);
        assertEquals("Along-track distance of product start", AtsrConstants.TRACK_DISTANCE_START_DESCRIPTION);
        assertEquals("Along-track distance of product end", AtsrConstants.TRACK_DISTANCE_END_DESCRIPTION);
        assertEquals("Universal time of data acquisition at product start", AtsrConstants.UT_PRODUCT_START_DESCRIPTION);
        assertEquals("Universal time of data acquisition at product end", AtsrConstants.UT_PRODUCT_END_DESCRIPTION);
        assertEquals("dec. deg.", AtsrConstants.CORNER_LAT_LON_UNITS);
        assertEquals("Latitude of product LHS corner point at start", AtsrConstants.CORNER_LAT_LHS_START_DESCRIPTION);
        assertEquals("Latitude of product RHS corner point at start", AtsrConstants.CORNER_LAT_RHS_START_DESCRIPTION);
        assertEquals("Latitude of product LHS corner point at end", AtsrConstants.CORNER_LAT_LHS_END_DESCRIPTION);
        assertEquals("Latitude of product RHS corner point at end", AtsrConstants.CORNER_LAT_RHS_END_DESCRIPTION);
        assertEquals("Longitude of product LHS corner point at start", AtsrConstants.CORNER_LON_LHS_START_DESCRIPTION);
        assertEquals("Longitude of product RHS corner point at start", AtsrConstants.CORNER_LON_RHS_START_DESCRIPTION);
        assertEquals("Longitude of product LHS corner point at end", AtsrConstants.CORNER_LON_LHS_END_DESCRIPTION);
        assertEquals("Longitude of product RHS corner point at end", AtsrConstants.CORNER_LON_RHS_END_DESCRIPTION);
        assertEquals("1st and 2nd ATSR-2 Pixel Selection Maps in nadir-view",
                     AtsrConstants.PIXEL_SELECTION_MAP_NADIR_DESCRIPTION);
        assertEquals("Along-track distance of 1st PSM change in nadir-view",
                     AtsrConstants.PSM_CHANGE_NADIR_DESCRIPTION);
        assertEquals("km", AtsrConstants.PSM_CHANGE_UNIT);
        assertEquals("1st and 2nd ATSR-2 Pixel Selection Maps in forward-view",
                     AtsrConstants.PIXEL_SELECTION_MAP_FORWARD_DESCRIPTION);
        assertEquals("Along-track distance of 1st PSM change in forward-view",
                     AtsrConstants.PSM_CHANGE_FORWARD_DESCRIPTION);
        assertEquals("ATSR-2 data-rate at start of nadir-view", AtsrConstants.ATSR2_DATA_RATE_NADIR_DESCRIPTION);
        assertEquals("Along-track distance of 1st ATSR-2 data-rate change in nadir-view",
                     AtsrConstants.ATSR2_DATA_RATE_CHANGE_NADIR_DESCRIPTION);
        assertEquals("km", AtsrConstants.ATSR2_DATA_RATE_CHANGE_UNIT);
        assertEquals("ATSR-2 data-rate at start of forward-view", AtsrConstants.ATSR2_DATA_RATE_FORWARD_DESCRIPTION);
        assertEquals("Along-track distance of 1st ATSR-2 data-rate change in forward-view",
                     AtsrConstants.ATSR2_DATA_RATE_CHANGE_FORWARD_DESCRIPTION);
        assertEquals("Minimum Stirling Cycle Cooler cold-tip temperature",
                     AtsrConstants.MIN_SCC_TEMPERATURE_DESCRIPTION);
        assertEquals("K", AtsrConstants.TEMPERATURES_UNIT);
        assertEquals("Minimum instrument detector temperature at 1200 nm",
                     AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_1200_NM_DESCRIPTION);
        assertEquals("Minimum instrument detector temperature at 1100 nm",
                     AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_1100_NM_DESCRIPTION);
        assertEquals("Minimum instrument detector temperature at 370 nm",
                     AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_370_NM_DESCRIPTION);
        assertEquals("Minimum instrument detector temperature at 160 nm",
                     AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_160_NM_DESCRIPTION);
        assertEquals("Minimum instrument detector temperature at 87 nm",
                     AtsrConstants.MIN_INSTRUMENT_TEMPERATURE_87_NM_DESCRIPTION);
        assertEquals("Maximum Stirling Cycle Cooler cold-tip temperature",
                     AtsrConstants.MAX_SCC_TEMPERATURE_DESCRIPTION);
        assertEquals("Maximum instrument detector temperature at 1200 nm",
                     AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_1200_NM_DESCRIPTION);
        assertEquals("Maximum instrument detector temperature at 1100 nm",
                     AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_1100_NM_DESCRIPTION);
        assertEquals("Maximum instrument detector temperature at 370 nm",
                     AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_370_NM_DESCRIPTION);
        assertEquals("Maximum instrument detector temperature at 160 nm",
                     AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_160_NM_DESCRIPTION);
        assertEquals("Maximum instrument detector temperature at 87 nm",
                     AtsrConstants.MAX_INSTRUMENT_TEMPERATURE_87_NM_DESCRIPTION);
        assertEquals("dec. deg.", AtsrConstants.ANGLE_UNIT);
        assertEquals("ERS platform modes during nadir view as # of scans in YSM",
                     AtsrConstants.ERS_MODE_YSM_NADIR_DESCRIPTION);
        assertEquals("ERS platform modes during nadir view as # of scans in FCM",
                     AtsrConstants.ERS_MODE_FCM_NADIR_DESCRIPTION);
        assertEquals("ERS platform modes during nadir view as # of scans in OCM",
                     AtsrConstants.ERS_MODE_OCM_NADIR_DESCRIPTION);
        assertEquals("ERS platform modes during nadir view as # of scans in FPM",
                     AtsrConstants.ERS_MODE_FPM_NADIR_DESCRIPTION);
        assertEquals("ERS platform modes during nadir view as # of scans in RTMM",
                     AtsrConstants.ERS_MODE_RTMM_NADIR_DESCRIPTION);
        assertEquals("ERS platform modes during nadir view as # of scans in RTMC",
                     AtsrConstants.ERS_MODE_RTMC_NADIR_DESCRIPTION);
        assertEquals("ERS platform modes during forward view as # of scans in YSM",
                     AtsrConstants.ERS_MODE_YSM_FORWARD_DESCRIPTION);
        assertEquals("ERS platform modes during forward view as # of scans in FCM",
                     AtsrConstants.ERS_MODE_FCM_FORWARD_DESCRIPTION);
        assertEquals("ERS platform modes during forward view as # of scans in OCM",
                     AtsrConstants.ERS_MODE_OCM_FORWARD_DESCRIPTION);
        assertEquals("ERS platform modes during forward view as # of scans in FPM",
                     AtsrConstants.ERS_MODE_FPM_FORWARD_DESCRIPTION);
        assertEquals("ERS platform modes during forward view as # of scans in RTMM",
                     AtsrConstants.ERS_MODE_RTMM_FORWARD_DESCRIPTION);
        assertEquals("ERS platform modes during forward view as # of scans in RTMC",
                     AtsrConstants.ERS_MODE_RTMC_FORWARD_DESCRIPTION);
        assertEquals("Acquisition of PCD information during nadir-view as # of scans for each condition",
                     AtsrConstants.PCD_INFO_NADIR_DESCRIPTION);
        assertEquals("Acquisition of PCD information during forward-view as # of scans for each condition",
                     AtsrConstants.PCD_INFO_FORWARD_DESCRIPTION);
        assertEquals("SADIST-2 packet validation during nadir-view as # of scans for each condition",
                     AtsrConstants.PACKET_INFO_NADIR_DESCRIPTION);
        assertEquals("SADIST-2 packet validation during forward-view as # of scans for each condition",
                     AtsrConstants.PACKET_INFO_FORWARD_DESCRIPTION);
        assertEquals("Maximum single-pixel error code", AtsrConstants.MAX_PIXEL_ERROR_CODE_DESCRIPTION);
        assertEquals("Nadir-view solar elevation", AtsrConstants.SUN_ELEVATION_NADIR_DESCRIPTION);
        assertEquals("Nadir-view satellite elevation", AtsrConstants.VIEW_ELEVATION_NADIR_DESCRIPTION);
        assertEquals("Nadir-view solar azimuth", AtsrConstants.SUN_AZIMUTH_NADIR_DESCRIPTION);
        assertEquals("Nadir-view satellite azimuth", AtsrConstants.VIEW_AZIMUTH_NADIR_DESCRIPTION);
        assertEquals("Forward-view solar elevation", AtsrConstants.SUN_ELEVATION_FORWARD_DESCRIPTION);
        assertEquals("Forward-view satellite elevation", AtsrConstants.VIEW_ELEVATION_FORWARD_DESCRIPTION);
        assertEquals("Forward-view solar azimuth", AtsrConstants.SUN_AZIMUTH_FORWARD_DESCRIPTION);
        assertEquals("Forward-view satellite azimuth", AtsrConstants.VIEW_AZIMUTH_FORWARD_DESCRIPTION);
        assertEquals("Longitudes of image pixels", AtsrConstants.LONGITUDE_DESCRIPTION);
        assertEquals("Latitudes of image pixels", AtsrConstants.LATITUDE_DESCRIPTION);
    }

    public void testMetadataConstants() {
        assertEquals("MPH", AtsrConstants.MPH_NAME);
        assertEquals("SPH", AtsrConstants.SPH_NAME);
    }

    public void testProductSizeConstants() {
        assertEquals(512, AtsrConstants.ATSR_SCENE_RASTER_WIDTH);
        assertEquals(512, AtsrConstants.ATSR_SCENE_RASTER_HEIGHT);
        assertEquals(11, AtsrConstants.ATSR_TIE_PT_GRID_WIDTH);
        assertEquals(2, AtsrConstants.ATSR_TIE_PT_GRID_HEIGHT);
        assertEquals(50, AtsrConstants.ATSR_TIE_PT_SUBS_X);
        assertEquals(512, AtsrConstants.ATSR_TIE_PT_SUBS_Y);
        assertEquals(6.f, AtsrConstants.ATSR_TIE_PT_OFFS_X, 1e-6);
        assertEquals(16, AtsrConstants.LAT_LON_SUBS_X);
        assertEquals(16, AtsrConstants.LAT_LON_SUBS_Y);
    }

    public void testPixelSizes() {
        assertEquals(2, AtsrConstants.BT_PIXEL_SIZE);
        assertEquals(2, AtsrConstants.REF_PIXEL_SIZE);
        assertEquals(2, AtsrConstants.SST_PIXEL_SIZE);
        assertEquals(2, AtsrConstants.SST_CONF_PIXEL_SIZE);
        assertEquals(4, AtsrConstants.LATLON_PIXEL_SIZE);
        assertEquals(1, AtsrConstants.OFFSET_PIXEL_SIZE);
        assertEquals(2, AtsrConstants.FLAGS_PIXEL_SIZE);
    }

    public void testConversionConstants() {
        assertEquals(0.001f, AtsrConstants.LAT_LON_CONVERSION, 1e-6);
    }

    public void testFlagCodings() {

        // @todo 3 nf/tb - remove and avoid tests checking values not used in the system's logic
        assertEquals("Nadir-view cloud-clearing/land-flagging results", AtsrConstants.NADIR_FLAGS_DESCRIPTION);
        // @todo 3 nf/tb - remove and avoid tests checking values not used in the system's logic
        assertEquals("Forward-view cloud-clearing/land-flagging results", AtsrConstants.FORWARD_FLAGS_DESCRIPTION);

        assertEquals("cloud_flags_nadir", AtsrConstants.NADIR_FLAGS_NAME);
        assertEquals("cloud_flags_fward", AtsrConstants.FORWARD_FLAGS_NAME);

        assertEquals("LAND", AtsrConstants.LAND_FLAG_NAME);
        assertEquals(0x1, AtsrConstants.LAND_FLAG_MASK);
        assertEquals("Pixel is over land", AtsrConstants.LAND_FLAG_DESCRIPTION);
        assertEquals("CLOUDY", AtsrConstants.CLOUD_FLAG_NAME);
        assertEquals(0x2, AtsrConstants.CLOUD_FLAG_MASK);
        assertEquals("Pixel is cloudy (result of all cloud tests)", AtsrConstants.CLOUD_FLAG_DESCRIPTION);
        assertEquals("SUN_GLINT", AtsrConstants.SUNGLINT_FLAG_NAME);
        assertEquals(0x4, AtsrConstants.SUNGLINT_FLAG_MASK);
        assertEquals("Sunglint detected in pixel", AtsrConstants.SUNGLINT_FLAG_DESCRIPTION);

        assertEquals("CLOUDY_REFL_HIST", AtsrConstants.REFL_HIST_FLAG_NAME);
        assertEquals(0x8, AtsrConstants.REFL_HIST_FLAG_MASK);
        assertEquals("1.6 um reflectance histogram test (day-time only)", AtsrConstants.REFL_HIST_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_SPAT_COHER_16", AtsrConstants.SPAT_COHER_16_FLAG_NAME);
        assertEquals(0x10, AtsrConstants.SPAT_COHER_16_FLAG_MASK);
        assertEquals("1.6 um spatial coherence test (day-time only)", AtsrConstants.SPAT_COHER_16_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_SPAT_COHER_11", AtsrConstants.SPAT_COHER_11_FLAG_NAME);
        assertEquals(0x20, AtsrConstants.SPAT_COHER_11_FLAG_MASK);
        assertEquals("11 um spatial coherence test", AtsrConstants.SPAT_COHER_11_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_GROSS_12", AtsrConstants.GROSS_12_FLAG_NAME);
        assertEquals(0x40, AtsrConstants.GROSS_12_FLAG_MASK);
        assertEquals("12 um gross cloud test", AtsrConstants.GROSS_12_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_CIRRUS_11_12", AtsrConstants.CIRRUS_11_12_FLAG_NAME);
        assertEquals(0x80, AtsrConstants.CIRRUS_11_12_FLAG_MASK);
        assertEquals("11/12 um thin cirrus test", AtsrConstants.CIRRUS_11_12_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_MED_HI_LEVEL_37_12", AtsrConstants.MED_HI_37_12_FLAG_NAME);
        assertEquals(0x100, AtsrConstants.MED_HI_37_12_FLAG_MASK);
        assertEquals("3.7/12 um medium/high level test (night-time only)", AtsrConstants.MED_HI_37_12_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_FOG_LOW_STRATUS_11_37", AtsrConstants.FOG_LOW_STRATUS_11_37_FLAG_NAME);
        assertEquals(0x200, AtsrConstants.FOG_LOW_STRATUS_11_37_FLAG_MASK);
        assertEquals("11/3.7 um fog/low stratus test (night-time only)",
                     AtsrConstants.FOG_LOW_STRATUS_11_37_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_VW_DIFF_11_12", AtsrConstants.VW_DIFF_11_12_FLAG_NAME);
        assertEquals(0x400, AtsrConstants.VW_DIFF_11_12_FLAG_MASK);
        assertEquals("11/12 um view difference test", AtsrConstants.VW_DIFF_11_12_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_VW_DIFF_37_11", AtsrConstants.VW_DIFF_37_11_FLAG_NAME);
        assertEquals(0x800, AtsrConstants.VW_DIFF_37_11_FLAG_MASK);
        assertEquals("3.7/11 um view difference test (night-time only)", AtsrConstants.VW_DIFF_37_11_FLAG_DESCRIPTION);
        assertEquals("CLOUDY_THERM_HIST_11_12", AtsrConstants.THERM_HIST_11_12_FLAG_NAME);
        assertEquals(0x1000, AtsrConstants.THERM_HIST_11_12_FLAG_MASK);
        assertEquals("11/12 um thermal histogram test", AtsrConstants.THERM_HIST_11_12_FLAG_DESCRIPTION);
    }
}
