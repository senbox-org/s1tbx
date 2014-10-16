package org.esa.snap.util;

import java.io.File;

/**
 * Paths to common input data
 */
public class TestData {

    public final static String sep = File.separator;
    public final static String inputSAR = TestUtils.rootPathTestProducts+sep+"input"+sep+"SAR"+sep;

    //ASAR
    public final static File inputASAR_WSM = new File(inputSAR+"ASAR"+sep+"subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");
    public final static File inputASAR_IMS = new File(inputSAR+"ASAR"+sep+"subset_3_ASA_IMS_1PNUPA20031203_061259_000000162022_00120_09192_0099.dim");

    public final static File inputStackIMS = new File(inputSAR+"Stack"+sep+"coregistered_stack.dim");

    //ERS
    public final static File inputERS_IMP = new File(inputSAR+"ERS"+sep+"subset_0_of_ERS-1_SAR_PRI-ORBIT_32506_DATE__02-OCT-1997_14_53_43.dim");
    public final static File inputERS_IMS = new File(inputSAR+"ERS"+sep+"subset_0_of_ERS-2_SAR_SLC-ORBIT_10249_DATE__06-APR-1997_03_09_34.dim");

    //RS2
    public final static File inputRS2_SQuad = new File(inputSAR+"RS2"+sep+"RS2-standard-quad.zip");

    //QuadPol
    public final static File inputQuad = new File(inputSAR+"QuadPol"+sep+"QuadPol_subset_0_of_RS2-SLC-PDS_00058900.dim");
    public final static File inputQuadFullStack = new File(inputSAR+"QuadPolStack"+sep+"RS2-Quad_Pol_Stack.dim");
    public final static File inputC3Stack = new File(inputSAR+"QuadPolStack"+sep+"RS2-C3-Stack.dim");
    public final static File inputT3Stack = new File(inputSAR+"QuadPolStack"+sep+"RS2-T3-Stack.dim");

    //S1
    public final static File inputS1_GRD = new File(inputSAR+"S1"+sep+"S1A_S1_GRDM_1SDV_20140607T172812_20140607T172836_000947_000EBD_7543.zip");
    public final static File inputS1_StripmapSLC = new File(inputSAR+"S1"+sep+"subset_2_S1A_S1_SLC__1SSV_20140807T142342_20140807T142411_001835_001BC1_05AA.dim");
}
