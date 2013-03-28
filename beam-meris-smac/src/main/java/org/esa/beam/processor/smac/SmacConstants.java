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
package org.esa.beam.processor.smac;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;

/**
 * Provides an interface defining all constants used with the SMAC processor.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class SmacConstants implements ProcessorConstants {

    // the required request type
    public static final String REQUEST_TYPE = "SMAC";

    /**
     * Parameter name for the request parameter describing input product type.
     */
    public static final String PRODUCT_TYPE_PARAM_NAME = "prod_type";
    /**
     * Parameter name for the request parameter describing the bands to process.
     */
    public static final String BANDS_PARAM_NAME = "bands";
    /**
     * Parameter name for the request parameter describing the aerosol type.
     */
    public static final String AEROSOL_TYPE_PARAM_NAME = "aero_type";
    /**
     * Parameter name for the request parameter describing the aerosol optical depth at 550 nm.
     */
    public static final String AEROSOL_OPTICAL_DEPTH_PARAM_NAME = "tau_aero_550";
    /**
     * Parameter name for the request parameter describing the horizontal visibility.
     */
    public static final String HORIZONTAL_VISIBILITY_PARAM_NAME = "Vis";
    /**
     * Parameter name for the request parameter to use the MERIS ADS.
     */
    public static final String USE_MERIS_ADS_PARAM_NAME = "useMerisADS";
    /**
     * Parameter name for the request parameter to use the MERIS lat/lon corrected ADS.
     */
    public static final String USE_LAT_LONG_CORRECT_PARAM_NAME = "use_lat_long";
    /**
     * Parameter name for the request parameter describing the air pressure at sea level.
     */
    public static final String SURFACE_AIR_PRESSURE_PARAM_NAME = "surf_press";
    /**
     * Parameter name for the request parameter describing the ozone content.
     */
    public static final String OZONE_CONTENT_PARAM_NAME = "u_o3";
    /**
     * Parameter name for the request parameter describing the water vapour content.
     */
    public static final String RELATIVE_HUMIDITY_PARAM_NAME = "u_h2o";
    /**
     * Parameter name for the request parameter value for pixels set to invalid.
     */
    public static final String DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME = "invalid";
    // Parameter name for the bitmask expression
    public static final String BITMASK_PARAM_NAME = "Bitmask";
    // Parameter name for the forward bitmask expression
    public static final String BITMASK_FORWARD_PARAM_NAME = "BitmaskForward";
    // Parameter name for the nadir bitmask expression
    public static final String BITMASK_NADIR_PARAM_NAME = "BitmaskNadir";

    /**
     * Parameter name for aerosol type "desert".
     */
    public static final String AER_TYPE_DESERT = "Desert";
    /**
     * Parameter name for aerosol type "continental".
     */
    public static final String AER_TYPE_CONTINENTAL = "Continental";

    /**
     * Enumerates all valid MERIS bands.
     */
    public static final String[] MERIS_L1B_BANDS = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;

    /**
     * Enumerates all valis AATSR bands.
     */
    public static final String[] AATSR_L1B_BANDS = new String[]{
            EnvisatConstants.AATSR_L1B_BAND_NAMES[3], EnvisatConstants.AATSR_L1B_BAND_NAMES[4],
            EnvisatConstants.AATSR_L1B_BAND_NAMES[5], EnvisatConstants.AATSR_L1B_BAND_NAMES[6],
            EnvisatConstants.AATSR_L1B_BAND_NAMES[10], EnvisatConstants.AATSR_L1B_BAND_NAMES[11],
            EnvisatConstants.AATSR_L1B_BAND_NAMES[12], EnvisatConstants.AATSR_L1B_BAND_NAMES[13]
    };

    /**
     * Default value for visibility type parameter valueSet
     */
    public static final String[] DEFAULT_VIS_TYPE_VALUESET = new String[]{"Clear", "Foggy", "FoggyFogg"};
    /**
     * Default value for visibility type parameter label
     */
    public static final String DEFAULT_VIS_TYPE_LABELTEXT = "WMO type";
    /**
     * Default value for visibility type parameter description
     */
    public static final String DEFAULT_VIS_TYPE_DESCRIPTION = "WMO visibility type";
    /**
     * Default value for visibility type parameter defaultValue
     */
    public static final String DEFAULT_VIS_TYPE_DEFAULTVALUE = "Foggy";

    /**
     * Default value for aerosol type parameter valueSet
     */
    public static final String[] DEFAULT_AER_TYPE_VALUESET = new String[]{AER_TYPE_DESERT, AER_TYPE_CONTINENTAL};
    /**
     * Default value for aerosol type parameter label
     */
    public static final String DEFAULT_AER_TYPE_LABELTEXT = "Aerosol type";
    /**
     * Default value for aerosol type parameter description
     */
    public static final String DEFAULT_AER_TYPE_DESCRIPTION = "Aerosol type";
    /**
     * Default value for aerosol type parameter defaultValue
     */
    public static final String DEFAULT_AER_TYPE_DEFAULTVALUE = "Continental";

    /**
     * Default value for product type parameter label
     */
    public static final String DEFAULT_PRODUCT_TYPE_LABELTEXT = "Product type";
    /**
     * Default value for product type parameter description
     */
    public static final String DEFAULT_PRODUCT_TYPE_DESCRIPTION = "Product type";
    /**
     * Default value for product type parameter defaultValue
     */
    public static final String DEFAULT_PRODUCT_TYPE_DEFAULTVALUE = EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME;

    /**
     * Default value for bands parameter valueSet
     */
    public static final String[] DEFAULT_BANDS_VALUESET = StringUtils.addArrays(MERIS_L1B_BANDS, AATSR_L1B_BANDS);
    /**
     * Default value for bands parameter label
     */
    public static final String DEFAULT_BANDS_LABELTEXT = "Bands";
    /**
     * Default value for bands parameter description
     */
    public static final String DEFAULT_BANDS_DESCRIPTION = "Bands to be corrected";
    /**
     * Default value for bands parameter defaultValue
     */
    public static final String[] DEFAULT_BANDS_DEFAULTVALUE = new String[]{
            "radiance_1",
            "radiance_2",
            "radiance_3",
            "radiance_4"
    };

    /**
     * Default value for UseMerisEcmwf parameter label
     */
    public static final String DEFAULT_USEMERIS_LABELTEXT = "Use MERIS ECMWF Data";
    /**
     * Default value for UseMerisEcmwf parameter description
     */
    public static final String DEFAULT_USEMERIS_DESCRIPTION = "Whether to use MERIS ECMWF data or not";

    /**
     * Default value for UseCorrectetLatLon parameter label
     */
    public static final String DEFAULT_USECORRLATLON_LABELTEXT = "Use corrected Lat./Long. info";
    /**
     * Default value for UseCorrectetLatLon parameter description
     */
    public static final String DEFAULT_USECORRLATLON_DESCRIPTION = "Whether to use corrected latitude/longitude or not";

    /**
     * Default value for default reflectance for invalid pixels parameter defaultValue
     */
    public static final Float DEFAULT_DEFREFLECT_DEFAULTVALUE = 0.0f;
    /**
     * Default value for default reflectance for invalid pixels parameter label
     */
    public static final String DEFAULT_DEFREFLECT_LABELTEXT = "Default value for invalid pixels";
    /**
     * Default value for default reflectance for invalid pixels parameter description
     */
    public static final String DEFAULT_DEFREFLECT_DESCRIPTION = "Default surface reflectance for invalid pixels (range 0..1)";
    /**
     * Default value for default reflectance for invalid pixels parameter maxValue
     */
    public static final Float DEFAULT_DEFREFLECT_MAXVALUE = 1.0f;
    /**
     * Default value for default reflectance for invalid pixels parameter valueUnit
     */
    public static final String DEFAULT_DEFREFLECT_VALUEUNIT = "";

    /**
     * Default value for water vapour parameter defaultValue
     */
    public static final Float DEFAULT_H2O_DEFAULTVALUE = 3.0f;
    /**
     * Default value for water vapour parameter maxValue
     */
    public static final Float DEFAULT_H2O_MAXVALUE = 7.0f;
    /**
     * Default value for water vapour parameter label
     */
    public static final String DEFAULT_H2O_LABELTEXT = "Water vapour";
    /**
     * Default value for water vapour parameter description
     */
    public static final String DEFAULT_H2O_DESCRIPTION = "Water vapour content (range 0..7)";
    /**
     * Default value for water vapour parameter valueUnit
     */
    public static final String DEFAULT_H2O_VALUEUNIT = "g/cm^2";

    /**
     * Default value for ozone content parameter defaultValue
     */
    public static final Float DEFAULT_OZONECONTENT_DEFAULTVALUE = 0.150f;
    /**
     * Default value for ozone content parameter maxValue
     */
    public static final Float DEFAULT_OZONECONTENT_MAXVALUE = 1.0f;
    /**
     * Default value for ozone content parameter label
     */
    public static final String DEFAULT_OZONECONTENT_LABELTEXT = "Ozone content";
    /**
     * Default value for ozone content parameter description
     */
    public static final String DEFAULT_OZONECONTENT_DESCRIPTION = "Ozone content";
    /**
     * Default value for ozone content parameter valueUnit
     */
    public static final String DEFAULT_OZONECONTENT_VALUEUNIT = "cm * atm";

    /**
     * Default value for surface air pressure parameter defaultValue
     */
    public static final Float DEFAULT_SURF_AIR_PRESS_DEFAULTVALUE = 1013.0f;
    /**
     * Default value for surface air pressure parameter minValue
     */
    public static final Float DEFAULT_SURF_AIR_PRESS_MINVALUE = 100.0f;
    /**
     * Default value for surface air pressure parameter maxValue
     */
    public static final Float DEFAULT_SURF_AIR_PRESS_MAXVALUE = 1100.0f;
    /**
     * Default value for surface air pressure parameter label
     */
    public static final String DEFAULT_SURF_AIR_PRESS_LABELTEXT = "Surface air pressure";
    /**
     * Default value for surface air pressure parameter description
     */
    public static final String DEFAULT_SURF_AIR_PRESS_DESCRIPTION = "Surface air pressure";
    /**
     * Default value for surface air pressure parameter valueUnit
     */
    public static final String DEFAULT_SURF_AIR_PRESS_VALUEUNIT = "hPa";

    /**
     * Default value for horizontal visibility parameter defaultValue
     */
    public static final Float DEFAULT_HORIZ_VIS_DEFAULTVALUE = 39.2f;
    /**
     * Default value for horizontal visibility parameter minValue
     */
    public static final Float DEFAULT_MIN_HORIZ_VIS_MINVALUE = 3.92f;
    /**
     * Default value for horizontal visibility parameter maxValue
     */
    public static final Float DEFAULT_MAX_HORIZ_VIS_MAXVALUE = 100.0f;
    /**
     * Default value for horizontal visibility parameter label
     */
    public static final String DEFAULT_MAX_HORIZ_VIS_LABELTEXT = "Horizontal visibility";
    /**
     * Default value for horizontal visibility parameter description
     */
    public static final String DEFAULT_MAX_HORIZ_VIS_DESCRIPTION = "Horizontal visibility";
    /**
     * Default value for horizontal visibility parameter valueUnit
     */
    public static final String DEFAULT_MAX_HORIZ_VIS_VALUEUNIT = "km";

    /**
     * Default value for aerosol optical depth parameter defaultValue
     */
    public static final Float DEFAULT_AER_OPT_DEPTH_DEFAULTVALUE = 0.2f;
    /**
     * Default value for aerosol optical depth parameter minValue
     */
    public static final Float DEFAULT_AER_OPT_DEPTH_MINVALUE = 0.0784f;
    /**
     * Default value for aerosol optical depth parameter maxValue
     */
    public static final Float DEFAULT_AER_OPT_DEPTH_MAXVALUE = 2.0f;
    /**
     * Default value for aerosol optical depth parameter label
     */
    public static final String DEFAULT_AER_OPT_DEPTH_LABELTEXT = "Aerosol optical depth";
    /**
     * Default value for aerosol optical depth parameter description
     */
    public static final String DEFAULT_AER_OPT_DEPTH_DESCRIPTION = "Aerosol optical depth @ 550nm (range 0..2)";
    /**
     * Default value for aerosol optical depth parameter valueUnit
     */
    public static final String DEFAULT_AER_OPT_DEPTH_VALUEUNIT = "";

    /**
     * Default value for logging file parameter default filename
     */
    public static final String DEFAULT_LOG_FILE_FILENAME = "smac_log.txt";
    /**
     * Default value for logging file parameter file selection mode
     */
    public static final int DEFAULT_LOG_FILE_FILESELECTIONMODE = ParamProperties.FSM_FILES_ONLY;

    /**
     * Default value for auxiliary coefficients directory parameter label
     */
    public static final String DEFAULT_AUXCOEFFDIR_LABELTEXT = "Aux. coeff. directory";
    /**
     * Default value for auxiliary coefficients directory parameter description
     */
    public static final String DEFAULT_AUXCOEFFDIR_DESCRIPTION = "Auxilary coefficient directory";
    /**
     * Default value for auxiliary coefficients directory parameter directory path
     */
    public static final String DEFAULT_AUXCOEFFDIR_COEFFDIR = SystemUtils.convertToLocalPath("auxdata/smac");
    /**
     * Default value for auxiliary coefficients directory parameter file selection mode
     */
    public static final int DEFAULT_AUXCOEFFDIR_FILESELECTIONMODE = ParamProperties.FSM_DIRECTORIES_ONLY;

    /**
     * Default file path parameter
     */
    public static final String DEFAULT_FILE_NAME = "smac_out.dim";

    /**
     * Productfile prefix for AATSR product
     */
    public static final String PREFIX_AATSR_PRODUCT = "ATS_";
    /**
     * Productfile prefix for MERIS product
     */
    public static final String PREFIX_MERIS_PRODUCT = "MER_";

    // The default valueset for Meris L1b products flags
    public static final String[] DEFAULT_MERIS_FLAGS_VALUESET = {
            "l1_flags.COSMETIC",
            "l1_flags.DUPLICATED",
            "l1_flags.GLINT_RISK",
            "l1_flags.SUSPECT",
            "l1_flags.LAND_OCEAN",
            "l1_flags.BRIGHT",
            "l1_flags.COASTLINE",
            "l1_flags.INVALID"
    };
    // the parameter default value
    public static final String DEFAULT_MERIS_FLAGS_VALUE = "l1_flags.LAND_OCEAN and not (l1_flags.INVALID or l1_flags.BRIGHT)";
    // the default Meris bitmask label
    public static final String DEFAULT_MERIS_BITMASK_LABEL = "Bitmask";
    // the description for the Meris L1b bitmask
    public static final String DEFAULT_MERIS_BITMASK_DESCRIPTION = "Bitmask expression for Meris L1b products";

    // the default aatsr forward bitmask valueset
    public static final String[] DEFAULT_FORWARD_FLAGS_VALUESET = {
            "cloud_flags_forward.LAND",
            "cloud_flags_forward.CLOUDY",
            "cloud_flags_forward.SUN_GLINT",
            "cloud_flags_forward.CLOUDY_REFL_HIST",
            "cloud_flags_forward.CLOUDY_SPAT_COHER_16",
            "cloud_flags_forward.CLOUDY_SPAT_COHER_11",
            "cloud_flags_forward.CLOUDY_GROSS_12",
            "cloud_flags_forward.CLOUDY_CIRRUS_11_12",
            "cloud_flags_forward.CLOUDY_MED_HI_LEVEL_37_12",
            "cloud_flags_forward.CLOUDY_FOG_LOW_STRATUS_11_37",
            "cloud_flags_forward.CLOUDY_VW_DIFF_11_12",
            "cloud_flags_forward.CLOUDY_VW_DIFF_37_11",
            "cloud_flags_forward.CLOUDY_THERM_HIST_11_12"
    };
    // the parameter default value
    public static final String DEFAULT_FORWARD_FLAGS_VALUE = "cloud_flags_fward.LAND and not cloud_flags_fward.CLOUDY";
    // the label for the aatsr forward bitmask parameter
    public static final String DEFAULT_AATSR_FORWARD_LABEL = "Bitmask forward";
    // the description for the aatsr forward
    public static final String DEFAULT_AATSR_FORWARD_DESCRIPTION = "Bitmask expression for AATSR forward bands";

    // the default aatsr nadir bitmask valueset
    public static final String[] DEFAULT_NADIR_FLAGS_VALUESET = {
            "cloud_flags_nadir.LAND",
            "cloud_flags_nadir.CLOUDY",
            "cloud_flags_nadir.SUN_GLINT",
            "cloud_flags_nadir.CLOUDY_REFL_HIST",
            "cloud_flags_nadir.CLOUDY_SPAT_COHER_16",
            "cloud_flags_nadir.CLOUDY_SPAT_COHER_11",
            "cloud_flags_nadir.CLOUDY_GROSS_12",
            "cloud_flags_nadir.CLOUDY_CIRRUS_11_12",
            "cloud_flags_nadir.CLOUDY_MED_HI_LEVEL_37_12",
            "cloud_flags_nadir.CLOUDY_FOG_LOW_STRATUS_11_37",
            "cloud_flags_nadir.CLOUDY_VW_DIFF_11_12",
            "cloud_flags_nadir.CLOUDY_VW_DIFF_37_11",
            "cloud_flags_nadir.CLOUDY_THERM_HIST_11_12"
    };
    // the parameter default bvalue
    public static final String DEFAULT_NADIR_FLAGS_VALUE = "cloud_flags_nadir.LAND and not cloud_flags_nadir.CLOUDY";
    // the label for the aatsr nadir bitmask parameter
    public static final String DEFAULT_AATSR_NADIR_LABEL = "Bitmask nadir";
    // the description for the aatsr nadir
    public static final String DEFAULT_AATSR_NADIR_DESCRIPTION = "Bitmask expression for AATSR nadir bands";

    // some logging messages
    public static final String LOGGER_NAME = "beam.processor.smac";
    public static final String DEFAULT_LOG_PREFIX = "smac";
    public static final String LOG_MSG_OPEN_COEFF_ERROR = "Unable to open coefficient map file!";
    public static final String LOG_MSG_AUX_DIR = "Using auxiliary data path: ";
    public static final String LOG_MSG_AUX_ERROR = "Error reading coefficients from: ";
    public static final String LOG_MSG_HEADER = "Logfile generated by BEAM SMAC Processor, version ";

    public static final String LOG_MSG_DEFAULT_NADIR_BITMASK = "... using default nadir bitmask: ";
    public static final String LOG_MSG_DEFAULT_FORWARD_BITMASK = "... using default forward bitmask: ";
    public static final String LOG_MSG_DEFAULT_BITMASK = "... using default bitmask: ";
    public static final String LOG_MSG_NADIR_BITMASK = "Using nadir bitmask: ";
    public static final String LOG_MSG_FORWARD_BITMASK = "Using forward bitmask: ";
    public static final String LOG_MSG_BITMASK = "Using bitmask: ";

    public static final String LOG_MSG_NO_INPUT_BANDS = "No input bands defined, processing cannot be performed";
    public static final String LOG_MSG_UNSUPPORTED_SENSOR = "Unsupported sensor type!";

    public static final String LOG_MSG_LOAD_MERIS_ADS = "Loading MERIS ADS needed ...";
    public static final String LOG_MSG_LOAD_AATSR_ADS = "Loading AATSR ADS needed ...";
    public static final String LOG_MSG_LOADED = "... loaded ";
    public static final String LOG_MSG_ILLEGAL_BAND = "Illegal band name ";

    public static final String LOG_MSG_LOADED_COEFFICIENTS = "Loaded sensor coefficient file ";
    public static final String LOG_MSG_ERROR_COEFFICIENTS = "Unable to load sensor coefficients for band ";

    public static final String LOG_MSG_COEFF_NOT_FOUND_1 = "Sensor coefficient file for spectral band '";
    public static final String LOG_MSG_COEFF_NOT_FOUND_2 = "' not found!";

    public static final String LOG_MSG_GENERATING_PIXEL_1 = "Generating pixels for band '";
    public static final String LOG_MSG_GENERATING_PIXEL_2 = "'...";

    public static final String LOG_MSG_PROCESSING_CORRECTION = "Processing atmospheric correction ..."; /*I18N*/

    public static final String LOG_MSG_INVALID_AEROSOL = "Invalid aerosol type ";

    public static final String LOG_MSG_UNSUPPORTED_INPUT_1 = "Unsupported input product of type '";
    public static final String LOG_MSG_UNSUPPORTED_INPUT_2 = "'.\nSMAC processes AATSR and MERIS L1b products.";

    public static final String SMAC_AUXDATA_DIR_PROPERTY = "smac.auxdata.dir";
}

