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

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

/**
 * Validates input products using commonly used verifications
 */
public class TestS1SLCInputProductValidator extends ReaderTest {

    public TestS1SLCInputProductValidator() {
        super(new DimapProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(TestData.inputS1_StripmapSLC + " not found", TestData.inputS1_StripmapSLC.exists());
    }

    @Test
    public void TestSentinel1SLCProduct() throws Exception {
        final Product sourceProduct = ProductIO.readProduct(TestData.inputS1_StripmapSLC);
        if(sourceProduct != null) {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);

            validator.checkIfSentinel1Product();
            validator.checkProductType(new String[]{"SLC"});
            validator.checkIfTOPSARBurstProduct(false);
            validator.checkAcquisitionMode(new String[]{"SM"});
        }
    }
}


