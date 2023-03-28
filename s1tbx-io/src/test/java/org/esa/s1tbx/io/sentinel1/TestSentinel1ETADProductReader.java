/*
 * Copyright (C) 2023 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Validates input products using commonly used verifications
 */
public class TestSentinel1ETADProductReader extends ReaderTest {

    public final static File inputS1ETAD_IW = new File(TestData.inputSAR+"S1_ETAD/ETAD/IW-Philippines/S1A_IW_ETA__AXSV_20200124T095712_20200124T095837_030941_038D5C_270C.SAFE/manifest.safe");
    public final static File inputS1ETAD_SM = new File(TestData.inputSAR+"S1_ETAD/ETAD/SM-Nigeria/S1A_S6_ETA__AXDV_20190810T044708_20190810T044822_028502_0338D0_985E.SAFE/manifest.safe");
    public final static File inputS1ETAD_SM_ZIP = new File(TestData.inputSAR+"S1_ETAD/ETAD/S1A_S6_ETA__AXDV_20190810T044708_20190810T044822_028502_0338D0_985E.SAFE.zip");

    public TestSentinel1ETADProductReader() {
        super(new Sentinel1ETADProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputS1ETAD_IW + " not found", inputS1ETAD_IW.exists());
        assumeTrue(inputS1ETAD_SM + " not found", inputS1ETAD_SM.exists());
        assumeTrue(inputS1ETAD_SM_ZIP + " not found", inputS1ETAD_SM_ZIP.exists());
    }

    @Test
    public void TestS1ETAD_SM() throws Exception {
        final Product sourceProduct = testReader(inputS1ETAD_SM.toPath());
        if(sourceProduct != null) {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);

            validator.checkIfSentinel1Product();
            validator.checkProductType(new String[]{"ETAD"});
            validator.checkIfTOPSARBurstProduct(false);
            validator.checkAcquisitionMode(new String[]{"S6"});
        }
    }

    @Test
    public void TestS1ETAD_SM_ZIP() throws Exception {
        final Product sourceProduct = testReader(inputS1ETAD_SM_ZIP.toPath());
        if(sourceProduct != null) {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);

            validator.checkIfSentinel1Product();
            validator.checkProductType(new String[]{"ETAD"});
            validator.checkIfTOPSARBurstProduct(false);
            validator.checkAcquisitionMode(new String[]{"S6"});
        }
    }

    @Test
    public void TestS1ETAD_IW() throws Exception {
        final Product sourceProduct = testReader(inputS1ETAD_IW.toPath());
        if(sourceProduct != null) {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);

            validator.checkIfSentinel1Product();
            validator.checkProductType(new String[]{"ETAD"});
            validator.checkIfTOPSARBurstProduct(false);
            validator.checkAcquisitionMode(new String[]{"IW"});
        }
    }
}


