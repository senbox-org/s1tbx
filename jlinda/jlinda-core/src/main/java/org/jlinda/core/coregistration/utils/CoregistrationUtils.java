package org.jlinda.core.coregistration.utils;

import org.apache.log4j.Logger;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Geometry;
import org.jlinda.core.Window;
import org.jlinda.core.utils.LinearAlgebraUtils;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.SarUtils;
import org.jlinda.core.utils.SpectralUtils;

public class CoregistrationUtils {

    static Logger logger = Logger.getLogger(CoregistrationUtils.class.getName());


    public static double crossCorrelateFFT(double[] offset,
                                           ComplexDoubleMatrix master, ComplexDoubleMatrix mask,
                                           int ovsfactor,
                                           int AccL, int AccP) {

        logger.trace("crosscorrelate (PM 15-Apr-2012)");

        // Internal variables
        final int L = master.rows;
        final int P = master.columns;
        final int twoL = 2 * L;
        final int twoP = 2 * P;
        final int halfL = L / 2;
        final int halfP = P / 2;

        double offsetL;
        double offsetP;

        // Check input
        if (master.rows != mask.rows || master.columns != mask.columns) {
            logger.error("mask, master not same size.");
            throw new IllegalArgumentException("mask, master not same size.");
        }

        if (!(MathUtils.isPower2(L) || MathUtils.isPower2(P))) {
            logger.error("mask, master size not power of 2.");
            throw new IllegalArgumentException("mask, master size not power of 2.");
        }

        if (!MathUtils.isPower2(ovsfactor)) {
            logger.error("coherencefft factor not power of 2");
            throw new IllegalArgumentException("coherencefft factor not power of 2");
        }

        // Zero mean magnitude images
        logger.debug("Using de-meaned magnitude patches for incoherent cross-correlation");
        DoubleMatrix magMaster = SarUtils.magnitude(master);
        DoubleMatrix magMask = SarUtils.magnitude(mask);
        magMaster.subi(magMaster.mean());
        magMask.subi(magMask.mean());

        // ======
        // (1) Compute cross-products of master/mask
        // Pad with N zeros to prevent periodical convolution
        ComplexDoubleMatrix master2 = ComplexDoubleMatrix.zeros(twoL, twoP); // initial 0
        ComplexDoubleMatrix mask2 = ComplexDoubleMatrix.zeros(twoL, twoP); // initial 0

        Window windef = new Window(); // defaults to total matrix
        Window win1 = new Window(0, L - 1, 0, P - 1);
        Window win2 = new Window(halfL, halfL + L - 1, halfP, halfP + P - 1);

        LinearAlgebraUtils.setdata(master2, win1, new ComplexDoubleMatrix(magMaster), windef); // zero-mean magnitude
        LinearAlgebraUtils.setdata(mask2, win2, new ComplexDoubleMatrix(magMask), windef); // zero-mean magnitude

        // Crossproducts in spectral/space domain
        // Use mask2 to store cross products temporarly
        SpectralUtils.fft2D_inplace(master2);
        SpectralUtils.fft2D_inplace(mask2);

        master2.conji();
        mask2.muli(master2); // corr = conj(M).*S

        // mask2.mmuli(Master2);

        SpectralUtils.invfft2D_inplace(mask2); // real(mask2): cross prod. in space

        // ======
        // (2) compute norms for all shifts
        // ....use tricks to do this efficient
        // ....real(mask2) contains cross-products
        // ....mask2(0,0):mask2(N,N) for shifts = -N/2:N/2
        // ....rest of this matrix should not be used
        // ....Use Master2 to store intensity here in re,im
        master2 = ComplexDoubleMatrix.zeros(twoL, twoP); // reset to zeros
        int l, p;
        // --- flipud(fliplr(master^2) in real ---
        // --- mask^2 in imag part; this saves a fft ---
        // --- automatically the real/imag parts contain the norms ---
        for (l = L; l < twoL; ++l) {
            for (p = P; p < twoP; ++p) {
                double realPart = magMaster.get(twoL - 1 - l, twoP - 1 - p);
                double imagPart = magMask.get(l - L, p - P);
                ComplexDouble value = new ComplexDouble(Math.pow(realPart, 2), Math.pow(imagPart, 2));
                master2.put(l, p, value);
            }
        }

        // allocate block for reuse
        ComplexDoubleMatrix BLOCK = new ComplexDoubleMatrix(0, 0);
        if (BLOCK.rows != twoL || BLOCK.columns != twoP) {
            logger.debug("crosscorrelate:changing static block to size [" + twoL + ", " + twoP + "]");
            BLOCK.resize(twoL, twoP);
            for (l = halfL; l < halfL + L; ++l)
                for (p = halfP; p < halfP + P; ++p)
                    BLOCK.put(l, p, new ComplexDouble(1, 0));
            SpectralUtils.fft2D_inplace(BLOCK);
            BLOCK.conji();// static variable: keep this for re-use
        }

        // Compute the cross-products, i.e., the norms for each shift ---
        // Master2(0,0):Master2(N,N) for shifts = -N/2:N/2
        SpectralUtils.fft2D_inplace(master2);

        master2.muli(BLOCK);

        SpectralUtils.invfft2D_inplace(master2);// real(Master2): powers of master; imag(Master2): mask

        // ======
        // (3) find maximum correlation at pixel level
        DoubleMatrix Covar = new DoubleMatrix(L + 1, P + 1);// correlation for each shift

        double maxCorr = -999.0f;
        long maxcorrL = 0;// local index in Covar of maxCorr
        long maxcorrP = 0;// local index in Covar of maxCorr

        ComplexDouble maskValueTemp;
        ComplexDouble master2ValueTemp;

        for (l = 0; l <= L; ++l) { // all shifts
            for (p = 0; p <= P; ++p) {// all shifts
                maskValueTemp = mask2.get(l, p);
                master2ValueTemp = master2.get(l, p);

                Covar.put(l, p, maskValueTemp.real() / Math.sqrt(master2ValueTemp.real() * master2ValueTemp.imag()));
                if (Covar.get(l, p) > maxCorr) {
                    maxCorr = Covar.get(l, p);
                    maxcorrL = l;// local index in Covar of maxCorr
                    maxcorrP = p;// local index in Covar of maxCorr
                }
            }
        }

        offsetL = -halfL + maxcorrL; // update by reference
        offsetP = -halfP + maxcorrP; // update by reference
        logger.debug("Pixel level offset:     " + offsetL + ", " + offsetP + " (corr=" + maxCorr + ")");

        // ======
        // (4) oversample to find peak sub-pixel
        // Estimate shift by oversampling estimated correlation
        if (ovsfactor > 1) {
            // --- (4a) get little chip around max. corr, if possible ---
            // --- make sure that we can copy the data ---
            if (maxcorrL < AccL) {
                logger.debug("Careful, decrease AccL or increase winsizeL");
                maxcorrL = AccL;
            }
            if (maxcorrP < AccP) {
                logger.debug("Careful, decrease AccP or increase winsizeP");
                maxcorrP = AccP;
            }
            if (maxcorrL > (L - AccL)) {
                logger.debug("Careful, decrease AccL or increase winsizeL");
                maxcorrL = L - AccL;
            }
            if (maxcorrP > (P - AccP)) {
                logger.debug("Careful, decrease AccP or increase winsizeP");
                maxcorrP = P - AccP;
            }

            Window win3 = new Window(maxcorrL - AccL, maxcorrL + AccL - 1, maxcorrP - AccP, maxcorrP + AccP - 1);

            final DoubleMatrix chip = new DoubleMatrix((int) win3.lines(), (int) win3.pixels()); // construct as part
            LinearAlgebraUtils.setdata(chip, Covar, win3);

            // (4b) oversample chip to obtain sub-pixel max : here I can also fit the PolyNomial - much faster!
            int offL;
            int offP;

            DoubleMatrix chipOversampled = SarUtils.oversample(new ComplexDoubleMatrix(chip), ovsfactor, ovsfactor).getReal();
            int corrIndex = chipOversampled.argmax();
            offL = chipOversampled.indexColumns(corrIndex); // lines are in columns - JBLAS column major
            offP = chipOversampled.indexRows(corrIndex); // pixels are index in rows - JBLAS is column major
            maxCorr = chipOversampled.get(corrIndex);

            offsetL = -halfL + maxcorrL - AccL + (double) offL / (double) ovsfactor;
            offsetP = -halfP + maxcorrP - AccP + (double) offP / (double) ovsfactor;

            logger.debug("Oversampling factor: " + ovsfactor);
            logger.debug("Sub-pixel level offset: " + offsetL + ", " + offsetP + " (corr=" + maxCorr + ")");
            logger.debug("Sub-pixel level offset: " + offsetL + ", " + offsetP + " (corr=" + maxCorr + ")");
        }

        offset[0] = offsetL;
        offset[1] = offsetP;

        return maxCorr;
    }


    public double crossCorrelateSPACE(double[] offset,
                                      ComplexDoubleMatrix master, ComplexDoubleMatrix mask,
                                      final int AccL, final int AccP, final int osFactor) {

        logger.trace("coherencespace (PM 14-Feb-2012)");

        // Internal variables
        final int L = master.rows;
        final int P = master.columns;
        // final int AccL = fineinput.AccL;
        // final int AccP = fineinput.AccP;
        final int factor = osFactor;

        // Select parts of master/slave
        final int MasksizeL = L - 2 * AccL;
        final int MasksizeP = P - 2 * AccP;

        // offset
        double offsetL;
        double offsetP;

        // ______ Check input ______
        if (!MathUtils.isPower2(AccL) || !MathUtils.isPower2(AccP)) {
            logger.error("AccL should be power of 2 for oversampling.");
            throw new IllegalArgumentException("AccL should be power of 2 for oversampling.");
        }
        if (MasksizeL < 4 || MasksizeP < 4) {
            logger.error("Correlationwindow size too small (<4; size= FC_winsize-2*FC_Acc).");
            throw new IllegalArgumentException("Correlationwindow size too small (<4; size= FC_winsize-2*FC_Acc).");
        }

        // ______Shift center of Slave over Master______
        Window winmask = new Window(AccL, AccL + MasksizeL - 1, AccP, AccP + MasksizeP - 1);
        DoubleMatrix coher = new DoubleMatrix(2 * AccL, 2 * AccP); // store result

        // 1st element: shift==AccL
        Window windef = new Window(); // defaults to total

        DoubleMatrix magMask = SarUtils.magnitude(mask); // magnitude
        magMask.subi(magMask.mean()); // subtract mean
        DoubleMatrix Mask2 = new DoubleMatrix((int) winmask.lines(), (int) winmask.pixels());
        LinearAlgebraUtils.setdata(Mask2, magMask, winmask); // construct as part
        double normmask = Math.pow(Mask2.norm2(), 2);
        DoubleMatrix Master2 = new DoubleMatrix(MasksizeL, MasksizeP);
        DoubleMatrix magMaster = SarUtils.magnitude(master);
        Geometry.center(magMaster); // magMaster.subi(magMaster.mean());
        Window winmaster = new Window();
        for (int i = 0; i < 2 * AccL; i++) {
            winmaster.linelo = i;
            winmaster.linehi = i + MasksizeL - 1;
            for (int j = 0; j < 2 * AccP; j++) {
                winmaster.pixlo = j;
                winmaster.pixhi = j + MasksizeP - 1;
                LinearAlgebraUtils.setdata(Master2, windef, magMaster, winmaster);
                // ______Coherence for this position______
                double cohs1s2 = 0.;
                double cohs1s1 = 0.;
                for (int k = 0; k < MasksizeL; k++) {
                    for (int l = 0; l < MasksizeP; l++) {
                        cohs1s2 += (Master2.get(k, l) * Mask2.get(k, l));
                        cohs1s1 += Math.pow(Master2.get(k, l), 2);
                    }
                }
                coher.put(i, j, cohs1s2 / Math.sqrt(cohs1s1 * normmask)); // [-1 1]
            }
        }

        // Correlation in space domain
        int offL;
        int offP;
        final DoubleMatrix coher8 = oversample(coher, factor, factor);

        int coher8MaxIndex = coher8.argmax();
        offL = coher8.indexRows(coher8MaxIndex);
        offP = coher8.indexColumns(coher8MaxIndex);
        final double macCorr = coher8.get(coher8MaxIndex);
        offsetL = AccL - offL / (double) (factor);
        offsetP = AccP - offP / (double) (factor);

        logger.debug("Oversampling factor: " + factor);
        logger.debug("Sub-pixel level offset: " + offsetL + ", " + offsetP + " (corr=" + macCorr + ")");

        offset[0] = offsetL;
        offset[1] = offsetP;

        return macCorr;
    }


    private DoubleMatrix oversample(final DoubleMatrix data, final int factor, final int factor1) {
        return SarUtils.oversample(new ComplexDoubleMatrix(data), factor, factor).getReal();
    }


}
