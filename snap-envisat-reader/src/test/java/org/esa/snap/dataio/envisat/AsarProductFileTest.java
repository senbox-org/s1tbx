/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.envisat;

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
