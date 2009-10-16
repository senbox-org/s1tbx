package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class AbstractReprojectionOpTest {

    protected static final String WGS84_CODE = "EPSG:4326";
    protected static final String UTM33N_CODE = "EPSG:32633";
    @SuppressWarnings({"StringConcatenation"})
    protected static final String UTM33N_WKT = "PROJCS[\"WGS 84 / UTM zone 33N\"," +
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
    protected static File wktFile;

    private static final float[] LATS = new float[]{
            50.0f, 50.0f,
            30.0f, 30.0f
    };
    private static final float[] LONS = new float[]{
            6.0f, 26.0f,
            6.0f, 26.0f
    };
    private static final String FLOAT_BAND_NAME = "floatData";
    private static final String INT_BAND_NAME = "intData";

    protected static Product sourceProduct;
    protected static Map<String, Object> parameterMap;

    private static OperatorSpi spi;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        spi = new ReprojectionOp.Spi();
        parameterMap = new HashMap<String, Object>(5);
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(spi);

        wktFile = new File(AbstractReprojectionOpTest.class.getResource("test.wkt").toURI());

        sourceProduct = new Product("source", "t", 50, 50);
        final TiePointGrid latGrid = new TiePointGrid("latGrid", 2, 2, 0.5f, 0.5f, 49, 49, LATS);
        final TiePointGrid lonGrid = new TiePointGrid("lonGrid", 2, 2, 0.5f, 0.5f, 49, 49, LONS);
        sourceProduct.addTiePointGrid(latGrid);
        sourceProduct.addTiePointGrid(lonGrid);
        sourceProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        Band floatDataBand = sourceProduct.addBand(FLOAT_BAND_NAME, ProductData.TYPE_FLOAT32);
        floatDataBand.setRasterData(createDataFor(floatDataBand));
        floatDataBand.setSynthetic(true);
        Band intDataBand = sourceProduct.addBand(INT_BAND_NAME, ProductData.TYPE_INT16);
        intDataBand.setRasterData(createDataFor(intDataBand));
        intDataBand.setSynthetic(true);
    }

    @AfterClass
    public static void tearDown() {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.removeOperatorSpi(spi);
    }

    @Before
    public void setupTestMethod(){
        parameterMap.clear();
    }


    protected Product createReprojectedProduct(Map<String, Product> sourceMap) {
        String operatorName = OperatorSpi.getOperatorAlias(ReprojectionOp.class);
        return GPF.createProduct(operatorName, parameterMap, sourceMap);
    }

    protected Product createReprojectedProduct() {
        String operatorName = OperatorSpi.getOperatorAlias(ReprojectionOp.class);
        return GPF.createProduct(operatorName, parameterMap, sourceProduct);
    }

    protected void testPixelValue(Product targetPoduct, float sourceX, float sourceY, double expectedPixelValue, double delta) throws
                                                                                                                               IOException {
        final Band sourceBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        final Band targetBand = targetPoduct.getBand(FLOAT_BAND_NAME);
        final PixelPos sourcePP = new PixelPos(sourceX, sourceY);
        final GeoPos geoPos = sourceBand.getGeoCoding().getGeoPos(sourcePP, null);
        final PixelPos targetPP = targetBand.getGeoCoding().getPixelPos(geoPos, null);
        final double[] pixels = new double[1];
        targetBand.readPixels((int) targetPP.x, (int) targetPP.y, 1,1, pixels);
        org.junit.Assert.assertEquals(expectedPixelValue, pixels[0], delta);
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
