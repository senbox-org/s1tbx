package org.jlinda.core.geocode;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.perf4j.StopWatch;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.junit.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Slant2HeightTest {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Slant2HeightTest.class);

    private static final String testDataDir = "/d2/test.processing/bam/bam.quake.dem/Outdata/";
    private static final String testResDir = "/d2/test.processing/bam/bam.quake.dem/";

    private static final int ORBIT_DEGREE = 3;

    private static final double DELTA_02 = 1e-02; // tolerance level of 0.01m

    private static DoubleMatrix unwrappedPhase;
    private static DoubleMatrix heights;

    private static Window dataWindow;
    private static Window tileWindow;

    private static SLCImage master;
    private static SLCImage slave;
    private static Orbit masterOrbit;
    private static Orbit slaveOrbit;

    // TODO: refactor data reader into util classes of  jlinda-core.io package

    @Before
    public void setUp() throws Exception {
        logger.setLevel(Level.DEBUG);
    }

    @BeforeClass
    public static void setUpTestData() throws Exception {

        /** load Input Binary Data */
        String unwFileName = "9192_6687.uint";
        String hghtFileName = "heights.raw.ORIGINAL";

        unwrappedPhase = castToDouble(512, 512, readFloatFile(512, 512, testDataDir + unwFileName));
        heights = castToDouble(512, 512, readFloatFile(512, 512, testDataDir + hghtFileName));

        File masterResFile = new File(testResDir + "9192.res");
        File slaveResFile = new File(testResDir + "6687.res");

        /** Construct metadata classes */
        master = new SLCImage();
        slave = new SLCImage();

        master.parseResFile(masterResFile);
        slave.parseResFile(slaveResFile);

        masterOrbit = new Orbit();

        masterOrbit.parseOrbit(masterResFile);
        masterOrbit.computeCoefficients(3);
        masterOrbit.computeCoefficients(ORBIT_DEGREE);

        slaveOrbit = new Orbit();

        slaveOrbit.parseOrbit(slaveResFile);
        slaveOrbit.computeCoefficients(3);
        slaveOrbit.computeCoefficients(ORBIT_DEGREE);

        dataWindow = new Window(1, 26897, 1, 5167);

        tileWindow = new Window(15000, 15511, 3000, 3511);

    }

    private static float[] readFloatFile(int lines, int cols, String fileName) throws IOException {

        RandomAccessFile file = new RandomAccessFile(fileName, "r");
        byte[] recordBuffer = new byte[4 * lines * cols];
        ByteBuffer record = ByteBuffer.wrap(recordBuffer);
        record.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatRecordBuffer = record.asFloatBuffer();

        file.read(recordBuffer);

        float[] out = new float[lines * cols];
        floatRecordBuffer.rewind();
        floatRecordBuffer.get(out, 0, out.length);
        return out;
    }

    private static DoubleMatrix castToDouble(int rows, int cols, float[] in) {
        DoubleMatrix out = new DoubleMatrix(rows, cols);
        for (int i = 0; i < out.rows; i++) {
            for (int j = 0; j < out.columns; j++) {
                out.put(i, j, in[i * cols + j]);
            }
        }
        return out;
    }

    @Test
    public void testSchwabisch_Prototype() throws Exception {

        int nPoints = 200;
        int nHeights = 3;
        int degree1d = 2;
        int degree2d = 5;

        DoubleMatrix inputTile = unwrappedPhase.dup();

        StopWatch watch = new StopWatch();
        watch.start();
        final Slant2Height tempSlant = new Slant2Height(nPoints, nHeights, degree1d, degree2d, master, masterOrbit, slave, slaveOrbit);
        tempSlant.setTileWindow(tileWindow);
        tempSlant.setDataWindow(dataWindow);
        tempSlant.setTile(inputTile);
        tempSlant.schwabischTotal();
        watch.stop();

        logger.info("Total processing time TOTAL: {} milli-seconds", watch.getElapsedTime());
        Assert.assertArrayEquals(heights.toArray(), tempSlant.getTile().toArray(), DELTA_02);

    }

    @Test
    public void testSchwabisch() throws Exception{

        int nPoints = 200;
        int nHeights = 3;
        int degree1d = 2;
        int degree2d = 5;

        DoubleMatrix inputTile = unwrappedPhase.dup();

        StopWatch watch = new StopWatch();

        watch.start();
        final Slant2Height slant = new Slant2Height(nPoints, nHeights, degree1d, degree2d, master, masterOrbit, slave, slaveOrbit);
        slant.setDataWindow(dataWindow);
        slant.schwabisch();
        slant.applySchwabisch(tileWindow, inputTile);
        watch.stop();
        logger.info("Total processing time for Slant2Height Schwabisch method: {} milli-seconds", watch.getElapsedTime());

        Assert.assertArrayEquals(heights.toArray(), inputTile.toArray(), DELTA_02);

    }

}