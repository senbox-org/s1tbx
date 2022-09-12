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
package com.iceye.esa.snap.dataio;

import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;


/**
 * @author Ahmad Hamouda
 */
public class TestIceyeReader extends ReaderTest {

    public static final String TESTING_IMAGE_PATH = "/home/ahmad/Documents/Projects/snap/test";

    private final static String inputIceyeFolder = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR" + S1TBXTests.sep  + "Iceye" + S1TBXTests.sep ;
    private final static File[] iceyeSLCFiles = S1TBXTests.loadFilePath(inputIceyeFolder + "SLC");
    private final static File[] iceyeGRDFiles = S1TBXTests.loadFilePath(inputIceyeFolder + "GRD");

    private final static File SL_GRD_ImageFile = new File(S1TBXTests.inputSAR + "Iceye/SLC/ICEYE_SLC_GRD_Example_Spotlight_SAR_Imagery/ICEYE_GRD_Data_Jurong_Island_Singapore_SL_092019/ICEYE_GRD_SL_10402_20190920T075151.tif");
    private final static File SL_SLC_ImageFile = new File(S1TBXTests.inputSAR + "Iceye/SLC/ICEYE_SLC_GRD_Example_Spotlight_SAR_Imagery/ICEYE_SLC_Data_Jurong_Island_Singapore_SL_092019/ICEYE_SLC_SL_10402_20190920T075151.h5");

    private final static File SL_GRD_MetadataFile = new File(S1TBXTests.inputSAR + "Iceye/SLC/ICEYE_SLC_GRD_Example_Spotlight_SAR_Imagery/ICEYE_GRD_Data_Jurong_Island_Singapore_SL_092019/ICEYE_GRD_SL_10402_20190920T075151.xml");
    private final static File SL_SLC_MetadataFile = new File(S1TBXTests.inputSAR + "Iceye/SLC/ICEYE_SLC_GRD_Example_Spotlight_SAR_Imagery/ICEYE_SLC_Data_Jurong_Island_Singapore_SL_092019/ICEYE_SLC_SL_10402_20190920T075151.xml");

    private String[] exceptionExemptions = {"not supported"};

    public TestIceyeReader() {
        super(new IceyeProductReaderPlugIn());
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() {
        TestProcessor testProcessor = new TestProcessor(100, 100, 100, 100, 1, true, false);

        File file = new File(TESTING_IMAGE_PATH);
        File[] folderPaths = new File[1];
        folderPaths[0] = file;
        try {
            testProcessor.recurseReadFolder(this, folderPaths, readerPlugIn, reader, null, exceptionExemptions);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testTestServerOpenAllSLC() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, iceyeSLCFiles, readerPlugIn, null, null, null);
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testTestServerOpenAllGRD() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, iceyeGRDFiles, readerPlugIn, null, null, null);
    }

    @Test
    public void testReadSL_meta_GRD() throws Exception {
        if(SL_GRD_MetadataFile.exists()) {
            Product prod = testReader(SL_GRD_MetadataFile.toPath());

            final ProductValidator validator = new ProductValidator(prod);
            validator.validateProduct();
            validator.validateMetadata();
            validator.validateBands(new String[]{"Amplitude_VV", "Intensity_VV"});
        }
    }

    @Test
    public void testReadSL_meta_SLC() throws Exception {
        if(SL_SLC_MetadataFile.exists()) {
            Product prod = testReader(SL_SLC_MetadataFile.toPath());

            final ProductValidator validator = new ProductValidator(prod);
            validator.validateProduct();
            validator.validateMetadata();
            validator.validateBands(new String[]{"i_VV", "q_VV", "Intensity_VV"});
        }
    }

    @Test
    public void testReadSL_GRD() throws Exception {
        if(SL_GRD_ImageFile.exists()) {
            Product prod = testReader(SL_GRD_ImageFile.toPath());

            final ProductValidator validator = new ProductValidator(prod);
            validator.validateProduct();
            validator.validateMetadata();
            validator.validateBands(new String[]{"Amplitude_VV", "Intensity_VV"});
        }
    }

    @Test
    public void testReadSL_SLC() throws Exception {
        if(SL_SLC_ImageFile.exists()) {
            Product prod = testReader(SL_SLC_ImageFile.toPath());

            final ProductValidator validator = new ProductValidator(prod);
            validator.validateProduct();
            validator.validateMetadata();
            validator.validateBands(new String[]{"i_VV", "q_VV", "Intensity_VV"});
        }
    }
}
