/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.spacety;

import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestSpacetyProductReader extends ReaderTest {

    final static File slc_sp1 = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SP_SLC_1SSV_20210405T201755_000480_0001E0");
    final static File slc_sp1_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SP_SLC_1SSV_20210405T201755_000480_0001E0.zip");

    final static File slc_sm1 = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SM_SLC_1SSV_20210324T214340_000394_00018A");
    final static File slc_sm1_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SM_SLC_1SSV_20210324T214340_000394_00018A.zip");
    final static File slc_sm2 = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SM_SLC_1SSV_20210327T145357_000411_00019B");
    final static File slc_sm2_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SM_SLC_1SSV_20210327T145357_000411_00019B.zip");

    final static File l2_sm_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/Level2/BC1_SM_ORG_2SSV_20210606T100859_000963_0003C3.zip");
    final static File l2_sp_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/Level2/BC1_SP_ORG_2SSV_20210521T082626_000804_000324.zip");

    final static File slc_ns1 = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_NS_SLC_1SSV_20210326T012419_000395_00018B");
    final static File slc_ns1_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_NS_SLC_1SSV_20210326T012419_000395_00018B.zip");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(slc_sp1 + " not found", slc_sp1.exists());
        assumeTrue(slc_sp1_zip + " not found", slc_sp1_zip.exists());
        assumeTrue(slc_sm1 + " not found", slc_sm1.exists());
        assumeTrue(slc_sm2 + " not found", slc_sm2.exists());

        assumeTrue(l2_sm_zip + " not found", l2_sm_zip.exists());
        assumeTrue(l2_sp_zip + " not found", l2_sp_zip.exists());

        assumeTrue(slc_ns1 + " not found", slc_ns1.exists());
        assumeTrue(slc_ns1_zip + " not found", slc_ns1_zip.exists());
    }

    public TestSpacetyProductReader() {
        super(new SpacetyProductReaderPlugIn());
    }

    // SP

    @Test
    public void testOpeningMetadataFile_SP1() throws Exception {
        Product prod = testReader(slc_sp1.toPath().resolve("manifest.safe"));

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningFolder_SP1() throws Exception {
        Product prod = testReader(slc_sp1.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningZip_SP1() throws Exception {
        Product prod = testReader(slc_sp1_zip.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    // SM

    @Test
    public void testOpeningMetadataFile_SM1() throws Exception {
        Product prod = testReader(slc_sm1.toPath().resolve("manifest.safe"));

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningFolder_SM1() throws Exception {
        Product prod = testReader(slc_sm1.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningZip_SM1() throws Exception {
        Product prod = testReader(slc_sm1_zip.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningMetadataFile_SM2() throws Exception {
        Product prod = testReader(slc_sm2.toPath().resolve("manifest.safe"));

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningFolder_SM2() throws Exception {
        Product prod = testReader(slc_sm2.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningZip_SM2() throws Exception {
        Product prod = testReader(slc_sm2_zip.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    // NS

    @Test
    public void testOpeningMetadataFile_NS1() throws Exception {
        Product prod = testReader(slc_ns1.toPath().resolve("manifest.safe"));

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV_NS1","q_VV_NS1","Intensity_VV_NS1","i_VV_NS2","q_VV_NS2","Intensity_VV_NS2","i_VV_NS3","q_VV_NS3","Intensity_VV_NS3","i_VV_NS4","q_VV_NS4","Intensity_VV_NS4"});
    }

    @Test
    public void testOpeningFolder_NS1() throws Exception {
        Product prod = testReader(slc_ns1.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV_NS1","q_VV_NS1","Intensity_VV_NS1","i_VV_NS2","q_VV_NS2","Intensity_VV_NS2","i_VV_NS3","q_VV_NS3","Intensity_VV_NS3","i_VV_NS4","q_VV_NS4","Intensity_VV_NS4"});
    }

    @Test
    public void testOpeningZip_NS1() throws Exception {
        Product prod = testReader(slc_ns1_zip.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"i_VV_NS1","q_VV_NS1","Intensity_VV_NS1","i_VV_NS2","q_VV_NS2","Intensity_VV_NS2","i_VV_NS3","q_VV_NS3","Intensity_VV_NS3","i_VV_NS4","q_VV_NS4","Intensity_VV_NS4"});
    }

    // level 2

    @Test
    @Ignore("no geocoding")
    public void testOpeningL2_SM() throws Exception {
        Product prod = testReader(l2_sm_zip.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude_VV","Intensity_VV"});
    }

    @Test
    @Ignore("no geocoding")
    public void testOpeningL2_SP() throws Exception {
        Product prod = testReader(l2_sp_zip.toPath());

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateMetadata();
        validator.validateBands(new String[] {"Amplitude_VV","Intensity_VV"});
    }
}
