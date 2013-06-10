package org.jlinda.core.filtering;

import org.apache.log4j.Logger;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Window;
import org.jlinda.core.utils.LinearAlgebraUtils;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.core.utils.SpectralUtils;

import static org.jblas.MatrixFunctions.powi;
import static org.jlinda.core.utils.MathUtils.isEven;

public class PhaseFilterUtils {

    static Logger logger = Logger.getLogger(PhaseFilter.class.getName());


    /**
     * phasefilter goldstein
     * Input is matrix of SIZE (e.g. 32) lines, and N range pixels.
     * Filtered OUTPUT is same size as input block.
     * Because of overlap, only write to disk in calling routine
     * part (in matrix coord.) [OVERLAP:SIZE-OVERLAP-1]
     * <p/>
     * Smoothing of the amplitude of the spectrum is performed by
     * spatial convolution with a block kernel of size 2*SMOOTH+1.
     * (Which is done by FFT's). e.g. a spatial moving average with
     * kernel (1d) k=[1 1 1 1 1]/5; kernel2d = transpose(k)*k.
     * Blocks in range direction.
     * <p/>
     * After Goldstein and Werner, Radar interferogram filtering
     * for geophysical applications. GRL 25-21 pp 4035-4038, 1998.
     * and: ESA Florence 1997, vol2, pp969-972, Goldstein & Werner
     * "Radar ice motion interferometry".
     */
    public static ComplexDoubleMatrix goldstein(
            final ComplexDoubleMatrix data,
            final float alpha,
            final int overlap,
            final double[] smoothKernel) { // lying down

        // ______ Allocate output matrix ______
        final int size = data.rows;
        final int npix = data.columns;

        final ComplexDoubleMatrix dataFilt = new ComplexDoubleMatrix(size, npix); // output

        // ______ Get block from buffer ______
        final int numOut = size - (2 * overlap);  // number of output pixels
        int dataPixLo = 0;                        // index in DATA to get 1st block
        int dataPixHi = size - 1;                 // index in DATA to get 1st block
        int outBlockPixLo = 0;                    // index in BLOCK (only 1st block)
        int outBlockPixHi = size - 1 - overlap;   // index in BLOCK (except last block)
        int outPixLo = outBlockPixLo;             // index in FILTERED (1st block)
        int outPixHi = outBlockPixHi;             // index in FILTERED
        boolean lastBlockDone = false;            // only just started...

        // note that int floors division
        int smooth = 0;   // half block size, odd kernel
        boolean doSmooth;
        try {
            smooth = smoothKernel.length / 2;
            doSmooth = (smooth != 0);
        } catch (Exception e) {
            doSmooth = false;
        }
//        PhaseFilterUtils.logger.debug("SMOOTH flag: " + doSmooth);  // problem with uint<0 index in smoothkernel

        // use FFT's for convolution with smoothkernel
        // this could also be done static, or in the calling routine
        // KERNEL2D is FFT2 of even kernel (no imag part after fft!)
        ComplexDoubleMatrix kernel2D = null;
        if (doSmooth) {
            ComplexDoubleMatrix kernel1D = new ComplexDoubleMatrix(1, size);             // init to zeros
            for (int ii = -smooth; ii <= smooth; ++ii) {// 1d kernel function of block
                int tmpValue_1 = (ii + size) % size;
                int tmpValue_2 = ii + smooth;// used to be ii-SMOOTH: wrong
//                PhaseFilterUtils.logger.debug("tmp1: " + tmpValue_1 + "; tmp2: " + tmpValue_2);
                kernel1D.put(0, tmpValue_1, new ComplexDouble(smoothKernel[tmpValue_2]));
            }

            kernel2D = LinearAlgebraUtils.matTxmat(kernel1D, kernel1D);
            SpectralUtils.fft2D_inplace(kernel2D);  // should be real sinc

        }
//        PhaseFilterUtils.logger.debug("kernel created for smoothing spectrum");

        // ====== Loop forever, stop after lastblockdone ======
        for (; ; ) {  //forever, like in c!

            if (dataPixHi >= npix - 1) {                  // check if we are doing the last block
                lastBlockDone = true;
                dataPixHi = npix - 1;                     // prevent reading after file
                dataPixLo = dataPixHi - size + 1;         // but make sure SIZE pixels are read
                outPixHi = dataPixHi;                     // index in FILTERED 2b written
                outBlockPixHi = size - 1;                 // write all to the end
                outBlockPixLo = outBlockPixHi - (outPixHi - outPixLo + 1) + 1;
            }

            Window winData = new Window(0, size - 1, dataPixLo, dataPixHi);
            Window winBlock = new Window(0, size - 1, outBlockPixLo, outBlockPixHi);
            Window winFiltered = new Window(0, size - 1, outPixLo, outPixHi);

            // Construct BLOCK as part of DATA
            ComplexDoubleMatrix block = new ComplexDoubleMatrix((int) winData.lines(), (int) winData.pixels());
            LinearAlgebraUtils.setdata(block, data, winData);

            // Get spectrum/amplitude/smooth/filter ______
            SpectralUtils.fft2D_inplace(block);
            DoubleMatrix amplitude = SarUtils.magnitude(block);

            // ______ use FFT's for convolution with rect ______
            if (doSmooth){
//                amplitude = smooth(amplitude, kernel2D);
                ComplexDoubleMatrix outData = new ComplexDoubleMatrix(amplitude);      // or define fft(R4)
                SpectralUtils.fft2D_inplace(outData);                               // or define fft(R4)
                LinearAlgebraUtils.dotmult_inplace(outData, kernel2D.conj());
                SpectralUtils.invfft2D_inplace(outData);         // convolution, but still complex...
                amplitude = outData.real();
            }

            double maxAmplitude = amplitude.max();

            final double goldsteinThreshold = 1e-20;

            if (maxAmplitude > goldsteinThreshold) { // how reliable this threshold is?
                amplitude.divi(maxAmplitude);
                powi(amplitude, alpha);
                LinearAlgebraUtils.dotmult_inplace(block, new ComplexDoubleMatrix(amplitude));
            } else {
                PhaseFilterUtils.logger.warn("no filtering, maxAmplitude < " + goldsteinThreshold + ", are zeros in this data block?");
            }

            SpectralUtils.invfft2D_inplace(block);

            // ______ Set correct part that is filtered in output matrix ______
            LinearAlgebraUtils.setdata(dataFilt, winFiltered, block, winBlock);

            // ______ Exit if finished ______
            if (lastBlockDone)
                return dataFilt;                  // return

            // ______ Update indexes in matrices, will be corrected for last block ______
            dataPixLo += numOut;              // next block
            dataPixHi += numOut;              // next block
            outBlockPixLo = overlap;          // index in block, valid for all middle blocks
            outPixLo = outPixHi + 1;          // index in FILTERED, next range line
            outPixHi = outPixLo + numOut - 1; // index in FILTERED

        } // for all blocks in this buffer


    }

    /**
     * phasefilter buffer by spatial conv. with kernel.
     * Input is matrix of SIZE (e.g. 256) lines, and N range pixels.
     * Filtered OUTPUT is same size as input block.
     * Because of overlap, only write to disk in calling routine
     * part (in matrix coord.) [OVERLAP:SIZE-OVERLAP-1]
     * (in line direction)
     * spatial convolution with a kernel function, such as a block
     * function 111 (1D) (By FFT's).
     * Processing is done in blocks in range direction.
     * For the first block the part [0:OVERLAP-1] is set to 0.
     * For the last block the part [NPIX-1-OVERLAP:NPIX-1] is 0.
     * <p/>
     * Input:
     * - matrix to be filtered of blocklines * numpixs
     * - kernel2d: fft2 of 2d spatial kernel.
     * - overlap: half of the kernel size, e.g., 1 for 111.
     * Output:
     * - filtered matrix.
     * ifft2d(BLOCK .* KERNEL2D) is returned, so if required for
     * non symmetrical kernel, offer the conj(KERNEL2D)!
     */
    public static ComplexDoubleMatrix convbuffer(final ComplexDoubleMatrix data, final ComplexDoubleMatrix kernel2d,
                                                 final int overlap) {         // overlap in column direction

        // Allocate output matrix
        int nRows = data.rows;
        int nCols = data.columns;
        final ComplexDoubleMatrix dataFiltered = new ComplexDoubleMatrix(nRows, nCols);          // allocate output (==0)

        // ______ Get block from buffer ______
        int numout = nRows - (2 * overlap);       // number of output pixels per block
        int dataPixLo = 0;                        // index in CINT to get 1st block
        int dataPixHi = nRows - 1;                // index in CINT to get 1st block
        //int32 outblockpixlo = 0;                // index in BLOCK (only 1st block)
        int outBlockPixLo = overlap;              // index in block
        int outBlockPixHi = nRows - 1 - overlap;  // index in BLOCK (except last block)
        int outPixLo = outBlockPixLo;             // index in FILTERED (1st block)
        int outPixHi = outBlockPixHi;             // index in FILTERED
        boolean lastBlockDone = false;            // only just started...

        // Loop forever, stop after lastblockdone
        for (; ; ) {

            if (dataPixHi >= nCols - 1) {                // check if we are doing the last block
                lastBlockDone = true;
                dataPixHi = nCols - 1;                   // prevent reading after file
                dataPixLo = dataPixHi - nRows + 1;       // but make sure SIZE pixels are read
                // leave last few==0
                outPixHi = nCols - 1 - overlap;          // index in FILTERED 2b written
                //outblockpixhi = SIZE-1;                // write all to the end
                outBlockPixLo = outBlockPixHi - (outPixHi - outPixLo + 1) + 1;
            }

            Window winData = new Window(0, nRows - 1, dataPixLo, dataPixHi);
            Window winBlock = new Window(0, nRows - 1, outBlockPixLo, outBlockPixHi);
            Window winFiltered = new Window(0, nRows - 1, outPixLo, outPixHi);

            // Construct BLOCK as part of DATA ______
            ComplexDoubleMatrix block = new ComplexDoubleMatrix((int) winData.lines(), (int) winData.pixels());
            LinearAlgebraUtils.setdata(block, data, winData);

            // Get spectrum/filter/ifft
            SpectralUtils.fft2D_inplace(block);
            LinearAlgebraUtils.dotmult_inplace(block, kernel2d); // the filter...
            SpectralUtils.invfft2D_inplace(block);

            // Set correct part that is filtered in output matrix
            LinearAlgebraUtils.setdata(dataFiltered, winFiltered, block, winBlock);

            // Exit if finished
            if (lastBlockDone)
                return dataFiltered;                  // return

            // ______ Update indexes in matrices, will be corrected for last block ______
            dataPixLo += numout;             // next block
            dataPixHi += numout;             // next block
            outPixLo = outPixHi + 1;         // index in FILTERED, next range line
            outPixHi = outPixLo + numout - 1;  // index in FILTERED

        } // for all blocks in this buffer

    } // END convbuffer

    /**
     * phasefilter spectral
     * Input is matrix of SIZE (e.g. 32) lines, and N range pixels.
     * Filtered OUTPUT is same size as input block.
     * Because of overlap, only write to disk in calling routine
     * part (in matrix coord.) [OVERLAP:SIZE-OVERLAP-1]
     * *
     * Filtering is performed by pointwise multiplication of the
     * spectrum per block by the KERNEL2D (input).
     * Blocks in range direction,
     */
    public static ComplexDoubleMatrix spectralfilt(final ComplexDoubleMatrix data,
                                                   final DoubleMatrix kernelInput,
                                                   final int blockOverlap) {


        // arrange kernel for 2d filtering : start
        // DoubleMatrix kernel2d = arrangeKernel2d(kernelInput, 1);
        final int kernelLines = kernelInput.rows;
        final int kernelPixels = kernelInput.columns;

        final int scaleFactor = 1;
        final int size = kernelLines;

        final int hbsL = (kernelLines / 2);
        final int hbsP = (kernelPixels / 2);
        final int extraL = isEven(kernelLines) ? 1 : 0; // 1 less to fill
        final int extraP = isEven(kernelPixels) ? 1 : 0; // 1 less to fill

        DoubleMatrix kernel2d = new DoubleMatrix(size, size); // allocate THE matrix

        int rowCnt = 0;
        int colCnt;

        for (int ii = -hbsL + extraL; ii <= hbsL; ++ii) {
            colCnt = 0;
            final int indexii = (ii + size) % size;
            for (int jj = -hbsP + extraP; jj <= hbsP; ++jj) {
                final int indexjj = (jj + size) % size;
                kernel2d.put(indexii, indexjj, kernelInput.get(rowCnt, colCnt));
                colCnt++;
            }
            rowCnt++;
        }
        if (scaleFactor != 1) {
            kernel2d.muli(scaleFactor);
        }

        // Start with filtering

        // Allocate output matrix
        final int nRows = data.rows;
        final int nCols = data.columns;
        final ComplexDoubleMatrix dataFiltered = new ComplexDoubleMatrix(nRows, nCols);

        // ______ Get block from buffer ______
        final int numOut = nRows - (2 * blockOverlap);  // number of output pixels
        int dataPixLo = 0;                              // index in CINT to get 1st block
        int dataPixHi = nRows - 1;                      // index in CINT to get 1st block
        int outBlockPixLo = 0;                          // index in BLOCK (only 1st block)
        int outBlockPixHi = nRows - 1 - blockOverlap;   // index in BLOCK (except last block)
        int outPixLo = outBlockPixLo;                   // index in FILTERED (1st block)
        int outPixHi = outBlockPixHi;                   // index in FILTERED
        boolean lastBlockDone = false;                  // only just started...


        // ====== Loop forever, stop after lastblockdone ======
        for (; ; )      //forever
        {
            if (dataPixHi >= nCols - 1) {                      // check if we are doing the last block
                lastBlockDone = true;
                dataPixHi = nCols - 1;                   // prevent reading after file
                dataPixLo = dataPixHi - nRows + 1;         // but make sure SIZE pixels are read
                outPixHi = dataPixHi;                // index in FILTERED 2b written
                outBlockPixHi = nRows - 1;                   // write all to the end
                outBlockPixLo = outBlockPixHi - (outPixHi - outPixLo + 1) + 1;
            }

            final Window winData = new Window(0, nRows - 1, dataPixLo, dataPixHi);
            final Window winBlock = new Window(0, nRows - 1, outBlockPixLo, outBlockPixHi);
            final Window winFiltered = new Window(0, nRows - 1, outPixLo, outPixHi);

            // Construct BLOCK as part of DATA
            ComplexDoubleMatrix block = new ComplexDoubleMatrix((int) winData.lines(), (int) winData.pixels());
            LinearAlgebraUtils.setdata(block, data, winData);

            // ______ Get spectrum/filter/ifft ______
            SpectralUtils.fft2D_inplace(block);
//            BLOCK.muli(new ComplexDoubleMatrix(kernel2d));                  // the filter...
            LinearAlgebraUtils.dotmult_inplace(block, new ComplexDoubleMatrix(kernel2d));
            SpectralUtils.invfft2D_inplace(block);

            // Set correct part that is filtered in output matrix
            LinearAlgebraUtils.setdata(dataFiltered, winFiltered, block, winBlock);

            // Exit if finished ______
            if (lastBlockDone)
                return dataFiltered;                  // return

            // ______ Update indexes in matrices, will be corrected for last block ______
            dataPixLo += numOut;                // next block
            dataPixHi += numOut;                // next block
            outBlockPixLo = blockOverlap;       // index in block, valid for all middle blocks
            outPixLo = outPixHi + 1;            // index in FILTERED, next range line
            outPixHi = outPixLo + numOut - 1;   // index in FILTERED

        } // for all blocks in this buffer

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

    /**
     * B = smooth(A,KERNEL)
     * (circular) spatial moving average with a (2N+1,2N+1) block.
     * See also matlab script smooth.m for some tests.
     * implementation as convolution with FFT's
     * input: KERNEL is the FFT of the kernel (block)
     */
    public static DoubleMatrix smooth(final DoubleMatrix inData, final ComplexDoubleMatrix kernel2d) {
        ComplexDoubleMatrix outData = new ComplexDoubleMatrix(inData);      // or define fft(R4)
        SpectralUtils.fft2D_inplace(outData);                               // or define fft(R4)
        LinearAlgebraUtils.dotmult_inplace(outData, kernel2d.conj());
        SpectralUtils.invfft2D_inplace(outData);         // convolution, but still complex...
        return outData.real();                           // you know it is real only...
    }

    public static ComplexDoubleMatrix goldstein2D(ComplexDoubleMatrix inData, int blockSize, int overlap,
                                                  double[] smoothKernel, float alpha) {

        // allocate output - same dimensions as input (there will be 0 values because of overlap!)
        int totalY = inData.rows;
        int totalX = inData.columns;
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

        /// THIS IS THE KERNEL COMPUTED IN GOLDSTEIN FILTER --------- [start here]
        // note that int floors division
        boolean doSmooth;
        try {
            doSmooth = ((smoothKernel.length / 2) != 0);
        } catch (Exception e) {
            doSmooth = false;
        }

        ComplexDoubleMatrix kernel2d = null;
        if (doSmooth) {
            kernel2d = constructSmoothingKernel(smoothKernel, blockSize);
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
                LinearAlgebraUtils.setdata(block, inData, winData);

                // Get spectrum / amplitude / smooth / filter ______
                SpectralUtils.fft2D_inplace(block);

                /// ---- this part depends on the filter [actual filtering]: start
                DoubleMatrix amplitude = SarUtils.magnitude(block);
                // ______ use FFT's for convolution with rect ______
                if (doSmooth)
                    amplitude = smooth(amplitude, kernel2d);
                goldsteinThresholding(alpha, block, amplitude);

                SpectralUtils.invfft2D_inplace(block);
                /// ---- this part depends on the filter [actual filtering]: stop

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
        return outData;

    }

    private static void goldsteinThresholding(double alpha, ComplexDoubleMatrix block, DoubleMatrix amplitude) {

        double maxAmplitude = amplitude.max();

        if (maxAmplitude > 1e-20) { // how reliable this threshold is?
            amplitude.divi(maxAmplitude);
            powi(amplitude, alpha);
            LinearAlgebraUtils.dotmult_inplace(block, new ComplexDoubleMatrix(amplitude));
        } else {
//            PhaseFilterUtils.logger.warn("no filtering, maxAmplitude < " + goldsteinThreshold + ", are zeros in this data block?");
        }
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

        // use FFT's for convolution with smoothkernel
    // this could also be done static, or in the calling routine
    // KERNEL2D is FFT2 of even kernel (no imag part after fft!)
    static ComplexDoubleMatrix constructSmoothingKernel(final double[] inKernel, final int nLines) {

        ComplexDoubleMatrix kernel2D;
        ComplexDoubleMatrix kernel1D = new ComplexDoubleMatrix(1, nLines);             // init to zeros

        int smooth = inKernel.length / 2;

        for (int ii = -smooth; ii <= smooth; ++ii) {// 1d kernel function of block
            int tmpValue_1 = (ii + nLines) % nLines;
            int tmpValue_2 = ii + smooth;// used to be ii-SMOOTH: wrong
            kernel1D.put(0, tmpValue_1, new ComplexDouble(inKernel[tmpValue_2]));
        }

        kernel2D = LinearAlgebraUtils.matTxmat(kernel1D, kernel1D);
        SpectralUtils.fft2D_inplace(kernel2D);  // should be real sinc

        return kernel2D;
    }


        public static ComplexDoubleMatrix convBuffer2D(ComplexDoubleMatrix inData,
                                                       int blockSize,
                                                       ComplexDoubleMatrix kernel2d,
                                                       int overlap) {

            // allocate output - same dimensions as input (there will be 0 values because of overlap!)
            int totalY = inData.rows;
            int totalX = inData.columns;
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
                    LinearAlgebraUtils.setdata(block, inData, winData);

                    // get spectrum + filter + ifft
                    SpectralUtils.fft2D_inplace(block);
                    LinearAlgebraUtils.dotmult_inplace(block, kernel2d); // apply the filter...
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
            return outData;
        }




}
