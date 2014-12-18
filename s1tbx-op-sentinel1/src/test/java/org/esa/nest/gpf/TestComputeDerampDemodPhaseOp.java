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

    @Test
    public void testSentinelPODOrbitFileOperations() throws Exception {
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

        final ComputeDerampDemodPhaseOp op = (ComputeDerampDemodPhaseOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false, false);

        final TAXIParameterFileReader reader = new TAXIParameterFileReader(inputParameterFile);
        reader.readParameterFile();

        final int width = 20564;
        for (int burstIndex = 0; burstIndex < 9; burstIndex++) {
            final float[] kt = op.computeDopplerRate(burstIndex);
            //String ktFileName = "kt" + burstIndex + ".txt";
            //outputToFile(kt, ktFileName);
            final float[] trimmedKt = new float[width];
            final float[] expectedKt = new float[width];
            final int actOffset = 71;
            final int expOffset = burstIndex*20564;
            for (int i = 0; i < width; i++) {
                trimmedKt[i] = kt[i + actOffset];
                expectedKt[i] = (float)reader.kt[i + expOffset];
            }
            TestUtils.compareArrays(trimmedKt, expectedKt, 1e-3f);
        }


        for (int burstIndex = 0; burstIndex < 9; burstIndex++) {
            final float[] fdc = op.computeDopplerCentroid(burstIndex);
            //String fdcFileName = "fdc" + burstIndex + ".txt";
            //outputToFile(fdc, fdcFileName);
            final float[] trimmedFdc = new float[width];
            final float[] expectedFdc = new float[width];
            final int actOffset = 71;
            final int expOffset = burstIndex*20564;
            for (int i = 0; i < width; i++) {
                trimmedFdc[i] = fdc[i + actOffset];
                expectedFdc[i] = (float)reader.DopplerCentroid[i + expOffset];
            }
            TestUtils.compareArrays(trimmedFdc, expectedFdc, 1e-3f);
        }

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

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
