package org.esa.beam.dataio.envi;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;

public class EnviProductReaderTest extends TestCase {

    private Product product;

    public void testParseBandNames_emptyBandnameProperty() throws IOException {
        final StringReader reader = new StringReader("band names = {}"); // empty bandname property
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(1, bandNames.length);
        assertEquals("Band", bandNames[0]);
    }

    public void testParseBandNames_noBandnameProperty() throws IOException {
        final StringReader reader = new StringReader(""); // no bandname property
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(1, bandNames.length);
        assertEquals("Band", bandNames[0]);
    }

    public void testParseBandNames_withBandnameProperty() throws IOException {
        final StringReader reader = new StringReader("band names = { myband_1, myband_2}");
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(2, bandNames.length);
        assertEquals("myband_1", bandNames[0]);
        assertEquals("myband_2", bandNames[1]);
    }

    public void testParseBandNames_withBandNumberProperty() throws IOException {
        final StringReader reader = new StringReader("bands = 3");
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(3, bandNames.length);
        assertEquals("Band_1", bandNames[0]);
        assertEquals("Band_2", bandNames[1]);
        assertEquals("Band_3", bandNames[2]);
    }

    public void testReadProductNodes_WithoutSensingStartStop() throws IOException, ParseException {
        final String sensingStartStop = "";
        final String headerContent = createHeaderFileContent(sensingStartStop);
        final File headerFile = createHeaderAndImageFile(headerContent, PRODUCT_NAME);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();
        product = reader.readProductNodes(headerFile, null);

        assertNotNull(product);
        assertEquals(PRODUCT_NAME, product.getName());
        assertEquals(PRODUCT_TYPE, product.getProductType());
        assertEquals(WIDTH, product.getSceneRasterWidth());
        assertEquals(HEIGHT, product.getSceneRasterHeight());
        assertEquals(6, product.getNumBands());
        // Band names from header replaced by validated names.
        // BEAM can not handle invalid node names, because there is no possibility to compute
        // bandarithmetic in the cases wher nodenames contains illegal characters.
        final Band band1 = product.getBand("data_molly_AVHRR_samer_SA81jul15a_n07_VIg");
        final Band band2 = product.getBand("data_molly_AVHRR_samer_SA81jul15b_n07_VIg");
        final Band band3 = product.getBand("data_molly_AVHRR_samer_SA81aug15a_n07_VIg");
        final Band band4 = product.getBand("data_molly_AVHRR_samer_SA81aug15b_n07_VIg");
        final Band band5 = product.getBand("data_molly_AVHRR_samer_SA81sep15a_n07_VIg");
        final Band band6 = product.getBand("data_molly_AVHRR_samer_SA81sep15b_n07_VIg");
        assertNotNull(band1);
        assertNotNull(band2);
        assertNotNull(band3);
        assertNotNull(band4);
        assertNotNull(band5);
        assertNotNull(band6);
        assertEquals("non formatted band name: _/data/molly/AVHRR/samer/SA81jul15a.n07-VIg", band1.getDescription());
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81jul15b.n07-VIg_", band2.getDescription());
        assertEquals("", band3.getDescription()); //bandname are valid
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81aug15b.n07-VIg", band4.getDescription());
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81sep15a.n07-VIg", band5.getDescription());
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81sep15b.n07-VIg", band6.getDescription());
    }

    public void testReadProductNodes_WithInvalidSensingStart() throws IOException {
        // valid DATE_FORMAT_PATTERN = "dd-MMM-yyyy HH:mm:ss";
        final String start = Header.SENSING_START;
        final String stop = Header.SENSING_STOP;
        final String sensingStartStop = start + " = 238746, " + stop + " = 17-feb-1999 6:7:8";
        final String headerContent = createHeaderFileContent(sensingStartStop);
        final File headerFile = createHeaderAndImageFile(headerContent, PRODUCT_NAME);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();
        try {
            product = reader.readProductNodes(headerFile, null);
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(Header.SENSING_START));
        }
    }

    public void testReadProductNodes_WithInvalidSensingStop() throws IOException {
        // valid DATE_FORMAT_PATTERN = "dd-MMM-yyyy HH:mm:ss";
        final String start = Header.SENSING_START;
        final String stop = Header.SENSING_STOP;
        final String sensingStartStop = start + " = 16-jan-1998 5:6:7, " + stop + " = jkhadsf";
        final String headerContent = createHeaderFileContent(sensingStartStop);
        final File headerFile = createHeaderAndImageFile(headerContent, PRODUCT_NAME);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();
        try {
            product = reader.readProductNodes(headerFile, null);
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(Header.SENSING_STOP));
        }
    }

    public void testReadProductNodes_WithValidSensingStartStop() throws IOException {
        // valid DATE_FORMAT_PATTERN = "dd-MMM-yyyy HH:mm:ss";
        final String start = Header.SENSING_START;
        final String stop = Header.SENSING_STOP;
        final String sensingStartStop = start + " = 16-jan-1998 5:6:7, " + stop + " = 17-feb-1999 6:7:8";
        final String headerContent = createHeaderFileContent(sensingStartStop);
        final File headerFile = createHeaderAndImageFile(headerContent, PRODUCT_NAME);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();
        product = reader.readProductNodes(headerFile, null);

        assertNotNull(product);
        assertEquals(PRODUCT_NAME, product.getName());
        assertEquals(PRODUCT_TYPE, product.getProductType());
        assertEquals(WIDTH, product.getSceneRasterWidth());
        assertEquals(HEIGHT, product.getSceneRasterHeight());
        assertEquals("16-JAN-1998 05:06:07.000000", product.getStartTime().format());
        assertEquals("17-FEB-1999 06:07:08.000000", product.getEndTime().format());
        assertEquals(6, product.getNumBands());
        // Band names from header replaced by validated names.
        // BEAM can not handle invalid node names, because there is no possibility to compute
        // bandarithmetic in the cases wher nodenames contains illegal characters.
        final Band band1 = product.getBand("data_molly_AVHRR_samer_SA81jul15a_n07_VIg");
        final Band band2 = product.getBand("data_molly_AVHRR_samer_SA81jul15b_n07_VIg");
        final Band band3 = product.getBand("data_molly_AVHRR_samer_SA81aug15a_n07_VIg");
        final Band band4 = product.getBand("data_molly_AVHRR_samer_SA81aug15b_n07_VIg");
        final Band band5 = product.getBand("data_molly_AVHRR_samer_SA81sep15a_n07_VIg");
        final Band band6 = product.getBand("data_molly_AVHRR_samer_SA81sep15b_n07_VIg");
        assertNotNull(band1);
        assertNotNull(band2);
        assertNotNull(band3);
        assertNotNull(band4);
        assertNotNull(band5);
        assertNotNull(band6);
        assertEquals("non formatted band name: _/data/molly/AVHRR/samer/SA81jul15a.n07-VIg", band1.getDescription());
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81jul15b.n07-VIg_", band2.getDescription());
        assertEquals("", band3.getDescription()); //bandname are valid
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81aug15b.n07-VIg", band4.getDescription());
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81sep15a.n07-VIg", band5.getDescription());
        assertEquals("non formatted band name: /data/molly/AVHRR/samer/SA81sep15b.n07-VIg", band6.getDescription());
    }

    public void testReadProductNodes_WithoutSensingStop() throws IOException {
        // valid DATE_FORMAT_PATTERN = "dd-MMM-yyyy HH:mm:ss";
        final String sensingStartStop = Header.SENSING_START + " = 16-jan-1998 5:6:7";
        final String headerContent = createHeaderFileContent(sensingStartStop);
        final File headerFile = createHeaderAndImageFile(headerContent, PRODUCT_NAME);

        final EnviProductReaderPlugIn plugIn = new EnviProductReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();
        product = reader.readProductNodes(headerFile, null);

        assertNotNull(product);
        assertEquals("16-JAN-1998 05:06:07.000000", product.getStartTime().format());
        assertEquals(null, product.getEndTime());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private static final String PROJECTION_INFO = "9, 6378137.0, 6356752.3, -17.500000, 63.500000,-32.500000, -2.500000, 7.0,0.0,WGS-84, SamerAlbers, units=Meters";
    private static final String INTERLEAVE = "Test Interleave";
    private static final String PRODUCT_NAME = "TestProductName";
    private static final String PRODUCT_TYPE = "unknown";
    private static final int WIDTH = 25;
    private static final int HEIGHT = 25;
    private static final int NUM_BANDS = 6;
    private static final int HEADER_OFFSET = 0;
    private static final String FILE_TYPE = "ENVI Standard";
    private static final int DATA_TYPE = 2;
    private static final String SENSOR_TYPE = "unknown";
    private static final int BYTE_ORDER = 1;
    private static final String WAVELENGTH_UNITS = "unknown";

    private String createHeaderFileContent(final String sensingStartStop) {
        final StringWriter writer = new StringWriter();
        final PrintWriter pw = new PrintWriter(writer);
        pw.println(EnviConstants.FIRST_LINE);
        pw.println("description = {");
        pw.print("\tENVI File \tCreated [Wed Feb 23 09:36:22 2005]");
        if (sensingStartStop.length() > 0) {
            pw.print("\t" + Header.BEAM_PROPERTIES + " = [" + sensingStartStop + "]");
        }
        pw.println("}");
        pw.println(EnviConstants.HEADER_KEY_SAMPLES + " = " + WIDTH);
        pw.println(EnviConstants.HEADER_KEY_LINES + " = " + HEIGHT);
        pw.println(EnviConstants.HEADER_KEY_BANDS + " = " + NUM_BANDS);
        pw.println(EnviConstants.HEADER_KEY_HEADER_OFFSET + " = " + HEADER_OFFSET);
        pw.println(EnviConstants.HEADER_KEY_FILE_TYPE + " = " + FILE_TYPE);
        pw.println(EnviConstants.HEADER_KEY_DATA_TYPE + " = " + DATA_TYPE);
        pw.println(EnviConstants.HEADER_KEY_INTERLEAVE + " = " + INTERLEAVE);
        pw.println(EnviConstants.HEADER_KEY_SENSOR_TYPE + " = " + SENSOR_TYPE);
        pw.println(EnviConstants.HEADER_KEY_BYTE_ORDER + " = " + BYTE_ORDER);
        pw.println();
        pw.println(EnviConstants.HEADER_KEY_MAP_INFO + " = {" + MAP_INFO + "}");
        pw.println(EnviConstants.HEADER_KEY_PROJECTION_INFO + " ={" + PROJECTION_INFO + "}");
        pw.println(EnviConstants.HEADER_KEY_WAVELENGTH_UNITS + " = " + WAVELENGTH_UNITS);
        pw.println(EnviConstants.HEADER_KEY_BAND_NAMES + " = {");
        pw.println(" _/data/molly/AVHRR/samer/SA81jul15a.n07-VIg,");
        pw.println(" /data/molly/AVHRR/samer/SA81jul15b.n07-VIg_,");
        pw.println(" data_molly_AVHRR_samer_SA81aug15a_n07_VIg,");
        pw.println(" /data/molly/AVHRR/samer/SA81aug15b.n07-VIg,");
        pw.println(" /data/molly/AVHRR/samer/SA81sep15a.n07-VIg,");
        pw.println(" /data/molly/AVHRR/samer/SA81sep15b.n07-VIg }");
        pw.flush();
        return writer.toString();
    }

    private File tDir;
    private static final String MAP_INFO = "SamerAlbers, 1.0000, 1.0000, -479862.9999, 1288756.5614, 8.0000000000e+03,8.0000000000e+03,WGS-84,units=Meters";

    private File createHeaderAndImageFile(final String headerContent, String headerFileName) throws IOException {
        tDir = new File("testDir");
        tDir.mkdirs();

        final File headerFile = new File(tDir, headerFileName + ".hdr");
        final File imageFile = new File(tDir, headerFileName + ".img");
        assertTrue(headerFile.createNewFile());
        assertTrue(imageFile.createNewFile());

        final FileOutputStream outputStream = new FileOutputStream(headerFile);
        outputStream.write(headerContent.getBytes());
        outputStream.close();
        return headerFile;
    }

    protected void tearDown() throws Exception {
        if (product != null) {
            product.dispose();
        }
        if (tDir != null) {
            TestUtils.deleteFileTree(tDir);
        }
    }

}