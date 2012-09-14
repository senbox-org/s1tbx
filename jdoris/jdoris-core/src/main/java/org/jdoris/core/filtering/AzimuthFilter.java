package org.jdoris.core.filtering;

import org.apache.log4j.Logger;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jdoris.core.SLCImage;
import org.jdoris.core.Window;
import org.jdoris.core.todo_classes.todo_classes;
import org.jdoris.core.utils.LinearAlgebraUtils;
import org.jdoris.core.utils.SpectralUtils;
import org.jdoris.core.utils.WeightWindows;

import static org.jblas.MatrixFunctions.pow;

/**
 * User: pmar@ppolabs.com
 * Date: 4/8/11
 * Time: 5:01 PM
 */
public class AzimuthFilter extends SlcDataFilter {

    static Logger logger = Logger.getLogger(AzimuthFilter.class.getName());

    // TODO
    todo_classes.inputgeneral generalInput;
    todo_classes.input_filtazi filtAziInput;

    SLCImage metadata1;

    private long nRows;
    private long nCols;

    private double PRF;               // pulse repetition freq. [Hz]
    private double ABW;  // azimuth band width [Hz]

    // TODO: for now hardcoded!
    private double hammingAlpha;
    private boolean doHamming;
    //    private double hammingAlpha = filtAziInput.getHammingAlpha();

    private boolean variableFilter = false;

    private float deltaF;
    private float freq;
    private double rsr2x;

    private Window absTile = new Window();

    DoubleMatrix filterVector;


    public AzimuthFilter() {
    }

    public DoubleMatrix getFilterVector() {
        return filterVector;
    }

    public void setHammingAlpha(double hammingAlpha) {
        this.hammingAlpha = hammingAlpha;
        doHamming = (hammingAlpha < 0.9999);
    }

    public void setVariableFilter(boolean variableFilter) {
        this.variableFilter = variableFilter;
    }

    public AzimuthFilter(SLCImage master, SLCImage slave, ComplexDoubleMatrix data, Window tileWindow) {

        this.setMetadata(master);
        this.setMetadata1(slave);
        this.setData(data);
        this.setTile(tileWindow);
        defineParameters();

    }

    public void setMetadata1(SLCImage metadata1) {
        this.metadata1 = metadata1;
    }

    public void defineParameters() {

        // declare filter matrix
        nRows = data.rows;
        nCols = data.columns;
        filter = new DoubleMatrix((int) nRows, (int) nCols); // filter

        // define absolute coordinates
        setAbsTile(tile);

        PRF = metadata.getPRF();
        ABW = metadata.getAzimuthBandwidth();
        deltaF = (float) (PRF / nRows);
        freq = (float) (-PRF / 2.0);
        rsr2x = metadata.getRsr2x();
    }

    private void setAbsTile(Window win) {
        absTile.linelo = metadata.getCurrentWindow().linelo + win.linelo;
        absTile.linehi = metadata.getCurrentWindow().linelo + nRows - 1;
        absTile.pixlo = metadata.getCurrentWindow().pixlo + win.pixlo;
        absTile.pixhi = metadata.getCurrentWindow().pixlo + nCols - 1;
    }

    @Override
    public void defineFilter() {

        if ((metadata.doppler.isF_DC_const() && metadata1.doppler.isF_DC_const()) || !variableFilter) {
            defineConstFilter();
        } else {
            defineVariableFilter();
        }
    }

    @Override
    public void applyFilter() {

        SpectralUtils.fft_inplace(data, 1);

//        if (metadata.doppler.isF_DC_const() && metadata1.doppler.isF_DC_const() && !variableFilter) {
//            // FILTERED = diagxmat(FILTER,SLCIMAGE)
            ComplexDoubleMatrix dataTemp = new ComplexDoubleMatrix(filter).mmul(data);
            data = dataTemp.dup();
//        } else {
//            data.muli(new ComplexDoubleMatrix(filter));
//        }

        SpectralUtils.invfft_inplace(data, 1);

    }

    private void defineConstFilter() {

        logger.trace("Filtering data by same fDC for each column.");
//        double fDC_m = metadata.doppler.getF_DC_a0();      // zero doppler freq. [Hz]
//        double fDC_s = metadata1.doppler.getF_DC_a0();       // zero doppler freq. [Hz]
        double fDC_m = metadata.doppler.getF_DC_const();   // const doppler freq. [Hz]
        double fDC_s = metadata1.doppler.getF_DC_const();  // const doppler freq. [Hz]

        logger.debug("Using constant dopplers for filtering");
        logger.debug("-------");
        logger.debug("Image #1 fDC: " + fDC_m);
        logger.debug("Image #2 fDC: " + fDC_s);
        logger.debug("-------");

        double fDC_mean = 0.5 * (fDC_m + fDC_s);   // mean doppler centroid freq.
        double ABW_new = Math.max(1.0, 2.0 * (0.5 * ABW - Math.abs(fDC_m - fDC_mean)));       // new bandwidth>1.0

        logger.debug("New Azimuth Bandwidth: " + ABW_new + " [Hz]");
        logger.debug("New central frequency: " + fDC_mean + " [Hz]");

        DoubleMatrix freqAxis = defineFrequencyAxis(nRows, freq, deltaF);
        DoubleMatrix filterVector;

        final double offset = 0.5;
        filterVector = columnFilterHamming(freqAxis, fDC_m, fDC_mean, ABW_new, offset);

        filter = DoubleMatrix.diag(filterVector);

    }

    private void defineVariableFilter() {

//        if (nCols != metadata.getCurrentWindow().pixels())
        if (nCols != absTile.pixels())
            logger.warn("this will crash, nRows input matrix not ok...");

        // Compute fDC_master, fDC_slave for all columns
        // Create axis to evaluate fDC polynomial for master/slave
        // fDC(column) = fdc_a0 + fDC_a1*(col/RSR) + fDC_a2*(col/RSR)^2
        // fDC = y = Ax
        // Capitals indicate matrices (FDC_M <-> fDC_m)
        logger.trace("Filtering data by evaluated polynomial fDC for each column.");


        // TODO: possible bug, different tiling is NEST : here buffer over the full range line!
        DoubleMatrix xAxis = defineAxis(absTile.pixlo, absTile.pixhi, rsr2x / 2.0, 0);
//        DoubleMatrix xAxis = defineAxis(metadata.getCurrentWindow().pixlo, metadata.getCurrentWindow().pixhi, metadata.getRsr2x() / 2.0, 0);
//        DoubleMatrix xAxis = defineAxis(metadata.getCurrentWindow().pixlo, metadata.getCurrentWindow().pixhi, metadata.getRsr2x() / 2.0);
        DoubleMatrix fDC_Master = defineDopplerAxis(metadata, xAxis);

        // redefine xAxis with different scale factor
        xAxis = defineAxis(absTile.pixlo, absTile.pixhi, rsr2x / 2.0, metadata1.getCoarseOffsetP());
        DoubleMatrix fDC_Slave = defineDopplerAxis(metadata1, xAxis);

        logger.debug("Dumping matrices fDC_m, fDC_s (__DEBUG defined)");
        logger.debug("fDC_m: " + fDC_Master.toString());
        logger.debug("fDC_s: " + fDC_Slave.toString());

        // Axis for filter in frequencies
        // use fft properties to shift...
        DoubleMatrix frequencyAxis = defineFrequencyAxis(nRows, freq, deltaF);
        DoubleMatrix filterVector; // filter per column

        /// design a filter
        double fDC_m;   // zero doppler freq. [Hz]
        double fDC_s;   // zero doppler freq. [Hz]
        double fDC_mean;// mean doppler centroid freq.
        double ABW_new; // new bandwidth > 1.0
        final double offset = 0;

        for (long i = 0; i < nCols; ++i) {

            fDC_m = fDC_Master.get(0, (int) i);
            fDC_s = fDC_Slave.get(0, (int) i);
            fDC_mean = 0.5 * (fDC_m + fDC_s);
            ABW_new = Math.max(1.0, 2.0 * (0.5 * ABW - Math.abs(fDC_m - fDC_mean)));

            if (doHamming) {
                filterVector = columnFilterHamming(frequencyAxis, fDC_m, fDC_mean, ABW_new, offset);
            } else {
                filterVector = columnFilterRect(frequencyAxis, fDC_mean, ABW_new, offset);
            }

            this.filter.putColumn((int) i, filterVector);   // store filter Vector in filter Matrix

        } // foreach column

    }

    private DoubleMatrix columnFilterHamming(final DoubleMatrix frequencyAxis, final double fDC_m, final double fDC_mean, final double ABW_new, final double offset) {

//        DoubleMatrix filterVector;

        DoubleMatrix inVerseHamming = WeightWindows.inverseHamming(frequencyAxis, ABW, PRF, hammingAlpha);

        // Shift this circular by myshift pixels
        long myShift = (long) (Math.rint((nRows * fDC_m / PRF) + offset)); // round
        LinearAlgebraUtils.wshift_inplace(inVerseHamming, (int) -myShift);    // center at fDC_m

        // Newhamming is scaled and centered around new mean
        myShift = (long) (Math.rint((nRows * fDC_mean / PRF) + offset));                   // round
        filterVector = WeightWindows.hamming(frequencyAxis, ABW_new, PRF, hammingAlpha); // fftshifted
        LinearAlgebraUtils.wshift_inplace(filterVector, (int) -myShift);                      // center at fDC_mean
        filterVector.muli(inVerseHamming);

        SpectralUtils.ifftshift_inplace(filterVector);           // fftsh works on data!
        return filterVector;

    }

    private DoubleMatrix columnFilterRect(DoubleMatrix frequencyAxis, double fDC_mean, double ABW_new, double offset) {
//        DoubleMatrix filterVector;
        long myShift = (long) (Math.rint((nRows * fDC_mean / PRF) + offset));   // round
        filterVector = WeightWindows.rect(frequencyAxis.divi(ABW_new));         // fftshifted
        LinearAlgebraUtils.wshift_inplace(filterVector, (int) -myShift);        // center at fDC_mean
        SpectralUtils.ifftshift_inplace(filterVector);                          // fftsh works on data!
        return filterVector;
    }


    private static DoubleMatrix defineAxis(final long min, final long max, final double scale, int offset) {
        DoubleMatrix xAxis = new DoubleMatrix(1, (int) max);  // lying
        for (long i = min; i <= max; ++i)
            xAxis.put(0, (int) (i - min), (i - 1.0 + offset) / (scale));
//        xAxis.divi(scale / 2.0);
        return xAxis;
    }

    private DoubleMatrix defineFrequencyAxis(final long size, final double freq, final double deltaF) {
        DoubleMatrix frequencyAxis = new DoubleMatrix(1, (int) size);
        for (int i = 0; i < size; ++i)
            frequencyAxis.put(0, i, freq + (i * deltaF)); // [-fr:df:fr-df]
        return frequencyAxis;
    }

    // doppler progression of image over input axis
    private static DoubleMatrix defineDopplerAxis(SLCImage master, DoubleMatrix xAxis) {
        DoubleMatrix fDC_Master = xAxis.mul(master.doppler.getF_DC_a1());
        fDC_Master.addi(master.doppler.getF_DC_a0());
        fDC_Master.addi(pow(xAxis, 2).mmul(master.doppler.getF_DC_a2()));
        return fDC_Master;
    }

    /**
     * azimuth filter per block
     * Input is matrix of SIZE (e.g. 1024) lines, and N range pixs.
     * Input is SLC of master. slave_info gives fDC polynomial
     * for slave + coarse offset. HAMMING is alpha for myhamming f.
     * Filtered OUTPUT is same nRows as input block.
     * Because of overlap (azimuth), only write to disk in calling
     * routine part (in matrix coord.) [OVERLAP:SIZE-OVERLAP-1]
     * = SIZE-(2*OVERLAP);  // number of output pixels
     * <p/>
     * Filtering is performed in the spectral domain
     * (1DFFT over azimuth for all columns at once)
     * Filter is different for each column due to shift in fd_c
     * doppler centroid frequency.
     */
    public ComplexDoubleMatrix filterBlock(
            final ComplexDoubleMatrix slcData,
            final SLCImage master, // PRF, BW, fd0
            final SLCImage slave,  // PRF, BW, fd0
            final double hamming) throws Exception {

        final long size = slcData.rows;     // fftlength
        final long nCols = slcData.columns; // width
//        if (nCols != master.getCurrentWindow().pixels())
        if (nCols != master.getCurrentWindow().pixels())
            logger.warn("this will crash, nRows input matrix not ok...");

        final boolean doHamming = (hamming < 0.9999);
        final double PRF = master.getPRF();               // pulse repetition freq. [Hz]
        final double ABW = master.getAzimuthBandwidth();  // azimuth band width [Hz]

        final float deltaF = (float) (PRF / size);
        final float freq = (float) (-PRF / 2.0);

        // Compute fDC_master, fDC_slave for all columns
        // Create axis to evaluate fDC polynomial for master/slave
        // fDC(column) = fdc_a0 + fDC_a1*(col/RSR) + fDC_a2*(col/RSR)^2
        // fDC = y = Ax
        // Capitals indicate matrices (FDC_M <-> fDC_m)
        logger.debug("Filtering data by evaluated polynomial fDC for each column.");

        DoubleMatrix xAxis = defineAxis(master.getCurrentWindow().pixlo, master.getCurrentWindow().pixhi, master.getRsr2x() / 2.0, metadata1.getCoarseOffsetP());
        DoubleMatrix fDC_Master = defineDopplerAxis(master, xAxis);

        // redefine xAxis with different scale factor
        xAxis = defineAxis(master.getCurrentWindow().pixlo, master.getCurrentWindow().pixhi, slave.getRsr2x() / 2.0, metadata1.getCoarseOffsetP());
        DoubleMatrix fDC_Slave = defineDopplerAxis(slave, xAxis);

        logger.debug("Dumping matrices fDC_m, fDC_s (__DEBUG defined)");
        logger.debug("fDC_m: " + fDC_Master.toString());
        logger.debug("fDC_s: " + fDC_Slave.toString());

        // Axis for filter in frequencies
        // TODO check, rather shift, test matlab... or wshift,1D over dim1
        // use fft properties to shift...
        DoubleMatrix freqAxis = new DoubleMatrix(1, (int) size);
        for (int i = 0; i < size; ++i)
            freqAxis.put(0, i, freq + (i * deltaF)); // [-fr:df:fr-df]

        DoubleMatrix filterVector; // filter per column
        DoubleMatrix filterMatrix = new DoubleMatrix((int) size, (int) nCols); // filter

        // design a filter
        double fDC_m;   // zero doppler freq. [Hz]
        double fDC_s;   // zero doppler freq. [Hz]
        double fDC_mean;// mean doppler centroid freq.
        double ABW_new; // new bandwidth > 1.0
        for (long i = 0; i < nCols; ++i) {

            fDC_m = fDC_Master.get(0, (int) i);
            fDC_s = fDC_Slave.get(0, (int) i);
            fDC_mean = 0.5 * (fDC_m + fDC_s);
            ABW_new = Math.max(1.0, 2.0 * (0.5 * ABW - Math.abs(fDC_m - fDC_mean)));

            if (doHamming) {
                // TODO: not a briliant implementation for per col.. cause wshift AND fftshift.
                // DE-weight spectrum at centered at fDC_m
                // spectrum should be periodic -> use of wshift
//                DoubleMatrix inVerseHamming = invertHamming(WeightWindows.hammingAlpha(freqAxis, ABW, PRF, hammingAlpha), nRows);
                DoubleMatrix inVerseHamming = WeightWindows.inverseHamming(freqAxis, ABW, PRF, hamming);

                // Shift this circular by myshift pixels
                long myShift = (long) (Math.rint((size * fDC_m / PRF))); // round
                LinearAlgebraUtils.wshift_inplace(inVerseHamming, (int) -myShift);    // center at fDC_m

                // Newhamming is scaled and centered around new mean
                myShift = (long) (Math.rint((size * fDC_mean / PRF)));                   // round
                filterVector = WeightWindows.hamming(freqAxis, ABW_new, PRF, hamming); // fftshifted
                LinearAlgebraUtils.wshift_inplace(filterVector, (int) -myShift);                      // center at fDC_mean
                filterVector.mmuli(inVerseHamming);

            } else {       // no weighting, but center at fDC_mean, nRows ABW_new

                long myShift = (long) (Math.rint((size * fDC_mean / PRF)));          // round
                filterVector = WeightWindows.rect(freqAxis.divi((float) ABW_new)); // fftshifted
                LinearAlgebraUtils.wshift_inplace(filterVector, (int) -myShift);                  // center at fDC_mean

            }

            SpectralUtils.ifftshift_inplace(filterVector);           // fftsh works on data!
            filterMatrix.putColumn((int) i, filterVector);   // store filter Vector in filter Matrix

        } // foreach column


        // Filter slcdata
        ComplexDoubleMatrix slcDataFiltered = slcData.dup();
        SpectralUtils.fft_inplace(slcDataFiltered, 1);                         // fft foreach column
        slcDataFiltered.mmuli(new ComplexDoubleMatrix(filterMatrix));
        SpectralUtils.invfft_inplace(slcDataFiltered, 1);                        // ifft foreach column
        return slcDataFiltered;

    }


/*
    private static DoubleMatrix invertHamming(DoubleMatrix hammingAlpha, long nRows) {
        for (long ii = 0; ii < nRows; ++ii)
            hammingAlpha.put(0, (int) ii, (float) (1.0 / hammingAlpha.get(0, (int) ii)));
        return hammingAlpha;
    }
*/


/*
    private DoubleMatrix temp(double fDC_m, double fDC_mean, double ABW_new, DoubleMatrix freqAxis, final double offset) {

        DoubleMatrix filterVector;

        if (doHamming) {
            // ______ NOT a good implementation for per col., cause wshift *AND* fftshift.
            // ______ DE-weight spectrum at centered at fDC_m ______
            // ______ spectrum should be periodic! (use wshift) ______
            DoubleMatrix inverseHamming = WeightWindows.inverseHamming(freqAxis, ABW, PRF, hammingAlpha);

            // ______ Shift this circular by myshift pixels ______
            int myshift = (int) (((nRows * fDC_m / PRF) + offset));  // round

            LinearAlgebraUtils.wshift_inplace(inverseHamming, -myshift);          // center at fDC_m


            // ______ Newhamming is scaled and centered around new mean ______
            myshift = (int)(((nRows * fDC_mean / PRF) + offset));             // round
            filterVector = WeightWindows.hamming(freqAxis,ABW_new, PRF, hammingAlpha);         // fftshifted
            LinearAlgebraUtils.wshift_inplace(filterVector, -myshift);

            filterVector.mmuli(inverseHamming);

        } else {
            int myshift = (int)(((nRows * fDC_mean / PRF) + offset));       // round

            filterVector = WeightWindows.rect(freqAxis.divi(ABW_new));
            LinearAlgebraUtils.wshift_inplace(filterVector, -myshift);
        }
        return filterVector;
    }
*/


//    /**
//     * azimuthfilter
//     * Loop over whole master and slave image and filter out
//     * part of the spectrum that is not common.
//     * Only do zero doppler freq. offset.
//     * do not use a polynomial from header for now.
//     * (next we will, but assume image are almost coreg. in range,
//     * so f_dc polynomial can be eval. same)
//     * Per block in azimuth [1024] use a certain overlap with the
//     * next block so that same data is partially used for spectrum
//     * (not sure if this is requried).
//     * Filter is composed of: DE-hammingAlpha, RE-hammingAlpha (for correct
//     * new nRows and center of the spectrum).
//     * Trick in processor.c: First call routine as:
//     * (generalinput,filtaziinput,master,slave)
//     * in order to process the master, and then as:
//     * (generalinput,filtaziinput,slave,master)
//     * to filter the slave slc image.
//     */
//    public static void azimuthfilter(final todo_classes.inputgeneral generalinput,
//                                     final todo_classes.input_filtazi fitaziinput,
//                                     SLCImage master, // not const, fdc possibly reset here?
//                                     SLCImage slave) {
//
//
//    }

}
