package org.esa.beam.jai;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.jai.SingleBandedSampleModel;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.HashMap;

public class BandOpImageTest extends TestCase {
    private static final int IMAGE_W = 10;
    private static final int IMAGE_H = 10;
    private static final int TILE_SIZE = 6;

    

    public void testThatAwtRastersAreWrittenCorrectly() throws IOException {
        ProductFactory productFactory = new ProductFactory();
        Product p = productFactory.readProductNodes(null, null);

        assertEquals(0, f(0));
        assertEquals(1, f(1));
        assertEquals(127, f(127));
        assertEquals(-1, f(255));
        assertEquals(-2, f(254));
        assertEquals(-128, f(128));

        testThatAwtRastersAreWrittenCorrectly(p, "B_FLOAT64", DataBuffer.TYPE_DOUBLE);
        testThatAwtRastersAreWrittenCorrectly(p, "B_FLOAT32", DataBuffer.TYPE_FLOAT);

        testThatAwtRastersAreWrittenCorrectly(p, "B_UINT32", DataBuffer.TYPE_INT);
        testThatAwtRastersAreWrittenCorrectly(p, "B_UINT16", DataBuffer.TYPE_USHORT);
        testThatAwtRastersAreWrittenCorrectly(p, "B_UINT8", DataBuffer.TYPE_BYTE);

        testThatAwtRastersAreWrittenCorrectly(p, "B_INT32", DataBuffer.TYPE_INT);
        testThatAwtRastersAreWrittenCorrectly(p, "B_INT16", DataBuffer.TYPE_SHORT);
        // todo - conversion from ProductData.B_INT8 --> DataBuffer.TYPE_BYTE still fails, see also [BEAM-1147] (nf - 20100527)
        testThatAwtRastersAreWrittenCorrectly(p, "B_INT8", DataBuffer.TYPE_BYTE);
    }

    private int f(int i) {
        // return (byte) i;
        // return i | 0x80;
        return (i & 0x7f) - (i & 0x80);
    }

    private void testThatAwtRastersAreWrittenCorrectly(Product p, String bandName, int dataBufferType) {
        Band band = p.getBand(bandName);

        MultiLevelImage image = band.getGeophysicalImage();

        SampleModel sampleModel = image.getSampleModel();
        assertTrue(sampleModel instanceof SingleBandedSampleModel);
        SingleBandedSampleModel sm = (SingleBandedSampleModel) sampleModel;
        assertEquals(dataBufferType, sm.getDataType());

        Raster[] rasters = image.getTiles();
        assertEquals(4, rasters.length);

        double[] coeff = ((ProductFactory) p.getProductReader()).getCoeff(band);

        testTileData(image.getTile(0, 0), coeff);
        testTileData(image.getTile(1, 0), coeff);
        testTileData(image.getTile(0, 1), coeff);
        testTileData(image.getTile(1, 1), coeff);
    }

    private void testTileData(Raster tile, double[] coeff) {
        DataBuffer db = tile.getDataBuffer();
        assertEquals(TILE_SIZE * TILE_SIZE, db.getSize());
        int pdIndex = 0; // product data index (points to valid product data elements)
        for (int dbIndex = 0; dbIndex < db.getSize(); dbIndex++) {
            int x = tile.getMinX() + dbIndex % tile.getWidth();
            int y = tile.getMinY() + dbIndex / tile.getWidth();
            double actual = db.getElemDouble(dbIndex);
            if (x >= 0 && x < IMAGE_W && y >= 0 && y < IMAGE_H) {
                double expected = coeff[0] * pdIndex + coeff[1];
                assertEquals("Inside image bounds: dataBuffer.getElemDouble(" + dbIndex + ")", expected, actual, 1.0e-5);
                pdIndex++;
            } else {
                double expected = 0.0;
                assertEquals("Outside image bounds: dataBuffer.getElemDouble(" + dbIndex + ")", expected, actual, 1.0e-5);
            }
        }
    }

    private static class ProductFactory extends AbstractProductReader {
        HashMap<Band, double[]> coeffs = new HashMap<Band, double[]>(10);

        public ProductFactory() {
            super(null);
        }

        @Override
        protected Product readProductNodesImpl() throws IOException {
            Product p = new Product("N", "T", IMAGE_W, IMAGE_H);
            p.setPreferredTileSize(TILE_SIZE, TILE_SIZE);
            createBand(p, "B_INT8", ProductData.TYPE_INT8, 1.0, -8.0);
            createBand(p, "B_UINT8", ProductData.TYPE_UINT8, 1.0, 8.0);
            createBand(p, "B_INT16", ProductData.TYPE_INT16, 1.0, -16.0);
            createBand(p, "B_UINT16", ProductData.TYPE_UINT16, 1.0, 16.0);
            createBand(p, "B_INT32", ProductData.TYPE_INT32, 1.0, -32.0);
            createBand(p, "B_UINT32", ProductData.TYPE_UINT32, 1.0, 32.0);
            createBand(p, "B_FLOAT32", ProductData.TYPE_FLOAT32, 1.0, 32.5);
            createBand(p, "B_FLOAT64", ProductData.TYPE_FLOAT64, 1.0, 64.5);
            return p;
        }

        private Band createBand(Product p, String bandName, int productDataType, double a, double b) {
            Band b1 = p.addBand(bandName, productDataType);
            coeffs.put(b1, new double[]{a, b});
            return b1;
        }

        double[] getCoeff(Band band) {
            return coeffs.get(band);
        }


        @Override
        protected void readBandRasterDataImpl(int sourceOffsetX,
                                              int sourceOffsetY,
                                              int sourceWidth,
                                              int sourceHeight,
                                              int sourceStepX,
                                              int sourceStepY,
                                              Band destBand,
                                              int destOffsetX,
                                              int destOffsetY,
                                              int destWidth,
                                              int destHeight,
                                              ProductData destBuffer,
                                              ProgressMonitor pm) throws IOException {
            double[] coeff = getCoeff(destBand);
            for (int i = 0; i < destBuffer.getNumElems(); i++) {
                destBuffer.setElemDoubleAt(i, coeff[0] * i + coeff[1]);
            }
        }
    }
}
