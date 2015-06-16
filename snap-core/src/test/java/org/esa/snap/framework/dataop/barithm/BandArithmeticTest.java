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
package org.esa.snap.framework.dataop.barithm;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.*;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.SymbolFactory;
import org.esa.snap.framework.datamodel.*;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.Matcher;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.VarargCapturingMatcher;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static org.junit.Assert.*;

public class BandArithmeticTest {

    private Band targetBand;
    private Product product1;
    private Product product2;
    private int width = 4;
    private int height = 3;

    @Before
    public void setUp() throws FactoryException, TransformException {
        targetBand = new Band("b1n", ProductData.TYPE_UINT16, width, height);
        product1 = new Product("p1", "t", width, height);
        product1.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, 0, 10, 0.1, 0.1));
        product1.addBand("b1", ProductData.TYPE_FLOAT32);
        product1.addBand("b2", ProductData.TYPE_UINT8);
        product1.setRefNo(1);
        product2 = new Product("p2", "t", width, height);
        product2.setGeoCoding(null);
        product2.addBand("b1", ProductData.TYPE_FLOAT32);
        product2.setRefNo(2);
    }

    @Test
    public void testComputeBandWithScaling() throws IOException,
            ParseException {
        targetBand.setScalingFactor(0.05);
        targetBand.setScalingOffset(-7);
        product1.getBand("b1").setDataElems(new float[]{
                2, 3, 4, 5,
                6, 7, 8, 9,
                10, 11, 12, 13
        });
        product1.setModified(false);
        targetBand.computeBand("b1", null, new Product[]{product1}, 0, false, false, 0, ProgressMonitor.NULL);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final float expected = 2 + y * width + x;
                assertEquals("at index(x,y) = " + x + "," + y,
                             expected,
                             targetBand.getPixelFloat(x, y), 1e-5);
            }
        }
    }

    @Test
    public void testComputeBandWithUshort() throws IOException,
            ParseException {
        product1.getBand("b1").setDataElems(new float[]{
                2.1f, 3.2f, 4.3f, 5.4f,
                6.3f, 6.69f, 8.32f, 8.8f,
                10.2f, 11.1f, 11.9f, 13.3f
        });
        product1.setModified(false);
        targetBand.computeBand("b1", null, new Product[]{product1}, 0, false, false, 0, ProgressMonitor.NULL);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final float expected = 2 + y * width + x;
                assertEquals("at index(x,y) = " + x + "," + y,
                             expected,
                             targetBand.getPixelFloat(x, y), 1e-5);
            }
        }
    }

    @Test
    public void testCreateDefaultNamespaceWithOneProduct() {
        Namespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{product1}, 0);

        assertSame(namespace.resolveSymbol("X"), BandArithmetic.PIXEL_X_SYMBOL);
        assertSame(namespace.resolveSymbol("Y"), BandArithmetic.PIXEL_Y_SYMBOL);
        assertThat(namespace.resolveSymbol("LAT"), instanceOf(MoreFuncs.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("LON"), instanceOf(MoreFuncs.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("TIME"), instanceOf(MoreFuncs.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("MJD"), instanceOf(MoreFuncs.PixelTimeSymbol.class)); // compatibility
        assertThat(namespace.resolveSymbol("b1"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("b1.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b1.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b1.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertThat(namespace.resolveSymbol("b2"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("b2.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b2.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("b2.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertNull(namespace.resolveSymbol("b3"));

        assertNull(namespace.resolveSymbol("$1.b1"));
    }

    @Test
    public void testCreateDefaultNamespaceWithMultipleProducts() {
        Namespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{product1, product2}, 0);

        assertSame(namespace.resolveSymbol("X"), BandArithmetic.PIXEL_X_SYMBOL);
        assertSame(namespace.resolveSymbol("Y"), BandArithmetic.PIXEL_Y_SYMBOL);
        assertThat(namespace.resolveSymbol("LAT"), instanceOf(MoreFuncs.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("LON"), instanceOf(MoreFuncs.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("TIME"), instanceOf(MoreFuncs.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("MJD"), instanceOf(MoreFuncs.PixelTimeSymbol.class)); // compatibility
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
        assertNull(namespace.resolveSymbol("$1.Y"));
        assertThat(namespace.resolveSymbol("$1.LAT"), instanceOf(MoreFuncs.PixelLatSymbol.class));
        assertThat(namespace.resolveSymbol("$1.LON"), instanceOf(MoreFuncs.PixelLonSymbol.class));
        assertThat(namespace.resolveSymbol("$1.TIME"), instanceOf(MoreFuncs.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("$1.MJD"), instanceOf(MoreFuncs.PixelTimeSymbol.class)); // compatibility
        assertThat(namespace.resolveSymbol("$1.b1"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("$1.b1.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b1.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b1.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertThat(namespace.resolveSymbol("$1.b2"), instanceOf(RasterDataSymbol.class));
        assertThat(namespace.resolveSymbol("$1.b2.solar_flux"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b2.spectral_wavelength"), instanceOf(SymbolFactory.ConstantD.class));
        assertThat(namespace.resolveSymbol("$1.b2.spectral_band_index"), instanceOf(SymbolFactory.ConstantI.class));
        assertNull(namespace.resolveSymbol("$1.b3"));

        assertNull(namespace.resolveSymbol("$2.X"));
        assertNull(namespace.resolveSymbol("$2.Y"));
        assertNull(namespace.resolveSymbol("$2.LAT"));
        assertNull(namespace.resolveSymbol("$2.LON"));
        assertThat(namespace.resolveSymbol("$2.TIME"), instanceOf(MoreFuncs.PixelTimeSymbol.class));
        assertThat(namespace.resolveSymbol("$2.MJD"), instanceOf(MoreFuncs.PixelTimeSymbol.class)); // compatibility
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
}
