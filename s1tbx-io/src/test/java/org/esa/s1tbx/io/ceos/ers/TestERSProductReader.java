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
package org.esa.s1tbx.io.ceos.ers;

import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test ERS CEOS Product Reader.
 *
 * @author lveci
 */
public class TestERSProductReader extends ReaderTest {

    final static File ers1_ceos_vmp_slc = new File(S1TBXTests.inputPathProperty +
            "/SAR/ERS/ERS_Tandem_Etna/ERS1_SLCI_VMP_CEOS_01081995_orbit 21159 frame 0747_IPAF/SCENE1/VDF_DAT.001");
    final static File ers2_ceos_pri = new File(S1TBXTests.inputPathProperty +
            "/SAR/ERS/ERS2.SAR.PRI_29JAN1996_orbit04055_frame2714/SCENE1/VDF_DAT.001");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(ers1_ceos_vmp_slc + " not found", ers1_ceos_vmp_slc.exists());
        assumeTrue(ers2_ceos_pri + " not found", ers2_ceos_pri.exists());
    }

    public TestERSProductReader() {
        super(new ERSProductReaderPlugIn());
    }

    @Test
    public void testERS1_CEOS_VMP_SLC() throws Exception {
        Product prod = testReader(ers1_ceos_vmp_slc.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i","q","Intensity"});
    }

    @Test
    public void testERS2_CEOS_PRI() throws Exception {
        Product prod = testReader(ers2_ceos_pri.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude","Intensity"});
    }
}
