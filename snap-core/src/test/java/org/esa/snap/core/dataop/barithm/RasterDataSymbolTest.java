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
 * @author Marco
 */
public class RasterDataSymbolTest {

    private Band floatBand;
    private RasterDataSymbol fgeo;
    private RasterDataSymbol fraw;
    private Band scaledIntBand;
    private RasterDataSymbol sigeo;
    private RasterDataSymbol siraw;

    @Before
    public void setUp() throws Exception {
        floatBand = new Band("floatBand", ProductData.TYPE_FLOAT32, 1, 1);
        fgeo = new RasterDataSymbol("fgeo", floatBand, RasterDataSymbol.Source.GEOPHYSICAL);
        fraw = new RasterDataSymbol("fraw", floatBand, RasterDataSymbol.Source.RAW);
        scaledIntBand = new Band("scaledIntBand", ProductData.TYPE_INT16, 1, 1);
        scaledIntBand.setScalingFactor(5f);
        sigeo = new RasterDataSymbol("sigeo", scaledIntBand, RasterDataSymbol.Source.GEOPHYSICAL);
        siraw = new RasterDataSymbol("siraw", scaledIntBand, RasterDataSymbol.Source.RAW);
    }

    @Test
    public void testConstructor() throws Exception {
        test(fgeo, "fgeo", Term.TYPE_D, floatBand, RasterDataSymbol.Source.GEOPHYSICAL);
        test(fraw, "fraw", Term.TYPE_D, floatBand, RasterDataSymbol.Source.RAW);
        test(sigeo, "sigeo", Term.TYPE_D, scaledIntBand, RasterDataSymbol.Source.GEOPHYSICAL);
        test(siraw, "siraw", Term.TYPE_I, scaledIntBand, RasterDataSymbol.Source.RAW);
    }

    @Test
    public void testClone() throws Exception {
        test(fgeo.clone(), "fgeo", Term.TYPE_D, floatBand, RasterDataSymbol.Source.GEOPHYSICAL);
        test(fraw.clone(), "fraw", Term.TYPE_D, floatBand, RasterDataSymbol.Source.RAW);
        test(sigeo.clone(), "sigeo", Term.TYPE_D, scaledIntBand, RasterDataSymbol.Source.GEOPHYSICAL);
        test(siraw.clone(), "siraw", Term.TYPE_I, scaledIntBand, RasterDataSymbol.Source.RAW);
    }

    static void test(RasterDataSymbol sym,
                     String expName,
                     int expType,
                     RasterDataNode expRaster,
                     RasterDataSymbol.Source expSource) {
        assertEquals(expName, sym.getName());
        assertSame(expRaster, sym.getRaster());
        assertEquals(expType, sym.getRetType());
        assertEquals(expSource, sym.getSource());
    }
}
