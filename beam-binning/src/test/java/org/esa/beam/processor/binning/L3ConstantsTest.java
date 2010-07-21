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
package org.esa.beam.processor.binning;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.util.ObjectUtils;

public class L3ConstantsTest extends TestCase {

    public L3ConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(L3ConstantsTest.class);
    }

    public void testGenericConstants() {
        assertEquals("BINNING", L3Constants.REQUEST_TYPE);
        assertEquals("type", L3Constants.REQUEST_TYPE_PARAM_NAME);
        assertEquals("l3_database.bindb", L3Constants.DEFAULT_DATABASE_NAME);
        assertEquals("db_dir", L3Constants.USER_DB_DIR);
        assertEquals("input_dir", L3Constants.USER_INPUT_DIR);
    }

    /**
     * Tests all constants concerning the output product parameter for correctness
     */
    public void testOutputProductConstants() {
        assertEquals("output_product", L3Constants.OUTPUT_PRODUCT_PARAM_NAME);
    }

    /**
     * Tests all constants concerning the processtype parameter for correctness
     */
    public void testProcessTypeParameterConstants() {
        String[] expValueSet = new String[]{L3Constants.PROCESS_TYPE_INIT,
                                            L3Constants.PROCESS_TYPE_UPDATE,
                                            L3Constants.PROCESS_TYPE_FINALIZE};
        assertEquals("process_type", L3Constants.PROCESS_TYPE_PARAM_NAME);
        assertEquals(true, ObjectUtils.equalObjects(expValueSet, L3Constants.PROCESS_TYPE_VALUE_SET));
        assertEquals("init", L3Constants.PROCESS_TYPE_INIT);
        assertEquals("update", L3Constants.PROCESS_TYPE_UPDATE);
        assertEquals("finalize", L3Constants.PROCESS_TYPE_FINALIZE);
        assertEquals(L3Constants.PROCESS_TYPE_INIT, L3Constants.PROCESS_TYPE_DEFAULT_VALUE);
    }

    /**
     * Tests all constants concerning the database_dir parameter for correctness
     */
    public void testDatabaseDirParameterConstants() {
        assertEquals("database", L3Constants.DATABASE_PARAM_NAME);
    }

    /**
     * Tests all constants concerning the grid_cell_size parameter for correctness
     */
    public void testGridCellSizeParameterConstants() {
        assertEquals("grid_cell_size", L3Constants.GRID_CELL_SIZE_PARAM_NAME);
        assertEquals("km (in sinusoidal grid)", L3Constants.GRID_CELL_SIZE_UNIT);
        assertEquals(9.28f, L3Constants.GRID_CELL_SIZE_DEFAULT.floatValue(), 1e-6);
        assertEquals(0.0001f, L3Constants.GRID_CELL_SIZE_MIN_VALUE.floatValue(), 1e-6);
        assertEquals(BinDatabaseConstants.PI_EARTH_RADIUS, L3Constants.GRID_CELL_SIZE_MAX_VALUE.floatValue(), 1e-6);
    }

    /**
     * Tests all constants concerning the band_names parameter for correctness
     */
    public void testBandNamesParameterConstants() {
        assertEquals("band_name", L3Constants.BAND_NAME_PARAMETER_NAME);
    }

    /**
     * Tests all constants concerning the binning_algorithm parameter for correctness
     */
    public void testBinningAlgorithmParameterConstants() {
        String[] expValueSet = new String[]{"Arithmetic Mean", "Maximum Likelihood", "Minimum/Maximum"};

        assertEquals("binning_algorithm", L3Constants.ALGORITHM_PARAMETER_NAME);
        assertEquals(expValueSet.length, L3Constants.ALGORITHM_VALUE_SET.length);
        for (int i = 0; i < expValueSet.length; i++) {
            assertEquals(expValueSet[i], L3Constants.ALGORITHM_VALUE_SET[i]);
        }
        assertEquals("Arithmetic Mean", L3Constants.ALGORITHM_DEFAULT_VALUE);
    }

    /**
     * Tests all constants concerning the weight_coefficient parameter for correctness
     */
    public void testWeightCoefficientParameterConstants() {
        assertEquals("weight_coefficient", L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME);
        assertEquals(0.5f, L3Constants.WEIGHT_COEFFICIENT_DEFAULT_VALUE.floatValue(), 1e-6);
    }

    public void testBitmaskParameterConstants() {
        assertEquals("bitmask", L3Constants.BITMASK_PARAMETER_NAME);
    }

    public void testDeleteDBParameterConstants() {
        assertEquals("delete_db", L3Constants.DELETE_DB_PARAMETER_NAME);
        assertEquals(new Boolean(false), L3Constants.DELETE_DB_DEFAULT_VALUE);
    }

    public void testLatLonParameterConstants() {
        assertEquals("lat_min", L3Constants.LAT_MIN_PARAMETER_NAME);
        assertEquals(-90.f, L3Constants.LAT_MIN_DEFAULT_VALUE.floatValue(), 1e-6f);

        assertEquals("lat_max", L3Constants.LAT_MAX_PARAMETER_NAME);
        assertEquals(90.f, L3Constants.LAT_MAX_DEFAULT_VALUE.floatValue(), 1e-6f);

        assertEquals("lon_min", L3Constants.LON_MIN_PARAMETER_NAME);
        assertEquals(-180.f, L3Constants.LON_MIN_DEFAULT_VALUE.floatValue(), 1e-6f);

        assertEquals("lon_max", L3Constants.LON_MAX_PARAMETER_NAME);
        assertEquals(180.f, L3Constants.LON_MAX_DEFAULT_VALUE.floatValue(), 1e-6f);

        assertEquals(-90.f, L3Constants.LAT_MINIMUM_VALUE.floatValue(), 1e-6f);
        assertEquals(90.f, L3Constants.LAT_MAXIMUM_VALUE.floatValue(), 1e-6f);
        assertEquals(-180.f, L3Constants.LON_MINIMUM_VALUE.floatValue(), 1e-6f);
        assertEquals(180.f, L3Constants.LON_MAXIMUM_VALUE.floatValue(), 1e-6f);
    }

    public void testLoggerName() {
        assertEquals("beam.processor.binning", L3Constants.LOGGER_NAME);
    }
}
