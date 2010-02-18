/*
 * $Id: SstConstantsTest.java,v 1.2 2006/09/11 10:02:02 norman Exp $
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
package org.esa.beam.processor.sst;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SstConstantsTest extends TestCase {

    public SstConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SstConstantsTest.class);
    }

    /**
     * Tests for the correct request type constant
     */
    public void testRequestType() {
        assertEquals("SST", SstConstants.REQUEST_TYPE);
        assertEquals("type", SstConstants.REQUEST_TYPE_PARAM_NAME);
    }

    /**
     * Tests for correct constants for the parameter process dual view sst
     */
    public void testProcessDualViewSst() {
        assertEquals("process_dual_view_sst", SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        assertEquals(Boolean.TRUE, SstConstants.DEFAULT_PROCESS_DUAL_VIEW_SST);
    }

    /**
     * Test for correct constants for parameter dual view coefficient file
     */
    public void testDualViewCoefficientFile() {
        assertEquals("dual_view_coeff_file", SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME);
        assertEquals("test coefficients dual view 1", SstConstants.DEFAULT_DUAL_VIEW_COEFF_FILE);
    }

    /**
     * Tests for correct constants for the dual view bitmask expression
     */
    public void testDualViewBitmask() {
        assertEquals("dual_view_bitmask", SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
        assertEquals(
                "!cloud_flags_nadir.LAND & !cloud_flags_nadir.CLOUDY & !cloud_flags_nadir.SUN_GLINT & !cloud_flags_fward.LAND & !cloud_flags_fward.CLOUDY & !cloud_flags_fward.SUN_GLINT",
                SstConstants.DEFAULT_DUAL_VIEW_BITMASK);
    }

    /**
     * Tests for correct constants for the parameter process nadir view sst
     */
    public void testProcessNadirViewSst() {
        assertEquals("process_nadir_view_sst", SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        assertEquals(Boolean.TRUE, SstConstants.DEFAULT_PROCESS_NADIR_VIEW_SST);
    }

    /**
     * Test for correct constants for parameter nadir view coefficient file
     */
    public void testNadirViewCoefficientFile() {
        assertEquals("nadir_view_coeff_file", SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME);
        assertEquals("test coefficients nadir 1", SstConstants.DEFAULT_NADIR_VIEW_COEFF_FILE);
    }

    /**
     * Tests for correct constants for the nadir view bitmask expression
     */
    public void testNadirViewBitmask() {
        assertEquals("nadir_view_bitmask", SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
        assertEquals("!cloud_flags_nadir.LAND & !cloud_flags_nadir.CLOUDY & !cloud_flags_nadir.SUN_GLINT",
                     SstConstants.DEFAULT_NADIR_VIEW_BITMASK);
    }

    /**
     * Tests for correct constants concerning the invalid pixel value handling
     */
    public void testInvalidPixelConstants() {
        assertEquals("invalid", SstConstants.INVALID_PIXEL_PARAM_NAME);
        assertEquals(new Float(-999f), SstConstants.DEFAULT_INVALID_PIXEL);
    }

    /**
     * Tests for correct constants concerning the output product parameter
     */
    public void testOutputProductConstants() {
        assertEquals("output_product", SstConstants.OUTPUT_PRODUCT_PARAM_NAME);
    }

    /**
     * Tests the correct logging file constants
     */
    public void testLogFileConstants() {
        assertEquals("log_file", SstConstants.LOG_FILE_PARAM_NAME);
        assertEquals("sst_log.txt", SstConstants.DEFAULT_LOG_FILE_FILENAME);
    }

    /**
     * Tests for correct band name constants
     */
    public void testBandNameConstants() {
        assertEquals("btemp_nadir_0370", SstConstants.NADIR_370_BAND);
        assertEquals("btemp_nadir_1100", SstConstants.NADIR_1100_BAND);
        assertEquals("btemp_nadir_1200", SstConstants.NADIR_1200_BAND);
        assertEquals("btemp_fward_0370", SstConstants.FORWARD_370_BAND);
        assertEquals("btemp_fward_1100", SstConstants.FORWARD_1100_BAND);
        assertEquals("btemp_fward_1200", SstConstants.FORWARD_1200_BAND);
        assertEquals("sun_elev_nadir", SstConstants.SUN_ELEV_NADIR);
        assertEquals("sun_elev_fward", SstConstants.SUN_ELEV_FORWARD);
    }

    /**
     * Tests whether the auxdata relative paths are correctly set
     */
    public void testAuxdataRelativePaths() {
        assertEquals("nadir_view", SstConstants.AUXPATH_NADIR_VIEW);
        assertEquals("dual_view", SstConstants.AUXPATH_DUAL_VIEW);
    }

    /**
     * Tests default file name and default path
     */
    public void testDefaultPathAndFile() {
        assertEquals("sst_out.dim", SstConstants.DEFAULT_FILE_NAME);
    }
}
