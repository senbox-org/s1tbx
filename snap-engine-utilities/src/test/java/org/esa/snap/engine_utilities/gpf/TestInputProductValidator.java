/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Validates input products using commonly used verifications
 */
public class TestInputProductValidator {

    public final static String sep = File.separator;
    public final static String rootPathTestProducts = SystemUtils.getApplicationHomeDir()+sep+".."+sep+".."+sep+".."+sep+".."+sep+"testdata";
    public final static String inputSAR = rootPathTestProducts + sep + "input" + sep + "SAR" + sep;
    public final static File inputASAR_WSM = new File(inputSAR + "ASAR" + sep + "subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");

    @Test
    public void TestNotSentinel1Product() throws Exception {
        final File inputFile = inputASAR_WSM;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }

        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);
        final InputProductValidator validator = new InputProductValidator(sourceProduct);

        try {
            validator.checkIfSentinel1Product();
        } catch (OperatorException e) {
            assertEquals(e.getMessage(), "Input should be a Sentinel-1 product.");
        }
        try {
            validator.checkProductType(new String[]{"GRD"});
        } catch (OperatorException e) {
            assertTrue(e.getMessage().contains("is not a valid product type"));
        }
        try {
            validator.checkIfMapProjected(false);
        } catch (OperatorException e) {
            assertEquals(e.getMessage(), "Source product should not be map projected");
        }
        try {
            validator.checkIfTOPSARBurstProduct(true);
        } catch (OperatorException e) {
            assertEquals(e.getMessage(), "Source product should be an SLC burst product");
        }
        try {
            validator.checkAcquisitionMode(new String[]{"IW", "EW"});
        } catch (OperatorException e) {
            assertTrue(e.getMessage().contains("is not a valid acquisition mode"));
        }
    }
}


