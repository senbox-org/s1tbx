/*
 * $Id: FlhMciConstants.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
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
package org.esa.beam.processor.flh_mci;

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.processor.ProcessorConstants;

/**
 * Provides an interface defining all constants used with the FLH_RTI processor.
 */
public class FlhMciConstants implements ProcessorConstants {

    // the required request type
    public static final String REQUEST_TYPE = "FLH_MCI";

    // Default file path parameter
    public static final String DEFAULT_FILE_NAME = "flh_mci_out.dim";

    // default value for logging file parameter default filename
    public static final String DEFAULT_LOG_FILE_FILENAME = "flh_mci_log.txt";
    // default value for logging file parameter file selection mode
    public static final int DEFAULT_LOG_FILE_FILESELECTIONMODE = ParamProperties.FSM_FILES_ONLY;
    // the default value for the parameter log_prefix
    public static final String DEFAULT_LOG_PREFIX = "flh_mci";

    // Parameter name for the request parameter describing the preset.
    public static final String PRESET_PARAM_NAME = "preset_name";
    // The value set valid for this parameter type
    public static final String[] PRESET_PARAM_VALUE_SET = {"MERIS L2 FLH",
                                                           "MERIS L1b MCI",
                                                           "MERIS L2 MCI",
                                                           "General baseline height"};
    // the default value to be set
    public static final String PRESET_PARAM_DEFAULT_VALUE = "General baseline height";
    // the label text for the parameter editor
    public static final String PRESET_PARAM_LABELTEXT = "Preset";
    // the description for this parameter
    public static final String PRESET_PARAM_DESCRIPTION = "Preset name";
    // the value unit of this parameter
    public static final String PRESET_PARAM_VALUE_UNIT = "";

    // Default value for band parameter unit
    public static final String DEFAULT_BAND_VALUEUNIT = "";

    // parameter name for the lower baseline band name
    public static final String BAND_LOW_PARAM_NAME = "band_low";
    // Default name for low band
    public static final String DEFAULT_BAND_LOW = "low_baseline_band";
    // Default value for lower baseline band name label
    public static final String DEFAULT_BAND_LOW_LABELTEXT = "Low baseline band name";
    // Default value for lower baseline band name parameter description
    public static final String DEFAULT_BAND_LOW_DESCRIPTION = "Low baseline band name";

    // parameter name for the high baseline band name
    public static final String BAND_HIGH_PARAM_NAME = "band_high";
    // Default name for high baseline band
    public static final String DEFAULT_BAND_HIGH = "high_baseline_band";
    // Default value for high baseline band name label
    public static final String DEFAULT_BAND_HIGH_LABELTEXT = "High baseline band name";
    // Default value for high baseline band name parameter description
    public static final String DEFAULT_BAND_HIGH_DESCRIPTION = "High baseline band name";

    // parameter name for the signal band name
    public static final String BAND_SIGNAL_PARAM_NAME = "band_signal";
    // Default name for signal band
    public static final String DEFAULT_BAND_SIGNAL = "signal_band";
    // Default value for signal band name label
    public static final String DEFAULT_BAND_SIGNAL_LABELTEXT = "Signal band name";
    // Default value for signal band name parameter description
    public static final String DEFAULT_BAND_SIGNAL_DESCRIPTION = "Signal band name";

    // Parameter name for the invalid pixel value parameter
    public static final String INVALID_PIXEL_VALUE_PARAM_NAME = "invalid";
    // Default value for invalid pixels
    public static final Float DEFAULT_INVALID_PIXEL_VALUE = new Float(0.f);
    // Default value for invalid pixel value label
    public static final String DEFAULT_INVALID_PIXEL_VALUE_LABELTEXT = "Default value for invalid pixels";
    // Default value for invalid pixel value parameter description
    public static final String DEFAULT_INVALID_PIXEL_VALUE_DESCRIPTION = "Default FLH/MCI value for invalid pixels";
    // Default value for nvalid pixel value parameter unit
    public static final String DEFAULT_INVALID_PIXEL_VALUE_VALUEUNIT = "mW/(m^2 * sr * nm)";

    // Parameter name for lineheight band name
    public static final String LINEHEIGHT_BAND_NAME_PARAM_NAME = "lineheight_band_name";
    // name of the parameter label for the parameter lineheight band name
    public static final String LINEHEIGHT_BAND_NAME_LABELTEXT = "Lineheight band name";
    // parameter description for the parameter lineheight band name
    public static final String LINEHEIGHT_BAND_NAME_DESCRIPTION = "Lineheight band name";
    // default value for parameter lineheight band name
    public static final String DEFAULT_LINE_HEIGHT_BAND_NAME = "flh";
    // lineheight band description
    public static final String LINEHEIGHT_BAND_DESCRIPTION = "Lineheight band";

    // Parameter name for parameter process slope
    public static final String PROCESS_SLOPE_PARAM_NAME = "process_slope";
    // parameter label for the parameter process slope
    public static final String PROCESS_SLOPE_LABELTEXT = "Output slope";
    // description for the parameter process slope
    public static final String PROCESS_SLOPE_DESCRIPTION = "Baseline slope is an output.";
    // default value for the parameter process slope
    public static final boolean DEFAULT_PROCESS_SLOPE = true;

    // Parameter name for the paramewter slope band name
    public static final String SLOPE_BAND_NAME_PARAM_NAME = "slope_band_name";
    // parameter label for the parameter slope band name
    public static final String SLOPE_BAND_NAME_LABELTEXT = "Slope band name";
    // parameter description for parameter slope band name
    public static final String SLOPE_BAND_NAME_DESCRIPTION = "Slope band name";
    // default value for parameter slope band name
    public static final String DEFAULT_SLOPE_BAND_NAME = "flh_slope";
    // the slope band description
    public static final String SLOPE_BAND_DESCRIPTION = "Baseline slope band";

    // parameter name for the parameter bitmask
    public static final String BITMASK_PARAM_NAME = "Bitmask";
    // parameter label for the parameter bitmask
    public static final String BITMASK_LABELTEXT = "Bitmask";
    // parameter description for the parameter bitmask
    public static final String BITMASK_DESCRIPTION = "Bitmask expression used to identify valid pixels";
    // default value for parameter bitmask
    public static final String DEFAULT_BITMASK = "";

    // parameter name for the parameter cloud correction factor
    public static final String CLOUD_CORRECTION_FACTOR_PARAM_NAME = "cloud_correct";
    // parameter label for the parameter cloud correction factor
    public static final String CLOUD_CORRECTION_FACTOR_LABELTEXT = "Cloud Correction Factor";
    // parameter description for the parameter cloud correction factor
    public static final String CLOUD_CORRECTION_FACTOR_DESCRIPTION = "Cloud correction factor";
    // default value for the parameter cloud correction factor
    public static final Float DEFAULT_CLOUD_CORRECTION_FACTOR = new Float(BaselineAlgorithm.DEFAULT_CLOUD_CORRECT);

    public static final String LOGGER_NAME = "beam.processor.flh_mci";

    // some messages
    public static final String ERROR_MSG_NEGATIVE_WAVELENGTH = "Negative wavelengths!";
    public static final String ERROR_MSG_NUMERATOR_ZERO = "Numerator is 0, low and signal wavelength are identical!";
    public static final String ERROR_MSG_DENOM_ZERO = "Denominator is 0, low and high wavelength are identical!";
    public static final String ERROR_NO_PARAMETER = "Unable to retrieve parameter: ";

    public static final String LOG_MSG_HEADER = "Logfile generated by BEAM FLH/MCI Processor, version ";
    public static final String LOG_MSG_INVALID_PIXEL = "Invalid pixel value: ";
    public static final String LOG_MSG_LINEHEIGHT_NAME = "Lineheight band name: ";
    public static final String LOG_MSG_NO_LINEHEIGHT = "No value for lineheight band name set!";

    public static final String LOG_MSG_SLOPE_ENABLED = "Generating baseline slope enabled.";
    public static final String LOG_MSG_SLOPE_BAND_NAME = "Slope band name: ";
    public static final String LOG_MSG_NO_SLOPE_BAND = "No value for slope band name set!";
    public static final String LOG_MSG_NO_SLOPE_PARAMETER = "No value for parameter 'process_slope' set!";
    public static final String LOG_MSG_NO_SLOPE_PROCESS = "... slope will not be processed!";

    public static final String LOG_MSG_CLOUD_CORRECT = "Cloud correction factor: ";
    public static final String LOG_MSG_NO_CLOUD_CORRECT = "No value for cloud correction factor set!";

    public static final String LOG_MSG_CENTER_WAVE = "... center wavelength: ";

    public static final String LOG_MSG_GENERATE_PIXEL = "Generating pixels for FLH/MCI band(s)...";


}
