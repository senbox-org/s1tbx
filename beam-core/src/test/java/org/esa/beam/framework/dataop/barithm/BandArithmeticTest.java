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
package org.esa.beam.framework.dataop.barithm;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.impl.ParserImpl;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.io.IOException;

public class BandArithmeticTest extends TestCase {

    private Band _targetBand;
    private Product _product1;
    private Product _product2;
    private int _width = 4;
    private int _height = 3;

    @Override
    protected void setUp() throws Exception {
        _targetBand = new Band("b1n", ProductData.TYPE_UINT16, _width, _height);
        _product1 = new Product("p1", "t", _width, _height);
        final Band band1 = _product1.addBand("b1", ProductData.TYPE_FLOAT32);
        final Band band1_3 = _product1.addBand("band1#3", ProductData.TYPE_FLOAT32);
        _product1.setRefNo(1);
        _product2 = new Product("p2", "t", _width, _height);
        _product2.addBand("b1", ProductData.TYPE_FLOAT32);
        _product2.setRefNo(2);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testComputeBandWithScaling() throws IOException,
                                                    ParseException {
        _targetBand.setScalingFactor(0.05);
        _targetBand.setScalingOffset(-7);
        _product1.getBand("b1").setDataElems(new float[]{
                2, 3, 4, 5,
                6, 7, 8, 9,
                10, 11, 12, 13
        });
        _product1.setModified(false);
        _targetBand.computeBand("b1", null, new Product[]{_product1}, 0, false, false, 0, ProgressMonitor.NULL);
        for (int y = 0; y < _height; y++) {
            for (int x = 0; x < _width; x++) {
                final float expected = 2 + y * _width + x;
                assertEquals("at index(x,y) = " + x + "," + y,
                             expected,
                             _targetBand.getPixelFloat(x, y), 1e-5);
            }
        }
    }

    public void testComputeBandWithUshort() throws IOException,
                                                   ParseException {
        _product1.getBand("b1").setDataElems(new float[]{
                2.1f, 3.2f, 4.3f, 5.4f,
                6.3f, 6.69f, 8.32f, 8.8f,
                10.2f, 11.1f, 11.9f, 13.3f
        });
        _product1.setModified(false);
        _targetBand.computeBand("b1", null, new Product[]{_product1}, 0, false, false, 0, ProgressMonitor.NULL);
        for (int y = 0; y < _height; y++) {
            for (int x = 0; x < _width; x++) {
                final float expected = 2 + y * _width + x;
                assertEquals("at index(x,y) = " + x + "," + y,
                             expected,
                             _targetBand.getPixelFloat(x, y), 1e-5);
            }
        }
    }

    public void testCreateDefaultNamespaceWithOneProduct() {
        final Namespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{_product1}, 0);

        final Symbol symbol = namespace.resolveSymbol("b1");
        assertNotNull(symbol);
        assertEquals("b1", symbol.getName());

        assertNull(namespace.resolveSymbol("$1.b1"));

        assertNull(namespace.resolveSymbol("fails"));
    }

    public void testCreateDefaultNamespaceWithMultipleProducts() {
        final Namespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{_product1, _product2}, 0);

        final Symbol symbolP1 = namespace.resolveSymbol("$1.b1");
        assertNotNull(symbolP1);
        assertTrue(symbolP1 instanceof RasterDataSymbol);
        assertEquals("$1.b1", symbolP1.getName());

        final Symbol symbolWithoutPrefix = namespace.resolveSymbol("b1");
        assertNotNull(symbolWithoutPrefix);
        assertTrue(symbolP1 instanceof RasterDataSymbol);
        assertEquals("b1", symbolWithoutPrefix.getName());

        final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(new RasterDataSymbol[]{
                (RasterDataSymbol) symbolWithoutPrefix,
                (RasterDataSymbol) symbolP1
        });
        assertEquals(1, refRasters.length);

        final Symbol symbolP2 = namespace.resolveSymbol("$2.b1");
        assertNotNull(symbolP2);
        assertEquals("$2.b1", symbolP2.getName());


        final Symbol symbolNotFound = namespace.resolveSymbol("fails");
        assertNull(symbolNotFound);
    }

    public void testGetRefRasterDataSymbols() throws ParseException {
        final Product[] products = new Product[]{_product1, _product2};
        final Parser parser = new ParserImpl(BandArithmetic.createDefaultNamespace(products, 0), false);
        String[] expectedSymbols = new String[]{"b1", "$2.b1"};
        final Term term = parser.parse("b1 + $2.b1");

        final RasterDataSymbol[] rasterSymbols = BandArithmetic.getRefRasterDataSymbols(term);

        assertEquals(2, rasterSymbols.length);
        for (int i = 0; i < expectedSymbols.length; i++) {
            String expectedSymbol = expectedSymbols[i];
            boolean found = false;
            for (int j = 0; j < rasterSymbols.length; j++) {
                RasterDataSymbol rasterSymbol = rasterSymbols[j];
                if (expectedSymbol.equals(rasterSymbol.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Expected symbol {" + expectedSymbol + "} not found");
            }
        }
    }
}
