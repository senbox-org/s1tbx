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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.jexp.impl.SymbolFactory;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class BandArithmeticTest {

    private Product product1;
    private Product product2;
    private int width = 4;
    private int height = 3;

    @Before
    public void setUp() throws FactoryException, TransformException {
        product1 = new Product("p1", "t", width, height);
        product1.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, 0, 10, 0.1, 0.1));
        product1.addBand("b1", ProductData.TYPE_FLOAT32);
        product1.addBand("b2", ProductData.TYPE_UINT8);
        product1.setRefNo(1);
        product2 = new Product("p2", "t", width, height);
        product2.setSceneGeoCoding(null);
        product2.addBand("b1", ProductData.TYPE_FLOAT32);
        product2.setRefNo(2);
    }

    @Test
    public void testCreateDefaultNamespaceWithOneProduct() {
        Namespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{product1}, 0);

        assertThat(namespace.resolveSymbol("X"), instanceOf(ProductNamespaceExtenderImpl.PixelXSymbol.class));
        assertThat(namespace.resolveSymbol("Y"), instanceOf(ProductNamespaceExtenderImpl.PixelYSymbol.class));
        assertThat(namespace.resolveSymbol("LAT"), instanceOf(ProductNamespaceExtenderImpl.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("LON"), instanceOf(ProductNamespaceExtenderImpl.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("TIME"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("MJD"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class)); // compatibility
        assertThat(namespace.resolveSymbol("b1"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("b1.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b1.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b1.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertThat(namespace.resolveSymbol("b2"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("b2.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b2.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b2.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertNull(namespace.resolveSymbol("b3"));

        assertNull(namespace.resolveSymbol("$1.X"));
        assertNull(namespace.resolveSymbol("$1.b1"));
        assertNull(namespace.resolveSymbol("$1.b2"));
    }

    @Test
    public void testCreateDefaultNamespaceWithMultipleProducts() {
        Namespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{product1, product2}, 0);

        assertThat(namespace.resolveSymbol("X"), instanceOf(ProductNamespaceExtenderImpl.PixelXSymbol.class));
        assertThat(namespace.resolveSymbol("Y"), instanceOf(ProductNamespaceExtenderImpl.PixelYSymbol.class));
        assertThat(namespace.resolveSymbol("LAT"), instanceOf(ProductNamespaceExtenderImpl.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("LON"), instanceOf(ProductNamespaceExtenderImpl.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("TIME"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("MJD"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class)); // compatibility
        assertThat(namespace.resolveSymbol("b1"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("b1.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b1.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b1.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertThat(namespace.resolveSymbol("b2"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("b2.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b2.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b2.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertNull(namespace.resolveSymbol("b3"));

        assertThat(namespace.resolveSymbol("$1.X"), instanceOf(ProductNamespaceExtenderImpl.PixelXSymbol.class));
        assertThat(namespace.resolveSymbol("$1.Y"), instanceOf(ProductNamespaceExtenderImpl.PixelYSymbol.class));
        assertThat(namespace.resolveSymbol("$1.LAT"), instanceOf(ProductNamespaceExtenderImpl.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("$1.LON"), instanceOf(ProductNamespaceExtenderImpl.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("$1.TIME"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("$1.MJD"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class)); // compatibility
        assertThat(namespace.resolveSymbol("$1.b1"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("$1.b1.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b1.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b1.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertThat(namespace.resolveSymbol("$1.b2"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("$1.b2.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b2.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b2.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertNull(namespace.resolveSymbol("$1.b3"));

        assertThat(namespace.resolveSymbol("$2.X"), instanceOf(ProductNamespaceExtenderImpl.PixelXSymbol.class));
        assertThat(namespace.resolveSymbol("$2.Y"), instanceOf(ProductNamespaceExtenderImpl.PixelYSymbol.class));
        assertThat(namespace.resolveSymbol("$2.LAT"), instanceOf(ProductNamespaceExtenderImpl.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("$2.LON"), instanceOf(ProductNamespaceExtenderImpl.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("$2.TIME"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("$2.MJD"), instanceOf(ProductNamespaceExtenderImpl.PixelTimeSymbol.class)); // compatibility
        assertThat(namespace.resolveSymbol("$2.b1"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("$2.b1.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$2.b1.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$2.b1.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertNull(namespace.resolveSymbol("$2.b2"));
        assertNull(namespace.resolveSymbol("$2.b2.solar_flux"));
        assertNull(namespace.resolveSymbol("$2.b2.spectral_wavelength"));
        assertNull(namespace.resolveSymbol("$2.b2.spectral_band_index"));
        assertNull(namespace.resolveSymbol("$2.b3"));
    }

    @Test
    public void testGetRefRasterDataSymbols() throws ParseException {
        final Product[] products = new Product[]{product1, product2};
        final Parser parser = new ParserImpl(BandArithmetic.createDefaultNamespace(products, 0), false);
        String[] expectedSymbols = new String[]{"b1", "$2.b1"};
        final Term term = parser.parse("b1 + $2.b1");

        final RasterDataSymbol[] rasterSymbols = BandArithmetic.getRefRasterDataSymbols(term);

        assertEquals(2, rasterSymbols.length);
        for (String expectedSymbol : expectedSymbols) {
            boolean found = false;
            for (RasterDataSymbol rasterSymbol : rasterSymbols) {
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

    @Test
    public void testAreRastersEqualInSize() throws ParseException {
        String[] compatibleExpressions = new String[]{"b1", "b2"};
        final Band anotherBand = new Band("anotherBand", ProductData.TYPE_UINT8, width + 1, height);
        product1.addBand(anotherBand);
        String[] incompatibleExpressions = new String[]{"b1", "b2", "anotherBand"};

        assertEquals(true, BandArithmetic.areRastersEqualInSize(product1));
        assertEquals(true, BandArithmetic.areRastersEqualInSize(product1, "b1"));
        assertEquals(true, BandArithmetic.areRastersEqualInSize(product1, compatibleExpressions));
        assertEquals(true, BandArithmetic.areRastersEqualInSize(product1, "anotherBand"));
        assertEquals(false, BandArithmetic.areRastersEqualInSize(product1, incompatibleExpressions));
    }

    @Test
    public void testAreRastersEqualInSize_multipleProducts() throws ParseException {
        assertEquals(true, BandArithmetic.areRastersEqualInSize(new Product[]{product1}, 0));
        assertEquals(true, BandArithmetic.areRastersEqualInSize(new Product[]{product1, product2}, 0, "b2 + $2.b1"));
        assertEquals(true, BandArithmetic.areRastersEqualInSize(new Product[]{product1, product2}, 1, "$1.b2 + b1"));
        assertEquals(true, BandArithmetic.areRastersEqualInSize(new Product[]{product1, product2}, 1, "$1.b2 + $2.b1"));
        try {
            assertEquals(false, BandArithmetic.areRastersEqualInSize(new Product[]{product1}, 0, "b2 + $2.b1"));
            fail("Exception expected");
        } catch (ParseException e) {
            assertEquals("Undefined symbol '$2.b1'.", e.getMessage());
        }
    }

    @Test
    public void testAreRastersEqualInSize_TiePointGrid() throws ParseException {
        final TiePointGrid tiePointGrid = new TiePointGrid("tiePointGrid", 2, 2, 0, 0, 2, 1, new float[]{1f, 1f, 1f, 1f});
        product1.addTiePointGrid(tiePointGrid);

        assertEquals(true, BandArithmetic.areRastersEqualInSize(product1, "b1", "tiePointGrid"));
    }
}
