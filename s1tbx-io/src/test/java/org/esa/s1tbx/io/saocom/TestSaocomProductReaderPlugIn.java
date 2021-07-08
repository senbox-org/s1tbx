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
package org.esa.s1tbx.io.saocom;

import org.esa.s1tbx.io.AbstractProductReaderPlugInTest;
import org.esa.snap.core.dataio.ProductReader;
import org.junit.Test;

import static org.esa.s1tbx.io.saocom.TestSaocomStripmapProductReader.*;
import static org.junit.Assert.*;

public class TestSaocomProductReaderPlugIn extends AbstractProductReaderPlugInTest {

    public TestSaocomProductReaderPlugIn() {
        super(new SaocomProductReaderPlugIn());
    }

    @Test
    public void testCreateReaderInstance() {
        ProductReader productReader = plugin.createReaderInstance();
        assertNotNull(productReader);
        assertTrue(productReader instanceof SaocomProductReader);
    }

    @Test
    public void testGetFormatNames() {
        assertArrayEquals(new String[]{"SAOCOM"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() {
        assertArrayEquals(new String[]{".xemt"}, plugin.getDefaultFileExtensions());
    }

    @Override
    protected String[] getValidPrimaryMetadataFileNames() {
        return new String[] {"S1A_xyz_extended.xemt", "S1B_XYZ_EXTENDED.xemt"};
    }

    @Override
    protected String[] getInvalidPrimaryMetadataFileNames() {
        return new String[] {"S1A_xyz_extended.xml"};
    }

    @Test
    public void testValidDecodeQualification() {
        isValidDecodeQualification(SM_GEC_DP_MetadataFile);
        isValidDecodeQualification(SM_GEC_SP_MetadataFile2);
        isValidDecodeQualification(SM_GEC_SP_Folder);
        isValidDecodeQualification(SM_GEC_QP_MetadataFile);
        isValidDecodeQualification(SM_GTC_QP_MetadataFile);
        isValidDecodeQualification(SM_DI_SP_MetadataFile);
        isValidDecodeQualification(SM_DI_DP_MetadataFile);
        isValidDecodeQualification(SM_DI_QP_MetadataFile);
        isValidDecodeQualification(SM_SLC_SP_MetadataFile);
        isValidDecodeQualification(SM_SLC_DP_MetadataFile);
        isValidDecodeQualification(SM_SLC_QP_MetadataFile);
    }
}
