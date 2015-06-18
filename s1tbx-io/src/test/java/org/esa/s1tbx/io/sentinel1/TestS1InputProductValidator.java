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
package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.TestData;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

/**
 * Validates input products using commonly used verifications
 */
public class TestS1InputProductValidator {

    @Test
    public void TestSentinel1GRDProduct() throws Exception {
        final File inputFile = TestData.inputS1_GRD;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);
        final InputProductValidator validator = new InputProductValidator(sourceProduct);

        validator.checkIfSentinel1Product();
        validator.checkProductType(new String[]{"GRD"});
        validator.checkIfTOPSARBurstProduct(false);
        validator.checkAcquisitionMode(new String[]{"SM"});
    }

    @Test
    public void TestSentinel1SLCProduct() throws Exception {
        final File inputFile = TestData.inputS1_StripmapSLC;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);
        final InputProductValidator validator = new InputProductValidator(sourceProduct);

        validator.checkIfSentinel1Product();
        validator.checkProductType(new String[]{"SLC"});
        validator.checkIfTOPSARBurstProduct(false);
        validator.checkAcquisitionMode(new String[]{"SM"});
    }
}


