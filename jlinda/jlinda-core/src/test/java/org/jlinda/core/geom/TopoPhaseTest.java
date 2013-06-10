package org.jlinda.core.geom;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.junit.*;

import java.io.File;

import static org.jlinda.core.io.DataReader.readDoubleData;
import static org.jlinda.core.io.DataReader.readFloatData;

public class TopoPhaseTest {

    static Logger logger = Logger.getLogger(TopoPhase.class.getName());
    private static String testDataDir = "/d2/etna_test/demTest/";
    private static final File masterResFile = new File("/d2/etna_test/demTest/master.res");
    private static final File slaveResFile = new File("/d2/etna_test/demTest/slave.res");

    private static final double DELTA_08 = 1e-08;
    private static final double DELTA_06 = 1e-06;
    private static final double DELTA_04 = 1e-04;

    final static int nRows = 418;
    final static int nCols = 532;

    static TopoPhase topoPhase;
    static DemTile dem;

    static SLCImage masterMeta;
    static Orbit masterOrbit;
    static SLCImage slaveMeta;
    static Orbit slaveOrbit;

    public static Logger initLog() {
        String filePathToLog4JProperties = "log4j.properties";
        Logger logger = Logger.getLogger(TopoPhase.class);
        PropertyConfigurator.configure(filePathToLog4JProperties);
        return logger;
    }

    @BeforeClass
    public static void setUpTestData() throws Exception {

//        initLog();

        // setup DEM tile
        double lat0 = 0.68067840827778847;
        double lon0 = 0.24434609527920614;
        int nLatPixels = 3601;
        int nLonPixels = 3601;
        double latitudeDelta = 1.4544410433280261e-05;
        double longitudeDelta = 1.4544410433280261e-05;
        long nodata = -32768;

        // initialize
        dem = new DemTile(lat0, lon0, nLatPixels, nLonPixels, latitudeDelta, longitudeDelta, nodata);

        // load test data
        String bufferFileName;
        bufferFileName = testDataDir + "dem_full_input.r4.swap";
        float[][] demBuffer = readFloatData(bufferFileName, nRows, nCols).toArray2();

        double[][] demBufferDouble = new double[nRows][nCols];
        // cast to double
        for (int i = 0; i < demBuffer.length; i++) {
            for (int j = 0; j < demBuffer[0].length; j++) {
                demBufferDouble[i][j] = (double) demBuffer[i][j];
            }
        }

        dem.setData(demBufferDouble);

        //// Tile corners : radar coordinates
        final Window tileWindow = new Window(10000, 10127, 1500, 2011);

        // initialize masterMeta
        masterMeta = new SLCImage();
        masterMeta.parseResFile(masterResFile);
        masterMeta.setMlAz(1);
        masterMeta.setMlRg(1);

        masterOrbit = new Orbit();
        masterOrbit.parseOrbit(masterResFile);
        masterOrbit.computeCoefficients(3);

        slaveMeta = new SLCImage();
        slaveMeta.parseResFile(slaveResFile);
        slaveMeta.setMlAz(1);
        slaveMeta.setMlRg(1);

        slaveOrbit = new Orbit();
        slaveOrbit.parseOrbit(slaveResFile);
        slaveOrbit.computeCoefficients(3);

        // initialize
        topoPhase = new TopoPhase(masterMeta, masterOrbit, slaveMeta, slaveOrbit, tileWindow, dem);

//        topoPhase.dem.stats();

    }

    @Test
    public void testRadarCoding() throws Exception {

        double[][] DEMline_buffer;  // dem_line_buffer.r4
        double[][] DEMpixel_buffer; // dem_pixel_buffer.r4
        double[][] input_buffer;    // dem_buffer.r4

        double[][] grd_EXPECTED; // output_buffer.r4

        // load test data
        String testDataDir = "/d2/etna_test/demTest/";
        String bufferFileName;

        bufferFileName = testDataDir + "dem_line_buffer.r8.swap";
        DEMline_buffer = readDoubleData(bufferFileName, nRows, nCols).toArray2();

        bufferFileName = testDataDir + "dem_pixel_buffer.r8.swap";
        DEMpixel_buffer = readDoubleData(bufferFileName, nRows, nCols).toArray2();

        bufferFileName = testDataDir + "dem_buffer.r8.swap";
        input_buffer = readDoubleData(bufferFileName, nRows, nCols).toArray2();

        bufferFileName = testDataDir + "output_buffer.r8.swap";
        grd_EXPECTED = readDoubleData(bufferFileName, 128, 512).toArray2();

        /* computation */
        long t0 = System.currentTimeMillis();
        topoPhase.radarCode();
        long t1 = System.currentTimeMillis();
        logger.info("Data radarcoded in: " + (0.001 * (t1 - t0)) + " sec");

        /* assert result */
        for (int i = 0; i < DEMline_buffer.length; i++) {
            double[] lineArray = DEMline_buffer[i];
            Assert.assertArrayEquals(lineArray, topoPhase.getDemRadarCode_y()[i], DELTA_06);
        }
        for (int i = 0; i < DEMpixel_buffer.length; i++) {
            double[] pixelArray = DEMpixel_buffer[i];
            Assert.assertArrayEquals(pixelArray, topoPhase.getDemRadarCode_x()[i], DELTA_06);
        }
        for (int i = 0; i < grd_EXPECTED.length; i++) {
            double[] phaseArray = input_buffer[i];
            Assert.assertArrayEquals(phaseArray, topoPhase.getDemRadarCode_phase()[i], DELTA_04);
        }

    }

    @Test
    public void testRngAzRatio() throws Exception {

        topoPhase.calculateScalingRatio();
        double ratio_EXPECTED = 5.2487532186594095;
        double ratio_ACTUAL = topoPhase.getRngAzRatio();
        Assert.assertEquals(ratio_EXPECTED, ratio_ACTUAL, DELTA_08);
    }

    @Test
    public void testGridding() throws Exception {

        double[][] grd_EXPECTED; // output_buffer.r4

        // load test data
        String testDataDir = "/d2/etna_test/demTest/";
        String bufferFileName;

        bufferFileName = testDataDir + "output_buffer.r8.swap";
        grd_EXPECTED = readDoubleData(bufferFileName, 128, 512).toArray2();

        /* computation */
        long t0 = System.currentTimeMillis();

        topoPhase.gridData();
        long t1 = System.currentTimeMillis();
        logger.info("Data set gridded in " + (0.001 * (t1 - t0)) + " sec");


        /* assert result */
        for (int i = 0; i < grd_EXPECTED.length; i++) {
            double[] doubles = grd_EXPECTED[i];
            Assert.assertArrayEquals(doubles, topoPhase.demPhase[i], DELTA_04);
        }

    }

}
