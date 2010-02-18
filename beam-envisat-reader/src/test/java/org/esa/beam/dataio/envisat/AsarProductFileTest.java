/*
 * $Id: MerisProductFileTest.java,v 1.1 2006/09/18 06:34:40 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

public class AsarProductFileTest extends TestCase {


    public void testDddbProductTypeReplacement() {

        //VERSION_UNKNOWN, ASAR_3K, ASAR_4A, ASAR_4B

        // IODD 3K
        final AsarProductFile.IODD v3K = AsarProductFile.IODD.ASAR_3K;
        testDddbProductTypeReplacement("ASA_IMG_1P_IODD_3K", "ASA_IMG_1P", v3K);
        testDddbProductTypeReplacement("ASAR", "ASAR", v3K);                     // unknown product type

        // IODD 4A
        final AsarProductFile.IODD v4A = AsarProductFile.IODD.ASAR_4A;
        testDddbProductTypeReplacement("ASA_IMG_1P_IODD_4A", "ASA_IMG_1P", v4A);
        testDddbProductTypeReplacement("ASAR", "ASAR", v4A);                     // unknown product type

        // IODD 4B
        final AsarProductFile.IODD v4B = AsarProductFile.IODD.ASAR_4B;
        testDddbProductTypeReplacement("ASA_WSM_1P_IODD_4B", "ASA_WSM_1P", v4B);
        testDddbProductTypeReplacement("ASA_IMG_1P_IODD_4A", "ASA_IMG_1P", v4B); // 4B doesn't exist so get 4A
        testDddbProductTypeReplacement("ASAR", "ASAR", v4B);                     // unknown product type

        // unknown IODD version
        final AsarProductFile.IODD vUnknown = AsarProductFile.IODD.VERSION_UNKNOWN;
        testDddbProductTypeReplacement("ASA_IMG_1P", "ASA_IMG_1P", vUnknown);
        testDddbProductTypeReplacement("ASAR", "ASAR", vUnknown);
    }

    private static void testDddbProductTypeReplacement(final String replacement, final String productType,
                                                final AsarProductFile.IODD ioddVersion) {
        String s;
        s = AsarProductFile.getDddbProductTypeReplacement(productType,
                                                           ioddVersion);
        assertEquals(replacement, s);
    }
}