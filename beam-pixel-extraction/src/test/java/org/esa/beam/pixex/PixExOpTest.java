package org.esa.beam.pixex;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.util.StringUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PixExOpTest {

    private static final String DUMMY_PRODUCT1 = "dummyProduct1.dim";
    private static final String DUMMY_PRODUCT2 = "dummyProduct2.dim";

    private Transferable clipboardContents;
    private PrintStream defaultOutStream;
    private PixExOp.Spi pixExOpSpi;


    @Before
    public void before() {
        try {
            pixExOpSpi = new PixExOp.Spi();
            GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(pixExOpSpi);
            clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            defaultOutStream = System.out;
            System.setOut(new PrintStream(new ByteArrayOutputStream(100))); // suppressing output to std.out
        } catch (HeadlessException e) {
            System.out.println("A " + PixExOpTest.class + " test has not been performed: HeadlessException");
        }
    }

    @After
    public void tearDown() throws IOException {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboardContents, null);
        System.setOut(defaultOutStream);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(pixExOpSpi);

    }

    @Test
    public void testUsingGraph() throws GraphException, IOException, UnsupportedFlavorException {
        if (clipboardContents == null) {
            return;
        }
        String parentDir = new File(getClass().getResource("dummyProduct1.dim").getFile()).getParent();
        int windowSize = 11;
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate("carlCoordinate", 60.1f, 3.0f, null),
                new Coordinate("cassandraCoordinate", 59.1f, 0.5f, null)
        };
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
                "        <copyToClipboard>true</copyToClipboard>\n" +
                "      </parameters>\n" +
                "    </node>\n" +
                "  </graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new PixExOp.Spi());

        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);

        List<Product> sourceProducts = new ArrayList<Product>();
        sourceProducts.add(readProduct(DUMMY_PRODUCT1));
        sourceProducts.add(readProduct(DUMMY_PRODUCT2));


        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String data = String.valueOf(clipboard.getData(clipboard.getAvailableDataFlavors()[0]));
        checkClipboardData(data.split("\n"), sourceProducts.toArray(new Product[sourceProducts.size()]), coordinates,
                           windowSize, null);
    }

    @Test
    public void testSingleProduct() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 20.0f, 20.0f, null)
        };
        int windowSize = 3;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("copyToClipboard", true);

        String[] bandNames = {"rad_1", "rad_2"};
        Product[] sourceProduct = {createTestProduct("andi", "level1", bandNames)};

        computeData(parameterMap, sourceProduct);
        final String[] lines = getDataFromClipboard();
        checkClipboardData(lines, sourceProduct, coordinates, windowSize, null);
    }

    @Test
    public void testTwoProductsSameType() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 20.0f, 20.0f, null),
                new Coordinate("coord3", 0.5f, 0.5f, null)
        };
        int windowSize = 5;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("copyToClipboard", true);

        String[] bandNames = {"rad_1", "rad_2"};

        Product[] products = {
                createTestProduct("kallegrabowski", "level1", bandNames),
                createTestProduct("keek", "level1", bandNames)
        };

        computeData(parameterMap, products);
        final String[] lines = getDataFromClipboard();
        checkClipboardData(lines, products, coordinates, windowSize, null);
    }

    @Test
    public void testTwentyProductsSameType() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord3", 0.5f, 0.5f, null)
        };
        int windowSize = 1;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("copyToClipboard", true);

        String[] bandNames = {"rad_1", "rad_2, radiance_3"};

        List<Product> productList = new ArrayList<Product>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type", bandNames));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);

        computeData(parameterMap, products);
        final String[] lines = getDataFromClipboard();
        checkClipboardData(lines, products, coordinates, windowSize, null);
    }

    @Test
    public void testTwoProductsTwoDifferentTypes() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 20.0f, 20.0f, null),
                new Coordinate("coord3", 0.5f, 0.5f, null)
        };
        int windowSize = 5;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("copyToClipboard", true);

        String[] bandNames = {"rad_1", "rad_2"};
        String[] bandNames2 = {"refl_1", "refl_2"};

        Product[] products = {
                createTestProduct("kallegrabowski", "level1", bandNames),
                createTestProduct("keek", "level2", bandNames2)
        };

        computeData(parameterMap, products);
        final String[] lines = getDataFromClipboard();
        checkClipboardData(lines, products, coordinates, windowSize, null);
    }

    @Test
    public void testTwoProductsWithTimeConstraints() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

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
        int windowSize = 1;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("copyToClipboard", false);
        parameterMap.put("timeDifference", "1D");

        String[] bandNames = {"rad_1", "rad_2"};
        String[] bandNames2 = {"refl_1", "refl_2"};

        final Product p1 = createTestProduct("kallegrabowski", "level1", bandNames);
        p1.setStartTime(ProductData.UTC.parse("01-MAR-2005 12:00:00"));
        p1.setEndTime(ProductData.UTC.parse("01-MAR-2005 13:00:00"));
        final Product p2 = createTestProduct("keek", "level1", bandNames2);
        p2.setStartTime(ProductData.UTC.parse("01-Jan-2006 0:00:00"));
        p2.setEndTime(ProductData.UTC.parse("01-Jan-2006 12:00:00"));
        final Map<String, Product> productMap = new HashMap<String, Product>();
        productMap.put("sourceProduct.0", p1);
        productMap.put("sourceProduct.1", p2);

        PixExOp pixEx = (PixExOp) pixExOpSpi.createOperator(parameterMap, productMap, null);
        pixEx.getTargetProduct(); // trigger computation
        final Map<String, List<Measurement>> map = pixEx.getMeasurements();

        assertEquals(1, map.size()); // one product type
        final List<Measurement> measurementList = map.get("level1");
        assertEquals(2, measurementList.size());
        final Measurement measurement = measurementList.get(0);
    }

    @Test
    public void testTwentyProductsWithDifferentTypes() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null),
                new Coordinate("coord2", 8.0f, 8.0f, null),
                new Coordinate("coord3", 2.5f, 1.0f, null),
                new Coordinate("coord4", 0.5f, 0.5f, null)
        };
        int windowSize = 13;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);
        parameterMap.put("copyToClipboard", true);

        List<Product> productList = new ArrayList<Product>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type" + i, new String[]{"band" + i}));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);

        computeData(parameterMap, products);
        final String[] lines = getDataFromClipboard();
        checkClipboardData(lines, products, coordinates, windowSize, null);
    }

    @Test(expected = OperatorException.class)
    public void testFailForEvenWindowSize() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", 10.0f, 10.0f, null)
        };
        int windowSize = 2; // not allowed !!

        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};
        Product[] sourceProduct = {createTestProduct("werner", "level1", bandNames)};
        computeData(parameterMap, sourceProduct);

        final String[] lines = getDataFromClipboard();
        checkClipboardData(lines, sourceProduct, coordinates, windowSize, null);
    }

    @Test
    public void testReadMeasurement() throws TransformException, FactoryException, IOException {
        if (clipboardContents == null) {
            return;
        }
        PixExOp op = new PixExOp();
        String[] bandNames = {"band_1", "band_2", "band_3"};
        Product product = createTestProduct("horst", "horse", bandNames);
        final Mask mask = Mask.BandMathsType.create("mask_0", "", product.getSceneRasterWidth(),
                                                    product.getSceneRasterHeight(),
                                                    "band_1 == 0", Color.RED, 0.0);
        product.getMaskGroup().add(mask);

        String productType = product.getProductType();
        HashMap<String, String[]> bandNamesMap = new HashMap<String, String[]>();
        bandNamesMap.put(productType, StringUtils.addArrays(bandNames, new String[]{"mask_0"}));
        op.setRasterNamesMap(bandNamesMap);
        op.setWindowSize(3);
        Map<String, List<Measurement>> measurements = new HashMap<String, List<Measurement>>();
        GeoPos geoPos = new GeoPos(20.5f, 10.5f);
        op.readMeasurement(product, new Coordinate("Coord_1", geoPos.lat, geoPos.lon, null), 1, measurements);
        geoPos = new GeoPos(21.5f, 9.5f);

        List<Measurement> measurementList = measurements.get(productType);
        assertNotNull(measurementList);
        assertTrue(!measurementList.isEmpty());

        for (int i = 0; i < measurementList.size(); i++) {
            assertEquals(3 * 3, measurementList.size());
            Measurement measurement = measurementList.get(i);
            assertEquals(1, measurement.getCoordinateID());
            assertEquals(geoPos.lat - i / 3, measurement.getLat(), 1.0e-4);
            assertEquals(geoPos.lon + i % 3, measurement.getLon(), 1.0e-4);
            assertEquals("Coord_1", measurement.getCoordinateName());
            assertNull(measurement.getTime());
            Number[] values = measurement.getValues();
            assertEquals(bandNames.length + 1, values.length);
            assertEquals(0.0, (Double) values[0], 1.0e-4);
            assertEquals(1.0, (Double) values[1], 1.0e-4);
            assertEquals(2.0, (Double) values[2], 1.0e-4);
            final int maskValue = (Integer) values[3];
            assertEquals(1, maskValue);
        }
    }

    @Test
    public void testTimeDeltaParsing() throws Exception {
        if (clipboardContents == null) {
            return;
        }
        final PixExOp op = new PixExOp();
        op.parseTimeDelta("2D");
        assertEquals(2, op.getTimeDelta());
        assertEquals(Calendar.DATE, op.getCalendarField());
        op.parseTimeDelta("30m");
        assertEquals(30, op.getTimeDelta());
        assertEquals(Calendar.MINUTE, op.getCalendarField());
        op.parseTimeDelta("12H");
        assertEquals(12, op.getTimeDelta());
        assertEquals(Calendar.HOUR, op.getCalendarField());
        op.parseTimeDelta("480M");
        assertEquals(480, op.getTimeDelta());
        assertEquals(Calendar.MINUTE, op.getCalendarField());
        op.parseTimeDelta("31d");
        assertEquals(31, op.getTimeDelta());
        assertEquals(Calendar.DATE, op.getCalendarField());
        op.parseTimeDelta("1h");
        assertEquals(1, op.getTimeDelta());
        assertEquals(Calendar.HOUR, op.getCalendarField());
    }

    private static Product readProduct(String s) throws IOException {
        final URL radianceProductUrl = PixExOpTest.class.getResource(s);
        return ProductIO.readProduct(radianceProductUrl.getFile());
    }

    private static void computeData(Map<String, Object> parameterMap, Product[] sourceProducts) {
        GPF.createProduct("PixEx", parameterMap, sourceProducts);
    }

    private static String[] getDataFromClipboard() throws UnsupportedFlavorException, IOException {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String data = String.valueOf(clipboard.getData(clipboard.getAvailableDataFlavors()[0]));
        return data.split("\n");
    }

    private static Product createTestProduct(String name, String type, String[] bandNames) throws FactoryException,
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
            band.setDataElems(generateData(bounds, i));
        }
        return product;
    }

    private static float[] generateData(Rectangle bounds, int val) {
        float[] floats = new float[bounds.width * bounds.height];
        Arrays.fill(floats, val);
        return floats;
    }

    private static void checkClipboardData(String[] lines, Product[] products, Coordinate[] coordinates, int windowSize,
                                    String expression) {

        List<String> productTypes = new ArrayList<String>();
        for (Product product : products) {
            String productType = product.getProductType();
            if (!productTypes.contains(productType)) {
                productTypes.add(productType);
            }
        }
        int mainHeaderLength = expression != null ? 6 : 5;
        int productIdMapLength = 4 + products.length;
        int lineCount = windowSize * windowSize * coordinates.length * products.length;
        lineCount += mainHeaderLength; // add offset for the main header
        lineCount += productIdMapLength;
        lineCount += productTypes.size() * 2; // add two lines for each header
        lineCount += productTypes.size() > 1 ? productTypes.size() : 0; // if more than one product type is present, add a line for each
        lineCount -= productTypes.size() > 1 ? 1 : 0; // if more than one product type is present, the last productType has no line break

        assertEquals(lineCount, lines.length);

        String header = "ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\t";

        List<Integer> headerLines = new ArrayList<Integer>();
        for (int i = 0; i < productTypes.size(); i++) {
            int headerLineIndex = i * (windowSize * windowSize * coordinates.length + 2) + (1 + i);
            headerLines.add(headerLineIndex + mainHeaderLength);
        }

        for (int headerLine : headerLines) {
            boolean containsBandNames = false;

            for (Product product : products) {
                String[] bandNames = product.getBandNames();
                String nameString = "";
                for (String bandName : bandNames) {
                    nameString += bandName + "\t";
                }
                if (lines[headerLine].contains(nameString)) {
                    containsBandNames = true;
                    break;
                }
            }

            assertTrue(lines[headerLine].startsWith(header));
            assertTrue(containsBandNames);
        }
    }

}
