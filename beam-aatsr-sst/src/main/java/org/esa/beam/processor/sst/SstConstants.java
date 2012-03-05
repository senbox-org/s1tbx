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
package org.esa.beam.processor.sst;

import org.esa.beam.framework.processor.ProcessorConstants;

/**
 * Provides an interface defining all constants used with the Sst processor.
 */
public class SstConstants implements ProcessorConstants {

    // the required request type
    @Deprecated
    public static final String REQUEST_TYPE = "SST";

    // Default output file path parameter
    @Deprecated
    public static final String DEFAULT_FILE_NAME = "sst_out.dim";

    // default value for logging file parameter default filename
    @Deprecated
    public static final String LOGGER_NAME = "beam.processor.sst";
    @Deprecated
    public static final String DEFAULT_LOG_PREFIX = "sst";
    // obsolete - but keep the functionality for request file backward compatibility
    @Deprecated
    public static final String DEFAULT_LOG_FILE_FILENAME = "sst_log.txt";

    // parameter name for the parameter "process dual view sst"
    @Deprecated
    public static final String PROCESS_DUAL_VIEW_SST_PARAM_NAME = "process_dual_view_sst";
    // labeltext for the parameter "process dual view sst"
    public static final String PROCESS_DUAL_VIEW_SST_LABELTEXT = "Generate dual-view SST";
    // descritption for the parameter "process dual view sst"
    public static final String PROCESS_DUAL_VIEW_SST_DESCRIPTION = "Enables/disables generation of the dual-view SST";
    // default value for parameter "process dual view sst"
    @Deprecated
    public static final Boolean DEFAULT_PROCESS_DUAL_VIEW_SST = Boolean.TRUE;

    // parameter name for the parameter "dual view coefficient file"
    @Deprecated
    public static final String DUAL_VIEW_COEFF_FILE_PARAM_NAME = "dual_view_coeff_file";
    // parameter name for the parameter dual view coefficient file description"
    @Deprecated
    public static final String DUAL_VIEW_COEFF_DESC_PARAM_NAME = "dual_view_coeff_desc";
    // labeltext for the parameter "dual view coefficient file"
    @Deprecated
    public static final String DUAL_VIEW_COEFF_FILE_LABELTEXT = "Coefficient file for dual-view";
    // descritption for the parameter "dual view coefficient file"
    public static final String DUAL_VIEW_COEFF_FILE_DESCRIPTION = "Coefficient file for the dual-view SST";
    // default value for parameter "process dual view sst"
    @Deprecated
    public static final String DEFAULT_DUAL_VIEW_COEFF_FILE = "test coefficients dual view 1";

    // parameter name for the parameter "dual view bitmask"
    @Deprecated
    public static final String DUAL_VIEW_BITMASK_PARAM_NAME = "dual_view_bitmask";
    // labeltext for the parameter "dual view bitmask"
    @Deprecated
    public static final String DUAL_VIEW_BITMASK_LABELTEXT = "Bitmask for dual-view";
    // description for the parameter  "dual view bitmask"
    @Deprecated
    public static final String DUAL_VIEW_BITMASK_DESCRIPTION = "Bitmask for dual-view SST";
    // default value for parameter "dual view bitmask"
    public static final String DEFAULT_DUAL_VIEW_BITMASK = "!cloud_flags_nadir.LAND and !cloud_flags_nadir.CLOUDY and !cloud_flags_nadir.SUN_GLINT and !cloud_flags_fward.LAND and !cloud_flags_fward.CLOUDY and !cloud_flags_fward.SUN_GLINT";

    // parameter name for the parameter "process nadir view sst"
    @Deprecated
    public static final String PROCESS_NADIR_VIEW_SST_PARAM_NAME = "process_nadir_view_sst";
    // labeltext for the parameter "process nadir view sst"
    public static final String PROCESS_NADIR_VIEW_SST_LABELTEXT = "Generate nadir-view SST";
    // descritption for the parameter "process nadir view sst"
    public static final String PROCESS_NADIR_VIEW_SST_DESCRIPTION = "Enables/disables generation of the nadir-view SST";
    // default value for parameter "process dual view sst"
    @Deprecated
    public static final Boolean DEFAULT_PROCESS_NADIR_VIEW_SST = Boolean.TRUE;

    // parameter name for the parameter "nadir view coefficient file"
    @Deprecated
    public static final String NADIR_VIEW_COEFF_FILE_PARAM_NAME = "nadir_view_coeff_file";
    // parameter name for the parameter nadi"r view coefficient file description"
    @Deprecated
    public static final String NADIR_VIEW_COEFF_DESC_PARAM_NAME = "nadir_view_coeff_desc";
    // labeltext for the parameter "nadir view coefficient file"
    @Deprecated
    public static final String NADIR_VIEW_COEFF_FILE_LABELTEXT = "Coefficient file for nadir-view";
    // descritption for the parameter "nadir view coefficient file"
    public static final String NADIR_VIEW_COEFF_FILE_DESCRIPTION = "Coefficient file for the nadir-view SST";
    // default value for parameter "process nadir view sst"
    @Deprecated
    public static final String DEFAULT_NADIR_VIEW_COEFF_FILE = "test coefficients nadir 1";

    // parameter name for the parameter "nadir view bitmask"
    @Deprecated
    public static final String NADIR_VIEW_BITMASK_PARAM_NAME = "nadir_view_bitmask";
    // labeltext for the parameter "nadir view bitmask"
    @Deprecated
    public static final String NADIR_VIEW_BITMASK_LABELTEXT = "Bitmask for nadir-view";
    // description for the parameter  "nadir view bitmask"
    @Deprecated
    public static final String NADIR_VIEW_BITMASK_DESCRIPTION = "Bimask for nadir-view SST";
    // default value for parameter "nadir view bitmask"
    public static final String DEFAULT_NADIR_VIEW_BITMASK = "!cloud_flags_nadir.LAND and !cloud_flags_nadir.CLOUDY and !cloud_flags_nadir.SUN_GLINT";

    // parameter name for the parameter "invalid pixel value"
    @Deprecated
    public static final String INVALID_PIXEL_PARAM_NAME = "invalid";
    // labeltext for the parameter "invalid pixel value"
    @Deprecated
    public static final String INVALID_PIXEL_LABELTEXT = "Invalid pixel value";
    // description for the parameter "invalid pixel value"
    @Deprecated
    public static final String INVALID_PIXEL_DESCRIPTION = "Value for no-data output pixels";
    // default value for the parameter "invalid pixel value"
    @Deprecated
    public static final Float DEFAULT_INVALID_PIXEL = new Float(-999f);

    // band name constants used in the processor
    // 3.7 u nadir
    public static final String NADIR_370_BAND = "btemp_nadir_0370";
    // 11 u nadir
    public static final String NADIR_1100_BAND = "btemp_nadir_1100";
    // 12 u nadir
    public static final String NADIR_1200_BAND = "btemp_nadir_1200";
    // 3.7 u forward
    public static final String FORWARD_370_BAND = "btemp_fward_0370";
    // 11 u forward
    public static final String FORWARD_1100_BAND = "btemp_fward_1100";
    // 12 u forward
    public static final String FORWARD_1200_BAND = "btemp_fward_1200";
    // sun elevation nadir
    public static final String SUN_ELEV_NADIR = "sun_elev_nadir";
    // sun elevation forward
    public static final String SUN_ELEV_FORWARD = "sun_elev_fward";

    // output band unit
    public static final String OUT_BAND_UNIT = "K";
    // output band nadir view description
    public static final String OUT_BAND_NADIR_DESCRIPTION = "Nadir-view sea surface temperature";
    // output band dual view description
    public static final String OUT_BAND_DUAL_DESCRIPTION = "Combined view sea surface temperature";

    public static final String AUXDATA_DIR_PROPERTY = "sst.auxdata.dir";

    // relative path to the nadir coefficients
    @Deprecated
    public static final String AUXPATH_NADIR_VIEW = "nadir_view";
    // relative path to the dual coefficients
    @Deprecated
    public static final String AUXPATH_DUAL_VIEW = "dual_view";
}
