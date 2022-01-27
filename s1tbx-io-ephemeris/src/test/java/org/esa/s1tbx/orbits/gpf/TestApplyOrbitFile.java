/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.orbits.gpf;

import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.envisat.EnvisatOrbitReader;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Sep 4, 2008
 * To change this template use File | Settings | File Templates.
 */
public class TestApplyOrbitFile {

    @Before
    public void setUp() {
        TestUtils.initTestEnvironment();
        // If any of the file does not exist: the test will be ignored
        assumeTrue(TestData.inputS1_GRDSubset + " not found", TestData.inputS1_GRDSubset.exists());
    }

    @Test
    public void testOpenFile() {

        EnvisatOrbitReader reader = new EnvisatOrbitReader();
        assertNotNull(reader);
    }

    @Test
    public void testApplyOrbit_S1() throws Exception {
        final Product srcProduct = ProductIO.readProduct(TestData.inputS1_GRDSubset);

        ApplyOrbitFileOp op = new ApplyOrbitFileOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();
    }

    @Test
    public void testApplyOrbit_ASAR_WSM() throws Exception {
        final Product srcProduct = ProductIO.readProduct(TestData.inputASAR_WSM);

        ApplyOrbitFileOp op = new ApplyOrbitFileOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();
    }

    @Test
    public void testApplyOrbit_ASAR_IMS() throws Exception {
        final Product srcProduct = ProductIO.readProduct(TestData.inputASAR_IMS);

        ApplyOrbitFileOp op = new ApplyOrbitFileOp();
        op.setSourceProduct(srcProduct);
        Product trgProduct = op.getTargetProduct();
    }

    @Test
    public void testApplyOrbit_ERS1() throws Exception {
        try {
            // http://step.esa.int/auxdata/orbits/ers_precise_orb/ERS1/1997.zip does not exist

            final Product srcProduct = ProductIO.readProduct(TestData.inputERS_IMP);

            ApplyOrbitFileOp op = new ApplyOrbitFileOp();
            op.setSourceProduct(srcProduct);
            Product trgProduct = op.getTargetProduct();
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("Unable to find suitable orbit file"));
        }
    }
}
