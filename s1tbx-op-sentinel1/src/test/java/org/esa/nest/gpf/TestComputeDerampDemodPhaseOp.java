package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.dataio.TAXI.TAXIParameterFileReader;
import org.esa.snap.util.ResourceUtils;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test ComputeDerampDemodPhaseOp
 */
public class TestComputeDerampDemodPhaseOp {

    private final static String inputPath =
            "P:\\s1tbx\\s1tbx\\Data\\testData\\input\\S1A_IW_SLC__1SDV_20140821T165547_20140821T165614_002041_001FC1_8601_split_orb.dim";

    public final static File inputParameterFile = new File(TestData.inputSAR+"InSAR"+File.separator+"pp_m20140809_s20140821_s1a-slc-vv_SS1_with_comments.xml");

    private final static OperatorSpi spi = new ComputeDerampDemodPhaseOp.Spi();
    private TAXIParameterFileReader reader = null;
    private ComputeDerampDemodPhaseOp op = null;
    private Product targetProduct = null;
    private final int width = 20564;
    private final int actOffset = 71;

    public TestComputeDerampDemodPhaseOp() throws Exception {

        final File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }

        if (!inputParameterFile.exists()) {
            TestUtils.skipTest(this, inputParameterFile + " not found");
            return;
        }

        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);
        op = (ComputeDerampDemodPhaseOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false, false);

        reader = new TAXIParameterFileReader(inputParameterFile);
        reader.readParameterFile();
    }

    @Test
    public void testReferenceTime() throws Exception {

        final double dt = 2.055556280538440e-03;    // azimuthTimeInterval
        final int Nburst = 1629;                    // number of lines per burst
        final double vs = reader.sensorVelocity;    // sensor velocity
        final double lambda = reader.waveLength;    // wave length
        final double omegaDeg = 1.590368784000000;  // azimuthSteeringRate
        final double omegaRad = omegaDeg/180*Math.PI;

        final float[] tref = op.computeReferenceTime(0);
        final float[] trimmedTref = new float[width];
        final float[] expectedTref = new float[width];
        for (int i = 0; i < width; i++) {
            trimmedTref[i] = tref[i + actOffset];
            final double Krot = 2*vs*omegaRad/lambda;
            final double Ka = reader.kt[i]*Krot/(reader.kt[i] - Krot);
            final double tc = -reader.DopplerCentroid[i]/Ka;
            expectedTref[i] = (float)(0.5*Nburst*dt + tc + reader.DopplerCentroid[0]/Ka);
        }
        TestUtils.compareArrays(trimmedTref, expectedTref, 1e-2f);
    }

    @Test
    public void testSlantRange() throws Exception {

        final float[] slr = op.computeSlantRange();
        final float[] trimmedSlr = new float[width];
        final float[] expectedSlr = new float[width];
        for (int i = 0; i < width; i++) {
            trimmedSlr[i] = slr[i + actOffset];
            expectedSlr[i] = (float) reader.slantRange[i];
        }
        TestUtils.compareArrays(trimmedSlr, expectedSlr, 0.2f);
    }

    @Test
    public void testKt() throws Exception {

        for (int burstIndex = 0; burstIndex < 9; burstIndex++) {
            final float[] kt = op.computeDopplerRate(burstIndex);
            //String ktFileName = "kt_s1_" + burstIndex + ".txt";
            //outputToFile(kt, ktFileName);
            final float[] trimmedKt = new float[width];
            final float[] expectedKt = new float[width];
            final int expOffset = burstIndex * 20564;
            for (int i = 0; i < width; i++) {
                trimmedKt[i] = kt[i + actOffset];
                expectedKt[i] = (float) reader.kt[i + expOffset];
            }
            TestUtils.compareArrays(trimmedKt, expectedKt, 1e-3f);
        }
    }

    @Test
    public void testFdc() throws Exception {

        for (int burstIndex = 0; burstIndex < 9; burstIndex++) {
            final float[] fdc = op.computeDopplerCentroid(burstIndex);
            //String fdcFileName = "fdc_s1_" + burstIndex + ".txt";
            //outputToFile(fdc, fdcFileName);
            final float[] trimmedFdc = new float[width];
            final float[] expectedFdc = new float[width];
            final int expOffset = burstIndex * 20564;
            for (int i = 0; i < width; i++) {
                trimmedFdc[i] = fdc[i + actOffset];
                expectedFdc[i] = (float) reader.DopplerCentroid[i + expOffset];
            }
            TestUtils.compareArrays(trimmedFdc, expectedFdc, 1e-3f);
        }
    }

    @Test
    public void testDerampPhase() throws Exception {

        //final Band band = targetProduct.getBandAt(0);
        //assertNotNull(band);

        // readPixels: execute computeTiles()
        //final float[] floatValues = new float[8];
        //band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        //final float[] expectedValues = {11.0f, 15.0f, 19.0f, 23.0f, 43.0f, 47.0f, 51.0f, 55.0f};
        //assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    private void outputToFile(final float[] dataArray, final String fileName) throws Exception {

        final File appUserDir = new File(ResourceUtils.getApplicationUserDir(true).getAbsolutePath() + File.separator + "log");
        if (!appUserDir.exists()) {
            appUserDir.mkdirs();
        }
        final File ftFile = new File(appUserDir.toString(), fileName);
        final FileOutputStream out = new FileOutputStream(ftFile.getAbsolutePath(), false);
        PrintStream p = new PrintStream(out);
        for (float data:dataArray) {
            p.format("%9.4f ", data);
        }
        p.println();
        p.close();
    }
}
