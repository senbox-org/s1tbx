/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.Term;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class SingleFlagSymbolTest {

    private Band intBand;
    private SingleFlagSymbol sx;
    private SingleFlagSymbol sy;

    @Before
    public void setUp() throws Exception {
        intBand = new Band("scaledIntBand", ProductData.TYPE_INT16, 1, 1);
        sx = new SingleFlagSymbol("s.x", intBand, 0x01);
        sy = new SingleFlagSymbol("s.y", intBand, 0x03, 3);
    }

    @Test
    public void testConstructor() throws Exception {
        test(sx, "s.x", 0x01, 1, intBand);
        test(sy, "s.y", 0x03, 3, intBand);
    }

    @Test
    public void testClone() throws Exception {
        test(sx.clone(), "s.x", 0x01, 1, intBand);
        test(sy.clone(), "s.y", 0x03, 3, intBand);
    }

    static void test(SingleFlagSymbol sym,
                     String expName,
                     int expMask,
                     int expValue,
                     RasterDataNode expRaster) {
        RasterDataSymbolTest.test(sym, expName, Term.TYPE_B, expRaster, RasterDataSymbol.Source.RAW);
        assertEquals(expMask, sym.getFlagMask());
        assertEquals(expValue, sym.getFlagValue());
    }
}
