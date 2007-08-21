/*
 * $Id: FlhMciConstantsTest.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.framework.param.ParamProperties;

public class FlhMciConstantsTest extends TestCase {

    public FlhMciConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FlhMciConstantsTest.class);
    }

    /**
     * Tests the correctness of all parameter name fields used in the request
     */
    public void testParameterNameFields() {
        assertEquals("FLH_MCI", FlhMciConstants.REQUEST_TYPE);
        assertEquals("type", FlhMciConstants.REQUEST_TYPE_PARAM_NAME);
        assertEquals("output_product", FlhMciConstants.OUTPUT_PRODUCT_PARAM_NAME);
        assertEquals("preset_name", FlhMciConstants.PRESET_PARAM_NAME);
        assertEquals("log_file", FlhMciConstants.LOG_FILE_PARAM_NAME);
        assertEquals("lineheight_band_name", FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME);
        assertEquals("process_slope", FlhMciConstants.PROCESS_SLOPE_PARAM_NAME);
        assertEquals("slope_band_name", FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME);
        assertEquals("Bitmask", FlhMciConstants.BITMASK_PARAM_NAME);

        assertEquals("band_low", FlhMciConstants.BAND_LOW_PARAM_NAME);
        assertEquals("band_signal", FlhMciConstants.BAND_SIGNAL_PARAM_NAME);
        assertEquals("band_high", FlhMciConstants.BAND_HIGH_PARAM_NAME);

        assertEquals("invalid", FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME);

        assertEquals("cloud_correct", FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME);
    }

    /**
     * Tests the correctness of all default path values set.
     */
    public void testDefaultPathValues() {
        assertEquals("flh_mci_out.dim", FlhMciConstants.DEFAULT_FILE_NAME);
    }

    /**
     * Tests the correctness of the default band name parameter values and labels etc ...
     */
    public void testDefaultBandNameValues() {
        assertEquals("", FlhMciConstants.DEFAULT_BAND_VALUEUNIT);

        assertEquals("low_baseline_band", FlhMciConstants.DEFAULT_BAND_LOW);
        assertEquals("signal_band", FlhMciConstants.DEFAULT_BAND_SIGNAL);
        assertEquals("high_baseline_band", FlhMciConstants.DEFAULT_BAND_HIGH);
    }

    /**
     * Tests the correctness of all things concerning the preset parameter
     */
    public void testPresetSet() {
        String[] expPresetSet = {
            "MERIS L2 FLH",
            "MERIS L1b MCI",
            "MERIS L2 MCI",
            "General baseline height"
        };
        assertEquals("preset_name", FlhMciConstants.PRESET_PARAM_NAME);

        for (int n = 0; n < expPresetSet.length; n++) {
            assertEquals(expPresetSet[n], FlhMciConstants.PRESET_PARAM_VALUE_SET[n]);
        }
        assertEquals("", FlhMciConstants.PRESET_PARAM_VALUE_UNIT);
    }

    /**
     * Tests the correctness of the invalid pixel parameter value and labels etc ...
     */
    public void testInvalidPixelStuff() {
        assertEquals(0.f, FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE.floatValue(), 1e-6);
        assertEquals("mW/(m^2 * sr * nm)", FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_VALUEUNIT);
    }

    /**
     * Tests all constants concerning the logging file
     */
    public void testLogFileConstants() {
        assertEquals("flh_mci_log.txt", FlhMciConstants.DEFAULT_LOG_FILE_FILENAME);
        assertEquals(ParamProperties.FSM_FILES_ONLY, FlhMciConstants.DEFAULT_LOG_FILE_FILESELECTIONMODE);
    }

    /**
     * Tests all constants concerning the lineheight band name parameter
     */
    public void testLineHeightBandNameConstants() {
        assertEquals("flh", FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME);
    }

    /**
     * Tests all constants concerning the process slope parameter
     */
    public void testProcessSlopeConstants() {
        assertEquals(true, FlhMciConstants.DEFAULT_PROCESS_SLOPE);
    }

    /**
     * Tests all constants concerning the slope band name parameter
     */
    public void testSlopeBandNameConstants() {
        assertEquals("flh_slope", FlhMciConstants.DEFAULT_SLOPE_BAND_NAME);
    }

    /**
     * Tests all constants concerning the bitmask name parameter
     */
    public void testBitmaskParameterConstants() {
        assertEquals("", FlhMciConstants.DEFAULT_BITMASK);
    }

    /**
     * Tests all constants concerning the cloud correction parameter
     */
    public void testCloudCorrectionParameterConstants() {
        assertEquals(BaselineAlgorithm.DEFAULT_CLOUD_CORRECT,
                     FlhMciConstants.DEFAULT_CLOUD_CORRECTION_FACTOR.floatValue(), 1e-6f);
    }

    /**
     * Tests the logging and error messages for correctness.
     */
    public void testMessages() {
        assertEquals("beam.processor.flh_mci", FlhMciConstants.LOGGER_NAME);
    }
}
