package org.esa.snap.core.datamodel;

import org.geotools.referencing.CRS;
import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.*;

public class PixelGeoCoding2_ImageGeometryTest {

    private static final int S = 2100;
    private static final int GW = 3;
    private static final int GH = 5;
    private static final int PW = (GW - 1) * S + 1;
    private static final int PH = (GH - 1) * S + 1;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    @Test
    public void testTargetGeometryCreation() throws Exception {
        Product pixelGcProduct = createProduct();
        pixelGcProduct.setSceneGeoCoding(new PixelGeoCoding2(pixelGcProduct.getBand("latBand"),
                                                             pixelGcProduct.getBand("lonBand"), null, 2));

        ImageGeometry pixelGcGeometry = ImageGeometry.createTargetGeometry(pixelGcProduct, CRS.decode("EPSG:32632"),
                                                                           null, null, null, null,
                                                                           null, null, null,
                                                                           null, null);

        Product tieGcProduct = createProduct();
        ImageGeometry tieGcGeometry = ImageGeometry.createTargetGeometry(tieGcProduct, CRS.decode("EPSG:32632"),
                                                                         null, null, null, null,
                                                                         null, null, null,
                                                                         null, null);

        Rectangle pixelGcRect = pixelGcGeometry.getImageRect();
        Rectangle tieGcRect = tieGcGeometry.getImageRect();
        assertEquals(tieGcRect.getWidth(), pixelGcRect.getWidth(), 1);
        assertEquals(tieGcRect.getHeight(), pixelGcRect.getHeight(),0);
    }


    private Product createProduct() {
        Product product = new Product("test", "test", PW, PH);

        TiePointGrid latGrid = new TiePointGrid("latGrid", GW, GH, 0.5, 0.5, S, S, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", GW, GH, 0.5, 0.5, S, S, createLonGridData());

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        Band latBand = product.addBand("latBand", ProductData.TYPE_FLOAT32);
        Band lonBand = product.addBand("lonBand", ProductData.TYPE_FLOAT32);

        latBand.setRasterData(ProductData.createInstance(createBandData(latGrid)));
        lonBand.setRasterData(ProductData.createInstance(createBandData(lonGrid)));

        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        return product;
    }

    private float[] createLatGridData() {
        return createLatGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createLonGridData(LON_1, LON_2);
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

    private static float[] createLatGridData(float lat0, float lat1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1.0f);
                float y = j / (GH - 1.0f);
                floats[j * GW + i] = lat0 + (lat1 - lat0) * y * y + 0.1f * (lat1 - lat0) * x * x;
            }
        }

        return floats;
    }

    private static float[] createLonGridData(float lon0, float lon1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1.0f);
                float y = j / (GH - 1.0f);
                final int index = j * GW + i;
                floats[(index)] = lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y;
            }
        }

        return floats;
    }
}