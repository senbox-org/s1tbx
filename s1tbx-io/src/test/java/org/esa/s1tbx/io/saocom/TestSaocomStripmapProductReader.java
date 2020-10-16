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
public class TestSaocomStripmapProductReader extends ReaderTest {

    // GEC
    private final static File SM_GEC_DP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/GEC/DualPol/10793-EOL1CSARSAO1A185028/S1A_OPER_SAR_EOSSP__CORE_L1C_OLVF_20190801T145831.xemt");
    private final static File SM_GEC_SP_MetadataFile2 = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/GEC/SinglePol/11245-EOL1CSARSAO1A198523/S1A_OPER_SAR_EOSSP__CORE_L1C_OLVF_20190814T135710.xemt");
    private final static File SM_GEC_SP_Folder = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/GEC/SinglePol/32895-EOL1CSARSAO1A329562/S1A_OPER_SAR_EOSSP__CORE_L1C_OLF_20191120T234028.xemt");
    private final static File SM_GEC_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/GEC/QuadPol/11248-EOL1CSARSAO1A198533/S1A_OPER_SAR_EOSSP__CORE_L1C_OLVF_20190814T135731.xemt");

    //GTC
    private final static File SM_GTC_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/GTC/QuadPol/EOL1DSARSAO1A198541/11250-EOL1DSARSAO1A198541/S1A_OPER_SAR_EOSSP__CORE_L1D_OLVF_20190814T135724.xemt");

    //DI
    private final static File SM_DI_SP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/DI/SinglePol/32897-EOL1BSARSAO1A329557/S1A_OPER_SAR_EOSSP__CORE_L1B_OLF_20191120T233614.xemt");
    private final static File SM_DI_DP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/DI/DualPol/36579-EOL1BSARSAO1A339522/S1A_OPER_SAR_EOSSP__CORE_L1B_OLVF_20191128T135208.xemt");
    private final static File SM_DI_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/DI/QuadPol/45116-EOL1BSARSAO1A403377/S1A_OPER_SAR_EOSSP__CORE_L1B_OLVF_20200103T183600.xemt");

    //SLC
    private final static File SM_SLC_SP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/SLC/SinglePol/33481-EOL1ASARSAO1A339416/S1A_OPER_SAR_EOSSP__CORE_L1A_OLF_20191128T125338.xemt");
    private final static File SM_SLC_DP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/SLC/DualPol/36587-EOL1ASARSAO1A339528/S1A_OPER_SAR_EOSSP__CORE_L1A_OLVF_20191128T135208.xemt");
    private final static File SM_SLC_QP_MetadataFile = new File(S1TBXTests.inputPathProperty + "/SAR/SAOCOM/Stripmap/SLC/QuadPol/45086-EOL1ASARSAO1A382215/S1A_OPER_SAR_EOSSP__CORE_L1A_OLVF_20191225T214531.xemt");


    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(SM_GEC_DP_MetadataFile + " not found", SM_GEC_DP_MetadataFile.exists());
        assumeTrue(SM_GEC_SP_MetadataFile2 + " not found", SM_GEC_SP_MetadataFile2.exists());
        assumeTrue(SM_GEC_SP_Folder + " not found", SM_GEC_SP_Folder.exists());
        assumeTrue(SM_GEC_QP_MetadataFile + " not found", SM_GEC_QP_MetadataFile.exists());

        assumeTrue(SM_GTC_QP_MetadataFile + " not found", SM_GTC_QP_MetadataFile.exists());

        assumeTrue(SM_DI_SP_MetadataFile + " not found", SM_DI_SP_MetadataFile.exists());
        assumeTrue(SM_DI_DP_MetadataFile + " not found", SM_DI_DP_MetadataFile.exists());
        assumeTrue(SM_DI_QP_MetadataFile + " not found", SM_DI_QP_MetadataFile.exists());

        assumeTrue(SM_SLC_SP_MetadataFile + " not found", SM_SLC_SP_MetadataFile.exists());
        assumeTrue(SM_SLC_DP_MetadataFile + " not found", SM_SLC_DP_MetadataFile.exists());
        assumeTrue(SM_SLC_QP_MetadataFile + " not found", SM_SLC_QP_MetadataFile.exists());
    }

    public TestSaocomStripmapProductReader() {
        super(new SaocomProductReaderPlugIn());
    }

    @Test
    public void testReadSM_GEC_SP_Metadata2() throws Exception {
        Product prod = testReader(SM_GEC_SP_MetadataFile2.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_VV"});
    }

    @Test
    public void testReadSM_GEC_SP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_GEC_SP_Folder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_VV"});
    }

    @Test
    public void testReadSM_GEC_DP_Metadata() throws Exception {
        Product prod = testReader(SM_GEC_DP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV"});
    }

    @Test
    public void testReadSM_GEC_QP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_GEC_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV","Sigma0_VV","Sigma0_VH"});
    }

    @Test
    public void testReadSM_GTC_QP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_GTC_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV","Sigma0_VV","Sigma0_VH"});
    }

    @Test
    public void testReadSM_DI_SP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_DI_SP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_VV"});
    }

    @Test
    public void testReadSM_DI_DP_Metadata() throws Exception {
        Product prod = testReader(SM_DI_DP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV"});
    }

    @Test
    public void testReadSM_DI_QP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_DI_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Sigma0_HH","Sigma0_HV","Sigma0_VV","Sigma0_VH"});
    }

    @Test
    public void testReadSM_SLC_SP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_SLC_SP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_VV","q_VV","Intensity_VV"});
    }

    @Test
    public void testReadSM_SLC_DP_Metadata() throws Exception {
        Product prod = testReader(SM_SLC_DP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_HH","q_HH","Intensity_HH","i_HV","q_HV","Intensity_HV"});
    }

    @Test
    public void testReadSM_SLC_QP_FolderMetadata() throws Exception {
        Product prod = testReader(SM_SLC_QP_MetadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "i_HH","q_HH","Intensity_HH",
                "i_HV","q_HV","Intensity_HV",
                "i_VV","q_VV","Intensity_VV",
                "i_VH","q_VH","Intensity_VH"});
    }
}
