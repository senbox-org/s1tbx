package org.esa.beam.pixex;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.measurement.Measurement;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PixExOpTest {

    private static PixExOp.Spi pixExOpSpi;


    @BeforeClass
    public static void beforeClass() {
        pixExOpSpi = new PixExOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(pixExOpSpi);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(pixExOpSpi);
    }

    @Test
    public void testUsingGraph() throws GraphException, IOException {
        String parentDir = new File(getClass().getResource("dummyProduct1.dim").getFile()).getParent();
        int windowSize = 11;
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate("carlCoordinate", 60.1f, 3.0f, null),
                new Coordinate("cassandraCoordinate", 59.1f, 0.5f, null)
        };
        final File outputDir = getOutpuDir("testUsingGraph", getClass());
        String graphOpXml =
                "<graph id=\"someGraphId\">\n" +
                "    <version>1.0</version>\n" +
                "    <node id=\"someNodeId\">\n" +
                "      <operator>PixEx</operator>\n" +
                "      <parameters>\n" +
                "        <inputPaths>\n" +
                "           " + parentDir +
                "        </inputPaths>\n" +
                "        <exportTiePoints>false</exportTiePoints>\n" +
                "        <exportBands>true</exportBands>\n" +
                "        <exportMasks>false</exportMasks>                \n" +
                "        <coordinates>\n" +
                "          <coordinate>\n" +
                "            <latitude>" + coordinates[0].getLat() + "</latitude>\n" +
                "            <longitude>" + coordinates[0].getLon() + "</longitude>\n" +
                "            <name>" + coordinates[0].getName() + "</name>\n" +
                "          </coordinate>\n" +
                "          <coordinate>\n" +
                "            <latitude>" + coordinates[1].getLat() + "</latitude>\n" +
                "            <longitude>" + coordinates[1].getLon() + "</longitude>\n" +
                "            <name>" + coordinates[1].getName() + "</name>\n" +
                "          </coordinate>\n" +
                "        </coordinates>\n" +
                "        <windowSize>" + windowSize + "</windowSize>\n" +
                "        <outputDir>" + outputDir.getAbsolutePath() + "</outputDir>\n" +
                "        <outputFilePrefix>" + "testUsingGraph" + "</outputFilePrefix>\n" +

                "      </parameters>\n" +
                "    </node>\n" +
                "  </graph>";
        Graph graph = GraphIO.read(new StringReader(graphOpXml));

        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new PixExOp.Spi());

        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);

        final PixExMeasurementReader reader = new PixExMeasurementReader(outputDir);
        List<Measurement> measurementList = convertToList(reader);
        assertEquals(windowSize * windowSize * 2 * 2, measurementList.size());
    }

    @Test
    public void testGetParsedInputPaths() throws Exception {
        final File testDir = getOutpuDir("testGetParsedInputPaths", getClass());
        final File subDir1 = new File(testDir, "subDir1");
        final File subDir2 = new File(testDir, "subDir2");
        final File subDir2_1 = new File(subDir2, "subDir2_1");
        final File subDir2_2 = new File(subDir2, "subDir2_2");
        testDir.mkdir();
        subDir1.mkdir();
        subDir2.mkdir();
        subDir2_1.mkdir();
        subDir2_2.mkdir();

        File[] parsedInputPaths = PixExOp.getParsedInputPaths(
                new File[]{new File(testDir.getAbsolutePath() + PixExOp.RECURSIVE_INDICATOR)});

        assertEquals(5, parsedInputPaths.length);
        List<File> dirList = Arrays.asList(parsedInputPaths);
        assertTrue("Missing dir '" + testDir.getAbsolutePath() + "'.", dirList.contains(testDir));
        assertTrue("Missing dir '" + subDir1.getAbsolutePath() + "'.", dirList.contains(subDir1));
        assertTrue("Missing dir '" + subDir2.getAbsolutePath() + "'.", dirList.contains(subDir2));
        assertTrue("Missing dir '" + subDir2_1.getAbsolutePath() + "'.", dirList.contains(subDir2_1));
        assertTrue("Missing dir '" + subDir2_2.getAbsolutePath() + "'.", dirList.contains(subDir2_2));

        parsedInputPaths = PixExOp.getParsedInputPaths(new File[]{testDir, subDir2_1});
        assertEquals(2, parsedInputPaths.length);
        dirList = Arrays.asList(parsedInputPaths);
        assertTrue("Missing dir '" + testDir.getAbsolutePath() + "'.", dirList.contains(testDir));
        assertTrue("Missing dir '" + subDir2_1.getAbsolutePath() + "'.", dirList.contains(subDir2_1));

    }

    @Test
    public void testSingleProduct() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 20.0f, 20.0f, null)
        };
        int windowSize = 3;

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        final File outputDir = getOutpuDir("testSingleProduct", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("outputFilePrefix", "pixels");
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("exportKmz", true);

        String[] bandNames = {"rad_1", "rad_2"};
        Product[] sourceProducts = {createTestProduct("andi", "type1", bandNames)};

        computeData(parameterMap, sourceProducts);

        assertTrue("Kmz file does not exists", new File(outputDir, "pixels_coordinates.kmz").exists());

        final PixExMeasurementReader reader = new PixExMeasurementReader(outputDir);
        try {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(windowSize * windowSize * sourceProducts.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 10.5f, 9.5f, 189.5f, 79.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 20.5f, 19.5f, 199.5f, 69.5f);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testTwoProductsSameType() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 20.0f, 20.0f, null),
                new Coordinate("coord3", 0.5f, 0.5f, null)
        };
        int windowSize = 5;

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        File outputDir = getOutpuDir("testTwoProductsSameType", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};

        Product[] products = {
                createTestProduct("kallegrabowski", "type1", bandNames),
                createTestProduct("keek", "type1", bandNames)
        };

        computeData(parameterMap, products);
        final PixExMeasurementReader reader = new PixExMeasurementReader(outputDir);
        try {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 10.5f, 9.5f, 189.5f, 79.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 20.5f, 19.5f, 199.5f, 69.5f);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testTwentyProductsSameType() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord3", 0.5f, 0.5f, null)
        };
        int windowSize = 1;

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        File outputDir = getOutpuDir("testTwentyProductsSameType", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2, radiance_3"};

        List<Product> productList = new ArrayList<Product>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type", bandNames));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);
        computeData(parameterMap, products);
        final PixExMeasurementReader reader = new PixExMeasurementReader(outputDir);
        try {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 9.5f, 10.5f, 190.5f, 80.5f);
            testForExistingMeasurement(measurementList, "coord3", 2, 0.5f, 0.5f, 180.5f, 89.5f);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testTwoProductsTwoDifferentTypes() throws Exception {
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 20.0f, 20.0f, null),
                new Coordinate("coord3", 0.5f, 0.5f, null)
        };
        int windowSize = 5;

        File outputDir = getOutpuDir("testTwoProductsTwoDifferentTypes", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};
        String[] bandNames2 = {"refl_1", "refl_2"};

        Product[] products = {
                createTestProduct("kallegrabowski", "type1", bandNames),
                createTestProduct("keek", "level2", bandNames2)
        };

        computeData(parameterMap, products);
        final PixExMeasurementReader reader = new PixExMeasurementReader(outputDir);
        final List<Measurement> measurementList = convertToList(reader);
        try {
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 9.5f, 10.5f, 190.5f, 80.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 20.5f, 19.5f, 199.5f, 69.5f);
            testForExistingMeasurement(measurementList, "coord3", 3, 0.5f, 0.5f, 180.5f, 89.5f);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testTwoProductsWithTimeConstraints() throws Exception {
        String[] bandNames = {"rad_1", "rad_2"};

        final Product p1 = createTestProduct("kallegrabowski", "type1", bandNames);
        p1.setStartTime(ProductData.UTC.parse("01-MAR-2005 12:00:00"));
        p1.setEndTime(ProductData.UTC.parse("01-MAR-2005 13:00:00"));
        final Product p2 = createTestProduct("keek", "type1", bandNames);
        p2.setStartTime(ProductData.UTC.parse("01-Jan-2006 0:00:00"));
        p2.setEndTime(ProductData.UTC.parse("01-Jan-2006 12:00:00"));

        final Calendar calInP1 = Calendar.getInstance();
        calInP1.set(2005, 2, 1, 12, 30, 0);
        final Calendar calInP2 = Calendar.getInstance();
        calInP2.set(2006, 0, 1, 6, 0, 0);
        final Calendar calOutsideBoth = Calendar.getInstance();
        calOutsideBoth.set(2010, 0, 1, 0, 0, 0);
        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, calInP1.getTime()),
                new Coordinate("coord2", 20.0f, 20.0f, calInP2.getTime()),
                new Coordinate("coord3", 0.5f, 0.5f, calOutsideBoth.getTime())
        };


        PixExOp pixEx = new PixExOp();
        File outputDir = getOutpuDir("testTwoProductsWithTimeConstraints", getClass());
        pixEx.setParameter("outputDir", outputDir);
        pixEx.setParameter("exportTiePoints", false);
        pixEx.setParameter("exportMasks", false);
        pixEx.setParameter("coordinates", coordinates);
        pixEx.setParameter("windowSize", 1);
        pixEx.setParameter("timeDifference", "1D");
        pixEx.setSourceProducts(new Product[]{p1, p2});

        final PixExMeasurementReader reader = (PixExMeasurementReader) pixEx.getTargetProperty(
                "measurements");// trigger computation
        try {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(2, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 9.5f, 10.5f, 190.5f, 80.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 19.5f, 20.5f, 200.5f, 70.5f);
        } finally {
            reader.close();
        }
    }


    @Test
    public void testTwentyProductsWithDifferentTypes() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord3", 2.5f, 1.0f, null),
                new Coordinate("coord4", 0.5f, 0.5f, null)
        };
        int windowSize = 1;

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        File outputDir = getOutpuDir("testTwentyProductsWithDifferentTypes", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        List<Product> productList = new ArrayList<Product>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type" + i, new String[]{"band" + i}));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);

        computeData(parameterMap, products);
        final PixExMeasurementReader measurementReader = new PixExMeasurementReader(outputDir);
        try {
            final List<Measurement> measurementList = convertToList(measurementReader);
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord3", 1, 2.5f, 1.5f, 181.5f, 87.5f);
            testForExistingMeasurement(measurementList, "coord4", 2, 0.5f, 0.5f, 180.5f, 89.5f);
        } finally {
            measurementReader.close();
        }
    }

    @Test(expected = OperatorException.class)
    public void testFailForEvenWindowSize() throws Exception {
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("coordinates", new Coordinate[]{new Coordinate("coord1", 10.0f, 10.0f, null)});
        parameterMap.put("windowSize", 2); // not allowed !!

        Product[] sourceProduct = {createTestProduct("werner", "type1", new String[]{"rad_1", "rad_2"})};
        computeData(parameterMap, sourceProduct);
    }

    public static File getOutpuDir(String methodName, Class testClass) {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File baseTestDir = new File(tmpDir, testClass.getSimpleName());
        final File dir = new File(baseTestDir, methodName);
        if (!dir.mkdirs()) { // already exists, so delete content
            final File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        return dir;
    }

    private void testForExistingMeasurement(List<Measurement> measurementList, String coordinateName, int id,
                                            float lat, float lon, float x, float y) {
        for (Measurement measurement : measurementList) {
            if (measurement.getCoordinateName().equals(coordinateName) && id == measurement.getCoordinateID() &&
                Float.compare(lat, measurement.getLat()) == 0 && Float.compare(lon, measurement.getLon()) == 0 &&
                Float.compare(x, measurement.getPixelX()) == 0 && Float.compare(y, measurement.getPixelY()) == 0) {
                return;
            }
        }
        fail("No measurement with the name " + coordinateName);
    }

    private List<Measurement> convertToList(Iterator<Measurement> measurementIterator) {
        final ArrayList<Measurement> list = new ArrayList<Measurement>();
        while (measurementIterator.hasNext()) {
            list.add(measurementIterator.next());
        }
        return list;
    }

    private static void computeData(Map<String, Object> parameterMap, Product[] sourceProducts) {
        GPF.createProduct("PixEx", parameterMap, sourceProducts);
    }

    public static Product createTestProduct(String name, String type, String[] bandNames) throws FactoryException,
                                                                                                 TransformException {
        Rectangle bounds = new Rectangle(360, 180);
        Product product = new Product(name, type, bounds.width, bounds.height);
        AffineTransform i2mTransform = new AffineTransform();
        final int northing = 90;
        final int easting = -180;
        i2mTransform.translate(easting, northing);
        final double scaleX = 360 / bounds.width;
        final double scaleY = 180 / bounds.height;
        i2mTransform.scale(scaleX, -scaleY);
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, bounds, i2mTransform);
        product.setGeoCoding(geoCoding);
        for (int i = 0; i < bandNames.length; i++) {
            Band band = product.addBand(bandNames[i], ProductData.TYPE_FLOAT32);
            band.setSourceImage(ConstantDescriptor.create((float) bounds.width, (float) bounds.height,
                                                          new Float[]{(float) i}, null));
        }
        return product;
    }

}
