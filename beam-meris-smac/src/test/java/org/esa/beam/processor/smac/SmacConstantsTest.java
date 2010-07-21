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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SmacConstantsTest extends TestCase {

    private String[] _expMerisL1bFlags = {
        "l1_flags.COSMETIC",
        "l1_flags.DUPLICATED",
        "l1_flags.GLINT_RISK",
        "l1_flags.SUSPECT",
        "l1_flags.LAND_OCEAN",
        "l1_flags.BRIGHT",
        "l1_flags.COASTLINE",
        "l1_flags.INVALID"
    };

    private String[] _expNadirFlags = {
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
        "cloud_flags_nadir.CLOUDY_THERM_HIST_11_12",
    };

    private String[] _expForwardFlags = {
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

    /**
     * Constructs the test case
     */
    public SmacConstantsTest(String testName) {
        super(testName);
    }

    /**
     * Exports the test class to framework
     */
    public static Test suite() {
        return new TestSuite(SmacConstantsTest.class);
    }

    /**
     * Tests the setting of the parequest parameter constants
     */
    public void testParameterConstants() {
        assertEquals("output_product", SmacConstants.OUTPUT_PRODUCT_PARAM_NAME);
        assertEquals("log_file", SmacConstants.LOG_FILE_PARAM_NAME);
        assertEquals("prod_type", SmacConstants.PRODUCT_TYPE_PARAM_NAME);
        assertEquals("bands", SmacConstants.BANDS_PARAM_NAME);
        assertEquals("aero_type", SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        assertEquals("tau_aero_550", SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        assertEquals("Vis", SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME);
        assertEquals("useMerisADS", SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        assertEquals("surf_press", SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME);
        assertEquals("u_o3", SmacConstants.OZONE_CONTENT_PARAM_NAME);
        assertEquals("u_h2o", SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME);
        assertEquals("invalid", SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME);
        assertEquals("Bitmask", SmacConstants.BITMASK_PARAM_NAME);
        assertEquals("BitmaskForward", SmacConstants.BITMASK_FORWARD_PARAM_NAME);
        assertEquals("BitmaskNadir", SmacConstants.BITMASK_NADIR_PARAM_NAME);
    }

    /**
     * Tests the bitmask constants
     */
    public void testBitmaskConstants() {
        for (int n = 0; n < SmacConstants.DEFAULT_MERIS_FLAGS_VALUESET.length; n++) {
            assertEquals(_expMerisL1bFlags[n], SmacConstants.DEFAULT_MERIS_FLAGS_VALUESET[n]);
        }

        assertEquals("l1_flags.LAND_OCEAN and not (l1_flags.INVALID or l1_flags.BRIGHT)",
                     SmacConstants.DEFAULT_MERIS_FLAGS_VALUE);

        for (int n = 0; n < SmacConstants.DEFAULT_FORWARD_FLAGS_VALUESET.length; n++) {
            assertEquals(_expForwardFlags[n], SmacConstants.DEFAULT_FORWARD_FLAGS_VALUESET[n]);
        }

        assertEquals("cloud_flags_fward.LAND and not cloud_flags_fward.CLOUDY",
                     SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE);

        for (int n = 0; n < SmacConstants.DEFAULT_NADIR_FLAGS_VALUESET.length; n++) {
            assertEquals(_expNadirFlags[n], SmacConstants.DEFAULT_NADIR_FLAGS_VALUESET[n]);
        }

        assertEquals("cloud_flags_nadir.LAND and not cloud_flags_nadir.CLOUDY",
                     SmacConstants.DEFAULT_NADIR_FLAGS_VALUE);
    }

    public void testFileNameAndPathConstants() {
        assertEquals("smac_out.dim", SmacConstants.DEFAULT_FILE_NAME);
        assertEquals("beam.processor.smac", SmacConstants.LOGGER_NAME);
    }
}
