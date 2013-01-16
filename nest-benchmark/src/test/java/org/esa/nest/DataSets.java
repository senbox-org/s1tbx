/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest;

import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.util.TestUtils;

import java.io.IOException;

/**
 * Test Datasets for benchmarks
 */
public class DataSets {

    public final static String perfRoot = TestUtils.rootPathExpectedProducts + "largeFiles\\";

    public final static String tiffFile1 = perfRoot+"Tiff\\imagery_HV.tif";
    public final static String tiffFile2 = perfRoot+"Tiff\\srtm_39_04.tif";

    //RS2
    public final static String vancouverRS2Quad = perfRoot+"Radarsat2\\Quad\\Vancouver_R2_FineQuad15_Frame1_SLC\\product.xml";
    public final Product RS2_quad_product;

    //ASAR
    public final static String ASAR_IMS = perfRoot+"ASAR\\IMS\\Thun_Switzerland\\ASA_IMS_1PNUPA20080423_094914_000000162068_00022_32140_2544.N1";
    public final static String ASAR_IMP = perfRoot+"ASAR\\IMP\\ASA_IMP_1PNDPA20040402_092740_000000152025_00351_10926_0004.N1";
    public final static String ASAR_APP = perfRoot+"ASAR\\APP\\ASA_APP_1PNIPA20030327_091854_000000162015_00036_05601_5420.N1";
    public final static String ASAR_APS = perfRoot+"ASAR\\APS\\ASA_APS_1PNIPA20030327_091854_000000162015_00036_05601_5421.N1";
    public final static String ASAR_WMS = perfRoot+"ASAR\\WSM\\ASA_WSM_1PNPDK20080119_093145_000002252065_00165_30780_3244.N1";
    public final Product ASAR_IMS_product;
    public final Product ASAR_IMP_product;
    public final Product ASAR_APP_product;
    public final Product ASAR_APS_product;
    public final Product ASAR_WMS_product;

    //ERS-1
    public final static String ERS1_PRI = perfRoot+"ERS1\\PRI\\ERS2_PRI_VMP_CEOS_12032000_orbit 25592 frame 0747_ASI IPAF\\SCENE1\\VDF_DAT.001";
    public final static String ERS1_SLC = perfRoot+"ERS1\\SLC\\ERS2_SLCI_VMP_CEOS_13052000_orbit 26472 frame 2727_UKPAF\\SCENE1\\VDF_DAT.001";
    public final Product ERS1_PRI_product;
    public final Product ERS1_SLC_product;

    //ERS-2
    public final static String ERS2_IMP = perfRoot+"ERS2\\IMP\\Vietnam\\SAR_IMP_1PXESA19960521_031648_00000017A011_00304_05669_1166.E2";
    public final static String ERS2_IMS = perfRoot+"ERS2\\IMS\\Vietnam\\SAR_IMS_1PXESA19960521_031648_00000017A011_00304_05669_0767.E2";
    public final Product ERS2_IMP_product;
    public final Product ERS2_IMS_product;

    //ALOS
    public final static String ALOS_L11 = perfRoot+"ALOS\\L1.1\\volcanoes_Tanzania\\450001\\VOL-ALPSRP072533680-H1.1__D";
    public final Product ALOS_L11_product;

    //Cosmo-Skymed

    //TerraSAR-X
    public final static String TSX_SSC = perfRoot+"TerraSARX\\SSC\\grand.canyon\\TSX1_SAR__SSC______SM_S_SRA_20080310T133220_20080310T133228\\TSX1_SAR__SSC______SM_S_SRA_20080310T133220_20080310T133228.xml";
    public final static String TSX_SSC_Quad = perfRoot+"TerraSARX\\SSC\\quad\\dims_op_oc_dfd2_369759996_1\\TSX-1.SAR.L1B\\TSX1_SAR__SSC______SM_Q_DRA_20100411T141511_20100411T141519\\TSX1_SAR__SSC______SM_Q_DRA_20100411T141511_20100411T141519.xml";
    public final Product TSX_SSC_product;
    public final Product TSX_SSC_Quad_product;

    //S-1
    public final static String S1_IW_SLC = perfRoot+"S1\\IW_SLC\\S1A_IW_SLC__1SDH_20120101T043302_20120101T043312_001771_000001_9B7B.SAFE\\manifest.safe";
    public final static String S1_IW_GRD = perfRoot+"S1\\IW_GRD\\S1A_IW_GRDH_1SDH_20120101T043302_20120101T043312_001771_000001_5854.SAFE\\manifest.safe";
    public final Product S1_SLC_product;
    public final Product S1_GRD_product;

    private static DataSets theInstance = null;

    public DataSets() throws IOException {
        RS2_quad_product = TestUtils.readSourceProduct(vancouverRS2Quad);

        ASAR_IMS_product = TestUtils.readSourceProduct(ASAR_IMS);
        ASAR_IMP_product = TestUtils.readSourceProduct(ASAR_IMP);
        ASAR_APP_product = TestUtils.readSourceProduct(ASAR_APP);
        ASAR_APS_product = TestUtils.readSourceProduct(ASAR_APS);
        ASAR_WMS_product = TestUtils.readSourceProduct(ASAR_WMS);

        ERS1_PRI_product = TestUtils.readSourceProduct(ERS1_PRI);
        ERS1_SLC_product = TestUtils.readSourceProduct(ERS1_SLC);

        ERS2_IMP_product = TestUtils.readSourceProduct(ERS2_IMP);
        ERS2_IMS_product = TestUtils.readSourceProduct(ERS2_IMS);

        ALOS_L11_product = TestUtils.readSourceProduct(ALOS_L11);

        TSX_SSC_product = TestUtils.readSourceProduct(TSX_SSC);
        TSX_SSC_Quad_product = TestUtils.readSourceProduct(TSX_SSC_Quad);

        S1_SLC_product = TestUtils.readSourceProduct(S1_IW_SLC);
        S1_GRD_product = TestUtils.readSourceProduct(S1_IW_GRD);
    }

    public static DataSets instance() throws IOException {
        if(theInstance == null) {
            theInstance = new DataSets();
        }
        return theInstance;
    }
}
