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

package org.esa.snap.core.datamodel;

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
