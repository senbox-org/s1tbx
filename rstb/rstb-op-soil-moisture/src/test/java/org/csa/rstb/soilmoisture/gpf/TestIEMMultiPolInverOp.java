package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.soilmoisture.gpf.support.IEMInverBase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for IEMMultiPolInverOperator.
 */
public class TestIEMMultiPolInverOp {

    private final static OperatorSpi spi = new IEMMultiPolInverOp.Spi();
    private final static double epsilon = 1.0e-4d;

    final private int rows = 3;
    final private int cols = 2;

    public static boolean almostEqual(final double a, final double b, final double epsilon) {

        return Math.abs(a - b) < epsilon;
    }

    private static void addClay(final Product prod, final int w, final int h) {

        final double[] clayValues = new double[w * h];
        final Band clayBand = prod.addBand("AAFC Canada Clay Pct", ProductData.TYPE_FLOAT64);
        clayBand.setUnit(Unit.REAL);
        clayValues[0] = 0.13 * 100.0;   // (1, 1)
        clayValues[1] = 0.23 * 100.0;   // (1, 2)
        clayValues[2] = 0.03 * 100.0;   // (2, 1)
        clayValues[3] = 0.03 * 100.0;   // (2, 2)
        clayValues[4] = 0.99 * 100.0;   // (3, 1)
        clayValues[5] = 0.23 * 100.0;   // (3, 2)
        ProductData clayData = ProductData.createInstance(clayValues);
        clayBand.setData(clayData);
    }

    private static void addSand(final Product prod, final int w, final int h) {

        final double[] sandValues = new double[w * h];
        final Band sandBand = prod.addBand("AAFC Canada Sand Pct", ProductData.TYPE_FLOAT64);
        sandBand.setUnit(Unit.REAL);
        sandValues[0] = 0.15 * 100.0;   // (1, 1)
        sandValues[1] = 0.83 * 100.0;   // (1, 2)
        sandValues[2] = 0.43 * 100.0;   // (2, 1)
        sandValues[3] = 0.98 * 100.0;   // (2, 2)
        sandValues[4] = 0.26 * 100.0;   // (3, 1)
        sandValues[5] = 0.75 * 100.0;   // (3, 2)
        ProductData sandData = ProductData.createInstance(sandValues);
        sandBand.setData(sandData);
    }

    /**
     * Creates a test product
     *
     * @param w    width
     * @param h    height
     * @param clay Include clay or not in test product
     * @param sand Include sand or not in test product
     * @return the created product
     */
    private static Product createOneTestProduct(final int w, final int h, final boolean clay, final boolean sand) {

        Product[] testProduct = new Product[1];

        testProduct[0] = TestUtils.createProduct("backscatter and LIA", w, h);

        // AM image

        final double[] sigmaHHValuesAM = new double[w * h];
        final Band sigmaHHBandAM = testProduct[0].addBand("Sigma0_HH_slv2_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaHHBandAM.setUnit("dB");
        sigmaHHValuesAM[0] = -4.766823504180530d;   // (1, 1) zero-based LUT section row index = 9999, LUT(9999+1,:)
        sigmaHHValuesAM[1] = -57.557342679677213d;  // (1, 2) zero-based LUT section row index = 13111, LUT(3*13252+13111+1,:)
        sigmaHHValuesAM[2] = -14.548655535009495d;  // (2, 1) zero-based LUT section row index = 0,  LUT(1*13252+0+1,:)
        sigmaHHValuesAM[3] = -55.231979908177991d;  // (2, 2) zero-based LUT section row index = 13251, LUT(2*13252+13251+1,:)
        sigmaHHValuesAM[4] = -10.174902142852103d;  // (3, 1) zero-based LUT section row index = 500, LUT(2*13252+500+1,:)
        sigmaHHValuesAM[5] = -4.511646814038612d;   // (3, 2)  zero-based LUT section row index = 6768, LUT(1*13252+6768+1,:)
        ProductData sigmaHHDataAM = ProductData.createInstance(sigmaHHValuesAM);
        sigmaHHBandAM.setData(sigmaHHDataAM);

        final double[] sigmaVVValuesAM = new double[w * h];
        final Band sigmaVVBandAM = testProduct[0].addBand("Sigma0_VV_mst_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaVVBandAM.setUnit("dB");
        sigmaVVValuesAM[0] = -6.638491443439992d;  // (1, 1)
        sigmaVVValuesAM[1] = -55.076346321006682d; // (1, 2)
        sigmaVVValuesAM[2] = -15.123436473461105d; // (2, 1)
        sigmaVVValuesAM[3] = -51.900190057043915d; // (2, 2)
        sigmaVVValuesAM[4] = -10.213734873895699d; // (3, 1)
        sigmaVVValuesAM[5] = -18.284058098109213d; // (3, 2)
        ProductData sigmaVVDataAM = ProductData.createInstance(sigmaVVValuesAM);
        sigmaVVBandAM.setData(sigmaVVDataAM);

        final double[] thetaValuesAM = new double[w * h];
        final Band thetaBandAM = testProduct[0].addBand("incidenceAngleFromEllipsoid_slv4_11Oct2013", ProductData.TYPE_FLOAT64);
        thetaBandAM.setUnit("degrees");
        thetaValuesAM[0] = 18.1;    // (1, 1)
        thetaValuesAM[1] = 21.2;    // (1, 2)
        thetaValuesAM[2] = 19.0;    // (2, 1)
        thetaValuesAM[3] = 20.0;    // (2, 2)
        thetaValuesAM[4] = 20.3;    // (3, 1)
        thetaValuesAM[5] = 19.3;    // (3, 2)
        ProductData thetaDataAM = ProductData.createInstance(thetaValuesAM);
        thetaBandAM.setData(thetaDataAM);

        final double[] sigmaHVValuesAM = new double[w * h];
        final Band sigmaHVBandAM = testProduct[0].addBand("Sigma0_HV_slv3_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaHVBandAM.setUnit("dB");
        for (int i = 0; i < sigmaHVValuesAM.length; i++) {
            sigmaHVValuesAM[i] = 0.0;
        }
        ProductData sigmaHVDataAM = ProductData.createInstance(sigmaHVValuesAM);
        sigmaHVBandAM.setData(sigmaHVDataAM);

        final double[] sigmaVHValuesAM = new double[w * h];
        final Band sigmaVHBandAM = testProduct[0].addBand("Sigma0_VH_slv1_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaVHBandAM.setUnit("dB");
        for (int i = 0; i < sigmaVHValuesAM.length; i++) {
            sigmaVHValuesAM[i] = 0.0;
        }
        ProductData sigmaVHDataAM = ProductData.createInstance(sigmaVHValuesAM);
        sigmaVHBandAM.setData(sigmaVHDataAM);


        final MetadataElement metadata0 = AbstractMetadata.getAbstractedMetadata(testProduct[0]);

        metadata0.setAttributeDouble(AbstractMetadata.incidence_near, 18.1d);
        metadata0.setAttributeDouble(AbstractMetadata.incidence_far, 21.2d);

        if (clay) {

            addClay(testProduct[0], w, h);
        }

        if (sand) {

            addSand(testProduct[0], w, h);
        }

        return testProduct[0];
    }

    /**
     * Tests IEM Multi-Polarization Inversion operator with a test product.
     *
     * @throws Exception general exception
     */
    @Test
    public void testIEMMultiPolInversionOfRealImage() throws Exception {

        final double epsilon = 1.0e-4d;

        myTest(false, false); // no clay, no sand
        myTest(true, false); // just clay, no sand
        myTest(false, true); // no clay, just sand
        myTest(true, true);  // both clay and sand
    }

    private void myTest(final boolean clay, final boolean sand) throws Exception {

        TestUtils.log.info("***************** myTest: clay = " + clay + " sand = " + sand + " epsilon = " + epsilon);

        int numBands = 2; // rms and RDC
        if (clay) {
            numBands++;
        }
        if (sand) {
            numBands++;
        }

        final Product sourceProduct = createOneTestProduct(cols, rows, clay, sand);
        final Path resourcePath = ResourceInstaller.findModuleCodeBasePath(IEMInverBase.class).resolve("auxdata/sm_luts");

        final IEMMultiPolInverOp op = (IEMMultiPolInverOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setLUTFile(resourcePath.resolve("IEMC_cb_nt_RDC_RSAT2_QP.mat").toFile().getAbsolutePath());
        op.setOptionalOutputs(true);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();

        TestUtils.log.info("Target product name = " + targetProduct.getName() +
                "; #bands in target product = " + targetProduct.getNumBands() +
                "; expected numBands = " + numBands);

        // target bands
        final Band[] band = new Band[numBands];
        for (int i = 0; i < band.length; i++) {
            band[i] = targetProduct.getBandAt(i);
            assertNotNull(band[i]);
        }

        // readPixels: execute computeTiles()
        final double[][] resultValues = new double[numBands][cols * rows];
        for (int i = 0; i < resultValues.length; i++) {

            final ProductData prodData = band[i].createCompatibleProductData((int) band[i].getNumDataElems());

            band[i].readRasterData(0, 0, cols, rows, prodData, ProgressMonitor.NULL);

            for (int j = 0; j < resultValues[i].length; j++) {

                resultValues[i][j] = prodData.getElemDoubleAt(j);
            }

            prodData.dispose();
        }

        final double[] rmsExpectedValues = {1.3d, 2.6d, 0.3d, 2.6d, 0.3d, 0.7d};
        final double[] rdcExpectedValues = {16.289295999999997d, 8.179600000000002d, 3.125824000000000d, 38.688400000000009d, 8.667135999999999d, 27.164943999999998d};

        final double[] clayExpectedValues = {13.0d, 23.0d, 3.0d, 3.0d, 99.0d, 23.0d};
        final double[] sandExpectedValues = {15.0d, 83.0d, 43.0d, 98.0d, 26.0d, 75.0};

        // compare with expected outputs:
        final double[][] expectedValues = new double[numBands][cols * rows];

        int idx = 2;

        expectedValues[0] = rmsExpectedValues; // 1st band rms is 1st col in LUT
        expectedValues[1] = rdcExpectedValues; // 2nd band rdc is 3rd col in LUT

        if (clay) {
            expectedValues[2] = clayExpectedValues; // band clay
            idx++;
        }

        if (sand) {
            expectedValues[idx] = sandExpectedValues; // band sand
            idx++;
        }

        final StringBuilder resultsStr = new StringBuilder("Result values:");
        for (double[] resultValue : resultValues) {
            for (double aResultValue : resultValue) {
                resultsStr.append(" ");
                resultsStr.append(aResultValue);
            }
            resultsStr.append("\n");
        }
        TestUtils.log.info(resultsStr.toString());

        final StringBuilder expectedStr = new StringBuilder("Expected values:");
        for (double[] expectedValue : expectedValues) {
            for (double anExpectedValue : expectedValue) {
                expectedStr.append(" ");
                expectedStr.append(anExpectedValue);
            }
            expectedStr.append("\n");
        }
        TestUtils.log.info(expectedStr.toString());

        for (int i = 0; i < resultValues.length; i++) {
            for (int j = 0; j < resultValues[i].length; j++) {
                assertTrue(almostEqual(resultValues[i][j], expectedValues[i][j], epsilon));
                if (!almostEqual(resultValues[i][j], expectedValues[i][j], epsilon)) {
                    TestUtils.log.warning("Not Equal: i = " + i + " j = " + j + " " + resultValues[i][j] + " " + expectedValues[i][j]);
                }
            }
        }
    }
}
