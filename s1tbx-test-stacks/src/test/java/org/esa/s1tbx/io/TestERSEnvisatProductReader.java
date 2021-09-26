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
package org.esa.s1tbx.io;

import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.envisat.EnvisatProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test ERS Envisat Product Reader.
 *
 * @author lveci
 */
public class TestERSEnvisatProductReader extends ReaderTest {

    final static File ers2_envisat_imm_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/ERS/SAR_IMM_1PXESA20110310_110538_00000007A166_00037_83057_1816.E2.zip");
    final static File ers1_envisat_imp = new File(S1TBXTests.inputPathProperty +
            "/SAR/ERS/SAR_IMP_1PXDLR19920517_025823_00000017C084_00218_04371_9963.E1");
    final static File ers2_envisat_imp = new File(S1TBXTests.inputPathProperty +
            "/SAR/ERS/SAR_IMP_1PXDLR19951227_025842_00000016A007_00218_03579_9945.E2");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(ers2_envisat_imm_zip + " not found", ers2_envisat_imm_zip.exists());
        assumeTrue(ers1_envisat_imp + " not found", ers1_envisat_imp.exists());
        assumeTrue(ers2_envisat_imp + " not found", ers2_envisat_imp.exists());
    }

    public TestERSEnvisatProductReader() {
        super(new EnvisatProductReaderPlugIn());
    }

    @Test
    public void testERS2_ENVISAT_IMM_ZIP() throws Exception {
        Product prod = read(ers2_envisat_imm_zip);

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        //validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude","Intensity"});
    }

    @Test
    public void testERS1_E1_IMP() throws Exception {
        Product prod = read(ers1_envisat_imp);

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        //validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude","Intensity"});
    }

    @Test
    public void testERS2_E2_IMP() throws Exception {
        Product prod = read(ers2_envisat_imp);

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        //validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude","Intensity"});
    }

    private Product read(final File file) throws Exception {
        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(file);
        if (canRead != DecodeQualification.INTENDED) {
            throw new Exception("Reader not intended");
        }

        final ProductReader reader = readerPlugIn.createReaderInstance();
        final Product product = reader.readProductNodes(file, null);
        if (product == null) {
            throw new Exception("Unable to read product");
        }
        return product;
    }
}
