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
package org.esa.s1tbx.io.cosmo;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestCosmoSkymedReader extends ReaderTest {

    private final static File inputSCS_H5 = new File(TestData.inputSAR + "Cosmo/level1B/hdf5/EL20100624_102783_1129476.6.2/CSKS2_SCS_B_S2_01_VV_RA_SF_20100623045532_20100623045540.h5");
    private final static File inputDGM_H5 = new File(TestData.inputSAR + "Cosmo/level1B/hdf5/EL20141029_928699_3776081.6.2/CSKS4_DGM_B_WR_03_VV_RA_SF_20141001061215_20141001061230.h5");

    private final static String inputCosmo = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR" + S1TBXTests.sep  + "Cosmo" + S1TBXTests.sep ;
    private final static File[] rootPathsCosmoSkymed = S1TBXTests.loadFilePath(inputCosmo);

    private String[] exceptionExemptions = {"not supported"};

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputSCS_H5 + " not found", inputSCS_H5.exists());
        assumeTrue(inputDGM_H5 + " not found", inputDGM_H5.exists());

        for (File file : rootPathsCosmoSkymed) {
            assumeTrue(file + " not found", file.exists());
        }
    }

    public TestCosmoSkymedReader() {
        super(new CosmoSkymedReaderPlugIn());
    }

    @Test
    public void testOpeningSCS_H5() throws Exception {
        Product prod = testReader(inputSCS_H5.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i","q","Intensity"});
    }

    @Test
    public void testOpeningDGM_H5() throws Exception {
        Product prod = testReader(inputDGM_H5.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude","Intensity"});
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsCosmoSkymed, readerPlugIn, reader, null, exceptionExemptions);
    }
}
