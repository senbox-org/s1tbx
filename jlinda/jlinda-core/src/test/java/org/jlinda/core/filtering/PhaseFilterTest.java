package org.jlinda.core.filtering;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;
import org.jlinda.core.Window;
import org.jlinda.core.utils.LinearAlgebraUtils;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.core.utils.SpectralUtils;
import org.jlinda.core.utils.WeightWindows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jblas.MatrixFunctions.powi;
import static org.jlinda.core.io.DataReader.readCplxFloatData;
import static org.jlinda.core.io.DataReader.readFloatData;

/**
 * User: pmar@ppolabs.com
 * Date: 6/8/11
 * Time: 12:04 PM
 */
public class PhaseFilterTest {

    private static final String testDirectoryGoldstein = "/d2/etna_test/phaseFiltTest/goldstein/";
    private static final String testDirectorySpectral = "/d2/etna_test/phaseFiltTest/spectral/";
    private static final String testDirectorySpatial = "/d2/etna_test/phaseFiltTest/spatialconv/";
    private static final double DELTA_01 = 1e-01;
    private static final double DELTA_03 = 1e-03;
    private static final double DELTA_005 = 5e-01;
    private DoubleMatrix kernel2d;

    private static Logger initLog() {
        String filePathToLog4JProperties = "log4j.properties";
        Logger logger = Logger.getLogger(PhaseFilterUtils.class);
        PropertyConfigurator.configure(filePathToLog4JProperties);
        return logger;
    }

    @BeforeClass
    public static void setUp() {
        initLog();
    }


    @Test
    public void testGoldstein_no_smoothing() throws Exception {

        // load input data
        // master data
        String cplxDataFileName = testDirectoryGoldstein + "data_input.cr4.swap";
        ComplexDoubleMatrix cplxData = readCplxFloatData(cplxDataFileName, 32, 512);

        String cplxDataFilteredFileName = testDirectoryGoldstein + "data_filtered.cr4.swap";
        ComplexDoubleMatrix cplxDataFilt_EXPECTED = readCplxFloatData(cplxDataFilteredFileName, 32, 512);

        double[] smoothKernel = null;
        float alpha = (float) 0.5;
        int overlap = 12;
        ComplexDoubleMatrix cplxDataFilt_ACTUAL = PhaseFilterUtils.goldstein(cplxData, alpha, overlap, smoothKernel);

        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.real().toArray(), cplxDataFilt_ACTUAL.real().toArray(), DELTA_01);
        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.imag().toArray(), cplxDataFilt_ACTUAL.imag().toArray(), DELTA_01);

    }

    @Test
    public void testGoldstein_with_smoothing() throws Exception {

        // load input data
        // master data
        String cplxDataFileName = testDirectoryGoldstein + "data_input.cr4.swap";
        ComplexDoubleMatrix cplxData = readCplxFloatData(cplxDataFileName, 32, 512);

        String cplxDataFilteredFileName = testDirectoryGoldstein + "data_filtered_smooth.cr4.swap";
        ComplexDoubleMatrix cplxDataFilt_EXPECTED = readCplxFloatData(cplxDataFilteredFileName, 32, 512);

        double[] smoothKernel = {0.2, 0.2, 0.2, 0.2, 0.2};
        float alpha = (float) 0.5;
        int overlap = 12;
        ComplexDoubleMatrix cplxDataFilt_ACTUAL = PhaseFilterUtils.goldstein(cplxData, alpha, overlap, smoothKernel);

        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.real().toArray(), cplxDataFilt_ACTUAL.real().toArray(), DELTA_01);
        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.imag().toArray(), cplxDataFilt_ACTUAL.imag().toArray(), DELTA_01);

    }

    @Before
    public void setUpFilterKernel2d() {

        final int blockSize = 32;
        final DoubleMatrix ones = DoubleMatrix.ones(1, blockSize);
        final DoubleMatrix hammingWind = new DoubleMatrix(WeightWindows.hamming(blockSize));
        kernel2d = hammingWind.mmul(ones);
        kernel2d = kernel2d.mul(kernel2d.transpose());
    }

    @Test
    public void TestArrangingKernel() throws Exception {

        final int scalefactor = 1;
        DoubleMatrix arrangedKernel2d_ACTUAL = PhaseFilterUtils.arrangeKernel2d(kernel2d, scalefactor);

        String kernel2dFileName = testDirectorySpectral + "kernel2d.r4.swap";
        FloatMatrix arrangedKernel2d_EXPECTED = readFloatData(kernel2dFileName, 32, 32);

        Assert.assertArrayEquals(arrangedKernel2d_EXPECTED.toArray(), arrangedKernel2d_ACTUAL.toFloat().toArray(), (float) DELTA_01);

    }

    @Test
    public void testSpectral() throws Exception {

        // load input data
        String cplxDataFileName = testDirectorySpectral + "data_input.cr4.swap";
        ComplexDoubleMatrix cplxData = readCplxFloatData(cplxDataFileName, 32, 512);

        String cplxDataFilteredFileName = testDirectorySpectral + "data_filtered.cr4.swap";
        ComplexDoubleMatrix cplxDataFilt_EXPECTED = readCplxFloatData(cplxDataFilteredFileName, 32, 512);

        int overlap = 12;
        ComplexDoubleMatrix cplxDataFilt_ACTUAL = PhaseFilterUtils.spectralfilt(cplxData, kernel2d, overlap);

        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.real().toArray(), cplxDataFilt_ACTUAL.real().toArray(), DELTA_005);
        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.imag().toArray(), cplxDataFilt_ACTUAL.imag().toArray(), DELTA_005);

    }


    @Test
    public void testSpatialConvFilter() throws Exception {

        String kernelFileName = testDirectorySpatial + "kernel2d.cr4.swap";
        ComplexDoubleMatrix kernel2d_EXPECTED = readCplxFloatData(kernelFileName, 128, 128);

        final double[] kernel = {0.2, 0.2, 0.2, 0.2, 0.2};
        DoubleMatrix kernel2d = PhaseFilterUtils.constructRectKernel(128, kernel);

        ComplexDoubleMatrix kernel2d_ACTUAL = new ComplexDoubleMatrix(kernel2d);
        SpectralUtils.fft2D_inplace(kernel2d_ACTUAL);
        kernel2d_ACTUAL.conji();

        Assert.assertArrayEquals(kernel2d_EXPECTED.real().toArray(), kernel2d_ACTUAL.real().toArray(), DELTA_03);
        Assert.assertArrayEquals(kernel2d_EXPECTED.imag().toArray(), kernel2d_ACTUAL.imag().toArray(), DELTA_03);

    }

    @Test
    public void testSpatialConv() throws Exception {

        // load input data
        String cplxDataFileName = testDirectorySpatial + "data_input.cr4.swap";
        ComplexDoubleMatrix cplxData = readCplxFloatData(cplxDataFileName, 128, 512);

        String cplxDataFilteredFileName = testDirectorySpatial + "data_filtered.cr4.swap";
        ComplexDoubleMatrix cplxDataFilt_EXPECTED = readCplxFloatData(cplxDataFilteredFileName, 128, 512);

        final double[] kernel = {0.2, 0.2, 0.2, 0.2, 0.2};
        DoubleMatrix kernel2d = PhaseFilterUtils.constructRectKernel(128, kernel);

        ComplexDoubleMatrix kernel2d_ACTUAL = new ComplexDoubleMatrix(kernel2d);
        SpectralUtils.fft2D_inplace(kernel2d_ACTUAL);
        kernel2d_ACTUAL.conji();

        int overlap = 2;

        ComplexDoubleMatrix cplxDataFilt_ACTUAL = PhaseFilterUtils.convbuffer(cplxData, kernel2d_ACTUAL, overlap);

        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.real().toArray(), cplxDataFilt_ACTUAL.real().toArray(), DELTA_01);
        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.imag().toArray(), cplxDataFilt_ACTUAL.imag().toArray(), DELTA_01);

    }
    @Test
    public void testGoldstein_2D_no_smoothing() throws Exception {

        // load input data
        // master data
        String cplxDataFileName = testDirectoryGoldstein + "data_input.cr4.swap";
        ComplexDoubleMatrix cplxData = readCplxFloatData(cplxDataFileName, 32, 512);

        String cplxDataFilteredFileName = testDirectoryGoldstein + "data_filtered.cr4.swap";
        ComplexDoubleMatrix cplxDataFilt_EXPECTED = readCplxFloatData(cplxDataFilteredFileName, 32, 512);

        double[] smoothKernel = null;
        float alpha = (float) 0.5;
        int overlap = 12;
        int blockSize = 32;
        ComplexDoubleMatrix cplxDataFilt_ACTUAL = goldstein_2D(cplxData, alpha, blockSize, overlap, smoothKernel);

        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.real().toArray(), cplxDataFilt_ACTUAL.real().toArray(), DELTA_01);
        Assert.assertArrayEquals(cplxDataFilt_EXPECTED.imag().toArray(), cplxDataFilt_ACTUAL.imag().toArray(), DELTA_01);

    }

    public static ComplexDoubleMatrix goldstein_2D(
            final ComplexDoubleMatrix data,
            final float alpha,
            final int blockSize,
            final int overlap,
            final double[] smoothKernel) { // lying down

        // ______ Allocate output matrix ______
        final int nLines = data.rows;
        final int nPixels = data.columns;

        final ComplexDoubleMatrix dataFilt = new ComplexDoubleMatrix(nLines, nPixels); // output

        // ______ Get block from buffer ______
        // initial values for range
        final int numOut = blockSize - (2 * overlap);   // number of output pixels

        int data_PixLo = 0;                          // index in DATA to get 1st block
        int data_PixHi = blockSize - 1;                 // index in DATA to get 1st block
        int outBlock_PixLo = 0;                      // index in BLOCK (only 1st block)
        int outBlock_PixHi = blockSize - 1 - overlap;   // index in BLOCK (except last block)
        int outFilt_PixLo = outBlock_PixLo;          // index in FILTERED (1st block)
        int outFilt_PixHi = outBlock_PixHi;          // index in FILTERED

        // inital values for azimuth
        int data_LinLo = 0;                          // index in DATA to get 1st block
        int data_LinHi = blockSize - 1;                 // index in DATA to get 1st block
        int outBlock_LinLo = 0;                      // index in BLOCK (only 1st block)
        int outBlock_LinHi = blockSize - 1 - overlap;   // index in BLOCK (except last block)
        int outFilt_LinLo = outBlock_LinLo;          // index in FILTERED (1st block)
        int outFilt_LinHi = outBlock_LinHi;          // index in FILTERED

        boolean lastAzimuthLine = false;          // only just started...
        boolean lastBlockInRange = false;          // only just started...

        // smoothing parameters
        boolean doSmooth;
        int smooth = 0;   // half block nLines, odd kernel
        ComplexDoubleMatrix kernel2D = null;

        // note that int floors division
        try {
            smooth = smoothKernel.length / 2;
            doSmooth = (smooth != 0);
        } catch (Exception e) {
            doSmooth = false;
        }

        if (doSmooth) {
            kernel2D = constructSmoothingKernel(smoothKernel, blockSize, smooth);
        }

        DoubleMatrix amplitude;



        int j = 0;
        while (j < nLines) {

            // check if we are doing the last block along range
            if (data_LinHi >= nLines - 1) {
                lastAzimuthLine = true;
                data_LinHi = nLines - 1;                        // prevent reading after file
                data_LinLo = data_LinHi - blockSize + 1;        // but make sure SIZE pixels are read
                outFilt_LinHi = data_LinHi;                     // index in FILTERED 2b written
                outBlock_LinHi = blockSize - 1;                 // write all to the end
                outBlock_LinLo = outBlock_LinHi - (outFilt_LinHi - outFilt_LinLo + 1) + 1;
            }

            int counter = 1;
            int i = 0;
            while (i < nPixels) {

                // check if we are doing the last block along range
                if (data_PixHi >= nPixels - 1) {
                    lastBlockInRange = true;
                    data_PixHi = nPixels - 1;                   // prevent reading after file
                    data_PixLo = data_PixHi - blockSize + 1;       // but make sure SIZE pixels are read
                    outFilt_PixHi = data_PixHi;                 // index in FILTERED 2b written
                    outBlock_PixHi = blockSize - 1;                // write all to the end
                    outBlock_PixLo = outBlock_PixHi - (outFilt_PixHi - outFilt_PixLo + 1) + 1;
                }

                // set data windows
                Window winData = new Window(data_LinLo, data_LinHi, data_PixLo, data_PixHi);
                Window winBlock = new Window(outBlock_LinLo, outBlock_LinHi, outBlock_PixLo, outBlock_PixHi);
                Window winFiltered = new Window(outFilt_LinLo, outFilt_LinHi, outFilt_PixLo, outFilt_PixHi);

//                System.out.println("Data: " + winData.toString());
//                System.out.println("Block: " + winBlock.toString());
//                System.out.println("Filtered: " + winFiltered.toString());
//                System.out.println("Counter:" + counter++);
//                System.out.println("-=-----------");

                ComplexDoubleMatrix block = new ComplexDoubleMatrix((int) winData.lines(), (int) winData.pixels());


                // Construct BLOCK as part of DATA
                LinearAlgebraUtils.setdata(block, data, winData);

                // Get spectrum/amplitude/smooth/filter ______
                SpectralUtils.fft2D_inplace(block);
                amplitude = SarUtils.magnitude(block);

                // ______ use FFT's for convolution with rect ______
                if (doSmooth)
                    amplitude = PhaseFilterUtils.smooth(amplitude, kernel2D);

                double maxAmplitude = amplitude.max();

                final double goldThresh = 1e-20;
                if (maxAmplitude > goldThresh) { // how reliable this threshold is?
                    amplitude.divi(maxAmplitude);
                    powi(amplitude, alpha);
                    LinearAlgebraUtils.dotmult_inplace(block, new ComplexDoubleMatrix(amplitude));
                }

                SpectralUtils.invfft2D_inplace(block);

                // ______ Set correct part that is filtered in output matrix ______
                LinearAlgebraUtils.setdata(dataFilt, winFiltered, block, winBlock);

                if (!lastBlockInRange) {
                    // ______ Update indexes in matrices, will be corrected for last block ______
                    data_PixLo += numOut;                       // next block
                    data_PixHi += numOut;                       // next block
                    outBlock_PixLo = overlap;                   // index in block, valid for all middle blocks
                    outFilt_PixLo = outFilt_PixHi + 1;          // index in FILTERED, next range line
                    outFilt_PixHi = outFilt_PixLo + numOut - 1; // index in FILTERED
                } else {
                    data_PixLo = 0;                          // index in DATA to get 1st block
                    data_PixHi = blockSize - 1;                 // index in DATA to get 1st block
                    outBlock_PixLo = 0;                      // index in BLOCK (only 1st block)
                    outBlock_PixHi = blockSize - 1 - overlap;   // index in BLOCK (except last block)
                    outFilt_PixLo = outBlock_PixLo;          // index in FILTERED (1st block)
                    outFilt_PixHi = outBlock_PixHi;          // index in FILTERED
                    break;
                }
                i++;
            }

            if (lastAzimuthLine && lastBlockInRange) {
                break;
            }
            // ______ Update indexes in matrices, will be corrected for last block ______
            data_LinLo += numOut;                       // next block
            data_LinHi += numOut;                       // next block
            outBlock_LinLo = overlap;                   // index in block, valid for all middle blocks
            outFilt_LinLo = outFilt_LinHi + 1;          // index in FILTERED, next range line
            outFilt_LinHi = outFilt_LinLo + numOut - 1; // index in FILTERED

            j++;
        }
        return dataFilt;
    }


    private static ComplexDoubleMatrix constructSmoothingKernel(double[] inKernel, int nLines, int smooth) {

        ComplexDoubleMatrix kernel2D = null;
        ComplexDoubleMatrix kernel1D = new ComplexDoubleMatrix(1, nLines);             // init to zeros

        for (int ii = -smooth; ii <= smooth; ++ii) {// 1d kernel function of block
            int tmpValue_1 = (ii + nLines) % nLines;
            int tmpValue_2 = ii + smooth;// used to be ii-SMOOTH: wrong
            kernel1D.put(0, tmpValue_1, new ComplexDouble(inKernel[tmpValue_2]));
        }

        kernel2D = LinearAlgebraUtils.matTxmat(kernel1D, kernel1D);
        SpectralUtils.fft2D_inplace(kernel2D);  // should be real sinc

        return kernel2D;
    }

    public static DoubleMatrix constructRectKernel(final int size, final double[] kernel) {
        final int overlapLines = (int) Math.floor(kernel.length / 2.);
        // ______ 1d kernel function ______
        final DoubleMatrix kernel1d = new DoubleMatrix(1, size); // init to zeros
        for (int ii = -overlapLines; ii <= overlapLines; ++ii) {
            kernel1d.put(0, (ii + size) % size, kernel[ii + overlapLines]);
        }
        return LinearAlgebraUtils.matTxmat(kernel1d, kernel1d);
    }


}
