/*
 * $Id: MerisProductFileTest.java,v 1.1 2006/09/18 06:34:40 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

public class MerisProductFileTest extends TestCase {

    public void testDddbProductTypeReplacement() {
        // IODD 5
        final int v5 = MerisProductFile.IODD_VERSION_5;
        testDddbProductTypeReplacement("MER_RR__1P_IODD5", "MER_RR__1P", v5);
        testDddbProductTypeReplacement("MER_FR__1P_IODD5", "MER_FR__1P", v5);
        testDddbProductTypeReplacement("MER_RR__2P_IODD6", "MER_RR__2P", v5); // L2 is compliant with L2 IODD 6
        testDddbProductTypeReplacement("MER_RR__1C_IODD5", "MER_RR__1C",
                                       v5); // similar child product, todo: (nf) there is no '2C' descriptor!
        testDddbProductTypeReplacement("MER_FR__2P_IODD6", "MER_FR__2P", v5); // L2 is compliant with L2 IODD 6
        testDddbProductTypeReplacement("MER_RR__2C_IODD6", "MER_RR__2C",
                                       v5); // similar child product, todo: (nf) there is no '2C' descriptor!
        testDddbProductTypeReplacement(null, "MERIS", v5); // unknown product type

        // IODD 6
        final int v6 = MerisProductFile.IODD_VERSION_6;
        testDddbProductTypeReplacement(null, "MER_RR__1P", v6); // L1 not affected
        testDddbProductTypeReplacement(null, "MER_FR__1P", v6); // L1 not affected
        testDddbProductTypeReplacement("MER_RR__2P_IODD6", "MER_RR__2P", v6); // L2 affected
        testDddbProductTypeReplacement("MER_FR__2P_IODD6", "MER_FR__2P", v6); // L2 affected
        testDddbProductTypeReplacement("MER_RR__2C_IODD6", "MER_RR__2C",
                                       v6); // similar child product, todo: (nf) there is no '2C' descriptor!
        testDddbProductTypeReplacement(null, "MERIS", v6);  // unknown product type

        // IODD 7
        final int v7 = MerisProductFile.IODD_VERSION_7;
        testDddbProductTypeReplacement(null, "MER_RR__1P", v7); // only full swath affected
        testDddbProductTypeReplacement(null, "MER_FR__1P", v7); // only full swath affected
        testDddbProductTypeReplacement(null, "MER_RR__2P", v7); // only full swath affected
        testDddbProductTypeReplacement(null, "MER_FR__2P", v7); // only full swath affected
        testDddbProductTypeReplacement(null, "MER_RR__1C", v7); // only full swath affected
        testDddbProductTypeReplacement(null, "MER_RR__2C", v7); // only full swath affected
        testDddbProductTypeReplacement("MER_FR__1P", "MER_FRS_1P", v7); // full swath affected
        testDddbProductTypeReplacement("MER_FR__1C", "MER_FRS_1C",
                                       v7); // similar child product, todo: (nf) there is no '2C' descriptor!
        testDddbProductTypeReplacement("MER_FR__2P", "MER_FRS_2P", v7); // full swath affected
        testDddbProductTypeReplacement("MER_FR__2C", "MER_FRS_2C",
                                       v7); // similar child product, todo: (nf) there is no '2C' descriptor!
        testDddbProductTypeReplacement(null, "MERIS", v7);  // unknown product type

        // invalid IODD version
        testDddbProductTypeReplacement(null, "MER_RR__1", -548);
    }

    private void testDddbProductTypeReplacement(final String replacement, final String productType,
                                                final int ioddVersion5) {
        String s;
        s = MerisProductFile.getDddbProductTypeReplacement(productType,
                                                           ioddVersion5);
        assertEquals(replacement, s);
    }
}
