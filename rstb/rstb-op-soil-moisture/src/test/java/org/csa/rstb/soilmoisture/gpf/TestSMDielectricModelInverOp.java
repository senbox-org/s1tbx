package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for SMDielectricModelInverOperator.
 */
public class TestSMDielectricModelInverOp {

    private final static OperatorSpi spi = new SMDielectricModelInverOp.Spi();

    static {
        TestUtils.initTestEnvironment();
    }

    public static boolean almostEqual(final double a, final double b, final double epsilon) {

        return Math.abs(a - b) < epsilon;
    }

    /**
     * Creates test products
     *
     * @param w width
     * @param h height
     * @return the created products
     */
    private static Product createTestProductForHallikainen(final int w, final int h) {

        Product testProduct = null;

        // RDC source product

        testProduct = TestUtils.createProduct("soil real dielectric constant", w, h);
        testProduct.setName("soil real dielectric constant");

        // rms band (this band is not used)
        final double[] rmsValues = new double[w * h];
        final Band rmsBand = testProduct.addBand("rms", ProductData.TYPE_FLOAT64);
        rmsBand.setUnit("cm");
        rmsValues[0] = 15.945;   // (1, 1)
        rmsValues[1] = 65.945;   // (1, 2)
        rmsValues[2] = 19.163;   // (2, 1)
        rmsValues[3] = 1.01;     // (2, 2)
        rmsValues[4] = 4.572;    // (3, 1)
        rmsValues[5] = 34.572;   // (3, 2)
        ProductData rmsData = ProductData.createInstance(rmsValues);
        rmsBand.setData(rmsData);

        // RDC band
        final double[] rdcValues = new double[w * h];
        final Band rdcBand = testProduct.addBand("RDC", ProductData.TYPE_FLOAT64);
        rdcBand.setUnit("Farad/m");
        rdcValues[0] = 55.6;    // (1, 1)
        rdcValues[1] = 5.6;     // (1, 2)
        rdcValues[2] = -5.6;    // (2, 1)
        rdcValues[3] = 15.6;    // (2, 2)
        rdcValues[4] = 11.6;    // (3, 1)
        rdcValues[5] = 20.6;    // (3, 2)
        ProductData rdcData = ProductData.createInstance(rdcValues);
        rdcBand.setData(rdcData);

        // sand band
        final double[] sandValues = new double[w * h];
        final Band sandBand = testProduct.addBand("Sand", ProductData.TYPE_FLOAT64);
        sandValues[0] = 0.02;   // (1, 1)
        sandValues[1] = 0.32;   // (1, 2)
        sandValues[2] = 0.02;   // (2, 1)
        sandValues[3] = 0.2;    // (2, 2)
        sandValues[4] = 0.01;   // (3, 1)
        sandValues[5] = 0.62;   // (3, 2)
        ProductData sandData = ProductData.createInstance(sandValues);
        sandBand.setData(sandData);

        // clay band
        final double[] clayValues = new double[w * h];
        final Band clayBand = testProduct.addBand("Clay", ProductData.TYPE_FLOAT64);
        clayValues[0] = 0.13;   // (1, 1)
        clayValues[1] = 0.23;   // (1, 2)
        clayValues[2] = 0.03;   // (2, 1)
        clayValues[3] = 0.03;   // (2, 2)
        clayValues[4] = 0.99;   // (3, 1)
        clayValues[5] = 0.23;   // (3, 2)
        ProductData sigmaHHDataPM = ProductData.createInstance(clayValues);
        clayBand.setData(sigmaHHDataPM);

        return testProduct;
    }

    /**
     * Creates test products
     *
     * @param w width
     * @param h height
     * @return the created products
     */
    private static Product createTestProductForMironov(final int w, final int h) {
        // RDC source product

        final Product testProduct = TestUtils.createProduct("soil real dielectric constant", w, h);
        testProduct.setName("soil real dielectric constant");

        // rms band (this band is not used)
        final double[] rmsValues = new double[w * h];
        final Band rmsBand = testProduct.addBand("rms", ProductData.TYPE_FLOAT64);
        rmsBand.setUnit("cm");
        rmsValues[0] = 15.945;   // (1, 1)
        rmsValues[1] = 65.945;   // (1, 2)
        rmsValues[2] = 19.163;   // (2, 1)
        rmsValues[3] = 1.01;     // (2, 2)
        rmsValues[4] = 4.572;    // (3, 1)
        rmsValues[5] = 34.572;   // (3, 2)
        ProductData rmsData = ProductData.createInstance(rmsValues);
        rmsBand.setData(rmsData);

        // RDC band
        final double[] rdcValues = new double[w * h];
        final Band rdcBand = testProduct.addBand("RDC", ProductData.TYPE_FLOAT64);
        rdcBand.setUnit("Farad/m");
        rdcValues[0] = 55.6;    // (1, 1)
        rdcValues[1] = 5.6;     // (1, 2)
        rdcValues[2] = -5.6;    // (2, 1)
        rdcValues[3] = 6.6;    // (2, 2)
        rdcValues[4] = 4.6;    // (3, 1)
        rdcValues[5] = 3.6;    // (3, 2)
        ProductData rdcData = ProductData.createInstance(rdcValues);
        rdcBand.setData(rdcData);

        // clay band
        final double[] clayValues = new double[w * h];
        final Band clayBand = testProduct.addBand("Clay", ProductData.TYPE_FLOAT64);
        clayValues[0] = 0.13;   // (1, 1)
        clayValues[1] = 0.23;   // (1, 2)
        clayValues[2] = 0.03;   // (2, 1)
        clayValues[3] = 0.03;   // (2, 2)
        clayValues[4] = 0.99;   // (3, 1)
        clayValues[5] = 0.23;   // (3, 2)
        ProductData sigmaHHDataPM = ProductData.createInstance(clayValues);
        clayBand.setData(sigmaHHDataPM);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(testProduct);
        absRoot.setAttributeDouble(AbstractMetadata.radar_frequency, 5.4e9 / Constants.oneMillion);

        return testProduct;
    }

    /**
     * Tests SM Dielectric Model Inversion operator using Hallikainen model with a test product.
     *
     * @throws Exception general exception
     */
    @Test
    public void testSMHallikainenInversionOfRealImage() throws Exception {

        TestUtils.log.info("testSMHallikainenInversionOfRealImage: called");

        final Product sourceProduct = createTestProductForHallikainen(2, 3);

        final SMDielectricModelInverOp op = (SMDielectricModelInverOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setModelToUse(DielectricModelFactory.HALLIKAINEN);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();

        final int numTargetBands = 1;

        final Band[] band = new Band[numTargetBands];
        for (int i = 0; i < numTargetBands; i++) {
            band[i] = targetProduct.getBandAt(i);
            assertNotNull(band[i]);
        }

        // readPixels: execute computeTiles()
        final double[][] resultValues = new double[numTargetBands][6];
        for (int i = 0; i < numTargetBands; i++) {

            final ProductData prodData = band[i].createCompatibleProductData((int) band[i].getNumDataElems());

            band[i].readRasterData(0, 0, 2, 3, prodData, ProgressMonitor.NULL);

            for (int j = 0; j < resultValues[i].length; j++) {

                resultValues[i][j] = prodData.getElemDoubleAt(j);
            }

            prodData.dispose();
        }

        // minSM = 0.0; maxSM = 0.55;
        // compare with expected outputs from matlab:
        final double[][] expectedValues = {
                {0.549941170179302d, 0.092493816012058d,
                        0.000058829820698d, 0.326805336945222d,
                        0.237618455824864d, 0.432998727049425d}
        };

        for (double[] resultValue : resultValues) {
            for (double aResultValue : resultValue) {
                System.out.print(" " + aResultValue);
            }
            System.out.println("");
        }

        for (double[] expectedValue : expectedValues) {
            for (double anExpectedValue : expectedValue) {
                System.out.print(" " + anExpectedValue);
            }
            System.out.println("");
        }


        for (int i = 0; i < resultValues.length; i++) {
            for (int j = 0; j < resultValues[i].length; j++) {
                //System.out.println(i + " " + j);
                assertTrue(almostEqual(resultValues[i][j], expectedValues[i][j], 1.0e-4d));
                if (!almostEqual(resultValues[i][j], expectedValues[i][j], 1.0e-4d)) {
                    System.out.println("i = " + i + " j = " + j + " " + resultValues[i][j] + " " + expectedValues[i][j]);
                }
            }
        }

    }

    /**
     * Tests SM Dielectric Model Inversion operator using Mironov model with a test product.
     *
     * @throws Exception general exception
     */
    @Test
    public void testSMMironovInversionOfRealImage() throws Exception {

        TestUtils.log.info("testSMMironovInversionOfRealImage: called");

        final Product sourceProduct = createTestProductForMironov(2, 3);

        final SMDielectricModelInverOp op = (SMDielectricModelInverOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setModelToUse(DielectricModelFactory.MIRONOV);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();

        final int numTargetBands = 1;

        final Band[] band = new Band[numTargetBands];
        for (int i = 0; i < numTargetBands; i++) {
            band[i] = targetProduct.getBandAt(i);
            assertNotNull(band[i]);
        }

        // readPixels: execute computeTiles()
        final double[][] resultValues = new double[numTargetBands][6];
        for (int i = 0; i < numTargetBands; i++) {

            final ProductData prodData = band[i].createCompatibleProductData((int) band[i].getNumDataElems());

            band[i].readRasterData(0, 0, 2, 3, prodData, ProgressMonitor.NULL);

            for (int j = 0; j < resultValues[i].length; j++) {

                resultValues[i][j] = prodData.getElemDoubleAt(j);
            }

            prodData.dispose();
        }

        // minSM = 0.0; maxSM = 0.55; Symmetrized is ON
        // There is no matlab code to compare with. The code has been tested against smppd code and is
        // assumed to be correct.
        final double[][] expectedValues = {
                {0.5499999993582143d, 0.08644842509003553d, 1.016862481617742E-9,
                        0.10965759608738125d, 0.06178982823243868d, 0.03219541458455383d}
        };

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
                //System.out.println(i + " " + j);
                assertTrue(almostEqual(resultValues[i][j], expectedValues[i][j], 1.0e-4d));
                if (!almostEqual(resultValues[i][j], expectedValues[i][j], 1.0e-4d)) {
                    TestUtils.log.warning("Not Equal: i = " + i + " j = " + j + " " + resultValues[i][j] + " " + expectedValues[i][j]);
                }
            }
        }

    }
}
