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
package org.esa.s1tbx.io.terrasarx;

import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
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
public class TestTerraSarXProductReader extends ReaderTest {

    private final static File mgdMetadataFile = new File(S1TBXTests.inputPathProperty + S1TBXTests.sep +"SAR/TerraSAR-X/Oslo_StaringSpotlight/TSX1_SAR__MGD_SE___ST_S_SRA_20130410T165445_20130410T165446/TSX1_SAR__MGD_SE___ST_S_SRA_20130410T165445_20130410T165446.xml");
    private final static File sscMetadataFile = new File(S1TBXTests.inputPathProperty + S1TBXTests.sep +"SAR/TerraSAR-X/Sendai_D_Orbit042_20101020/dims_op_oc_dfd2_338933326_1/TSX-1.SAR.L1B/TSX1_SAR__SSC______SM_S_SRA_20101020T204312_20101020T204320\\TSX1_SAR__SSC______SM_S_SRA_20101020T204312_20101020T204320.xml");

    private final static String inputTerraSarX = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR/TerraSAR-X/";
    private final static File[] rootPathsTerraSarX = S1TBXTests.loadFilePath(inputTerraSarX);

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        for (File file : rootPathsTerraSarX) {
            assumeTrue(file + " not found", file.exists());
        }
        assumeTrue(mgdMetadataFile + " not found", mgdMetadataFile.exists());
        assumeTrue(sscMetadataFile + " not found", sscMetadataFile.exists());
    }

    public TestTerraSarXProductReader() {
        super(new TerraSarXProductReaderPlugIn());
    }

    @Test
    public void testOpenMGDMetadata() throws Exception {
        Product prod = testReader(mgdMetadataFile.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude_HH","Intensity_HH"});
    }

    @Test
    public void testOpenSSCMetadata() throws Exception {
        Product prod = testReader(sscMetadataFile.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_HH","q_HH","Intensity_HH"});
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsTerraSarX, readerPlugIn, reader, null, null);
    }
}
