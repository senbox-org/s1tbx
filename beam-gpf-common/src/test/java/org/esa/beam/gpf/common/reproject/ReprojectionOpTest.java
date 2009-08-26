package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ReprojectionOpTest {
    private static final String WGS84_CODE = "EPSG:4326";
    private static final String UTM33N_CODE = "EPSG:32633";
    @SuppressWarnings({"StringConcatenation"})
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
    
    private static File wktFile;

    private static final float[] LATS = new float[]{
            50.0f, 50.0f,
            30.0f, 30.0f
    };

    private static final float[] LONS = new float[]{
            6.0f, 26.0f,
            6.0f, 26.0f
    };

    private static Product sourceProduct;
    private static OperatorSpi spi;
    
    private final Map<String, Object> parameterMap = new HashMap<String, Object>(5);

    @BeforeClass
    public static void setup() throws URISyntaxException {
        spi = new ReprojectionOp.Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(spi);
        
        wktFile = new File(ReprojectionOpTest.class.getResource("test.wkt").toURI());
        
        sourceProduct = new Product("source", "t", 50, 50);
        final TiePointGrid latGrid = new TiePointGrid("latGrid", 2, 2, 0.5f, 0.5f, 49, 49, LATS);
        final TiePointGrid lonGrid = new TiePointGrid("lonGrid", 2, 2, 0.5f, 0.5f, 49, 49, LONS);
        sourceProduct.addTiePointGrid(latGrid);
        sourceProduct.addTiePointGrid(lonGrid);
        sourceProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        Band dataBand = sourceProduct.addBand(BAND_NAME, ProductData.TYPE_FLOAT32);
        dataBand.setRasterData(createDataFor(dataBand));
        dataBand.setSynthetic(true);
    }
    
    @AfterClass
    public static void tearDown() {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.removeOperatorSpi(spi);
    }

    @Test
    public void testGeoLatLon() throws IOException {
        parameterMap.put("crsCode", WGS84_CODE);
        final Product targetPoduct = createReprojectedProduct();

        assertNotNull(targetPoduct);
        // because source is rectangular the size of source is preserved
        assertEquals(50, targetPoduct.getSceneRasterWidth());
        assertEquals(50, targetPoduct.getSceneRasterHeight());
        assertNotNull(targetPoduct.getGeoCoding());

        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testUTMWithWktText() throws IOException {
        parameterMap.put("wkt", UTM33N_WKT);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testWithWktFile() {
        parameterMap.put("wktFile", wktFile);
        final Product targetPoduct = createReprojectedProduct();

        assertNotNull(targetPoduct);
    }
    
    @Test
    public void testWithCollocationProduct() {
        Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        parameterMap.put("crsCode", "AUTO:42002");
        final Product collocationProduct = createReprojectedProduct(productMap);

        productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        productMap.put("collocate", collocationProduct);
        parameterMap.remove("crsCode");
        final Product targetProduct = createReprojectedProduct(productMap);

        assertNotNull(targetProduct);
    }
    
    @Test(expected = OperatorException.class)
    public void testEmptyParameterMap() {
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_epsgCode() {
        parameterMap.put("wkt", UTM33N_WKT);
        parameterMap.put("crsCode", UTM33N_CODE);
        createReprojectedProduct();
    }
    
    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_wktFile() {
        parameterMap.put("wkt", UTM33N_WKT);
        parameterMap.put("wktFile", wktFile);
        createReprojectedProduct();
    }
    
    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_collocateProduct() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        productMap.put("collocate", sourceProduct);
        parameterMap.put("wkt", UTM33N_WKT);
        createReprojectedProduct(productMap);
    }

    @Test(expected = OperatorException.class)
    public void testUnknownResamplingMethode() {
        parameterMap.put("resampling", "Super_Duper_Resampling");
        createReprojectedProduct();
    }
    
    @Test(expected = OperatorException.class)
    public void testMissingPixelSizeY() {
        parameterMap.put("pixelSizeX", 0.024);
        createReprojectedProduct();
    }
    
    @Test(expected = OperatorException.class)
    public void testMissingPixelSizeX() {
        parameterMap.put("pixelSizeY", 0.024);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingPixelX() {
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 1234.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingpixelY() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("easting", 1234.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }
    

    @Test(expected = OperatorException.class)
    public void testMissingReferencingNorthing() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 1234.5);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingEasting() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }
    
    @Test
    public void testUTM() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testUTM_Bilinear() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        parameterMap.put("resampling", "Bilinear");
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        assertNotNull(targetPoduct.getGeoCoding());
        // 299, 312
        // 322, 336
        // interpolated = 317.25 for pixel (24, 14)
        testPixelValue(targetPoduct, 24.0f, 14.0f, 317.25, 1.0e-2);
    }

    @Test
    public void testSpecifyingTargetDimension() throws IOException {
        final int width = 200;
        final int height = 300;
        parameterMap.put("crsCode", WGS84_CODE);
        parameterMap.put("width", width);
        parameterMap.put("height", height);
        final Product targetPoduct = createReprojectedProduct();

        assertNotNull(targetPoduct);
        assertEquals(width, targetPoduct.getSceneRasterWidth());
        assertEquals(height, targetPoduct.getSceneRasterHeight());

        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }

    @Test
    public void testSpecifyingPixelSize() throws IOException {
        final double sizeX = 5; // degree
        final double sizeY = 10;// degree
        parameterMap.put("crsCode", WGS84_CODE);
        parameterMap.put("pixelSizeX", sizeX);
        parameterMap.put("pixelSizeY", sizeY);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        // 20° Width / 5° PixelSizeX = 4 SceneWidth
        assertEquals(4, targetPoduct.getSceneRasterWidth());
        // 20° Height / 10° PixelSizeX = 2 SceneHeight
        assertEquals(2, targetPoduct.getSceneRasterHeight());
    }
    
    @Test
    public void testSpecifyingReferencing() throws IOException {
        parameterMap.put("crsCode", WGS84_CODE);
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 9.0);   // just move it 3° degrees eastward
        parameterMap.put("northing", 52.0); // just move it 2° degrees up
        parameterMap.put("orientation", 0.0);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        final GeoPos geoPos = targetPoduct.getGeoCoding().getGeoPos(new PixelPos(0.5f, 0.5f), null);
        assertEquals(new GeoPos(52.0f, 9.0f), geoPos);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }

    @Test
    public void testIncludeTiePointGrids() throws Exception {
        parameterMap.put("crsCode", WGS84_CODE);
        Product targetPoduct = createReprojectedProduct();
        
        TiePointGrid[] tiePointGrids = targetPoduct.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);
        Band latGrid = targetPoduct.getBand("latGrid");
        assertNotNull(latGrid);
        
        parameterMap.put("includeTiePointGrids", false);
        targetPoduct = createReprojectedProduct();
        tiePointGrids = targetPoduct.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);
        latGrid = targetPoduct.getBand("latGrid");
        assertNull(latGrid);
    }
    
    private Product createReprojectedProduct(Map<String, Product> sourceMap) {
        String operatorName = OperatorSpi.getOperatorAlias(ReprojectionOp.class);
        return GPF.createProduct(operatorName, parameterMap, sourceMap);
    }
    
    private Product createReprojectedProduct() {
        String operatorName = OperatorSpi.getOperatorAlias(ReprojectionOp.class);
        return GPF.createProduct(operatorName, parameterMap, sourceProduct);
    }

    private void testPixelValue(Product targetPoduct, float sourceX, float sourceY, double expectedPixelValue, double delta) throws IOException {
        final Band sourceBand = sourceProduct.getBand(BAND_NAME);
        final Band targetBand = targetPoduct.getBand(BAND_NAME);
        final PixelPos sourcePP = new PixelPos(sourceX, sourceY);
        final GeoPos geoPos = sourceBand.getGeoCoding().getGeoPos(sourcePP, null);
        final PixelPos targetPP = targetBand.getGeoCoding().getPixelPos(geoPos, null);
        final double[] pixels = new double[1];
        targetBand.readPixels((int) targetPP.x, (int) targetPP.y, 1,1, pixels);
        assertEquals(expectedPixelValue, pixels[0], delta);
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
