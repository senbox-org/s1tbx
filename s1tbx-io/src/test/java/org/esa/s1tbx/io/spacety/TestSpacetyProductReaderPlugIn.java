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

import org.esa.s1tbx.io.AbstractProductReaderPlugInTest;
import org.esa.snap.core.dataio.ProductReader;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.esa.s1tbx.io.spacety.TestSpacetyProductReader.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSpacetyProductReaderPlugIn extends AbstractProductReaderPlugInTest {

    public TestSpacetyProductReaderPlugIn() {
        super(new SpacetyProductReaderPlugIn());
    }

    @Test
    public void testCreateReaderInstance() {
        ProductReader productReader = plugin.createReaderInstance();
        assertNotNull(productReader);
        assertTrue(productReader instanceof SpacetyProductReader);
    }

    @Test
    public void testGetFormatNames() {
        assertArrayEquals(new String[]{"Spacety"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() {
        assertArrayEquals(new String[]{"safe", "zip"}, plugin.getDefaultFileExtensions());
    }

    @Test
    public void testDecodeWithS2Data() throws URISyntaxException {
        File dir = new File(getClass().getResource("S2A_MSIL1C_20170719T103021_N0205_R108_T33UUA_20170719T103023.SAFE").toURI());
        File manifestFile = new File(getClass().getResource("S2A_MSIL1C_20170719T103021_N0205_R108_T33UUA_20170719T103023.SAFE/manifest.safe").toURI());

        isInValidDecodeQualification(dir);
        isInValidDecodeQualification(manifestFile);
    }

    @Override
    protected String[] getValidPrimaryMetadataFileNames() {
        return new String[]{"manifest.safe", "manifest.SAFE"};
    }

    @Override
    protected String[] getInvalidPrimaryMetadataFileNames() {
        return new String[]{"Capella_xyz_extended.xml"};
    }

    @Test
    public void testValidDecodeQualification() {
        isValidDecodeQualification(slc_sp1);
        isValidDecodeQualification(slc_sp1_zip);
        isValidDecodeQualification(slc_sm1);
        isValidDecodeQualification(slc_sm1_zip);
        isValidDecodeQualification(slc_sm2);
        isValidDecodeQualification(slc_sm2_zip);
        isValidDecodeQualification(slc_ns1);
        isValidDecodeQualification(slc_ns1_zip);
    }
}
