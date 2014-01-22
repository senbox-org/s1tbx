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

package org.esa.beam.framework.dataop.barithm;

import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

public class RasterDataSymbolTest {

    @Test
    public void testConstructor() throws Exception {
        Band floatBand = new Band("floatBand", ProductData.TYPE_FLOAT32, 1, 1);

        RasterDataSymbol fgeo = new RasterDataSymbol("fgeo", floatBand, RasterDataSymbol.Source.GEOPHYSICAL);
        assertEquals("fgeo", fgeo.getName());
        assertSame(floatBand, fgeo.getRaster());
        assertEquals(Term.TYPE_D, fgeo.getRetType());
        assertEquals(RasterDataSymbol.Source.GEOPHYSICAL, fgeo.getSource());

        RasterDataSymbol fraw = new RasterDataSymbol("fraw", floatBand, RasterDataSymbol.Source.RAW);
        assertEquals("fraw", fraw.getName());
        assertSame(floatBand, fraw.getRaster());
        assertEquals(Term.TYPE_D, fraw.getRetType());
        assertEquals(RasterDataSymbol.Source.RAW, fraw.getSource());

        Band scaledIntBand = new Band("scaledIntBand", ProductData.TYPE_INT16, 1, 1);
        scaledIntBand.setScalingFactor(5f);

        RasterDataSymbol sigeo = new RasterDataSymbol("sigeo", scaledIntBand, RasterDataSymbol.Source.GEOPHYSICAL);
        assertEquals("sigeo", sigeo.getName());
        assertSame(scaledIntBand, sigeo.getRaster());
        assertEquals(Term.TYPE_D, sigeo.getRetType());
        assertEquals(RasterDataSymbol.Source.GEOPHYSICAL, sigeo.getSource());

        RasterDataSymbol siraw = new RasterDataSymbol("siraw", scaledIntBand, RasterDataSymbol.Source.RAW);
        assertEquals("siraw", siraw.getName());
        assertSame(scaledIntBand, siraw.getRaster());
        assertEquals(Term.TYPE_I, siraw.getRetType());
        assertEquals(RasterDataSymbol.Source.RAW, siraw.getSource());
    }
}
