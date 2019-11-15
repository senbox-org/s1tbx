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
 * Unit test for IEMHybridInverOperator.
 */
@Ignore
public class TestIEMHybridInverOp {

    private final static OperatorSpi spi = new IEMHybridInverOp.Spi();
    private final static double epsilon = 1.0e-4d;

    final private int rows = 3;
    final private int cols = 2;

    private static boolean almostEqual(final double a, final double b, final double epsilon) {
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
        final ProductData clayData = ProductData.createInstance(clayValues);
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
        final ProductData sandData = ProductData.createInstance(sandValues);
        sandBand.setData(sandData);
    }

    private static Product createOneTestProduct(final int w, final int h, final boolean clay, final boolean sand) {

        final Product[] testProduct = new Product[1];

        testProduct[0] = TestUtils.createProduct("backscatter and LIA", w, h);

        // AM image

        final double[] sigmaHHValuesAM = new double[w * h];
        final Band sigmaHHBandAM = testProduct[0].addBand("Sigma0_HH_slv2_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaHHBandAM.setUnit("dB");
        sigmaHHValuesAM[0] = -5.945;    // (1, 1)
        sigmaHHValuesAM[1] = -65.945;   // (1, 2)
        sigmaHHValuesAM[2] = -19.163;   // (2, 1)
        sigmaHHValuesAM[3] = 1.01;     // (2, 2)
        sigmaHHValuesAM[4] = -4.572;    // (3, 1)
        sigmaHHValuesAM[5] = -64.572;   // (3, 2)
        final ProductData sigmaHHDataAM = ProductData.createInstance(sigmaHHValuesAM);
        sigmaHHBandAM.setData(sigmaHHDataAM);

        final double[] sigmaVVValuesAM = new double[w * h];
        final Band sigmaVVBandAM = testProduct[0].addBand("Sigma0_VV_mst_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaVVBandAM.setUnit("dB");
        sigmaVVValuesAM[0] = -14.78;    // (1, 1)
        sigmaVVValuesAM[1] = -18.78;    // (1, 2)
        sigmaVVValuesAM[2] = -8.262;    // (2, 1)
        sigmaVVValuesAM[3] = 1.6;       // (2, 2)
        sigmaVVValuesAM[4] = 4.597;     // (3, 1)
        sigmaVVValuesAM[5] = -64.597;   // (3, 2)
        final ProductData sigmaVVDataAM = ProductData.createInstance(sigmaVVValuesAM);
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
        final ProductData thetaDataAM = ProductData.createInstance(thetaValuesAM);
        thetaBandAM.setData(thetaDataAM);

        final double[] sigmaHVValuesAM = new double[w * h];
        final Band sigmaHVBandAM = testProduct[0].addBand("Sigma0_HV_slv3_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaHVBandAM.setUnit("dB");
        for (int i = 0; i < sigmaHVValuesAM.length; i++) {
            sigmaHVValuesAM[i] = 0.0;
        }
        final ProductData sigmaHVDataAM = ProductData.createInstance(sigmaHVValuesAM);
        sigmaHVBandAM.setData(sigmaHVDataAM);

        final double[] sigmaVHValuesAM = new double[w * h];
        final Band sigmaVHBandAM = testProduct[0].addBand("Sigma0_VH_slv1_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaVHBandAM.setUnit("dB");
        for (int i = 0; i < sigmaVHValuesAM.length; i++) {
            sigmaVHValuesAM[i] = 0.0;
        }
        final ProductData sigmaVHDataAM = ProductData.createInstance(sigmaVHValuesAM);
        sigmaVHBandAM.setData(sigmaVHDataAM);

        // PM source image

        final double[] sigmaHHValuesPM = new double[w * h];
        final Band sigmaHHBandPM = testProduct[0].addBand("Sigma0_HH_slv7_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaHHBandPM.setUnit(Unit.INTENSITY_DB);
        sigmaHHValuesPM[0] = -2.2722;  // (1, 1)
        sigmaHHValuesPM[1] = -2.2722;   // (1, 2)
        sigmaHHValuesPM[2] = -15.134;   // (2, 1)
        sigmaHHValuesPM[3] = -5.134;    // (2, 2)
        sigmaHHValuesPM[4] = -11.012;   // (3, 1)
        sigmaHHValuesPM[5] = -55.12;    // (3, 2)
        final ProductData sigmaHHDataPM = ProductData.createInstance(sigmaHHValuesPM);
        sigmaHHBandPM.setData(sigmaHHDataPM);

        final double[] sigmaVVValuesPM = new double[w * h];
        final Band sigmaVVBandPM = testProduct[0].addBand("Sigma0_VV_slv5_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaVVBandPM.setUnit(Unit.INTENSITY_DB);
        sigmaVVValuesPM[0] = -21.2389;  // (1, 1)
        sigmaVVValuesPM[1] = -1.2389;   // (1, 2)
        sigmaVVValuesPM[2] = -8.443;    // (2, 1)
        sigmaVVValuesPM[3] = -18.443;   // (2, 2)
        sigmaVVValuesPM[4] = -10.318;   // (3, 1)
        sigmaVVValuesPM[5] = -40.318;   // (3, 2)
        ProductData sigmaVVDataPM = ProductData.createInstance(sigmaVVValuesPM);
        sigmaVVBandPM.setData(sigmaVVDataPM);

        final double[] thetaValuesPM = new double[w * h];
        final Band thetaBandPM = testProduct[0].addBand("incidenceAngleFromEllipsoid_slv9_11Oct2013", ProductData.TYPE_FLOAT64);
        thetaBandPM.setUnit("degrees");
        thetaValuesPM[0] = 30.3;    // (1, 1)
        thetaValuesPM[1] = 31.4;    // (1, 2)
        thetaValuesPM[2] = 30.9;    // (2, 1)
        thetaValuesPM[3] = 32.20;   // (2, 2)
        thetaValuesPM[4] = 32.4;    // (3, 1)
        thetaValuesPM[5] = 30.6;    // (3, 2)
        final ProductData thetaDataPM = ProductData.createInstance(thetaValuesPM);
        thetaBandPM.setData(thetaDataPM);

        final double[] sigmaHVValuesPM = new double[w * h];
        final Band sigmaHVBandPM = testProduct[0].addBand("Sigma0_HV_slv8_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaHVBandPM.setUnit("dB");
        for (int i = 0; i < sigmaHVValuesPM.length; i++) {
            sigmaHVValuesPM[i] = 0.0;
        }
        final ProductData sigmaHVDataPM = ProductData.createInstance(sigmaHVValuesPM);
        sigmaHVBandPM.setData(sigmaHVDataPM);

        final double[] sigmaVHValuesPM = new double[w * h];
        final Band sigmaVHBandPM = testProduct[0].addBand("Sigma0_VH_slv6_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaVHBandPM.setUnit("dB");
        for (int i = 0; i < sigmaVHValuesPM.length; i++) {
            sigmaVHValuesPM[i] = 0.0;
        }
        final ProductData sigmaVHDataPM = ProductData.createInstance(sigmaVHValuesPM);
        sigmaVHBandPM.setData(sigmaVHDataPM);

        final MetadataElement metadata0 = AbstractMetadata.getAbstractedMetadata(testProduct[0]);

        metadata0.setAttributeDouble(AbstractMetadata.incidence_near, 18.1d);
        metadata0.setAttributeDouble(AbstractMetadata.incidence_far, 21.2d);

        final MetadataElement slvMetadata = AbstractMetadata.getSlaveMetadata(testProduct[0].getMetadataRoot());

        final MetadataElement metadata1 = new MetadataElement("slv");

        metadata1.setAttributeDouble(AbstractMetadata.incidence_near, 30.3d);
        metadata1.setAttributeDouble(AbstractMetadata.incidence_far, 32.4d);

        slvMetadata.addElement(metadata1);

        if (clay) {
            addClay(testProduct[0], w, h);
        }

        if (sand) {
            addSand(testProduct[0], w, h);
        }

        return testProduct[0];
    }

    /**
     * Tests IEM Hybrid Inversion operator with a test product.
     *
     * @throws Exception general exception
     */
    @Test
    public void testIEMHybridInversionOfRealImage() throws Exception {

        myTest(false, false); // no clay, no sand
        myTest(true, false); // just clay, no sand
        myTest(false, true); // no clay, just sand
        myTest(true, true);  // both clay and sand
    }

    private void myTest(final boolean clay, final boolean sand) throws Exception {

        TestUtils.log.info("***************** myTest: clay = " + clay + " sand = " + sand + " epsilon = " + epsilon);

        int numBands = 3;
        if (clay) {
            numBands++;
        }
        if (sand) {
            numBands++;
        }

        final Product sourceProduct = createOneTestProduct(cols, rows, clay, sand);
        final Path resourcePath = ResourceInstaller.findModuleCodeBasePath(IEMInverBase.class).resolve("auxdata/sm_luts");

        final IEMHybridInverOp op = (IEMHybridInverOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setLUTFile(resourcePath.resolve("IEM_cb_nt_RDC_RSAT2_QP.mat").toFile().getAbsolutePath());
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

        final double[] rmsExpectedValues = {1.5d, 1.7d, 1.6d, 1.0d, 0.9d, 2.4d};
        final double[] rdcExpectedValues = {30.780304d, 3.125824d, 9.684544d, 38.6884d, 13.075456d, 3.748096d};
        final double[] clExpectedValues = {4.6d, 1.0d, 1.4d, 4.2d, 3.0d, 1.0d};

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

        expectedValues[idx] = clExpectedValues; // band cl is 2nd col in LUT

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
                    //throw new Exception("");
                }
            }
        }
    }
}
