package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for IEMMultiAngleInverOperator.
 */
public class TestIEMMultiAngleInverOp {

    private final static OperatorSpi spi = new IEMMultiAngleInverOp.Spi();

    static {
        TestUtils.initTestEnvironment();
    }

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
     * @param pol  sigma polarizations
     * @param w    width
     * @param h    height
     * @param clay Include clay or not in test product
     * @param sand Include sand or not in test product
     * @return the created product
     */

    private static Product createOneTestProduct(final POL pol, final int w, final int h, final boolean clay, final boolean sand) {

        Product[] testProduct = new Product[1];

        testProduct[0] = TestUtils.createProduct("backscatter and LIA", w, h);

        // AM image

        final double[] sigmaHHValuesAM = new double[w * h];
        final Band sigmaHHBandAM = testProduct[0].addBand("Sigma0_HH_slv2_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaHHBandAM.setUnit("dB");
        switch (pol) {
            case HH1HH2:
                sigmaHHValuesAM[0] = -58.7907;  // (1, 1) row 13200
                sigmaHHValuesAM[1] = -9.3162;   // (1, 2) row 132
                sigmaHHValuesAM[2] = -4.4118;   // (2, 1) row 5000
                sigmaHHValuesAM[3] = -5.8039;   // (2, 2) row 7676
                sigmaHHValuesAM[4] = -5.0071;   // (3, 1) row 10030
                sigmaHHValuesAM[5] = -5.2757;   // (3, 2) row 4131
                break;

            case HH1VV2:
                sigmaHHValuesAM[0] = -20.6695;  // (1, 1) row 12111
                sigmaHHValuesAM[1] = -4.0866;   // (1, 2) row 9043
                sigmaHHValuesAM[2] = -7.4583;   // (2, 1) row 11111
                sigmaHHValuesAM[3] = -6.9675;   // (2, 2) row 2545
                sigmaHHValuesAM[4] = -51.0246;  // (3, 1) row 12965
                sigmaHHValuesAM[5] = -4.8064;   // (3, 2) row 6666
                break;

            case VV1VV2:
                sigmaHHValuesAM[0] = 0.0;   // (1, 1)
                sigmaHHValuesAM[1] = 0.0;   // (1, 2)
                sigmaHHValuesAM[2] = 0.0;   // (2, 1)
                sigmaHHValuesAM[3] = 0.0;   // (2, 2)
                sigmaHHValuesAM[4] = 0.0;   // (3, 1)
                sigmaHHValuesAM[5] = 0.0;   // (3, 2)
                break;

            case VV1HH2:
                sigmaHHValuesAM[0] = 0.0;   // (1, 1)
                sigmaHHValuesAM[1] = 0.0;   // (1, 2)
                sigmaHHValuesAM[2] = 0.0;   // (2, 1)
                sigmaHHValuesAM[3] = 0.0;   // (2, 2)
                sigmaHHValuesAM[4] = 0.0;   // (3, 1)
                sigmaHHValuesAM[5] = 0.0;   // (3, 2)
                break;

            default:
                break;
        }
        ProductData sigmaHHDataAM = ProductData.createInstance(sigmaHHValuesAM);
        sigmaHHBandAM.setData(sigmaHHDataAM);

        final double[] sigmaVVValuesAM = new double[w * h];
        final Band sigmaVVBandAM = testProduct[0].addBand("Sigma0_VV_mst_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaVVBandAM.setUnit("dB");
        switch (pol) {
            case HH1HH2:
                sigmaVVValuesAM[0] = 0.0;   // (1, 1)
                sigmaVVValuesAM[1] = 0.0;   // (1, 2)
                sigmaVVValuesAM[2] = 0.0;   // (2, 1)
                sigmaVVValuesAM[3] = 0.0;   // (2, 2)
                sigmaVVValuesAM[4] = 0.0;   // (3, 1)
                sigmaVVValuesAM[5] = 0.0;   // (3, 2)
                break;

            case HH1VV2:
                sigmaVVValuesAM[0] = 0.0;   // (1, 1)
                sigmaVVValuesAM[1] = 0.0;   // (1, 2)
                sigmaVVValuesAM[2] = 0.0;   // (2, 1)
                sigmaVVValuesAM[3] = 0.0;   // (2, 2)
                sigmaVVValuesAM[4] = 0.0;   // (3, 1)
                sigmaVVValuesAM[5] = 0.0;   // (3, 2)
                break;

            case VV1VV2:
                sigmaVVValuesAM[0] = -20.6792;  // (1, 1) row 7666
                sigmaVVValuesAM[1] = -7.8770;   // (1, 2) row 9997
                sigmaVVValuesAM[2] = -11.8382;  // (2, 1) row 8333
                sigmaVVValuesAM[3] = -8.4695;   // (2, 2) row 10134
                sigmaVVValuesAM[4] = -15.6050;  // (3, 1) row 11536
                sigmaVVValuesAM[5] = -14.8652;  // (3, 2) row 11834
                break;

            case VV1HH2:
                sigmaVVValuesAM[0] = -23.5524;  // (1, 1) row 12260
                sigmaVVValuesAM[1] = -8.9440;   // (1, 2) row 10654
                sigmaVVValuesAM[2] = -36.2008;  // (2, 1) row 6964
                sigmaVVValuesAM[3] = -29.6032;  // (2, 2) row 12404
                sigmaVVValuesAM[4] = -6.7042;   // (3, 1) row 9414
                sigmaVVValuesAM[5] = -8.7774;   // (3, 2) row 2999
                break;

            default:
                break;
        }
        ProductData sigmaVVDataAM = ProductData.createInstance(sigmaVVValuesAM);
        sigmaVVBandAM.setData(sigmaVVDataAM);

        final double[] thetaValuesAM = new double[w * h];
        final Band thetaBandAM = testProduct[0].addBand("incidenceAngleFromEllipsoid_slv4_11Oct2013", ProductData.TYPE_FLOAT64);
        thetaBandAM.setUnit("degrees");
        thetaValuesAM[0] = 18.1;    // (1, 1)
        thetaValuesAM[1] = 21.2;    // (1, 2)
        thetaValuesAM[2] = 18.8;    // (2, 1)
        thetaValuesAM[3] = 20.0;    // (2, 2)
        thetaValuesAM[4] = 19.6;    // (3, 1)
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

        // PM source image

        final double[] sigmaHHValuesPM = new double[w * h];
        final Band sigmaHHBandPM = testProduct[0].addBand("Sigma0_HH_slv7_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaHHBandPM.setUnit(Unit.INTENSITY_DB);
        switch (pol) {
            case HH1HH2:
                sigmaHHValuesPM[0] = -43.9183;  // (1, 1)
                sigmaHHValuesPM[1] = -12.9086;  // (1, 2)
                sigmaHHValuesPM[2] = -8.4450;   // (2, 1)
                sigmaHHValuesPM[3] = -5.8039;   // (2, 2)
                sigmaHHValuesPM[4] = -7.2756;   // (3, 1)
                sigmaHHValuesPM[5] = -9.4275;   // (3, 2)
                break;

            case HH1VV2:
                sigmaHHValuesPM[0] = 0;  // (1, 1)
                sigmaHHValuesPM[1] = 0;  // (1, 2)
                sigmaHHValuesPM[2] = 0;  // (2, 1)
                sigmaHHValuesPM[3] = 0;  // (2, 2)
                sigmaHHValuesPM[4] = 0;  // (3, 1)
                sigmaHHValuesPM[5] = 0;  // (3, 2)
                break;

            case VV1VV2:
                sigmaHHValuesPM[0] = 0;  // (1, 1)
                sigmaHHValuesPM[1] = 0;  // (1, 2)
                sigmaHHValuesPM[2] = 0;  // (2, 1)
                sigmaHHValuesPM[3] = 0;  // (2, 2)
                sigmaHHValuesPM[4] = 0;  // (3, 1)
                sigmaHHValuesPM[5] = 0;  // (3, 2)
                break;

            case VV1HH2:
                sigmaHHValuesPM[0] = -20.2471;  // (1, 1)
                sigmaHHValuesPM[1] = -9.3769;   // (1, 2)
                sigmaHHValuesPM[2] = -7.7864;   // (2, 1)
                sigmaHHValuesPM[3] = -32.1973;  // (2, 2)
                sigmaHHValuesPM[4] = -6.1913;   // (3, 1)
                sigmaHHValuesPM[5] = -11.4129;  // (3, 2)
                break;

            default:
                break;
        }
        ProductData sigmaHHDataPM = ProductData.createInstance(sigmaHHValuesPM);
        sigmaHHBandPM.setData(sigmaHHDataPM);

        final double[] sigmaVVValuesPM = new double[w * h];
        final Band sigmaVVBandPM = testProduct[0].addBand("Sigma0_VV_slv5_11Oct2013", ProductData.TYPE_FLOAT64);
        sigmaVVBandPM.setUnit(Unit.INTENSITY_DB);
        switch (pol) {
            case HH1HH2:
                sigmaVVValuesPM[0] = 0;  // (1, 1)
                sigmaVVValuesPM[1] = 0;  // (1, 2)
                sigmaVVValuesPM[2] = 0;  // (2, 1)
                sigmaVVValuesPM[3] = 0;  // (2, 2)
                sigmaVVValuesPM[4] = 0;  // (3, 1)
                sigmaVVValuesPM[5] = 0;  // (3, 2)
                break;

            case HH1VV2:
                sigmaVVValuesPM[0] = -14.7323;  // (1, 1)
                sigmaVVValuesPM[1] = -12.3031;  // (1, 2)
                sigmaVVValuesPM[2] = -7.5348;   // (2, 1)
                sigmaVVValuesPM[3] = -8.2769;   // (2, 2)
                sigmaVVValuesPM[4] = -33.9247;  // (3, 1)
                sigmaVVValuesPM[5] = -18.2454;  // (3, 2)
                break;

            case VV1VV2:
                sigmaVVValuesPM[0] = -28.4048;  // (1, 1)
                sigmaVVValuesPM[1] = -10.2234;  // (1, 2)
                sigmaVVValuesPM[2] = -16.0854;  // (2, 1)
                sigmaVVValuesPM[3] = -8.4695;   // (2, 2)
                sigmaVVValuesPM[4] = -13.8730;  // (3, 1)
                sigmaVVValuesPM[5] = -11.9203;  // (3, 2)
                break;

            case VV1HH2:
                sigmaVVValuesPM[0] = 0; // (1, 1)
                sigmaVVValuesPM[1] = 0; // (1, 2)
                sigmaVVValuesPM[2] = 0; // (2, 1)
                sigmaVVValuesPM[3] = 0; // (2, 2)
                sigmaVVValuesPM[4] = 0; // (3, 1)
                sigmaVVValuesPM[5] = 0; // (3, 2)
                break;

            default:
                break;
        }
        ProductData sigmaVVDataPM = ProductData.createInstance(sigmaVVValuesPM);
        sigmaVVBandPM.setData(sigmaVVDataPM);

        final double[] thetaValuesPM = new double[w * h];
        final Band thetaBandPM = testProduct[0].addBand("incidenceAngleFromEllipsoid_slv9_11Oct2013", ProductData.TYPE_FLOAT64);
        thetaBandPM.setUnit("degrees");
        thetaValuesPM[0] = 30.3;    // (1, 1)
        thetaValuesPM[1] = 31.4;    // (1, 2)
        thetaValuesPM[2] = 30.9;    // (2, 1)
        thetaValuesPM[3] = 20.2;    // (2, 2)
        thetaValuesPM[4] = 32.4;    // (3, 1)
        thetaValuesPM[5] = 30.6;    // (3, 2)
        ProductData thetaDataPM = ProductData.createInstance(thetaValuesPM);
        thetaBandPM.setData(thetaDataPM);

        final double[] sigmaHVValuesPM = new double[w * h];
        final Band sigmaHVBandPM = testProduct[0].addBand("Sigma0_HV_slv8_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaHVBandPM.setUnit("dB");
        for (int i = 0; i < sigmaHVValuesPM.length; i++) {
            sigmaHVValuesPM[i] = 0.0;
        }
        ProductData sigmaHVDataPM = ProductData.createInstance(sigmaHVValuesPM);
        sigmaHVBandPM.setData(sigmaHVDataPM);

        final double[] sigmaVHValuesPM = new double[w * h];
        final Band sigmaVHBandPM = testProduct[0].addBand("Sigma0_VH_slv6_11Oct2103", ProductData.TYPE_FLOAT64);
        sigmaVHBandPM.setUnit("dB");
        for (int i = 0; i < sigmaVHValuesPM.length; i++) {
            sigmaVHValuesPM[i] = 0.0;
        }
        ProductData sigmaVHDataPM = ProductData.createInstance(sigmaVHValuesPM);
        sigmaVHBandPM.setData(sigmaVHDataPM);

        final MetadataElement metadata0 = AbstractMetadata.getAbstractedMetadata(testProduct[0]);

        metadata0.setAttributeDouble(AbstractMetadata.incidence_near, 18.1d);
        metadata0.setAttributeDouble(AbstractMetadata.incidence_far, 21.2d);

        final MetadataElement slvMetadata = AbstractMetadata.getSlaveMetadata(testProduct[0].getMetadataRoot());

        final MetadataElement metadata1 = new MetadataElement("slv");

        metadata1.setAttributeDouble(AbstractMetadata.incidence_near, 20.2d);
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
     * Tests IEM Multi-Angle Inversion operator with a test product.
     *
     * @throws Exception general exception
     */
    @Test
    public void testIEMMultiAngleInversionOfRealImage() throws Exception {

        final double epsilon = 1.0e-4d;

        for (POL pol : POL.values()) {
            myTest(pol, false, false, true, epsilon); // no clay, no sand
            myTest(pol, true, false, true, epsilon); // just clay, no sand
            myTest(pol, false, true, true, epsilon); // no clay, just sand
            myTest(pol, true, true, true, epsilon);  // both clay and sand
        }

        for (POL pol : POL.values()) {
            myTest(pol, false, false, false, epsilon); // no clay, no sand
            myTest(pol, true, false, false, epsilon); // just clay, no sand
            myTest(pol, false, true, false, epsilon); // no clay, just sand
            myTest(pol, true, true, false, epsilon);  // both clay and sand
        }
    }

    private void myTest(final POL pol, final boolean clay, final boolean sand, boolean useMatlabLUT, double epsilon) throws Exception {

        TestUtils.log.info("***************** myTest: pol = " + pol + " clay = " + clay + " sand = " + sand + " useMatlabLUT = " + useMatlabLUT + " epsilon = " + epsilon);

        int numBands = 2;
        if (clay) {
            numBands++;
        }
        if (sand) {
            numBands++;
        }

        final Product sourceProduct = createOneTestProduct(pol, cols, rows, clay, sand);

        final IEMMultiAngleInverOp op = (IEMMultiAngleInverOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        if (useMatlabLUT) {
            op.setLUTFile("P:\\asmers\\asmers\\data\\CCN\\Testing\\cecilia\\LUTs\\IEMC_cb_nt_RDC_RSAT2_QP_MAT\\IEMC_cb_nt_RDC_RSAT2_QP.mat");
        } else {
            op.setLUTFile("P:\\asmers\\asmers\\data\\CCN\\Testing\\cecilia\\LUTs\\IEMC_cb_nt_RDC_RSAT2_QP_CSV\\IEMC_cb_nt_RDC_RSAT2_QP.csv");
        }
        switch (pol) {

            case HH1HH2:
                op.setSigmaPol("HH1-HH2");
                break;
            case HH1VV2:
                op.setSigmaPol("HH1-VV2");
                break;
            case VV1VV2:
                op.setSigmaPol("VV1-VV2");
                break;
            case VV1HH2:
                op.setSigmaPol("VV1-HH2");
                break;
            default:
                break;
        }
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

        final double[] rmsExpectedValuesHH1HH2 = {2.6d, 0.3d, 0.6d, 0.9d, 1.3d, 0.5d};
        final double[] rdcExpectedValuesHH1HH2 = {19.1144d, 13.69d, 38.6884d, 12.4750d, 18.3869d, 30.7803d};

        final double[] rmsExpectedValuesHH1VV2 = {2.0d, 1.1d, 1.6d, 0.4d, 2.5d, 0.7d};
        final double[] rdcExpectedValuesHH1VV2 = {32.6727d, 27.1649d, 36.6267d, 21.3814d, 16.2893d, 22.9633d};

        final double[] rmsExpectedValuesVV1VV2 = {0.9d, 1.3d, 1.0d, 1.4d, 1.8d, 1.9d};
        final double[] rdcExpectedValuesVV1VV2 = {7.2469d, 14.3187d, 24.6016d, 9.6845d, 6.8017d, 31.7194d};

        final double[] rmsExpectedValuesVV1HH2 = {2.1d, 1.5d, 0.8d, 2.2d, 1.2d, 0.4d};
        final double[] rdcExpectedValuesVV1HH2 = {27.1649d, 10.2144d, 28.0476d, 11.8887d, 35.6170d, 15.6183d};

        final double[] clayExpectedValues = {13.0d, 23.0d, 3.0d, 3.0d, 99.0d, 23.0d};
        final double[] sandExpectedValues = {15.0d, 83.0d, 43.0d, 98.0d, 26.0d, 75.0};

        // compare with expected outputs:
        final double[][] expectedValues = new double[numBands][cols * rows];

        int idx = 2;

        switch (pol) {
            case HH1HH2:
                expectedValues[0] = rmsExpectedValuesHH1HH2; // 1st band rms is 1st col in LUT
                expectedValues[1] = rdcExpectedValuesHH1HH2; // 2nd band rdc is 3rd col in LUT
                break;

            case HH1VV2:
                expectedValues[0] = rmsExpectedValuesHH1VV2; // 1st band rms is 1st col in LUT
                expectedValues[1] = rdcExpectedValuesHH1VV2; // 2nd band rdc is 3rd col in LUT
                break;

            case VV1VV2:
                expectedValues[0] = rmsExpectedValuesVV1VV2; // 1st band rms is 1st col in LUT
                expectedValues[1] = rdcExpectedValuesVV1VV2; // 2nd band rdc is 3rd col in LUT
                break;

            case VV1HH2:
                expectedValues[0] = rmsExpectedValuesVV1HH2; // 1st band rms is 1st col in LUT
                expectedValues[1] = rdcExpectedValuesVV1HH2; // 2nd band rdc is 3rd col in LUT
                break;

            default:
                break;
        }

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

    private enum POL {HH1HH2, HH1VV2, VV1VV2, VV1HH2}
}
