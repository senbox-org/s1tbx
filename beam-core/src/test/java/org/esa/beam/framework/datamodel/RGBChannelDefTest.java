package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

public class RGBChannelDefTest extends TestCase {
    public void testGamma () {
        final RGBChannelDef def = new RGBChannelDef();
        testChannelGamma(def, 0);
        testChannelGamma(def, 1);
        testChannelGamma(def, 2);
        testChannelGamma(def, 3);
    }

    private void testChannelGamma(RGBChannelDef def, int index) {
        assertEquals(1.0, def.getGamma(index), 1e-12);
        assertEquals(false, def.isGammaUsed(index));
        def.setGamma(index, 2.0);
        assertEquals(2.0, def.getGamma(index), 1e-12);
        assertEquals(true, def.isGammaUsed(index));
        def.setGamma(index, 0.5);
        assertEquals(0.5, def.getGamma(index), 1e-12);
        assertEquals(true, def.isGammaUsed(index));
        def.setGamma(index, 1.0);
        assertEquals(1.0, def.getGamma(index), 1e-12);
        assertEquals(false, def.isGammaUsed(index));
    }
}
