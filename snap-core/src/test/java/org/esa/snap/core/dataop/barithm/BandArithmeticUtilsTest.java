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
package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.ParseException;
import org.junit.Test;

import static org.junit.Assert.*;

public class BandArithmeticUtilsTest {

    @Test
    public void testGetRefRasterDataSymbols() throws ParseException {
        FlagCoding fc = createFlagCoding();
        Product p1 = createProduct(fc, 1);

        RasterDataNode[] rasters;

        rasters = BandArithmetic.getRefRasters("c + w * q - w", p1);
        assertNotNull(rasters);
        assertEquals(3, rasters.length);
        assertSame(p1.getBand("c"), rasters[0]);
        assertSame(p1.getBand("w"), rasters[1]);
        assertSame(p1.getBand("q"), rasters[2]);

        Product p2 = createProduct(fc, 2);
        Product p3 = createProduct(fc, 3);

        rasters = BandArithmetic.getRefRasters("c + ($2.w - $1.w) * $3.q + ($2.l - $1.l) * $3.q", p1, p3, p2);
        assertNotNull(rasters);
        assertEquals(6, rasters.length);
        assertSame(p1.getBand("c"), rasters[0]);
        assertSame(p2.getBand("w"), rasters[1]);
        assertSame(p1.getBand("w"), rasters[2]);
        assertSame(p3.getBand("q"), rasters[3]);
        assertSame(p2.getBand("l"), rasters[4]);
        assertSame(p1.getBand("l"), rasters[5]);
    }

    @Test
    public void testGetValidMaskExpression() throws ParseException {
        FlagCoding fc = createFlagCoding();
        Product p1 = createProduct(fc, 1);

        String vme;

        vme = BandArithmetic.getValidMaskExpression("c + w * q - w", p1, null);
        assertEquals("(f.CLOUD && !f.INVALID) " + "&& (f.WATER && !f.INVALID)", vme);

        vme = BandArithmetic.getValidMaskExpression("c + w * q - w", p1, "");
        assertEquals("(f.CLOUD && !f.INVALID) " + "&& (f.WATER && !f.INVALID)", vme);

        vme = BandArithmetic.getValidMaskExpression("c + w * q - w", p1, "c >= 0.0");
        assertEquals("(c >= 0.0) " + "&& (f.CLOUD && !f.INVALID) " + "&& (f.WATER && !f.INVALID)", vme);

        Product p2 = createProduct(fc, 2);
        Product p3 = createProduct(fc, 3);

        vme = BandArithmetic.getValidMaskExpression("c + ($2.w - $1.w) * $3.q + ($2.l - $1.l) * $3.c",
                                                    new Product[]{p1, p3, p2}, 0, null);
        assertEquals("(f.CLOUD && !f.INVALID) " +
                             "&& ($2.f.WATER && !$2.f.INVALID) " +
                             "&& (f.WATER && !f.INVALID) " +
                             "&& ($2.f.LAND && !$2.f.INVALID) " +
                             "&& (f.LAND && !f.INVALID) " +
                             "&& ($3.f.CLOUD && !$3.f.INVALID)", vme);

        vme = BandArithmetic.getValidMaskExpression("c + ($2.w - $1.w) * $3.q + ($2.l - $1.l) * $3.c",
                                                    new Product[]{p1, p3, p2}, 0, "c >= 0.0");
        assertEquals("(c >= 0.0) " +
                             "&& (f.CLOUD && !f.INVALID) " +
                             "&& ($2.f.WATER && !$2.f.INVALID) " +
                             "&& (f.WATER && !f.INVALID) " +
                             "&& ($2.f.LAND && !$2.f.INVALID) " +
                             "&& (f.LAND && !f.INVALID) " +
                             "&& ($3.f.CLOUD && !$3.f.INVALID)", vme);

        vme = BandArithmetic.getValidMaskExpression("$2.w", new Product[]{p1, p2}, 0, null);
        assertEquals("($2.f.WATER && !$2.f.INVALID)", vme);
    }

    private FlagCoding createFlagCoding() {
        FlagCoding fc = new FlagCoding("flags");
        fc.addFlag("INVALID", 0x01, "Invalid");
        fc.addFlag("LAND", 0x02, "Land");
        fc.addFlag("WATER", 0x04, "Water");
        fc.addFlag("CLOUD", 0x08, "Cloud");
        return fc;
    }

    private Product createProduct(FlagCoding fc, int i) {
        Product p3 = new Product("p" + i, "t" + i, 16, 16);
        p3.setRefNo(i);
        p3.getFlagCodingGroup().add(fc);
        p3.addBand("f", ProductData.TYPE_UINT8).setSampleCoding(fc);
        p3.addBand("q", ProductData.TYPE_UINT32);
        p3.addBand("l", ProductData.TYPE_UINT32).setValidPixelExpression("f.LAND && !f.INVALID");
        p3.addBand("w", ProductData.TYPE_UINT32).setValidPixelExpression("f.WATER && !f.INVALID");
        p3.addBand("c", ProductData.TYPE_UINT32).setValidPixelExpression("f.CLOUD && !f.INVALID");
        return p3;
    }


}
