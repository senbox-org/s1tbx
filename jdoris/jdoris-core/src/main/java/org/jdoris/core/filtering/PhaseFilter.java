package org.jdoris.core.filtering;

import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jdoris.core.Window;
import org.jdoris.core.utils.LinearAlgebraUtils;
import org.jdoris.core.utils.SarUtils;
import org.jdoris.core.utils.SpectralUtils;

import static org.jblas.MatrixFunctions.powi;
import static org.jdoris.core.utils.MathUtils.isEven;

public class PhaseFilter {

    // PhaseFilter class for now only aggregates static methods for phase filtering of InSAR data

    private static final double GOLDSTEIN_THRESHOLD = 1e-20;

    private String method;
    private ComplexDoubleMatrix data;
    private int blockSize;
    private int overlap;
    private double[] kernelArray;
    private ComplexDoubleMatrix kernel2d;
    private float goldsteinAlpha;

    public PhaseFilter(String method, ComplexDoubleMatrix data, int blockSize, int overlap, double[] kernelArray, float goldsteinAlpha) {
        this.method = method;
        this.data = data;
        this.blockSize = blockSize;
        this.overlap = overlap;
        this.kernelArray = kernelArray;
        this.goldsteinAlpha = goldsteinAlpha;

        constructKernel();

    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setData(ComplexDoubleMatrix data) {
        this.data = data;
    }

    public ComplexDoubleMatrix getData() {
        return data;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }

    public void setKernelArray(double[] kernelArray) {
        this.kernelArray = kernelArray;
    }

    public void setGoldsteinAlpha(float goldsteinAlpha) {
        this.goldsteinAlpha = goldsteinAlpha;
    }

    public void setKernel2d(ComplexDoubleMatrix kernel2d) {
        this.kernel2d = kernel2d;
    }

    public void filter() {

        // allocate output - same dimensions as input (there will be 0 values because of overlap!)
        int totalY = data.rows;
        int totalX = data.columns;
        final ComplexDoubleMatrix outData = new ComplexDoubleMatrix(totalY, totalX);

        // squared block assumed!
        int numOut = blockSize - (2 * overlap);      // number of output pixels per block

        // Indices for loop in Y direction
        int inData_y0 = 0;                       // y index in inData to get 1st block
        int inData_yN = blockSize - 1;           // y index in inData to get 1st block
        int block_y0 = overlap;                  // y index in dataBlock
        int block_yN = blockSize - 1 - overlap;  // y index in dataBlock (except last block)
        int outData_y0 = block_y0;               // y index in outData (1st block)
        int outLin_yN = block_yN;                // y index in outData

        // Declare indices for loops in X direction
        int dataPix_x0;                         // x index in inData to get 1st block
        int dataPix_xN;                         // x index in inData to get 1st block
        int outBlockPix_x0;                     // x index in dataBlock
        int outBlockPix_xN;                     // x index in dataBlock (except last block)
        int outPix_x0;                          // x index in outData (1st block)
        int outPix_xN;                          // x index in outData


        boolean lastBlock_Y = false;            // only just started...
        boolean lastBlock_X = false;            // only just started...

        boolean doSmooth = false;
        if (method.contains("goldstein")) {
            try {
                doSmooth = ((kernel2d.length / 2) != 0);
            } catch (Exception e) {
                doSmooth = false;
            }
        }


        // loop until all blocks finished
        while (!lastBlock_Y && !lastBlock_X) {

            // check if we are doing the last block
            if (inData_yN >= totalY - 1) {
                lastBlock_Y = true;
                inData_yN = totalY - 1;                    // prevent reading outside inData
                inData_y0 = inData_yN - blockSize + 1;     // but make sure blockData pixels are read
                outLin_yN = totalY - 1 - overlap;          // index in outData to be written
                block_y0 = block_yN - (outLin_yN - outData_y0 + 1) + 1;
            }

            // Indices for loop in X direction
            dataPix_x0 = 0;
            dataPix_xN = blockSize - 1;
            outBlockPix_x0 = overlap;
            outBlockPix_xN = blockSize - 1 - overlap;
            outPix_x0 = outBlockPix_x0;
            outPix_xN = outBlockPix_xN;

            // loop until lastBlock_X
            while (!lastBlock_X) {

                if (dataPix_xN >= totalX - 1) {                // check if we are doing the last block
                    lastBlock_X = true;
                    dataPix_xN = totalX - 1;                   // prevent reading after file
                    dataPix_x0 = dataPix_xN - blockSize + 1;       // but make sure SIZE pixels are read
                    outPix_xN = totalX - 1 - overlap;          // index in FILTERED 2b written     [??????]
                    outBlockPix_x0 = outBlockPix_xN - (outPix_xN - outPix_x0 + 1) + 1;
                }

                // define windows read/write data
                Window winData = new Window(inData_y0, inData_yN, dataPix_x0, dataPix_xN);
                Window winBlock = new Window(block_y0, block_yN, outBlockPix_x0, outBlockPix_xN);
                Window winFiltered = new Window(outData_y0, outLin_yN, outPix_x0, outPix_xN);

                // pull block of data from inData
                ComplexDoubleMatrix block = new ComplexDoubleMatrix((int) winData.lines(), (int) winData.pixels());
                LinearAlgebraUtils.setdata(block, data, winData);

                // get spectrum + filter + ifft
                SpectralUtils.fft2D_inplace(block);

                if (method.contains("convolution")) {

                    LinearAlgebraUtils.dotmult_inplace(block, kernel2d); // the filter...

                } else if (method.contains("goldstein")) {

                    DoubleMatrix amplitude = SarUtils.magnitude(block);
                    if (doSmooth) {
                        amplitude = smooth(amplitude, kernel2d);
                    }
                    goldsteinThresholding(block, amplitude);

                }

                SpectralUtils.invfft2D_inplace(block);

                // set correct part that is filtered in output matrix
                LinearAlgebraUtils.setdata(outData, winFiltered, block, winBlock);

                // checks for loop in X
                if (lastBlock_X) {
                    // after processing last block in X break
                    break;
                } else {
                    // update X direction indices -- corrected if last block
                    dataPix_x0 += numOut;                // next block
                    dataPix_xN += numOut;                // next block
                    outPix_x0 = outPix_xN + 1;           // index in X direction in outData
                    outPix_xN = outPix_x0 + numOut - 1;  // index in X direction in outData
                }


            }
            // check for loop in X an Y
            if (lastBlock_X && lastBlock_Y) {
                break; // after processing last block in Y and X, break
            } else {
                // reset flag for last block in X
                lastBlock_X = false;
                // update Y direction indices in Y
                inData_y0 += numOut;                    // next block
                inData_yN += numOut;                    // next block
                outData_y0 = outLin_yN + 1;             // index in Y direction in outData
                outLin_yN = outData_y0 + numOut - 1;    // index in Y direction in outData
            }
        }

        data = outData;
    }

    public static DoubleMatrix arrangeKernel2d(DoubleMatrix kernel2dIn, final double scaleFactor) {

        final int kernelLines = kernel2dIn.rows;
        final int kernelPixels = kernel2dIn.columns;

        final int size = kernelLines;

        final int hbsL = (kernelLines / 2);
        final int hbsP = (kernelPixels / 2);
        final int extraL = isEven(kernelLines) ? 1 : 0; // 1 less to fill
        final int extraP = isEven(kernelPixels) ? 1 : 0; // 1 less to fill

        DoubleMatrix kernel2dOut = new DoubleMatrix(size, size); // allocate THE matrix
        int rowCnt = 0;
        int colCnt;

        for (int ii = -hbsL + extraL; ii <= hbsL; ++ii) {
            colCnt = 0;
            final int indexii = (ii + size) % size;
            for (int jj = -hbsP + extraP; jj <= hbsP; ++jj) {
                final int indexjj = (jj + size) % size;
                kernel2dOut.put(indexii, indexjj, kernel2dIn.get(rowCnt, colCnt));
                colCnt++;
            }
            rowCnt++;
        }

        if (scaleFactor != 1) {
            kernel2dOut.muli(scaleFactor);
        }

        return kernel2dOut;
    }

    private void goldsteinThresholding(ComplexDoubleMatrix block, DoubleMatrix amplitude) {

        double maxAmplitude = amplitude.max();

        if (maxAmplitude > GOLDSTEIN_THRESHOLD) { // how reliable this threshold is?
            amplitude.divi(maxAmplitude);
            powi(amplitude, goldsteinAlpha);
            LinearAlgebraUtils.dotmult_inplace(block, new ComplexDoubleMatrix(amplitude));
        } else {
//            PhaseFilterUtils.logger.warn("no filtering, maxAmplitude < " + goldsteinThreshold + ", are zeros in this data block?");
        }
    }


    /**
     * B = smooth(A,KERNEL)
     * (circular) spatial moving average with a (2N+1,2N+1) block.
     * See also matlab script smooth.m for some tests.
     * implementation as convolution with FFT's
     * input: KERNEL is the FFT of the kernel (block)
     */
    private DoubleMatrix smooth(final DoubleMatrix inData, final ComplexDoubleMatrix kernel2d) {
        ComplexDoubleMatrix outData = new ComplexDoubleMatrix(inData);      // or define fft(R4)
        SpectralUtils.fft2D_inplace(outData);                               // or define fft(R4)
        LinearAlgebraUtils.dotmult_inplace(outData, kernel2d.conj());
        SpectralUtils.invfft2D_inplace(outData);         // convolution, but still complex...
        return outData.real();                           // you know it is real only...
    }


    /**
     * B = smooth(A,blocksize)
     * (circular) spatial moving average with a (2N+1,2N+1) block.
     * See also matlab script smooth.m for some tests.
     */
    @Deprecated
    public static DoubleMatrix smoothSpace(final DoubleMatrix data, final int blockSize) {

        if (blockSize == 0)
            return data;

        final int nRows = data.rows;
        final int nCols = data.columns;
        final DoubleMatrix smoothData = new DoubleMatrix(nRows, nCols);

        double sum = 0.;
        double nSmooth = (2 * blockSize + 1) * (2 * blockSize + 1);
        int indexii;
        for (int i = 0; i < nRows; ++i) {
            for (int j = 0; j < nCols; ++j) {
                // Smooth this pixel
                for (int ii = -blockSize; ii <= blockSize; ++ii) {
                    indexii = (i + ii + nRows) % nRows;
                    for (int jj = -blockSize; jj <= blockSize; ++jj) {
                        sum += data.get(indexii, (j + jj + nCols) % nCols);
                    }
                }
                smoothData.put(i, j, sum / nSmooth);
                sum = 0.;
            }
        }
        return smoothData;
    }

    // Do the same as smoothSpace but faster   -----> for Goldstein filter ??????
    // some overhead due to conversion r4<->cr4
    private DoubleMatrix smoothSpectral(final DoubleMatrix data, final int blockSize) {

        final int nRows = data.rows;
        final int nCols = data.columns;
        final ComplexDoubleMatrix smoothData = new ComplexDoubleMatrix(nRows, nCols); // init to zero...

        SpectralUtils.fft2D_inplace(smoothData); // or define fft(R4)
        ComplexDoubleMatrix kernel = new ComplexDoubleMatrix(1, nRows); // init to zeros

        // design 1d kernel function of block
        for (int ii = -blockSize; ii <= blockSize; ++ii) {
            kernel.put(0, (ii + nRows) % nRows, new ComplexDouble(1.0 / (2 * blockSize + 1), 0.0));
        }

        ComplexDoubleMatrix kernel2d = LinearAlgebraUtils.matTxmat(kernel, kernel);
        SpectralUtils.fft2D_inplace(kernel2d); // should be real sinc
        LinearAlgebraUtils.dotmult(smoothData, kernel2d);
        SpectralUtils.invfft2D_inplace(smoothData);  // convolution, but still complex...
        return smoothData.real();
    }


    // use FFT's for convolution with smoothkernel
    // this could also be done static, or in the calling routine
    // KERNEL2D is FFT2 of even kernel (no imag part after fft!)
    private void constructSmoothingKernel() {

        ComplexDoubleMatrix kernel1D = new ComplexDoubleMatrix(1, blockSize);             // init to zeros

        int smooth = kernelArray.length / 2;

        for (int ii = -smooth; ii <= smooth; ++ii) {// 1d kernel function of block
            int tmpValue_1 = (ii + blockSize) % blockSize;
            int tmpValue_2 = ii + smooth;// used to be ii-SMOOTH: wrong
            kernel1D.put(0, tmpValue_1, new ComplexDouble(kernelArray[tmpValue_2]));
        }

        kernel2d = LinearAlgebraUtils.matTxmat(kernel1D, kernel1D);
        SpectralUtils.fft2D_inplace(kernel2d);  // should be real sinc
    }

    private void constructRectKernel() {

        // 1d kernel
        final DoubleMatrix kernel1d = new DoubleMatrix(1, blockSize); // init to zeros
        final int overlapLines = (int) Math.floor(kernelArray.length / 2.);

        // 1d kernel function
        for (int ii = -overlapLines; ii <= overlapLines; ++ii) {
            kernel1d.put(0, (ii + blockSize) % blockSize, kernelArray[ii + overlapLines]);
        }

        kernel2d = new ComplexDoubleMatrix(LinearAlgebraUtils.matTxmat(kernel1d, kernel1d));
        SpectralUtils.fft2D_inplace(kernel2d);
        kernel2d.conji();
    }

    private void constructKernel() {
        if (method.contains("goldstein")) {
            constructSmoothingKernel();
        } else if (method.contains("convolution")) {
            constructRectKernel();
        }
    }

}
