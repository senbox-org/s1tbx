/*
 * $Id: BandArithmeticTest.java,v 1.2 2006/12/08 13:48:37 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

public class BandArithmeticUtilsTest extends TestCase {

    public void testGetRefRasterDataSymbols() throws ParseException {
        FlagCoding fc = createFlagCoding();
        Product p1 = createProduct(fc, 1);

        RasterDataNode[] rasters;

        rasters = BandArithmetic.getRefRasters("c + w * q - w",
                                               new Product[]{p1});
        assertNotNull(rasters.length);
        assertEquals(3, rasters.length);
        assertSame(p1.getBand("c"), rasters[0]);
        assertSame(p1.getBand("w"), rasters[1]);
        assertSame(p1.getBand("q"), rasters[2]);

        Product p2 = createProduct(fc, 2);
        Product p3 = createProduct(fc, 3);

        rasters = BandArithmetic.getRefRasters("c + ($2.w - $1.w) * $3.q + ($2.l - $1.l) * $3.q",
                                               new Product[]{p1, p3, p2});
        assertNotNull(rasters.length);
        assertEquals(6, rasters.length);
        assertSame(p1.getBand("c"), rasters[0]);
        assertSame(p2.getBand("w"), rasters[1]);
        assertSame(p1.getBand("w"), rasters[2]);
        assertSame(p3.getBand("q"), rasters[3]);
        assertSame(p2.getBand("l"), rasters[4]);
        assertSame(p1.getBand("l"), rasters[5]);
    }

    public void testGetValidMaskExpression() throws ParseException {
        FlagCoding fc = createFlagCoding();
        Product p1 = createProduct(fc, 1);

        String vme;

        vme = BandArithmetic.getValidMaskExpression("c + w * q - w",
                                                    new Product[]{p1}, 0,
                                                    null);
        assertEquals("(f.CLOUD) && (f.WATER)", vme);

        vme = BandArithmetic.getValidMaskExpression("c + w * q - w",
                                                    new Product[]{p1}, 0,
                                                    "c >= 0.0");
        assertEquals("(c >= 0.0) && (f.CLOUD) && (f.WATER)", vme);

        Product p2 = createProduct(fc, 2);
        Product p3 = createProduct(fc, 3);

        vme = BandArithmetic.getValidMaskExpression("c + ($2.w - $1.w) * $3.q + ($2.l - $1.l) * $3.c",
                                                    new Product[]{p1, p3, p2}, 0,
                                                    null);
        assertEquals("(f.CLOUD) && ($2.f.WATER) && (f.WATER) && ($2.f.LAND) && (f.LAND) && ($3.f.CLOUD)", vme);

        vme = BandArithmetic.getValidMaskExpression("c + ($2.w - $1.w) * $3.q + ($2.l - $1.l) * $3.c",
                                                    new Product[]{p1, p3, p2}, 0,
                                                    "c >= 0.0");
        assertEquals("(c >= 0.0) && (f.CLOUD) && ($2.f.WATER) && (f.WATER) && ($2.f.LAND) && (f.LAND) && ($3.f.CLOUD)", vme);
    }

    private FlagCoding createFlagCoding() {
        FlagCoding fc = new FlagCoding("flags");
        fc.addFlag("LAND", 0x01, "Land");
        fc.addFlag("WATER", 0x02, "Water");
        fc.addFlag("CLOUD", 0x04, "Cloud");
        return fc;
    }

    private Product createProduct(FlagCoding fc, int i) {
        Product p3 = new Product("p" + i, "t" + i, 16, 16);
        p3.setRefNo(i);
        p3.getFlagCodingGroup().add(fc);
        p3.addBand("f", ProductData.TYPE_UINT8).setSampleCoding(fc);
        p3.addBand("q", ProductData.TYPE_UINT32);
        p3.addBand("l", ProductData.TYPE_UINT32).setValidPixelExpression("f.LAND");
        p3.addBand("w", ProductData.TYPE_UINT32).setValidPixelExpression("f.WATER");
        p3.addBand("c", ProductData.TYPE_UINT32).setValidPixelExpression("f.CLOUD");
        return p3;
    }


}
