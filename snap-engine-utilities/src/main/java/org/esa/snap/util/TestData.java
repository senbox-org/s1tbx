/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import java.io.File;

/**
 * Paths to common input data
 */
public class TestData {

    public final static String sep = File.separator;
    public final static String input = TestUtils.rootPathTestProducts + sep + "input" + sep;
    public final static String inputSAR = input + "SAR" + sep;

    //ASAR
    public final static File inputASAR_IMM = new File(inputSAR + "ASAR" + sep + "ASA_IMM.zip");
    public final static File inputASAR_APM = new File(inputSAR + "ASAR" + sep + "ASA_APM.zip");
    public final static File inputASAR_WSM = new File(inputSAR + "ASAR" + sep + "subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");
    public final static File inputASAR_IMS = new File(inputSAR + "ASAR" + sep + "subset_3_ASA_IMS_1PNUPA20031203_061259_000000162022_00120_09192_0099.dim");
    public final static File inputASAR_IMMSub = new File(inputSAR + "ASAR" + sep + "subset_0_of_ENVISAT-ASA_IMM_1P_0739.dim");

    public final static File inputStackIMS = new File(inputSAR + "Stack" + sep + "coregistered_stack.dim");

    //ERS
    public final static File inputERS_IMP = new File(inputSAR + "ERS" + sep + "subset_0_of_ERS-1_SAR_PRI-ORBIT_32506_DATE__02-OCT-1997_14_53_43.dim");
    public final static File inputERS_IMS = new File(inputSAR + "ERS" + sep + "subset_0_of_ERS-2_SAR_SLC-ORBIT_10249_DATE__06-APR-1997_03_09_34.dim");

    //RS2
    public final static File inputRS2_SQuad = new File(inputSAR + "RS2" + sep + "RS2-standard-quad.zip");

    //QuadPol
    public final static File inputQuad = new File(inputSAR + "QuadPol" + sep + "QuadPol_subset_0_of_RS2-SLC-PDS_00058900.dim");
    public final static File inputQuadFullStack = new File(inputSAR + "QuadPolStack" + sep + "RS2-Quad_Pol_Stack.dim");
    public final static File inputC3Stack = new File(inputSAR + "QuadPolStack" + sep + "RS2-C3-Stack.dim");
    public final static File inputT3Stack = new File(inputSAR + "QuadPolStack" + sep + "RS2-T3-Stack.dim");

    //ALOS
    public final static File inputALOS1_1 = new File(inputSAR + "ALOS"+sep+"subset_0_of_ALOS-H1_1__A-ORBIT__ALPSRP076360690.dim");

    //S1
    public final static File inputS1_GRD = new File(inputSAR + "S1" + sep + "S1A_S1_GRDM_1SDV_20140607T172812_20140607T172836_000947_000EBD_7543.zip");
    public final static File inputS1_StripmapSLC = new File(inputSAR + "S1" + sep + "subset_2_S1A_S1_SLC__1SSV_20140807T142342_20140807T142411_001835_001BC1_05AA.dim");
}
