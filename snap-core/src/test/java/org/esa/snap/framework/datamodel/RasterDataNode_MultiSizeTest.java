package org.esa.snap.framework.datamodel;

import org.junit.*;

import java.awt.geom.NoninvertibleTransformException;

import static org.junit.Assert.*;

public class RasterDataNode_MultiSizeTest {

    private Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("A", "B", 4, 8);
        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 4 - 1, 8 - 1, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 4 - 1, 8 - 1, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
        TiePointGrid lat2 = new TiePointGrid("lat2", 2, 2, 0, 0, 2 - 1, 4 - 1, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon2 = new TiePointGrid("lon2", 2, 2, 0, 0, 2 - 1, 4 - 1, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat2);
        product.addTiePointGrid(lon2);

//        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 4, 8, "X");
//        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 2, 4, "Y");
//        product.addBand(band1);
//        product.addBand(band2);
    }

    @Test
    public void testGetProductSampleInt() {
        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 4, 8, "X");
        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 2, 4, "Y");
        product.addBand(band1);
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 4; y++) {
                assertEquals(x + 1, band1.getProductSampleInt(x, y));
                assertEquals(y + 1, band2.getProductSampleInt(x, y));
            }
        }

        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(x + 1, band1.getProductSampleInt(x, y));
                assertEquals(1 + (y / 2), band2.getProductSampleInt(x, y));
            }
        }
    }

    @Test
    public void testGetProductSampleFloat() {
        Band band1 = new VirtualBand("B1", ProductData.TYPE_FLOAT64, 4, 8, "X");
        Band band2 = new VirtualBand("B2", ProductData.TYPE_FLOAT64, 2, 4, "Y");
        product.addBand(band1);
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 4; y++) {
                assertEquals(x + 0.5, band1.getProductSampleFloat(x, y), 1e-8);
                assertEquals(y + 0.5, band2.getProductSampleFloat(x, y), 1e-8);
            }
        }
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(x + 0.5, band1.getProductSampleFloat(x, y), 1e-8);
                assertEquals((y / 2) + 0.5, band2.getProductSampleFloat(x, y), 1e-8);
            }
        }
    }

    @Test
    @Ignore
    public void testIsProductPixelValid() throws NoninvertibleTransformException {
        final Band band1 = product.getBand("B1");
        final Band band2 = product.getBand("B2");
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");

        assertEquals(true, band1.isProductPixelValid(8, 15));
        assertEquals(false, band2.isProductPixelValid(8, 15));

        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));

        assertEquals(true, band1.isProductPixelValid(8, 15));
        assertEquals(true, band2.isProductPixelValid(8, 15));
    }

    @Test
    public void testGetProductPixelInt() {
        Band band1 = new Band("B1", ProductData.TYPE_INT32, 4, 8);
        band1.setRasterData(ProductData.createInstance(new int[]{1, 2, 3, 4,
                1, 2, 3, 4,
                1, 2, 3, 4,
                1, 2, 3, 4,
                1, 2, 3, 4,
                1, 2, 3, 4,
                1, 2, 3, 4,
                1, 2, 3, 4
        }));
        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT32, 2, 4, "Y");
        band2.setRasterData(ProductData.createInstance(new int[]{1, 1,
                2, 2,
                3, 3,
                4, 4
        }));
        product.addBand(band1);
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 4; y++) {
                assertEquals(x + 1, band1.getProductPixelInt(x, y));
                assertEquals(y + 1, band2.getProductPixelInt(x, y));
            }
        }

        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(x + 1, band1.getProductPixelInt(x, y));
                assertEquals(1 + (y / 2), band2.getProductPixelInt(x, y));
            }
        }
    }

    @Test
    public void testGetProductPixelFloat() {
        Band band1 = new Band("B1", ProductData.TYPE_FLOAT32, 4, 8);
        band1.setRasterData(ProductData.createInstance(new float[]{0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f,
                0.5f, 1.5f, 2.5f, 3.5f
        }));
        Band band2 = new VirtualBand("B2", ProductData.TYPE_FLOAT32, 2, 4, "Y");
        band2.setRasterData(ProductData.createInstance(new float[]{0.5f, 0.5f,
                1.5f, 1.5f,
                2.5f, 2.5f,
                3.5f, 3.5f
        }));
        product.addBand(band1);
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 4; y++) {
                assertEquals(x + 0.5, band1.getProductPixelFloat(x, y), 1e-8);
                assertEquals(y + 0.5, band2.getProductPixelFloat(x, y), 1e-8);
            }
        }
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(x + 0.5, band1.getProductPixelFloat(x, y), 1e-8);
                assertEquals((y / 2) + 0.5, band2.getProductPixelFloat(x, y), 1e-8);
            }
        }
    }

    @Test
    public void testGetProductPixelDouble() {
        Band band1 = new Band("B1", ProductData.TYPE_FLOAT64, 4, 8);
        band1.setRasterData(ProductData.createInstance(new double[]{0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5,
                0.5, 1.5, 2.5, 3.5
        }));
        Band band2 = new VirtualBand("B2", ProductData.TYPE_FLOAT64, 2, 4, "Y");
        band2.setRasterData(ProductData.createInstance(new double[]{0.5, 0.5,
                1.5, 1.5,
                2.5, 2.5,
                3.5, 3.5
        }));
        product.addBand(band1);
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 4; y++) {
                assertEquals(x + 0.5, band1.getProductPixelDouble(x, y), 1e-8);
                assertEquals(y + 0.5, band2.getProductPixelDouble(x, y), 1e-8);
            }
        }
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(x + 0.5, band1.getProductPixelDouble(x, y), 1e-8);
                assertEquals((y / 2) + 0.5, band2.getProductPixelDouble(x, y), 1e-8);
            }
        }
    }

    @Test
    public void testSetProductPixelInt() {
        Band band2 = new Band("B2", ProductData.TYPE_INT32, 2, 4);
        band2.setRasterData(ProductData.createInstance(new int[]{1, 1,
                2, 2,
                3, 3,
                4, 4
        }));
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(1 + (y / 2), band2.getProductPixelInt(x, y));
            }
        }
        band2.setProductPixelInt(1, 1, 5);
        band2.setProductPixelInt(2, 2, 6);
        band2.setProductPixelInt(0, 5, 7);
        band2.setProductPixelInt(3, 7, 8);
        assertEquals(5, band2.getPixelInt(0, 0));
        assertEquals(1, band2.getPixelInt(1, 0));
        assertEquals(2, band2.getPixelInt(0, 1));
        assertEquals(6, band2.getPixelInt(1, 1));
        assertEquals(7, band2.getPixelInt(0, 2));
        assertEquals(3, band2.getPixelInt(1, 2));
        assertEquals(4, band2.getPixelInt(0, 3));
        assertEquals(8, band2.getPixelInt(1, 3));
    }

    @Test
    public void testSetProductPixelFloat() {
        Band band2 = new Band("B2", ProductData.TYPE_FLOAT32, 2, 4);
        band2.setRasterData(ProductData.createInstance(new float[]{1.5f, 1.5f,
                2.5f, 2.5f,
                3.5f, 3.5f,
                4.5f, 4.5f
        }));
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(1.5 + (y / 2), band2.getProductPixelFloat(x, y), 1e-8);
            }
        }
        band2.setProductPixelFloat(1, 1, 5.5f);
        band2.setProductPixelFloat(2, 2, 6.5f);
        band2.setProductPixelFloat(0, 5, 7.5f);
        band2.setProductPixelFloat(3, 7, 8.5f);
        assertEquals(5.5f, band2.getPixelFloat(0, 0), 1e-8);
        assertEquals(1.5f, band2.getPixelFloat(1, 0), 1e-8);
        assertEquals(2.5f, band2.getPixelFloat(0, 1), 1e-8);
        assertEquals(6.5f, band2.getPixelFloat(1, 1), 1e-8);
        assertEquals(7.5f, band2.getPixelFloat(0, 2), 1e-8);
        assertEquals(3.5f, band2.getPixelFloat(1, 2), 1e-8);
        assertEquals(4.5f, band2.getPixelFloat(0, 3), 1e-8);
        assertEquals(8.5f, band2.getPixelFloat(1, 3), 1e-8);
    }

    @Test
    public void testSetProductPixelDouble() {
        Band band2 = new Band("B2", ProductData.TYPE_FLOAT64, 2, 4);
        band2.setRasterData(ProductData.createInstance(new double[]{1.5, 1.5,
                2.5, 2.5,
                3.5, 3.5,
                4.5, 4.5
        }));
        product.addBand(band2);
        final TiePointGrid lat2 = product.getTiePointGrid("lat2");
        final TiePointGrid lon2 = product.getTiePointGrid("lon2");
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(1.5 + (y / 2), band2.getProductPixelDouble(x, y), 1e-8);
            }
        }
        band2.setProductPixelDouble(1, 1, 5.5f);
        band2.setProductPixelDouble(2, 2, 6.5f);
        band2.setProductPixelDouble(0, 5, 7.5f);
        band2.setProductPixelDouble(3, 7, 8.5f);
        assertEquals(5.5f, band2.getPixelDouble(0, 0), 1e-8);
        assertEquals(1.5f, band2.getPixelDouble(1, 0), 1e-8);
        assertEquals(2.5f, band2.getPixelDouble(0, 1), 1e-8);
        assertEquals(6.5f, band2.getPixelDouble(1, 1), 1e-8);
        assertEquals(7.5f, band2.getPixelDouble(0, 2), 1e-8);
        assertEquals(3.5f, band2.getPixelDouble(1, 2), 1e-8);
        assertEquals(4.5f, band2.getPixelDouble(0, 3), 1e-8);
        assertEquals(8.5f, band2.getPixelDouble(1, 3), 1e-8);
    }

} 