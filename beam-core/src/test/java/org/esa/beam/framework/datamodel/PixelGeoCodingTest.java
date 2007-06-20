/*
 * $Id: PixelGeoCodingTest.java,v 1.3 2007/01/25 10:10:28 marcop Exp $
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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.io.IOException;

public class PixelGeoCodingTest extends TestCase {

    private static final int S = 4;
    private static final int GW = 3;
    private static final int GH = 5;
    private static final int PW = (GW - 1) * S + 1;
    private static final int PH = (GH - 1) * S + 1;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    public PixelGeoCodingTest(String testName) {
        super(testName);
    }

    public void testIllegalConstructorCalls() throws IOException {
        Product product;
        Band b1, b2;

        product = createProduct();
        b1 = product.getBand("latBand");
        b2 = product.getBand("lonBand");

        // Should be ok
        assertNotNull(b1);
        assertNotNull(b2);
        testIllegalArgumentExceptionNotThrownByConstructor(b1, b2, null, 7);
        // b1 is null
        testIllegalArgumentExceptionThrownByConstructor(null, b2, null, 7);
        // b2 is null
        testIllegalArgumentExceptionThrownByConstructor(b1, null, null, 7);
        // b2 not attached to product
        product.removeBand(b2);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 7);
        // b1 not attached to product
        product.addBand(b2);
        product.removeBand(b1);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 7);

        // illegal search radius'
        product.addBand(b1);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 0);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, -6);

        // illegal raster size (==1)
        product = new Product("test", "test", 1, 1);
        b1 = product.addBand("x", ProductData.TYPE_FLOAT32);
        b2 = product.addBand("y", ProductData.TYPE_FLOAT32);
        b1.setRasterData(ProductData.createInstance(new float[1]));
        b2.setRasterData(ProductData.createInstance(new float[1]));
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 5);

        // legal raster size  (>1)
        product = new Product("test", "test", 2, 2);
        b1 = product.addBand("x", ProductData.TYPE_FLOAT32);
        b2 = product.addBand("y", ProductData.TYPE_FLOAT32);
        b1.setRasterData(ProductData.createInstance(new float[4]));
        b2.setRasterData(ProductData.createInstance(new float[4]));
        testIllegalArgumentExceptionNotThrownByConstructor(b1, b2, null, 5);
    }

    private static void testIllegalArgumentExceptionNotThrownByConstructor(Band b1, Band b2, String validMask,
                                                                           int searchRadius) {
        try {
            new PixelGeoCoding(b1, b2, validMask, searchRadius, ProgressMonitor.NULL);
        } catch (IOException e) {
            fail();
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    private static void testIllegalArgumentExceptionThrownByConstructor(Band b1, Band b2, String validMask,
                                                                        int searchRadius) {
        try {
            new PixelGeoCoding(b1, b2, validMask, searchRadius, ProgressMonitor.NULL);
            fail();
        } catch (IOException e) {
            fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    public void testGetGeoPos() throws IOException {
        Product product = createProduct();
        TiePointGeoCoding oldGeoCoding = (TiePointGeoCoding) product.getGeoCoding();
        PixelGeoCoding newGeoCoding = new PixelGeoCoding(product.getBand("latBand"),
                                                         product.getBand("lonBand"), null, 5, ProgressMonitor.NULL);
        product.setGeoCoding(newGeoCoding);

        String gp;

        gp = new GeoPos(oldGeoCoding.getLatGrid().getTiePoints()[0],
                        oldGeoCoding.getLonGrid().getTiePoints()[0]).toString();
        assertEquals(gp, oldGeoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null).toString());
        assertEquals(gp, newGeoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null).toString());

        gp = new GeoPos(oldGeoCoding.getLatGrid().getTiePoints()[GW - 1],
                        oldGeoCoding.getLonGrid().getTiePoints()[GW - 1]).toString();
        assertEquals(gp, oldGeoCoding.getGeoPos(new PixelPos(PW - 0.5f, 0.5f), null).toString());
        assertEquals(gp, newGeoCoding.getGeoPos(new PixelPos(PW - 0.5f, 0.5f), null).toString());

        gp = new GeoPos(oldGeoCoding.getLatGrid().getTiePoints()[GW * (GH - 1)],
                        oldGeoCoding.getLonGrid().getTiePoints()[GW * (GH - 1)]).toString();
        assertEquals(gp, oldGeoCoding.getGeoPos(new PixelPos(0.5f, PH - 0.5f), null).toString());
        assertEquals(gp, newGeoCoding.getGeoPos(new PixelPos(0.5f, PH - 0.5f), null).toString());

        gp = new GeoPos(oldGeoCoding.getLatGrid().getTiePoints()[GW * GH - 1],
                        oldGeoCoding.getLonGrid().getTiePoints()[GW * GH - 1]).toString();
        assertEquals(gp, oldGeoCoding.getGeoPos(new PixelPos(PW - 0.5f, PH - 0.5f), null).toString());
        assertEquals(gp, newGeoCoding.getGeoPos(new PixelPos(PW - 0.5f, PH - 0.5f), null).toString());
    }

    public void testThatPixelPosEstimatorIsAlsoTransferred() throws IOException {
        Product sourceProduct = createProduct();
        PixelGeoCoding newGeoCoding = new PixelGeoCoding(sourceProduct.getBand("latBand"),
                                                         sourceProduct.getBand("lonBand"), null, 5,
                                                         ProgressMonitor.NULL);
        sourceProduct.setGeoCoding(newGeoCoding);

        Product targetProduct = createProduct();
        targetProduct.setGeoCoding(null);   // remove geo-coding of target product

        sourceProduct.transferGeoCodingTo(targetProduct, null);

        PixelGeoCoding targetGC = (PixelGeoCoding) targetProduct.getGeoCoding();
        assertNotNull(targetGC.getPixelPosEstimator());
    }

    private Product createProduct() {
        Product product = new Product("test", "test", PW, PH);

        TiePointGrid latGrid = new TiePointGrid("latGrid", GW, GH, 0.5f, 0.5f, S, S, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", GW, GH, 0.5f, 0.5f, S, S, createLonGridData());

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        Band latBand = product.addBand("latBand", ProductData.TYPE_FLOAT32);
        Band lonBand = product.addBand("lonBand", ProductData.TYPE_FLOAT32);

        latBand.setRasterData(ProductData.createInstance(createBandData(latGrid)));
        lonBand.setRasterData(ProductData.createInstance(createBandData(lonGrid)));

        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));

        return product;
    }

    private float[] createLatGridData() {
        return createGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createGridData(LON_1, LON_2);
    }

    private static float[] createBandData(TiePointGrid grid) {
        float[] floats = new float[PW * PH];
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                floats[y * PW + x] = grid.getPixelFloat(x, y);
            }
        }
        return floats;
    }

    private static float[] createGridData(float lon0, float lon1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1f);
                float y = j / (GH - 1f);
                floats[j * GW + i] = lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y;
            }
        }

        return floats;
    }

}
