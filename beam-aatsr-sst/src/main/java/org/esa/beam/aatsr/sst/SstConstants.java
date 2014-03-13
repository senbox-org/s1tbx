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
package org.esa.beam.aatsr.sst;

/**
 * Provides an interface defining all constants used with the Sst processor.
 */
class SstConstants {

    public static final String LOGGER_NAME = "beam.processor.sst";
    public static final String PROCESS_DUAL_VIEW_SST_LABELTEXT = "Generate dual-view SST";
    public static final String PROCESS_DUAL_VIEW_SST_DESCRIPTION = "Enables/disables generation of the dual-view SST";
    public static final String DEFAULT_DUAL_VIEW_BITMASK = "!cloud_flags_nadir.LAND and !cloud_flags_nadir.CLOUDY and !cloud_flags_nadir.SUN_GLINT and !cloud_flags_fward.LAND and !cloud_flags_fward.CLOUDY and !cloud_flags_fward.SUN_GLINT";
    public static final String PROCESS_NADIR_VIEW_SST_LABELTEXT = "Generate nadir-view SST";
    public static final String PROCESS_NADIR_VIEW_SST_DESCRIPTION = "Enables/disables generation of the nadir-view SST";
    public static final String NADIR_VIEW_COEFF_FILE_DESCRIPTION = "Coefficient file for the nadir-view SST";
    public static final String DUAL_VIEW_COEFF_FILE_DESCRIPTION = "Coefficient file for the dual-view SST";
    public static final String DEFAULT_NADIR_VIEW_BITMASK = "!cloud_flags_nadir.LAND and !cloud_flags_nadir.CLOUDY and !cloud_flags_nadir.SUN_GLINT";
    public static final String NADIR_370_BAND = "btemp_nadir_0370";
    public static final String NADIR_1100_BAND = "btemp_nadir_1100";
    public static final String NADIR_1200_BAND = "btemp_nadir_1200";
    public static final String FORWARD_370_BAND = "btemp_fward_0370";
    public static final String FORWARD_1100_BAND = "btemp_fward_1100";
    public static final String FORWARD_1200_BAND = "btemp_fward_1200";
    public static final String SUN_ELEV_NADIR = "sun_elev_nadir";
    public static final String SUN_ELEV_FORWARD = "sun_elev_fward";
    public static final String OUT_BAND_UNIT = "K";
    public static final String OUT_BAND_NADIR_DESCRIPTION = "Nadir-view sea surface temperature";
    public static final String OUT_BAND_DUAL_DESCRIPTION = "Combined view sea surface temperature";
}
