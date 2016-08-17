package org.esa.snap.pixex;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.measurement.Measurement;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class PixExOpTest {

    @Test
    public void testUsingGraph() throws GraphException, IOException {
        String parentDir = new File(getClass().getResource("dummyProduct1.dim").getFile()).getParent();
        int windowSize = 11;
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate("carlCoordinate", 60.1, 3.0, null),
                new Coordinate("cassandraCoordinate", 59.1, 0.5, null)
        };
        final File outputDir = getOutputDir("testUsingGraph", getClass());
        String graphOpXml =
                "<graph id=\"someGraphId\">\n" +
                        "    <version>1.0</version>\n" +
                        "    <node id=\"someNodeId\">\n" +
                        "      <operator>PixEx</operator>\n" +
                        "      <parameters>\n" +
                        "        <sourceProductPaths>\n" +
                        "           " + parentDir + File.separator + "*.dim" +
                        "        </sourceProductPaths>\n" +
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

        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);

        final PixExMeasurementReader reader = new PixExMeasurementReader(outputDir);
        List<Measurement> measurementList = convertToList(reader);
        assertEquals(windowSize * windowSize * 2 * 2, measurementList.size());
    }

    @Test
    public void testGetParsedInputPaths() throws Exception {
        final File testDir = getOutputDir("testGetParsedInputPaths", getClass());
        final File subDir1 = new File(testDir, "subDir1");
        final File subDir2 = new File(testDir, "subDir2");
        final File subDir2_1 = new File(subDir2, "subDir2_1");
        final File subDir2_2 = new File(subDir2, "subDir2_2");
        testDir.mkdir();
        subDir1.mkdir();
        subDir2.mkdir();
        subDir2_1.mkdir();
        subDir2_2.mkdir();

        final String pattern = testDir.getCanonicalPath() + File.separator + PixExOp.RECURSIVE_INDICATOR;
        Set<File> dirList = PixExOp.getSourceProductFileSet(new String[]{pattern}, Logger.getAnonymousLogger());

        //assertEquals(5, dirList.size());
        //assertTrue("Missing dir '" + testDir.getAbsolutePath() + "'.", dirList.contains(testDir));
        assertEquals(4, dirList.size());
        assertTrue("Missing dir '" + subDir1.getCanonicalPath() + "'.", dirList.contains(subDir1.getCanonicalFile()));
        assertTrue("Missing dir '" + subDir2.getCanonicalPath() + "'.", dirList.contains(subDir2.getCanonicalFile()));
        assertTrue("Missing dir '" + subDir2_1.getCanonicalPath() + "'.",
                   dirList.contains(subDir2_1.getCanonicalFile()));
        assertTrue("Missing dir '" + subDir2_2.getCanonicalPath() + "'.",
                   dirList.contains(subDir2_2.getCanonicalFile()));

        dirList = PixExOp.getSourceProductFileSet(new String[]{testDir.getPath(), subDir2_1.getPath()}, Logger.getAnonymousLogger());
        assertEquals(2, dirList.size());
        assertTrue("Missing dir '" + testDir.getCanonicalPath() + "'.", dirList.contains(testDir.getCanonicalFile()));
        assertTrue("Missing dir '" + subDir2_1.getCanonicalPath() + "'.", dirList.contains(subDir2_1.getCanonicalFile()));
    }

    @Test
    public void testSingleProduct() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0, 10.0, null),
                new Coordinate("coord2", 20.0, 20.0, null)
        };
        int windowSize = 3;

        HashMap<String, Object> parameterMap = new HashMap<>();
        final File outputDir = getOutputDir("testSingleProduct", getClass());
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

        try (PixExMeasurementReader reader = new PixExMeasurementReader(outputDir)) {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(windowSize * windowSize * sourceProducts.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 10.5f, 9.5f, 189.5f, 79.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 20.5f, 19.5f, 199.5f, 69.5f);
        }
    }

    @Test
    public void testTimeExtractionFromFilename() throws Exception {

        Coordinate[] coordinates = {new Coordinate("coord", 20.0, 20.0, null)};
        int windowSize = 1;

        HashMap<String, Object> parameterMap = new HashMap<>();
        final File outputDir = getOutputDir("testSingleProduct", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("extractTimeFromFilename", true);
        parameterMap.put("dateInterpretationPattern", "yyyyMMdd");
        parameterMap.put("filenameInterpretationPattern", "*${startDate}*");

        String[] bandNames = {"rad_1", "rad_2"};
        Product p1 = createTestProduct("andi", "type1", bandNames);
        p1.setStartTime(ProductData.UTC.parse("22/08/1999", "dd/MM/yyyy"));

        Product p2 = createTestProduct("bob", "type1", bandNames);
        p2.setFileLocation(new File("bob_20010320.nc"));
        p2.setStartTime(ProductData.UTC.parse("30/03/1920", "dd/MM/yyyy"));

        Product p3 = createTestProduct("jane", "type1", bandNames);
        p3.setFileLocation(new File("bob_20101114.nc"));
        Product[] sourceProducts = {p1, p2, p3};

        computeData(parameterMap, sourceProducts);

        try (PixExMeasurementReader reader = new PixExMeasurementReader(outputDir)) {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(sourceProducts.length, measurementList.size());
            assertEquals(ProductData.UTC.parse("22/08/1999", "dd/MM/yyyy").getAsDate(),
                         measurementList.get(0).getTime().getAsDate());
            assertEquals(ProductData.UTC.parse("20/03/2001", "dd/MM/yyyy").getAsDate(),
                         measurementList.get(1).getTime().getAsDate());
            assertEquals(ProductData.UTC.parse("14/11/2010", "dd/MM/yyyy").getAsDate(),
                         measurementList.get(2).getTime().getAsDate());
        }
    }

    @Test
    public void testTwoProductsSameType() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0, 10.0, null),
                new Coordinate("coord2", 20.0, 20.0, null),
                new Coordinate("coord3", 0.5, 0.5, null)
        };
        int windowSize = 5;

        HashMap<String, Object> parameterMap = new HashMap<>();
        File outputDir = getOutputDir("testTwoProductsSameType", getClass());
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
        try (PixExMeasurementReader reader = new PixExMeasurementReader(outputDir)) {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 10.5f, 9.5f, 189.5f, 79.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 20.5f, 19.5f, 199.5f, 69.5f);
        }
    }

    @Test
    public void testTwentyProductsSameType() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0, 10.0, null),
                new Coordinate("coord3", 0.5, 0.5, null)
        };
        int windowSize = 1;

        HashMap<String, Object> parameterMap = new HashMap<>();
        File outputDir = getOutputDir("testTwentyProductsSameType", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2, radiance_3"};

        List<Product> productList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type", bandNames));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);
        computeData(parameterMap, products);
        try (PixExMeasurementReader reader = new PixExMeasurementReader(outputDir)) {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 9.5f, 10.5f, 190.5f, 80.5f);
            testForExistingMeasurement(measurementList, "coord3", 2, 0.5f, 0.5f, 180.5f, 89.5f);
        }
    }

    @Test
    public void testTwoProductsTwoDifferentTypes() throws Exception {
        HashMap<String, Object> parameterMap = new HashMap<>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0, 10.0, null),
                new Coordinate("coord2", 20.0, 20.0, null),
                new Coordinate("coord3", 0.5, 0.5, null)
        };
        int windowSize = 5;

        File outputDir = getOutputDir("testTwoProductsTwoDifferentTypes", getClass());
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
        calInP1.set(2005, Calendar.MARCH, 1, 12, 30, 0);
        final Calendar calInP2 = Calendar.getInstance();
        calInP2.set(2006, Calendar.JANUARY, 1, 6, 0, 0);
        final Calendar calOutsideBoth = Calendar.getInstance();
        calOutsideBoth.set(2010, Calendar.JANUARY, 1, 0, 0, 0);
        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0, 10.0, calInP1.getTime()),
                new Coordinate("coord2", 20.0, 20.0, calInP2.getTime()),
                new Coordinate("coord3", 0.5, 0.5, calOutsideBoth.getTime())
        };


        PixExOp pixEx = new PixExOp();
        pixEx.setParameterDefaultValues();
        File outputDir = getOutputDir("testTwoProductsWithTimeConstraints", getClass());
        pixEx.setParameter("outputDir", outputDir);
        pixEx.setParameter("exportTiePoints", false);
        pixEx.setParameter("exportMasks", false);
        pixEx.setParameter("coordinates", coordinates);
        pixEx.setParameter("windowSize", 1);
        pixEx.setParameter("timeDifference", "1D");
        pixEx.setSourceProducts(p1, p2);

        try (PixExMeasurementReader reader = (PixExMeasurementReader) pixEx.getTargetProperty(
                "measurements")) {
            final List<Measurement> measurementList = convertToList(reader);
            assertEquals(2, measurementList.size());
            testForExistingMeasurement(measurementList, "coord1", 1, 9.5f, 10.5f, 190.5f, 80.5f);
            testForExistingMeasurement(measurementList, "coord2", 2, 19.5f, 20.5f, 200.5f, 70.5f);
        }
    }


    @Test
    public void testTwentyProductsWithDifferentTypes() throws Exception {

        Coordinate[] coordinates = {
                new Coordinate("coord3", 2.5, 1.0, null),
                new Coordinate("coord4", 0.5, 0.5, null)
        };
        int windowSize = 1;

        HashMap<String, Object> parameterMap = new HashMap<>();
        File outputDir = getOutputDir("testTwentyProductsWithDifferentTypes", getClass());
        parameterMap.put("outputDir", outputDir);
        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        List<Product> productList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type" + i, new String[]{"band" + i}));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);

        computeData(parameterMap, products);
        try (PixExMeasurementReader measurementReader = new PixExMeasurementReader(outputDir)) {
            final List<Measurement> measurementList = convertToList(measurementReader);
            assertEquals(windowSize * windowSize * products.length * coordinates.length, measurementList.size());
            testForExistingMeasurement(measurementList, "coord3", 1, 2.5f, 1.5f, 181.5f, 87.5f);
            testForExistingMeasurement(measurementList, "coord4", 2, 0.5f, 0.5f, 180.5f, 89.5f);
        }
    }

    @Test(expected = OperatorException.class)
    public void testFailForEvenWindowSize() throws Exception {
        HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("coordinates", new Coordinate[]{new Coordinate("coord1", 10.0, 10.0, null)});
        parameterMap.put("windowSize", 2); // not allowed !!

        Product[] sourceProduct = {createTestProduct("werner", "type1", new String[]{"rad_1", "rad_2"})};
        computeData(parameterMap, sourceProduct);
    }

    @Test
    public void testExtractMatchupCoordinates() throws Exception {
        final File testFile = new File(getClass().getResource("test.csv").getFile());
        final List<Coordinate> coordinates = PixExOp.extractMatchupCoordinates(testFile);

        assertEquals(2, coordinates.size());
        assertEquals("0", coordinates.get(0).getName());
        assertEquals(56.0123, (double) coordinates.get(0).getLat(), 0.0001);
        assertEquals(6.2345, (double) coordinates.get(0).getLon(), 0.0001);
        final Coordinate.OriginalValue[] originalValues = coordinates.get(0).getOriginalValues();
        assertEquals(4, originalValues.length);
        assertEquals("test1.1", originalValues[1].value);
        assertEquals(0.2F, Float.parseFloat(originalValues[2].value), 0.001);
        assertEquals(0.3F, Float.parseFloat(originalValues[3].value), 0.001);

        assertEquals("1", coordinates.get(1).getName());
        assertEquals(56.0124, (double) coordinates.get(1).getLat(), 0.0001);
        assertEquals(6.2346, (double) coordinates.get(1).getLon(), 0.0001);
        final Coordinate.OriginalValue[] originalValues1 = coordinates.get(1).getOriginalValues();
        assertEquals(4, originalValues.length);
        assertEquals("test1.2", originalValues1[1].value);
        assertEquals(0.21F, Float.parseFloat(originalValues1[2].value), 0.001);
        assertEquals(0.31F, Float.parseFloat(originalValues1[3].value), 0.001);
    }

    public static File getOutputDir(String methodName, Class testClass) {
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
                    Double.compare(lat, measurement.getLat()) == 0 && Double.compare(lon, measurement.getLon()) == 0 &&
                    Double.compare(x, measurement.getPixelX()) == 0 && Double.compare(y, measurement.getPixelY()) == 0) {
                return;
            }
        }
        fail("No measurement with the name " + coordinateName);
    }

    private List<Measurement> convertToList(Iterator<Measurement> measurementIterator) {
        final ArrayList<Measurement> list = new ArrayList<>();
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
        product.setSceneGeoCoding(geoCoding);
        for (int i = 0; i < bandNames.length; i++) {
            Band band = product.addBand(bandNames[i], ProductData.TYPE_FLOAT32);
            band.setSourceImage(ConstantDescriptor.create((float) bounds.width, (float) bounds.height,
                                                          new Float[]{(float) i}, null));
        }
        return product;
    }

}
