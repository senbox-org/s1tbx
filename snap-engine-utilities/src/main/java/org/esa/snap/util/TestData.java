package org.esa.snap.util;

import java.io.File;

/**
 * Paths to common input data
 */
public class TestData {

    //ASAR
    public final static File inputASAR_WSM = new File(TestUtils.rootPathTestProducts, "input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");

    //ERS
    public final static File inputERS_IMP = new File(TestUtils.rootPathTestProducts, "input\\subset_0_of_ERS-1_SAR_PRI-ORBIT_32506_DATE__02-OCT-1997_14_53_43.dim");
    public final static File inputERS_IMS = new File(TestUtils.rootPathTestProducts, "input\\subset_0_of_ERS-2_SAR_SLC-ORBIT_10249_DATE__06-APR-1997_03_09_34.dim");

    //S1
    public final static File inputS1_GRD = new File(TestUtils.rootPathTestProducts, "input\\S1\\S1A_S1_GRDM_1SDV_20140607T172812_20140607T172836_000947_000EBD_7543.zip");
}
