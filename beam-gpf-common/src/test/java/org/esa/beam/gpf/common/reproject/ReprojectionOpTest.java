package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class ReprojectionOpTest {
    private static final String WGS84_CODE = "EPSG:4326";
    private static final String UTM33N_CODE = "EPSG:32633";
    private static final String UTM33N_WKT = "PROJCS[\"WGS 84 / UTM zone 33N\"," +
    		"GEOGCS[\"WGS 84\"," +
    		"  DATUM[\"World Geodetic System 1984\"," +
    		"    SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]]," +
    		"    AUTHORITY[\"EPSG\",\"6326\"]]," +
    		"  PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]]," +
    		"  UNIT[\"degree\", 0.017453292519943295]," +
    		"  AXIS[\"Geodetic longitude\", EAST]," +
    		"  AXIS[\"Geodetic latitude\", NORTH]," +
    		"  AUTHORITY[\"EPSG\",\"4326\"]]," +
    		"PROJECTION[\"Transverse Mercator\", AUTHORITY[\"EPSG\",\"9807\"]]," +
    		"PARAMETER[\"central_meridian\", 15.0]," +
    		"PARAMETER[\"latitude_of_origin\", 0.0]," +
    		"PARAMETER[\"scale_factor\", 0.9996]," +
    		"PARAMETER[\"false_easting\", 500000.0]," +
    		"PARAMETER[\"false_northing\", 0.0]," +
    		"UNIT[\"m\", 1.0]," +
    		"AXIS[\"Easting\", EAST]," +
    		"AXIS[\"Northing\", NORTH]," +
    		"AUTHORITY[\"EPSG\",\"32633\"]]";
    private static final String BAND_NAME = "data";

    private static final float[] LATS = new float[]{
            50.0f, 50.0f,
            30.0f, 30.0f
    };

    private static final float[] LONS = new float[]{
            6.0f, 26.0f,
            6.0f, 26.0f
    };

    private static Product sourceProduct;

    @BeforeClass
    public static void setup() {
        sourceProduct = new Product("source", "t", 50, 50);
        final TiePointGrid latGrid = new TiePointGrid("latGrid", 2, 2, 0.5f, 0.5f, 49, 49, LATS);
        final TiePointGrid lonGrid = new TiePointGrid("lonGrid", 2, 2, 0.5f, 0.5f, 49, 49, LONS);
        sourceProduct.addTiePointGrid(latGrid);
        sourceProduct.addTiePointGrid(lonGrid);
        sourceProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        Band dataBand = sourceProduct.addBand(BAND_NAME, ProductData.TYPE_INT32);
        dataBand.setRasterData(createDataFor(dataBand));
        dataBand.setSynthetic(true);
//        try {
//            final String path = "C:\\Dokumente und Einstellungen\\Marco Peters\\Eigene Dateien\\EOData\\temp\\TestProd_5050.dim";
//            ProductIO.writeProduct(sourceProduct, path, ProductIO.DEFAULT_FORMAT_NAME);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Test
    public void testGeoLatLon() throws IOException {
        final ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setEpsgCode(WGS84_CODE);
        final Product targetPoduct = repOp.getTargetProduct();
        assertNotNull(targetPoduct);
        // because source is rectangular the size of source is preserved
        assertEquals(50, targetPoduct.getSceneRasterWidth());
        assertEquals(50, targetPoduct.getSceneRasterHeight());
        assertNotNull(targetPoduct.getGeoCoding());

        testPixelValue(targetPoduct, 23.5f, 13.5f, 299);
    }


    @Test
    public void testUTM() throws IOException {
        final ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setResamplingName("Nearest");  // setting Nearest cause a particualr value is checked
        repOp.setEpsgCode(UTM33N_CODE);
        final Product targetPoduct = repOp.getTargetProduct();
        
        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299);
    }
    
    @Test
    public void testUTMWithWktText() throws IOException {
        final ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setResamplingName("Nearest");  // setting Nearest cause a particualr value is checked
        repOp.setWkt(UTM33N_WKT);
        final Product targetPoduct = repOp.getTargetProduct();
        
        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299);
    }


    @Test
    public void testSpecifyingTargetDimension() throws IOException {
        final int width = 200;
        final int height = 300;
        final ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setEpsgCode(WGS84_CODE);
        repOp.setWidth(width);
        repOp.setHeight(height);
        final Product targetPoduct = repOp.getTargetProduct();
        assertNotNull(targetPoduct);
        assertEquals(width, targetPoduct.getSceneRasterWidth());
        assertEquals(height, targetPoduct.getSceneRasterHeight());

        testPixelValue(targetPoduct, 23.5f, 13.5f, 299);
    }

    @Test
    public void testSpecifyingPixelSize() throws IOException {
        final int sizeX = 5; // degree
        final int sizeY = 10;// degree
        final ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setEpsgCode(WGS84_CODE);
        repOp.setPixelSizeX(sizeX);
        repOp.setPixelSizeY(sizeY);
        final Product targetPoduct = repOp.getTargetProduct();
        assertNotNull(targetPoduct);
        // 20° Width / 5° PixelSizeX = 4 SceneWidth
        assertEquals(4, targetPoduct.getSceneRasterWidth());
        // 20° Height / 10° PixelSizeX = 2 SceneHeight
        assertEquals(2, targetPoduct.getSceneRasterHeight());
    }
    
    @Test
    public void testSpecifyingReferencing() throws IOException {
        final ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setEpsgCode(WGS84_CODE);
        repOp.setReferencePixelX(0.5);
        repOp.setReferencePixelY(0.5);
        repOp.setEasting(9);        // just move it 3° degrees eastward
        repOp.setNorthing(52);      // just move it 2° degrees up
        repOp.setOrientation(0);
        final Product targetPoduct = repOp.getTargetProduct();
        assertNotNull(targetPoduct);
        final GeoPos geoPos = targetPoduct.getGeoCoding().getGeoPos(new PixelPos(0.5f, 0.5f), null);
        assertEquals(new GeoPos(52.0f, 9.0f), geoPos);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299);
    }

    @Test
    public void testIncludeTiePointGrids() throws Exception {
        ReprojectionOp repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setEpsgCode(WGS84_CODE);
        Product targetPoduct = repOp.getTargetProduct();
        TiePointGrid[] tiePointGrids = targetPoduct.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);
        Band latGrid = targetPoduct.getBand("latGrid");
        assertNotNull(latGrid);
        
        repOp = new ReprojectionOp();
        repOp.setSourceProduct(sourceProduct);
        repOp.setEpsgCode(WGS84_CODE);
        repOp.setIncludeTiePointGrids(false);
        targetPoduct = repOp.getTargetProduct();
        tiePointGrids = targetPoduct.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);
        latGrid = targetPoduct.getBand("latGrid");
        assertNull(latGrid);
    }

    private void testPixelValue(Product targetPoduct, float sourceX, float sourceY, int expectedPixelValue) throws IOException {
        final Band sourceBand = sourceProduct.getBand(BAND_NAME);
        final Band targetBand = targetPoduct.getBand(BAND_NAME);
        final PixelPos sourcePP = new PixelPos(sourceX, sourceY);        // pixelValue = 23 * 13 = 299
        final GeoPos geoPos = sourceBand.getGeoCoding().getGeoPos(sourcePP, null);
        final PixelPos targetPP = targetBand.getGeoCoding().getPixelPos(geoPos, null);
        final int[] pixels = new int[1];
        targetBand.readPixels((int) targetPP.x, (int) targetPP.y, 1,1, pixels);
        assertEquals(expectedPixelValue, pixels[0]);
    }

    private static ProductData createDataFor(Band dataBand) {
        final int width = dataBand.getSceneRasterWidth();
        final int height = dataBand.getSceneRasterHeight();
        final ProductData data = ProductData.createInstance(dataBand.getDataType(), width * height);
        for (int y = 0; y < height; y++) {
            final int line = y * width;
            for (int x = 0; x < width; x++) {
                data.setElemIntAt(line + x, x * y);
            }
        }
        return data;
    }
}
