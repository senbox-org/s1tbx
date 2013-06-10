package org.jlinda.core.geocode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang.time.StopWatch;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.utils.SarUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class DInSARTest {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(DInSARTest.class);

    private static final double PHASE2DEFO = (0.056d / (4d * Math.PI));

    private static final String defoResDir = "/d2/test.processing/bam/bam.quake.defo/";
    private static final String defoDataDir = defoResDir + "Outdata/";

    private static final String topoResDir = "/d2/test.processing/bam/bam.quake.topo/";
    private static final String topoDataDir = topoResDir + "Outdata/";

    private static final int ORBIT_DEGREE = 3;
    private static final double DELTA_02 = 2 * 1e-02; // 0.002m allowed

    private static DoubleMatrix topoPhase; // unwrapped
    private static ComplexDoubleMatrix defoCplxIfg; // expected
    private static ComplexDoubleMatrix cplxIfg;

    private static Window totalDataWindow;

    private static Window tileWindow;
    private static SLCImage masterMeta;
    private static SLCImage topoSlaveMeta;

    private static SLCImage defoSlaveMeta;
    private static Orbit masterOrbit;
    private static Orbit topoSlaveOrbit;
    private static Orbit defoSlaveOrbit;


    @Before
    public void setUp() throws Exception {
        logger.setLevel(Level.DEBUG);
    }

    @BeforeClass
    public static void setUpTestData() throws Exception {

        /** load Input Binary Data */
        String unwFileName = "9192_6687.uint";
        String dInSARFileName = "9192_9693.dcint";
        String inSARFileName = "9192_9693.srp";

        topoPhase = toDoubleMatrix(512, 512, readFloatFile(512, 512, topoDataDir + unwFileName));
        defoCplxIfg = toComplexDoubleMatrix(512, 512, readComplexFloatFile(512, 512, defoDataDir + dInSARFileName));
        cplxIfg = toComplexDoubleMatrix(512, 512, readComplexFloatFile(512, 512, defoDataDir + inSARFileName));

        File masterResFile = new File(topoResDir + "9192.res");
        File topoResFile = new File(topoResDir + "6687.res");
        File defoResFile = new File(defoResDir + "9693.res");

        /** Construct metadata classes */
        masterMeta = new SLCImage();
        masterMeta.parseResFile(masterResFile);

        defoSlaveMeta = new SLCImage();
        defoSlaveMeta.parseResFile(topoResFile);

        topoSlaveMeta = new SLCImage();
        topoSlaveMeta.parseResFile(defoResFile);

        masterOrbit = new Orbit();
        masterOrbit.parseOrbit(masterResFile);
        masterOrbit.computeCoefficients(3);
        masterOrbit.computeCoefficients(ORBIT_DEGREE);

        topoSlaveOrbit = new Orbit();
        topoSlaveOrbit.parseOrbit(topoResFile);
        topoSlaveOrbit.computeCoefficients(3);
        topoSlaveOrbit.computeCoefficients(ORBIT_DEGREE);

        defoSlaveOrbit = new Orbit();
        defoSlaveOrbit.parseOrbit(defoResFile);
        defoSlaveOrbit.computeCoefficients(3);
        defoSlaveOrbit.computeCoefficients(ORBIT_DEGREE);

        totalDataWindow = new Window(1, 26897, 1, 5167);

        tileWindow = new Window(15000, 15511, 3000, 3511);
    }

    @Test
    public void testDinsarTotal() throws Exception {

        ComplexDoubleMatrix defoData = cplxIfg.dup();

        StopWatch watch = new StopWatch();
        watch.start();
        DInSAR dinsar = new DInSAR(masterMeta, masterOrbit, defoSlaveMeta, defoSlaveOrbit, topoSlaveMeta, topoSlaveOrbit);
        dinsar.setDataWindow(totalDataWindow);
        dinsar.setTileWindow(tileWindow);
        dinsar.setTopoData(topoPhase);
        dinsar.setDefoData(defoData);
        dinsar.dinsar();
        watch.stop();

        logger.info("Total processing time: {} milli-seconds", watch.getTime());

        // subtracted expected minus computed and convert to deformation : check on +/- 0.001m level
        ComplexDoubleMatrix expected = SarUtils.computeIfg(defoCplxIfg, dinsar.getDefoData());

        int numOfElements = expected.length;

        double[] defoDelta = new double[numOfElements];
        for (int i = 0; i < numOfElements; i++) {
            defoDelta[i] = Math.atan2(expected.getImag(i), expected.getReal(i)) * PHASE2DEFO;
        }
        Assert.assertArrayEquals(DoubleMatrix.zeros(defoCplxIfg.length).toArray(), defoDelta, DELTA_02);
    }

    @Test
    public void testDinsar() throws Exception {

        ComplexDoubleMatrix defoData = cplxIfg.dup();

        StopWatch watch = new StopWatch();
        watch.start();
        DInSAR dinsar = new DInSAR(masterMeta, masterOrbit, defoSlaveMeta, defoSlaveOrbit, topoSlaveMeta, topoSlaveOrbit);
        dinsar.setDataWindow(totalDataWindow);
        dinsar.computeBperpRatios();

        dinsar.applyDInSAR(tileWindow, defoData, topoPhase);
        watch.stop();

        logger.info("Total processing time: {} milli-seconds", watch.getTime());

        // subtracted expected minus computed and convert to deformation : check on +/- 0.001m level
        ComplexDoubleMatrix expected = SarUtils.computeIfg(defoCplxIfg, defoData);

        int numOfElements = expected.length;

        double[] defoDelta = new double[numOfElements];
        for (int i = 0; i < numOfElements; i++) {
            defoDelta[i] = Math.atan2(expected.getImag(i), expected.getReal(i)) * PHASE2DEFO;
        }
        Assert.assertArrayEquals(DoubleMatrix.zeros(defoCplxIfg.length).toArray(), defoDelta, DELTA_02);
    }

    private static DoubleMatrix toDoubleMatrix(final int rows, final int cols, final float[] in) {
        DoubleMatrix out = new DoubleMatrix(rows, cols);
        for (int i = 0; i < out.rows; i++) {
            for (int j = 0; j < out.columns; j++) {
                out.put(i, j, in[i * cols + j]);
            }
        }
        return out;
    }

    private static ComplexDoubleMatrix toComplexDoubleMatrix(final int rows, final int cols, final float[] in) {
        ComplexDoubleMatrix out = new ComplexDoubleMatrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            int cnt = 0;
            for (int j = 0; j < 2 * cols; j = j + 2) {
                double re = in[i * (2 * cols) + j];
                double im = in[i * (2 * cols) + (j + 1)];
                out.put(i, cnt, new ComplexDouble(re, im));
                cnt++;
            }
        }
        return out;
    }

    private static double[] toDoubleArray(float[] in) {
        double[] temp = new double[in.length];
        for (int i = 0; i < in.length; i++) {
            temp[i] = (double) in[i];
        }
        return temp;
    }

    private static float[] readFloatFile(final int lines, final int cols, final String fileName) throws IOException {

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

    private static float[] readComplexFloatFile(final int lines, final int cols, final String fileName) throws IOException {
        return readFloatFile(lines, 2 * cols, fileName);
    }


}
