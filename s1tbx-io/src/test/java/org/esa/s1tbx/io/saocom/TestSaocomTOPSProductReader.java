/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
public class TestSaocomTOPSProductReader extends ReaderTest {

    //GEC
    private final static File TS_GEC_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/TOPSAR/GEC/41363-EOL1CSARSAO1A387058/S1A_OPER_SAR_EOSSP__CORE_L1C_OLF_20191228T014911.xemt");

    //DI
    private final static File TS_DI_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/TOPSAR/DI/42029-EOL1BSARSAO1A390515/S1A_OPER_SAR_EOSSP__CORE_L1B_OLF_20191228T014903.xemt");

    //SLC
    private final static File TS_SLC_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/TOPSAR/SLC/41365-EOL1ASARSAO1A387069/S1A_OPER_SAR_EOSSP__CORE_L1A_OLF_20191228T014855.xemt");


    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(TS_GEC_QP_MetadataFile + " not found", TS_GEC_QP_MetadataFile.exists());

        assumeTrue(TS_DI_QP_MetadataFile + " not found", TS_DI_QP_MetadataFile.exists());

        assumeTrue(TS_SLC_QP_MetadataFile + " not found", TS_SLC_QP_MetadataFile.exists());
    }

    public TestSaocomTOPSProductReader() {
        super(new SaocomProductReaderPlugIn());
    }

    @Test
    public void testReadTS_GEC_QP_Metadata2() throws Exception {
        Product prod = testReader(TS_GEC_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV","Sigma0_VV","Sigma0_VH"});
    }

    @Test
    public void testReadTS_DI_QP_FolderMetadata() throws Exception {
        Product prod = testReader(TS_DI_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV","Sigma0_VV","Sigma0_VH"});
    }

    @Test
    public void testReadTS_SLC_QP_FolderMetadata() throws Exception {
        Product prod = testReader(TS_SLC_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);

    }
}
