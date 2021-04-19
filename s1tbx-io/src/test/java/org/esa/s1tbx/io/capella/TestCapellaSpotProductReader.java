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
package org.esa.s1tbx.io.capella;

import org.esa.s1tbx.commons.test.MetadataValidator;
import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestCapellaSpotProductReader extends ReaderTest {

    private final static File inputSLCMeta = new File(S1TBXTests.inputPathProperty + "/SAR/Capella/Spot/SLC/CAPELLA_C02_SP_SLC_HH_20201209213329_20201209213332.json");
    private final static File inputSLCTif = new File(S1TBXTests.inputPathProperty + "/SAR/Capella/Spot/SLC/CAPELLA_C02_SP_SLC_HH_20201209213329_20201209213332.tif");
    private final static File inputSLCFolder = new File(S1TBXTests.inputPathProperty + "/SAR/Capella/Spot/SLC");

    final static MetadataValidator.ValidationOptions options = new MetadataValidator.ValidationOptions();

    public TestCapellaSpotProductReader() {
        super(new CapellaProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputSLCMeta + " not found", inputSLCMeta.exists());
        assumeTrue(inputSLCFolder + " not found", inputSLCFolder.exists());

        options.validateSRGR = false;
        options.validateDopplerCentroids = false;
    }

    @Test
    public void testOpeningSLCFolder() throws Exception {
        Product prod = testReader(inputSLCFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod, options);
        validateBands(prod, new String[] {"i_HH","q_HH","Intensity_HH"});
    }

    @Test
    public void testOpeningSLCMetadata() throws Exception {
        Product prod = testReader(inputSLCMeta.toPath());
        validateProduct(prod);
        validateMetadata(prod, options);
        validateBands(prod, new String[] {"i_HH","q_HH","Intensity_HH"});
    }

    @Test
    public void testOpeningSLCTif() throws Exception {
        Product prod = testReader(inputSLCTif.toPath());
        validateProduct(prod);
        validateMetadata(prod, options);
        validateBands(prod, new String[] {"i_HH","q_HH","Intensity_HH"});
    }
}
