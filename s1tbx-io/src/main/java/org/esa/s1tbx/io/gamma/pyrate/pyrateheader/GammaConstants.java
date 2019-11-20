/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma.pyrate.pyrateheader;

public class GammaConstants {

    static final String HEADER_KEY_NAME = "title";
    static final String HEADER_KEY_SAMPLES = "range_samples";
    static final String HEADER_KEY_SAMPLES1 = "range_samp_1";
    static final String HEADER_KEY_WIDTH = "width";
    static final String HEADER_KEY_NCOLUMNS = "ncolumns";
    static final String HEADER_KEY_LINES = "azimuth_lines";
    static final String HEADER_KEY_LINES1 = "az_samp_1";
    static final String HEADER_KEY_HEIGHT = "height";
    static final String HEADER_KEY_NLINES = "nlines";
    static final String HEADER_KEY_BANDS = "bands";
    static final String HEADER_KEY_HEADER_OFFSET = "line_header_size";
    static final String HEADER_KEY_DATA_FORMAT = "data_format";
    static final String HEADER_KEY_DATA_TYPE = "image_format";
    static final String HEADER_KEY_SENSOR_TYPE = "sensor";
    static final String HEADER_KEY_BYTE_ORDER = "byte order";
    static final String HEADER_KEY_BAND_NAMES = "band names";
    static final String HEADER_KEY_DESCRIPTION = "sensor";

    static final String HEADER_KEY_DATE = "date";
    static final String HEADER_KEY_START_TIME = "start_time";
    static final String HEADER_KEY_CENTER_TIME = "center_time";
    static final String HEADER_KEY_END_TIME = "end_time";
    static final String HEADER_KEY_LINE_TIME_INTERVAL = "azimuth_line_time";

    static final String HEADER_KEY_RANGE_LOOKS = "range_looks";
    static final String HEADER_KEY_AZIMUTH_LOOKS = "azimuth_looks";
    static final String HEADER_KEY_IMAGE_GEOMETRY = "image_geometry";
    static final String HEADER_KEY_CENTER_LATITUDE = "center_latitude";
    static final String HEADER_KEY_CENTER_LONGITUDE = "center_longitude";
    static final String HEADER_KEY_HEADING = "heading";
    static final String HEADER_KEY_RANGE_PIXEL_SPACING = "range_pixel_spacing";
    static final String HEADER_KEY_AZIMUTH_PIXEL_SPACING = "azimuth_pixel_spacing";
    static final String HEADER_KEY_RADAR_FREQUENCY = "radar_frequency";
    static final String HEADER_KEY_PRF = "prf";
    static final String HEADER_KEY_AZIMUTH_PROC_BANDWIDTH = "azimuth_proc_bandwidth";
    static final String HEADER_KEY_NEAR_RANGE_SLC = "near_range_slc";
    static final String HEADER_KEY_CENTER_RANGE_SLC = "center_range_slc";
    static final String HEADER_KEY_FAR_RANGE_SLC = "far_range_slc";

    static final String HEADER_KEY_SAR_TO_EARTH_CENTER = "sar_to_earth_center";
    static final String HEADER_KEY_EARTH_RADIUS_BELOW_SENSOR = "earth_radius_below_sensor";
    static final String HEADER_KEY_EARTH_SEMI_MAJOR_AXIS = "earth_semi_major_axis";
    static final String HEADER_KEY_EARTH_SEMI_MINOR_AXIS = "earth_semi_minor_axis";

    static final String HEADER_KEY_NUM_STATE_VECTORS = "number_of_state_vectors";
    static final String HEADER_KEY_TIME_FIRST_STATE_VECTORS = "time_of_first_state_vector";
    static final String HEADER_KEY_STATE_VECTOR_INTERVAL = "state_vector_interval";
    static final String HEADER_KEY_STATE_VECTOR_POSITION = "state_vector_position";
    static final String HEADER_KEY_STATE_VECTOR_VELOCITY = "state_vector_velocity";

    static final String HEADER_KEY_DEM_PROJECTION = "DEM_projection";
    static final String HEADER_KEY_DEM_HGT_OFFSET = "DEM_hgt_offset";
    static final String HEADER_KEY_DEM_SCALE = "DEM_scale";
    static final String HEADER_KEY_DEM_CORNER_NORTH = "corner_north";
    static final String HEADER_KEY_DEM_CORNER_EAST = "corner_east";
    static final String HEADER_KEY_DEM_POST_NORTH = "post_north"; //DEM posting (pixel spacing) North direction; negative numbers indicate that DEM lines are listed from North to South
    static final String HEADER_KEY_DEM_POST_EAST = "post_east";

    static final String HEADER_KEY_DATUM_NAME = "datum_name";
    static final String HEADER_KEY_DATUM_SHIFT_DX = "datum_shift_dx";
    static final String HEADER_KEY_DATUM_SHIFT_DY = "datum_shift_dy";
    static final String HEADER_KEY_DATUM_SHIFT_DZ = "datum_shift_dz";
    static final String HEADER_KEY_DATUM_SCALE = "datum_scale_m";
    static final String HEADER_KEY_DATUM_ROTATION_ALPHA = "datum_rotation_alpha";
    static final String HEADER_KEY_DATUM_ROTATION_BETA = "datum_rotation_beta";
    static final String HEADER_KEY_DATUM_ROTATION_GAMMA = "datum_rotation_gamma";

    static final String HEADER_KEY_PROJECTION_NAME = "projection_name";
    static final String HEADER_KEY_PROJECTION_ZONE = "projection_zone";
    static final String HEADER_KEY_PROJECTION_FALSE_EASTING = "false_easting";
    static final String HEADER_KEY_PROJECTION_FALSE_NORTHING = "false_northing";
    static final String HEADER_KEY_PROJECTION_K0 = "projection_k0";
    static final String HEADER_KEY_PROJECTION_CENTER_LON = "center_longitude";
    static final String HEADER_KEY_PROJECTION_CENTER_LAT = "center_latitude";

    static final String PROJECTION_EQA  = "EQA"; // 	Equiangular
    static final String PROJECTION_UTM  = "UTM"; //  	Universal Transverse Mercator
    static final String PROJECTION_TM   = "TM";  //  	Transverse Mercator
    static final String PROJECTION_OMCH = "OMCH"; // 	Oblique Mercator (Switzerland)
    static final String PROJECTION_LCC  = "LCC"; //  	Lambert Conformal Conic (France)
    static final String PROJECTION_PS   = "PS";  // 	Polar Stereographic
    static final String PROJECTION_SCH  = "SCH"; // 	SAR coordinates used at JPL
    static final String PROJECTION_PC   = "PC";  // 	Polyconic Projection
    static final String PROJECTION_AEAC = "AEAC"; //  	Albers Equal Area Conic
    static final String PROJECTION_LCC2 = "LCC2"; //  	Lambert Conformal Conic Projection (Secant solution, Belgium)
    static final String PROJECTION_OM   = "OM";  // 	Oblique Mercator
    static final String PROJECTION_HOM  = "HOM"; // 	Hotine Oblique Mercator

    public final static String SLC_EXTENSION = ".rslc";
    final static String PAR_EXTENSION = ".par";






    private GammaConstants() {
    }
}
