/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class BinningOpRobustnessTest {

    @Before
    public void setUp() throws Exception {
        BinningOpTest.TESTDATA_DIR.mkdir();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteTree(BinningOpTest.TESTDATA_DIR);
    }


    @Test
    public void testNoSourceProductSet() throws Exception {
        final BinningOp binningOp = new BinningOp();
        testThatOperatorExceptionIsThrown(binningOp, ".*or parameter 'sourceProductPaths' must be.*");
    }

    @Test
    public void testNumRowsNotSet() throws Exception {
        final BinningOp binningOp = new BinningOp();
        binningOp.setSourceProduct(BinningOpTest.createSourceProduct(1, 0.3f));
        // not ok, numRows == 0
        testThatOperatorExceptionIsThrown(binningOp, ".*parameter 'numRows'.*");
    }

    @Test
    public void testBinningConfigNotSet() throws Exception {
        final BinningOp binningOp = new BinningOp();
        binningOp.setSourceProduct(BinningOpTest.createSourceProduct(1, 0.3f));
        binningOp.setNumRows(2);
        testThatOperatorExceptionIsThrown(binningOp, "No aggregators have been defined");
    }

//    @Test
//    public void testNoStartDateSet() throws Exception {
//        final BinningOp binningOp = new BinningOp();
//        binningOp.setSourceProduct(BinningOpTest.createSourceProduct());
//        binningOp.setBinningConfig(BinningOpTest.createBinningConfig());
//        binningOp.setFormatterConfig(BinningOpTest.createFormatterConfig());
//        testThatOperatorExceptionIsThrown(binningOp, ".*determine 'startDate'.*");
//    }

//    @Test
//    public void testNoEndDateSet() throws Exception {
//        final BinningOp binningOp = new BinningOp();
//        binningOp.setSourceProduct(BinningOpTest.createSourceProduct());
//        binningOp.setStartDate("2007-06-21");
//        binningOp.setBinningConfig(BinningOpTest.createBinningConfig());
//        binningOp.setFormatterConfig(BinningOpTest.createFormatterConfig());
//        testThatOperatorExceptionIsThrown(binningOp, ".*determine 'endDate'.*");
//    }

    private void testThatOperatorExceptionIsThrown(BinningOp binningOp, String regex) {
        String message = "OperatorException expected with message regex: " + regex;
        try {
            binningOp.getTargetProduct();
            fail(message);
        } catch (OperatorException e) {
            System.out.println("e = " + e);
            assertTrue(message + ", got [" + e.getMessage() + "]", Pattern.matches(regex, e.getMessage()));
        }
    }

}
