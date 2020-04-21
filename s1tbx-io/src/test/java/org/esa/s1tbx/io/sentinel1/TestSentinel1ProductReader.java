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

import org.esa.s1tbx.commons.test.*;
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
public class TestSentinel1ProductReader extends ReaderTest {

    private final static File inputS1_AnnotGRD = new File(TestData.inputSAR+"S1"+File.separator+"S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.SAFE");
    private final static File inputS1_AnnotGRDZip = new File(TestData.inputSAR+"S1"+File.separator+"S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.zip");
    private final static File inputS1_meta1GRD = new File(TestData.inputSAR+"S1"+File.separator+"bandless1"+File.separator+"manifest.safe");
    private final static File inputS1_meta2GRD = new File(TestData.inputSAR+"S1"+File.separator+"bandless2"+File.separator+"manifest.safe");

    private final static File inputGRDFolder = new File(TestData.inputSAR + File.separator + "S1" + File.separator + "AWS" + File.separator + "S1A_IW_GRDH_1SDV_20180719T002854_20180719T002919_022856_027A78_042A");

    private String[] productTypeExemptions = {"RAW","OCN"};

    private final static String inputS1 = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR" + S1TBXTests.sep  + "S1" + S1TBXTests.sep ;
    private final static File[] rootPathsSentinel1 = S1TBXTests.loadFilePath(inputS1);

    private final static File inputS1_GRDFile = TestData.inputS1_GRD;

    final static ProductValidator.ValidationOptions productOptions = new ProductValidator.ValidationOptions();

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputS1_GRDFile + " not found", inputS1_GRDFile.exists());
        assumeTrue(inputS1_AnnotGRD + " not found", inputS1_AnnotGRD.exists());
        assumeTrue(inputS1_meta1GRD + " not found", inputS1_meta1GRD.exists());
        assumeTrue(inputS1_meta2GRD + " not found", inputS1_meta2GRD.exists());
        assumeTrue(inputS1_AnnotGRDZip + " not found", inputS1_AnnotGRDZip.exists());
        assumeTrue(inputGRDFolder + " not found", inputGRDFolder.exists());

        for (File file : rootPathsSentinel1) {
            assumeTrue(file + " not found", file.exists());
        }

        productOptions.verifyBands = false;
    }

    public TestSentinel1ProductReader() {
        super(new Sentinel1ProductReaderPlugIn());
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsSentinel1, readerPlugIn, reader, productTypeExemptions, null);
    }

    @Test
    public void testOpeningFile() throws Exception {
        Product prod = testReader(inputS1_AnnotGRD.toPath().resolve("manifest.safe"));
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {});
    }

    @Test
    public void testOpeningBandlessMetadataFile1() throws Exception {
        Product prod = testReader(inputS1_meta1GRD.toPath());
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {});
    }

    @Test
    public void testOpeningBandlessMetadataFile2() throws Exception {
        Product prod = testReader(inputS1_meta2GRD.toPath());
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {});
    }

    @Test
    public void testOpeningAnnotFolder() throws Exception {
        Product prod = testReader(inputS1_AnnotGRD.toPath());
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {});
    }

    @Test
    public void testOpeningZip() throws Exception {
        Product prod = testReader(inputS1_GRDFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_VV","Intensity_VV","Amplitude_VH","Intensity_VH"});
    }

    @Test
    public void testOpeningAnnotationProduct() throws Exception {
        Product prod = testReader(inputS1_AnnotGRDZip.toPath());
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {});
    }

    @Test
    public void testOpeningFolder() throws Exception {
        Product prod = testReader(inputGRDFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_VV","Intensity_VV","Amplitude_VH","Intensity_VH"});
    }
}
