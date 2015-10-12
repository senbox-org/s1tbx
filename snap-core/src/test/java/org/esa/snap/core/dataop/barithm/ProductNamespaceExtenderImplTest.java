/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.Symbol;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 * @author Thomas Storm
 */
public class ProductNamespaceExtenderImplTest {

    @Test
    public void testPixelXYSymbols() throws Exception {

        Symbol pixelXSymbol = new ProductNamespaceExtenderImpl.PixelXSymbol("X");
        Symbol pixelYSymbol = new ProductNamespaceExtenderImpl.PixelYSymbol("Y");

        RasterDataEvalEnv rasterEnv;

        rasterEnv = new RasterDataEvalEnv(0, 0, 10, 10);
        assertEquals(0.5, pixelXSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(0.5, pixelYSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(0, 9, 10, 10);
        assertEquals(0.5, pixelXSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(9.5, pixelYSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 0, 10, 10);
        assertEquals(9.5, pixelXSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(0.5, pixelYSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 9, 10, 10);
        assertEquals(9.5, pixelXSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(9.5, pixelYSymbol.evalD(rasterEnv), 1E-6);
    }

    @Test
    public void testPixelLatLonSymbols() throws Exception {
        final Product product = new Product("name", "type", 10, 10);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 0, 0, 0.1, 0.1));
        Symbol pixelLatSymbol = new ProductNamespaceExtenderImpl.PixelLatSymbol("LAT", product.getSceneGeoCoding(), 10, 10);
        Symbol pixelLonSymbol = new ProductNamespaceExtenderImpl.PixelLonSymbol("LON", product.getSceneGeoCoding(), 10, 10);

        RasterDataEvalEnv rasterEnv;

        rasterEnv = new RasterDataEvalEnv(0, 0, 10, 10);
        assertEquals(0.0, pixelLatSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(0.0, pixelLonSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(0, 9, 10, 10);
        assertEquals(-0.9, pixelLatSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(0.0, pixelLonSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 0, 10, 10);
        assertEquals(0.0, pixelLatSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(0.9, pixelLonSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 9, 10, 10);
        assertEquals(-0.9, pixelLatSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(0.9, pixelLonSymbol.evalD(rasterEnv), 1E-6);
    }

    @Test
    public void testPixelLatLonSymbols_no_geo() throws Exception {
        final Product product = new Product("name", "type", 10, 10);

        Symbol pixelLatSymbol = new ProductNamespaceExtenderImpl.PixelLatSymbol("LAT", product.getSceneGeoCoding(), 10, 10);
        Symbol pixelLonSymbol = new ProductNamespaceExtenderImpl.PixelLonSymbol("LON", product.getSceneGeoCoding(), 10, 10);

        RasterDataEvalEnv rasterEnv;

        rasterEnv = new RasterDataEvalEnv(0, 0, 10, 10);
        assertEquals(Double.NaN, pixelLatSymbol.evalD(rasterEnv), 1E-6);
        assertEquals(Double.NaN, pixelLonSymbol.evalD(rasterEnv), 1E-6);
    }

    @Test
    public void testPixelTimeSymbol() throws Exception {
        final Product product = new Product("name", "type", 10, 10);
        ProductData.UTC startTime = ProductData.UTC.parse("01-Jan-2010 00:00:00");
        ProductData.UTC endTime = ProductData.UTC.parse("01-Jan-2010 10:00:00");

        product.setStartTime(startTime);
        product.setEndTime(endTime);
        Symbol pixelTimeSymbol = new ProductNamespaceExtenderImpl.PixelTimeSymbol("TIME", product);

        RasterDataEvalEnv rasterEnv = new RasterDataEvalEnv(0, 0, 10, 10);
        assertEquals(startTime.getMJD(), pixelTimeSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 0, 10, 10);
        assertEquals(startTime.getMJD(), pixelTimeSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(0, 9, 10, 10);
        assertEquals(endTime.getMJD(), pixelTimeSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 9, 10, 10);
        assertEquals(endTime.getMJD(), pixelTimeSymbol.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(-1, 4, 10, 10);
        double centralTime = (startTime.getMJD() * 5 + endTime.getMJD() * 4) / 9;
        assertEquals(centralTime, pixelTimeSymbol.evalD(rasterEnv), 1E-6);
    }

    @Test
    public void testPixelTimeSymbol_no_time() throws Exception {
        final Product product = new Product("name", "type", 10, 10);
        Symbol pixelTimeSymbol = new ProductNamespaceExtenderImpl.PixelTimeSymbol("TIME", product);

        assertTrue(Double.isNaN(pixelTimeSymbol.evalD(new RasterDataEvalEnv(-1, -1, -1, -1))));
        assertTrue(Double.isNaN(pixelTimeSymbol.evalD(new RasterDataEvalEnv(0, 0, 1, 1))));
    }
}
