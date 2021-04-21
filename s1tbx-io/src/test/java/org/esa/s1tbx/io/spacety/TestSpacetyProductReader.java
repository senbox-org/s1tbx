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

    private final static File slc_metadata_earlyProduct = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SP_SLC__1SVV_20210301T082613_20210301T082620_000000_000210_4597.SAFE");
    private final static File slc_zip_earlyProduct = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SP_SLC__1SVV_20210301T095938_20210301T095944_000000_000211_38C2.SAFE.zip");

    private final static File slc_metadata = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SP_SLC__1SVV_20210324T042344_20210324T042351_000038_000176_1F3A.SAFE");
    private final static File slc_zip = new File(S1TBXTests.inputPathProperty +
            "/SAR/Spacety/SLC/BC1_SP_SLC__1SVV_20210309T040153_20210309T040159_000038_000107_F895.SAFE.zip");

    final static ProductValidator.ValidationOptions productOptions = new ProductValidator.ValidationOptions();

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(slc_metadata_earlyProduct + " not found", slc_metadata_earlyProduct.exists());
        assumeTrue(slc_zip_earlyProduct + " not found", slc_zip_earlyProduct.exists());

        productOptions.verifyBands = false;
    }

    public TestSpacetyProductReader() {
        super(new SpacetyProductReaderPlugIn());
    }

    @Test
    @Ignore
    public void testOpeningMetadataFile_earlyProduct() throws Exception {
        Product prod = testReader(slc_metadata_earlyProduct.toPath().resolve("manifest.safe"));
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    @Ignore
    public void testOpeningFolder_earlyProduct() throws Exception {
        Product prod = testReader(slc_metadata_earlyProduct.toPath());
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    @Ignore
    public void testOpeningZip_earlyProduct() throws Exception {
        Product prod = testReader(slc_zip_earlyProduct.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningMetadataFile() throws Exception {
        Product prod = testReader(slc_metadata.toPath().resolve("manifest.safe"));
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningFolder() throws Exception {
        Product prod = testReader(slc_metadata.toPath());
        validateProduct(prod, productOptions);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testOpeningZip() throws Exception {
        Product prod = testReader(slc_zip.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }
}
