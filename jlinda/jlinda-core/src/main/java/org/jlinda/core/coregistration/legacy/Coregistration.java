package org.jlinda.core.coregistration.legacy;

import org.apache.log4j.Logger;
import org.jblas.*;
import org.jlinda.core.*;
import org.jlinda.core.coregistration.LUT;
import org.jlinda.core.todo_classes.Input;
import org.jlinda.core.todo_classes.todo_classes;
import org.jlinda.core.utils.*;

import java.util.Arrays;
import java.util.Comparator;

import static java.lang.Math.*;
import static org.jlinda.core.utils.PolyUtils.normalize2;
import static org.jlinda.core.utils.PolyUtils.polyval;

public class Coregistration implements ICoregistration {

    static Logger logger = Logger.getLogger(Coregistration.class.getName());

    @Override
    public void coarseporbit(Ellipsoid ell, SLCImage master, SLCImage slave, Orbit masterorbit, Orbit slaveorbit, Baseline baseline) throws Exception {

        logger.trace("coarseporbit (PM 01-Apr-2012)");
        final int MAXITER = 10; // maximum number of iterations
        final double CRITERPOS = 1e-6; // 1micrometer
        final double CRITERTIM = 1e-10; // seconds (~10-6 m)

        // Get (approx) center pixel of current Window master
        final long cen_lin = (master.getCurrentWindow().linelo
                + master.getCurrentWindow().linehi) / 2;
        final long cen_pix = (master.getCurrentWindow().pixlo
                + master.getCurrentWindow().pixhi) / 2;
        final double HEI = 0.0d;

        // ______ Compute x,y,z (fill P) ______
        // ______ P.x/y/z contains (converged) solution ______
        Point xyzP = masterorbit.lp2xyz(cen_lin, cen_pix, master);

        // ______Compute line,pixel for slave of this xyz______
        Point lpP = slaveorbit.xyz2lp(xyzP, slave);

        // ______ offset = P_slave - P_master = lin - cen_lin ______
        logger.info("Estimated translation (l,p): " + Math.floor(lpP.y - cen_lin + .5)
                + ", " + Math.floor(lpP.x - cen_pix + .5));

        logger.debug("\n\n*******************************************************************");
        logger.debug("\n* COARSE_COREGISTRATION Orbits");
        logger.debug("\n*******************************************************************");
        logger.debug("\n(Approximate) center master (line,pixel,hei): " + cen_lin + ", " + cen_pix + ", " + HEI);
        logger.debug("\nEllipsoid WGS84 coordinates of this pixel (x,y,z): (" + xyzP.x + ", " + xyzP.y + ", " + xyzP.z + ")");
        logger.debug("\n(line,pixel) of these coordinates in slave: " + lpP.y + ", " + lpP.x);
        logger.debug("\nEstimated translation slave w.r.t. master (l,p):" + Math.round(lpP.y - cen_lin) + ", " + Math.round(lpP.x - cen_pix));
        //        logger.debug("\nMaximum number of iterations: " + MAXITER)
        //        logger.debug("\nCriterium for position (m): " + CRITERPOS)
        //        logger.debug("\nCriterium for azimuth time (s): " + CRITERTIM + " (=~ ")
        //        logger.debug(CRITERTIM * 7.e3 + "m)")
        //        logger.debug("\nNumber of iterations conversion line,pixel to xyz: ")
        //        logger.debug(xyzP)
        //        logger.debug("\nNumber of iterations conversion xyz to line,pixel: ")
        //        logger.debug(lpP)
        logger.debug("\n*******************************************************************\n");
    }

    @Override
    public void coarsecorrel(Input.CoarseCorr input, SLCImage minfo, SLCImage sinfo) {

        logger.trace("coarsecorrel (PM 01-Apr-2012)");

        String dummyline; // for errormessages
        final int Nwin = input.Nwin; // number of windows
        int NwinNANrm = input.Nwin; ///MA number of windows w/o -999
        final long initoffsetL = input.initoffsetL; // initila offset
        final long initoffsetP = input.initoffsetP; // initila offset
        int MasksizeL = input.MasksizeL; // size of correlation Window
        int MasksizeP = input.MasksizeP; // size of correlation Window
        final int AccL = input.AccL; // accuracy of initial offset
        final int AccP = input.AccP; // accuracy of initial offset
        boolean pointsrandom = true;
        if (input.ifpositions != null) // filename specified
            pointsrandom = false; // only use those points


        // ______Only odd Masksize possible_____
        boolean forceoddl = false;
        boolean forceoddp = false;
        if (!MathUtils.isOdd(MasksizeL)) {
            forceoddl = true;
            MasksizeL += 1; // force oddness
        }
        if (!MathUtils.isOdd(MasksizeP)) {
            forceoddp = true;
            MasksizeP += 1; // force oddness
        }

        // ______Corners of slave in master system______
        // ______offset = A(slave system) - A(master system)______
        final long sl0 = sinfo.getCurrentWindow().linelo - initoffsetL;
        final long slN = sinfo.getCurrentWindow().linehi - initoffsetL;
        final long sp0 = sinfo.getCurrentWindow().pixlo - initoffsetP;
        final long spN = sinfo.getCurrentWindow().pixhi - initoffsetP;

        // ______Corners of useful overlap master,slave in master system______
        final int BORDER = 20;// slightly smaller
        final int l0 = (int) (max(minfo.getCurrentWindow().linelo, sl0) + 0.5
                * MasksizeL + AccL + BORDER);
        final int lN = (int) (Math.min(minfo.getCurrentWindow().linehi, slN) - 0.5
                * MasksizeL - AccL - BORDER);
        final int p0 = (int) (max(minfo.getCurrentWindow().pixlo, sp0) + 0.5
                * MasksizeP + AccP + BORDER);
        final int pN = (int) (Math.min(minfo.getCurrentWindow().pixhi, spN) - 0.5
                * MasksizeP - AccP - BORDER);
        final Window overlap = new Window(l0, lN, p0, pN);

        // ______Distribute Nwin points over window______
        // ______Centers(i,0): line, (i,1): pixel, (i,2) flagfromdisk______
        int[][] Centers = new int[Nwin][1];
        if (pointsrandom) { // no filename specified
            Centers = MathUtils.distributePoints(Nwin, overlap);
        } else { // read positions from input file
            /*
                        // read in points (center of windows) from file
                        Centers.resize(Nwin, 3);
                        ifstream ifpos;
                        openfstream(ifpos, coarsecorrinput.ifpositions);
                        bk_assert(ifpos, coarsecorrinput.ifpositions, __FILE__, __LINE__);
                        int ll, pp;
                        for (int i = 0; i < Nwin; ++i) {
                            ifpos >> ll >> pp;
                            Centers(i, 0) =int(ll); // correct for lower left corner
                            Centers(i, 1) =int(pp); // correct for lower left corner
                            Centers(i, 2) =int(1); // flag from file
                            ifpos.getline(dummyline, ONE27, '\n'); // goto next line.
                        }
                        ifpos.close();

                        // ______ Check last point ivm. EOL after last position in file ______
                        if (Centers(Nwin - 1, 0) == Centers(Nwin - 2, 0)
                                && Centers(Nwin - 1, 1) == Centers(Nwin - 2, 1)) {
                            Centers(Nwin - 1, 0) =int(.5 * (lN + l0) + 27); // random
                            Centers(Nwin - 1, 1) =int(.5 * (pN + p0) + 37); // random
                            logger.warn("CC: there should be no EOL after last point in file: "
                                    + coarsecorrinput.ifpositions;
                            WARNING.print();
                        }

                        // ______ Check if points are in overlap ______
                        // ______ no check for uniqueness of points ______
                        boolean troubleoverlap = false;
                        for (int i = 0; i < Nwin; ++i) {
                            if (Centers(i, 0) < l0) {
                                troubleoverlap = true;
                                logger.warn("COARSE_CORR: point from file: " + i + 1 + " "
                                        + Centers(i, 0) + " " + Centers(i, 1)
                                        + " outside overlap master, slave. New position: ";
                                Centers(i, 0) = l0 + l0 - Centers(i, 0);
                                logger.warn(Centers(i, 0) + " " + Centers(i, 1);
                                WARNING.print();
                            }
                            if (Centers(i, 0) > lN) {
                                troubleoverlap = true;
                                logger.warn("COARSE_CORR: point from file: " + i + 1 + " "
                                        + Centers(i, 0) + " " + Centers(i, 1)
                                        + " outside overlap master, slave. New position: ";
                                Centers(i, 0) = lN + lN - Centers(i, 0);
                                logger.warn(Centers(i, 0) + " " + Centers(i, 1);
                                WARNING.print();
                            }
                            if (Centers(i, 1) < p0) {
                                troubleoverlap = true;
                                logger.warn("COARSE_CORR: point from file: " + i + 1 + " "
                                        + Centers(i, 0) + " " + Centers(i, 1)
                                        + " outside overlap master, slave. New position: ";
                                Centers(i, 1) = p0 + p0 - Centers(i, 1);
                                logger.warn(Centers(i, 0) + " " + Centers(i, 1);
                                WARNING.print();
                            }
                            if (Centers(i, 1) > pN) {
                                troubleoverlap = true;
                                logger.warn("COARSE_CORR: point from file: " + i + 1 + " "
                                        + Centers(i, 0) + " " + Centers(i, 1)
                                        + " outside overlap master, slave. New position: ";
                                Centers(i, 1) = pN + pN - Centers(i, 1);
                                logger.warn(Centers(i, 0) + " " + Centers(i, 1);
                                WARNING.print();
                            }
                        }
                        if (troubleoverlap) // give some additional info
                        {
                            WARNING
                                    + "FINE: there were points from file outside overlap (l0,lN,p0,pN): "
                                    + l0 + " " + lN + " " + p0 + " " + pN + ends;
                            WARNING.print();
                        }
            */
        }

        // Compute correlation of these points
        ComplexDoubleMatrix Mcmpl;
        ComplexDoubleMatrix Scmpl;
        DoubleMatrix Master; // amplitude master
        DoubleMatrix Mask; // amplitude slave
        DoubleMatrix Correl; // matrix with correlations
        DoubleMatrix Result = new DoubleMatrix(Nwin, 3);// R(i,0)=correlation; (i,1)=delta l; (i,2)=delta p;

        for (int i = 0; i < Nwin; i++) {

            // Center of Window in master system
            int cenMwinL = Centers[i][0];
            int cenMwinP = Centers[i][1];

            Window master = new Window(); // size=masksize+2*acc.
            master.linelo = cenMwinL - (MasksizeL - 1) / 2 - AccL; // ML is forced odd
            master.linehi = master.linelo + MasksizeL + 2 * AccL - 1;
            master.pixlo = cenMwinP - (MasksizeP - 1) / 2 - AccP; // MP is forced odd
            master.pixhi = master.pixlo + MasksizeP + 2 * AccP - 1;

            // Same points in slave system (disk)
            Window slavemask = new Window(); // size=masksize
            int cenSwinL = (int) (cenMwinL + initoffsetL); // adjust initoffset
            int cenSwinP = (int) (cenMwinP + initoffsetP); // adjust initoffset
            slavemask.linelo = cenSwinL - (MasksizeL - 1) / 2; // ML is forced odd
            slavemask.linehi = slavemask.linelo + MasksizeL - 1;
            slavemask.pixlo = cenSwinP - (MasksizeP - 1) / 2; // MP is forced odd
            slavemask.pixhi = slavemask.pixlo + MasksizeP - 1;

            // Read windows from files, compute magnitude
            // TODO: this is where data is loaded!!!
            //            Mcmpl = minfo.readdata(master);
            //            Scmpl = sinfo.readdata(slavemask);
            Mcmpl = readdata(master);
            Scmpl = readdata(slavemask);
            Master = SarUtils.magnitude(Mcmpl);
            Mask = SarUtils.magnitude(Scmpl);

            // Compute correlation matrix and find maxima
            Correl = correlate(Master, Mask);

            int corrIndex = Correl.argmax();
            int L = Correl.indexRows(corrIndex);
            int P = Correl.indexColumns(corrIndex);
            double corr = Correl.get(corrIndex);

            if (corr == 0) {
                corr = -999;
            }

            long relcenML = master.linehi - cenMwinL; // system of matrix
            long relcenMP = master.pixhi - cenMwinP;  // system of matrix
            long reloffsetL = relcenML - L;
            long reloffsetP = relcenMP - P;
            long offsetL = reloffsetL + initoffsetL;  // estimated offset lines
            long offsetP = reloffsetP + initoffsetP;  // estimated offset pixels

            Result.put(i, 0, corr);
            Result.put(i, 1, offsetL);
            Result.put(i, 2, offsetP);
        }

        // Get correct offsetL, offsetP
        long offsetLines = -999;
        long offsetPixels = -999;
        getoffset(Result, offsetLines, offsetPixels);

        logger.debug("*******************************************************************"
                + "\n* COARSE_COREGISTRATION: Correlation"
                + "\n*******************************************************************");
        logger.debug("Number of correlation windows: \t" + Nwin
                + "\nCorrelation Window size (l,p): \t" + MasksizeL + ", " + MasksizeP);

        if (forceoddl)
            logger.debug("(l forced odd) ");
        if (forceoddp)
            logger.debug("(p forced odd)");

        logger.debug("\nSearchwindow size (l,p): \t\t" + MasksizeL + 2 * AccL + ", " + MasksizeP + 2 * AccP);

        logger.debug("\nNumber \tposl \tposp \toffsetl offsetp \tcorrelation\n");
        for (int k = 0; k < Nwin; k++) {

            if (Result.get(k, 0) == -999)
                NwinNANrm = NwinNANrm - 1;

            logger.debug(k + "\t" + Centers[k][0] + "\t" + Centers[k][1]
                    + "\t" + Result.get(k, 1) + "\t" + Result.get(k, 2) + "\t"
                    + Result.get(k, 0));
        }

        logger.debug("Estimated total offset (l,p): \t" + offsetLines + ", " + offsetPixels);
        logger.debug("*******************************************************************");

        logger.info("\n*******************************************************************");
        logger.info("\n*_Start_process: " + "Coarse Coregistration");
        logger.info("\n*******************************************************************");
        logger.info("\nEstimated translation slave w.r.t. master:");
        logger.info("\nCoarse_correlation_translation_lines: \t" + offsetLines);
        logger.info("\nCoarse_correlation_translation_pixels: \t" + offsetPixels);
        logger.info("\nNumber of correlation windows: \t\t" + NwinNANrm + " of " + Nwin);
        logger.info("\n\n#     center(l,p)   coherence   offsetL   offsetP\n");
        for (int k = 0; k < Nwin; k++) {
            if (Result.get(k, 0) == -999)
                continue;
            logger.info(k + " \t" + Centers[k][0] + " \t" + Centers[k][1]
                    + " \t" + Result.get(k, 0) + " \t" + Result.get(k, 1) + " \t"
                    + Result.get(k, 2) + "\n");
        }

        logger.info("\n*******************************************************************");
        logger.info("\n*_End_process: " + "Coarse Coregistration");
        logger.info("\n*******************************************************************");

    }

    // TODO: dummy method to pass the testing
    private ComplexDoubleMatrix readdata(Window dummy) {
        return new ComplexDoubleMatrix((int) dummy.lines(), (int) dummy.pixels());
    }

    @Override
    public void coarsecorrelfft(Input.CoarseCorr coarsecorrinput, SLCImage minfo, SLCImage sinfo) {

        logger.trace("coarsecorrelfft (PM 28-Feb-2012)");
        if (!coarsecorrinput.method.equals("cc_magfft")) {
            logger.error("unknown method, This routine is only for cc_magfft method.");
            throw new IllegalArgumentException("unknown method, This routine is only for cc_magfft method.");
        }

        //final  int Mfilelines   = minfo.getCurrentWindow().lines();
        //final  int Sfilelines   = sinfo.getCurrentWindow().lines();
        final int Nwin = coarsecorrinput.Nwin; // number of windows
        int NwinNANrm = coarsecorrinput.Nwin; ///MA number of windows w/o -999
        final long initoffsetL = coarsecorrinput.initoffsetL;// initial offset
        final long initoffsetP = coarsecorrinput.initoffsetP;// initial offset
        final int MasksizeL = coarsecorrinput.MasksizeL; // size of correlation Window
        final int MasksizeP = coarsecorrinput.MasksizeP; // size of correlation Window

        boolean pointsrandom = true;
        if (coarsecorrinput.ifpositions != null) { // filename specified
            pointsrandom = false; // only use these points
        }

        // ______Only pow2 Masksize possible_____
        if (!MathUtils.isPower2(MasksizeL)) {
            logger.error("coarse correl fft: MasksizeL should be 2^n");
            throw new IllegalArgumentException("coarse correl fft: MasksizeL should be 2^n");
        }
        if (!MathUtils.isPower2(MasksizeP)) {
            logger.error("coarse correl fft: MasksizeP should be 2^n");
            throw new IllegalArgumentException("coarse correl fft: MasksizeP should be 2^n");
        }

        // ______Corners of slave in master system______
        // ______offset = [A](slave system) - [A](master system)______
        final long sl0 = sinfo.getCurrentWindow().linelo - initoffsetL;
        final long slN = sinfo.getCurrentWindow().linehi - initoffsetL;
        final long sp0 = sinfo.getCurrentWindow().pixlo - initoffsetP;
        final long spN = sinfo.getCurrentWindow().pixhi - initoffsetP;

        // ______Corners of useful overlap master,slave in master system______
        final int BORDER = 20;// slightly smaller
        final long l0 = max(minfo.getCurrentWindow().linelo, sl0) + BORDER;
        final long lN = Math.min(minfo.getCurrentWindow().linehi, slN) - MasksizeL - BORDER;
        final long p0 = max(minfo.getCurrentWindow().pixlo, sp0) + BORDER;
        final long pN = Math.min(minfo.getCurrentWindow().pixhi, spN) - MasksizeP - BORDER;
        final Window overlap = new Window(l0, lN, p0, pN);

        // ______Distribute Nwin points over window______
        // ______Minlminp(i,0): line, (i,1): pixel, (i,2) flagfromdisk______
        int[][] Minlminp = new int[Nwin][1];
        if (pointsrandom) // no filename specified
        {
            Minlminp = distributepoints(Nwin, overlap);
        } else {// read in points (center of windows) from file
            /*
                            Minlminp.resize(Nwin, 3);
                            ifstream ifpos;
                            openfstream(ifpos, coarsecorrinput.ifpositions);
                            bk_assert(ifpos, coarsecorrinput.ifpositions, __FILE__, __LINE__);
                            int ll, pp;
                            for (int i = 0; i < Nwin; ++i) {
                                ifpos >> ll >> pp;
                                Minlminp(i, 0) = int(ll - 0.5 * MasksizeL); // correct for lower left corner
                                Minlminp(i, 1) = int(pp - 0.5 * MasksizeP); // correct for lower left corner
                                Minlminp(i, 2) = int(1); // flag from file
                                ifpos.getline(dummyline, ONE27, '\n'); // goto next line.
                            }
                            ifpos.close();

                            // ______ Check last point ivm. EOL after last position in file ______
                            if (Minlminp(Nwin - 1, 0) == Minlminp(Nwin - 2, 0) && Minlminp(
                                    Nwin - 1, 1) == Minlminp(Nwin - 2, 1)) {
                                Minlminp(Nwin - 1, 0) = int(.5 * (lN + l0) + 27); // random
                                Minlminp(Nwin - 1, 1) = int(.5 * (pN + p0) + 37); // random
                            }

                            // ______ Check if points are in overlap ______
                            // ______ no check for uniqueness of points ______
                            boolean troubleoverlap = false;
                            for (int i = 0; i < Nwin; ++i) {
                                if (Minlminp(i, 0) < l0) {
                                    troubleoverlap = true;
                                    logger.warn("COARSECORR: point from file: " << i + 1 << " "
                                            << Minlminp(i, 0) + .5 * MasksizeL << " " << Minlminp(
                                            i, 1) + .5 * MasksizeP
                                            << " outside overlap master, slave. New position: ";
                                    Minlminp(i, 0) = l0 + l0 - Minlminp(i, 0);
                                    logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                                    WARNING.print();
                                }
                                if (Minlminp(i, 0) > lN) {
                                    troubleoverlap = true;
                                    logger.warn("COARSECORR: point from file: " << i + 1 << " "
                                            << Minlminp(i, 0) + .5 * MasksizeL << " " << Minlminp(
                                            i, 1) + .5 * MasksizeP
                                            << " outside overlap master, slave. New position: ";
                                    Minlminp(i, 0) = lN + lN - Minlminp(i, 0);
                                    logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                                    WARNING.print();
                                }
                                if (Minlminp(i, 1) < p0) {
                                    troubleoverlap = true;
                                    logger.warn("COARSECORR: point from file: " << i + 1 << " "
                                            << Minlminp(i, 0) + .5 * MasksizeL << " " << Minlminp(
                                            i, 1) + .5 * MasksizeP
                                            << " outside overlap master, slave. New position: ";
                                    Minlminp(i, 1) = p0 + p0 - Minlminp(i, 1);
                                    logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                                    WARNING.print();
                                }
                                if (Minlminp(i, 1) > pN) {
                                    troubleoverlap = true;
                                    logger.warn("COARSECORR: point from file: " << i + 1 << " "
                                            << Minlminp(i, 0) + 0.5 * MasksizeL << " " << Minlminp(
                                            i, 1) + 0.5 * MasksizeP
                                            << " outside overlap master, slave. New position: ";
                                    Minlminp(i, 1) = pN + pN - Minlminp(i, 1);
                                    logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                                    WARNING.print();
                                }
                            }
                            if (troubleoverlap) // give some additional info
                            {
                                WARNING
                                        << "COARSECORR: point in input file outside overlap (l0,lN,p0,pN): "
                                        << l0 << " " << lN << " " << p0 << " " << pN;
                                WARNING.print();
                            }
            */
        }

        // ______Compute coherence of these points______
        ComplexDoubleMatrix Master;
        ComplexDoubleMatrix Mask;
        DoubleMatrix Result = new DoubleMatrix(Nwin, 3); // R(i,0):delta l; R(i,1):delta p; R(i,2):correl

        for (int i = 0; i < Nwin; ++i) {

            // ______Minlminp (lower left corners) of Window in master system______
            final int minMwinL = Minlminp[i][0];
            final int minMwinP = Minlminp[i][1];
            logger.debug("Window: " + i + " [" + minMwinL + ", " + minMwinP + "]");

            Window master = new Window(minMwinL, minMwinL + MasksizeL - 1, minMwinP, minMwinP + MasksizeP - 1);// size=masksize

            // ______Same points in slave system (disk)______
            Window mask = new Window(minMwinL + initoffsetL, minMwinL + initoffsetL + MasksizeL - 1, minMwinP + initoffsetP, minMwinP + initoffsetP + MasksizeP - 1);

            // ______Read windows from files______
            Master = readdata(master);
            Mask = readdata(mask);

            // ______ Coherence/max correlation ______
            float offsetL = 0;
            float offsetP = 0;
            //final  real4 coheren = corrfft(absMaster,absMask,offsetL,offsetP);
            //final  real4 coheren = coherencefft(Master, Mask,
            //  1, MasksizeL/2, MasksizeP/2, //do not ovs, search full matrix for max
            //  offsetL,offsetP);// returned

            //do not ovs, search full matrix for max
            final double coheren = crosscorrelate(Master, Mask, 1, MasksizeL / 2, MasksizeP / 2, offsetL, offsetP);// returned
            logger.debug("Offset between chips (l,p)    = " + offsetL + ", " + offsetP);

            // ______ Store result of this patch ______
            Result.put(i, 0, coheren);
            Result.put(i, 1, initoffsetL + offsetL);// total estimated offset
            Result.put(i, 2, initoffsetP + offsetP);// total estimated offset
            logger.debug("Offset between images on disk = " + Result.get(i, 1) + ", " + Result.get(i, 2) + " (corr=" + coheren + ")");
        } // for nwin

        // ______ Position approx. with respect to center of Window ______
        // ______ correct position array for center instead of lower left ______
        for (int i = 0; i < Nwin; i++) {
            Minlminp[i][0] += (int) (0.5 * MasksizeL);
            Minlminp[i][1] += (int) (0.5 * MasksizeP);
        }

        // ______ Get good general estimate for offsetL, offsetP ______
        int offsetLines = -999;
        int offsetPixels = -999;
        getoffset(Result, offsetLines, offsetPixels);

        logger.debug("*******************************************************************"
                + "\n* COARSE_COREGISTRATION: Correlation"
                + "\n*******************************************************************");
        logger.debug("Number of correlation windows: \t" + Nwin
                + "\nCorrelation Window size (l,p): \t" + MasksizeL + ", " + MasksizeP);

        logger.debug("\nNumber \tposl \tposp \toffsetl offsetp \tcorrelation\n");
        for (int k = 0; k < Nwin; k++) {

            if (Result.get(k, 0) == -999)
                NwinNANrm = NwinNANrm - 1;

            logger.debug(k + "\t" + Minlminp[k][0] + "\t" + Minlminp[k][1]
                    + "\t" + Result.get(k, 1) + "\t" + Result.get(k, 2) + "\t"
                    + Result.get(k, 0));
        }

        logger.debug("Estimated total offset (l,p): \t" + offsetLines + ", " + offsetPixels);
        logger.debug("*******************************************************************");

        logger.info("\n*******************************************************************");
        logger.info("\n*_Start_process: " + "Coarse Coregistration");
        logger.info("\n*******************************************************************");
        logger.info("\nEstimated translation slave w.r.t. master:");
        logger.info("\nCoarse_correlation_translation_lines: \t" + offsetLines);
        logger.info("\nCoarse_correlation_translation_pixels: \t" + offsetPixels);
        logger.info("\nNumber of correlation windows: \t\t" + NwinNANrm + " of " + Nwin);
        logger.info("\n\n#     center(l,p)   coherence   offsetL   offsetP\n");
        for (int k = 0; k < Nwin; k++) {
            if (Result.get(k, 0) == -999)
                continue;
            logger.info(k + " \t" + Minlminp[k][0] + " \t" + Minlminp[k][1]
                    + " \t" + Result.get(k, 0) + " \t" + Result.get(k, 1) + " \t"
                    + Result.get(k, 2) + "\n");
        }

        logger.info("\n*******************************************************************");
        logger.info("\n*_End_process: " + "Coarse Coregistration");
        logger.info("\n*******************************************************************");

    } // END coarsecorrelfft

    @Override
    public void mtiming_correl(Input.MasterTiming input, SLCImage minfo, todo_classes.productinfo sinfo) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mtiming_correlfft(Input.MasterTiming input, SLCImage minfo, todo_classes.productinfo sinfo) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int[][] distributepoints(float numberofpoints, Window win) {
        return new int[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getoffset(DoubleMatrix Result, long offsetLines, long offsetPixels) {

        logger.trace("getoffset (PM 01-Mar-2012)");
        if (Result.columns != 3) {
            logger.error("code 901: input not 3 width");
        }

        // First sort estimated offsets on coherence ascending!
        logger.debug("sorting on coherence.");

        // sort matrix on first column (coh)
        double[][] coherenceArray = Result.toArray2();
        Arrays.sort(coherenceArray, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                final Double value1 = o1[0];
                final Double value2 = o2[0];
                return value1.compareTo(value2);
            }
        });

        // TODO: check this
        DoubleMatrix sortResult = new DoubleMatrix(coherenceArray);

        // --- Set offset to highest coherence estimate ---
        offsetLines = (int) (Math.round(sortResult.get(0, 1)));//rounds negative too
        offsetPixels = (int) (Math.round(sortResult.get(0, 2)));//rounds negative too
        final int nW = sortResult.rows;
        int nWNANrm = sortResult.rows;
        if (nW == 1)
            return;

        // --- Threshold on coherence ---
        float var_coh = 0.0f;
        double mean_coh = 0.0f;
        for (int i = 0; i < nW; i++) { //MA fix to ignore -999 values from statistics
            if (sortResult.get(i, 0) == -999) {
                nWNANrm = nWNANrm - 1;
                continue;
            }
            mean_coh += sortResult.get(i, 0);
        }
        //mean_coh /= real4(nW);
        mean_coh /= (float) nWNANrm;
        for (int i = 0; i < nW; i++) { //MA fix to ignore -999 values from statistics
            if (sortResult.get(i, 0) == -999)
                continue;
            var_coh += Math.pow(sortResult.get(i, 0), 2 - mean_coh);
        }
        //var_coh /= real4(nW-1);
        var_coh /= (float) (nWNANrm - 1);
        logger.info("Mean coherence at estimated positions: " + mean_coh);

        final double std_coh = Math.sqrt(var_coh);
        logger.info("Standard deviation coherence:          " + std_coh);

        final double thresh_coh = mean_coh;
        logger.info("Using as threshold:                    " + thresh_coh);

        int cnt = 1;                        // estimates above threshold
        mean_coh = sortResult.get(0, 0);        // mean above threshold
        logger.info("Using following data to determine coarse image offset:");
        logger.info("coherence    offset_L    offset_P");
        logger.info("------------------------------------------------------");
        logger.info(sortResult.get(0, 0) + "      " + sortResult.get(0, 1) + "        " + sortResult.get(0, 2));

        for (int i = 1; i < nW; i++) {
            if (sortResult.get(i, 0) >= thresh_coh) {
                cnt++;
                mean_coh += sortResult.get(i, 0);
                offsetLines += (int) (Math.round(sortResult.get(i, 1)));// round
                offsetPixels += (int) (Math.round(sortResult.get(i, 2)));// round
                logger.info(sortResult.get(i, 0) + "      " + sortResult.get(i, 1) + "        " + sortResult.get(i, 2));
            }
        }

        // ___ Report stats ___
        if (cnt > 1) {
            mean_coh /= cnt;
            final double meanL = (double) offsetLines / (double) cnt;
            final double meanP = (double) offsetPixels / (double) cnt;
            offsetLines = Math.round((double) offsetLines / (double) cnt);
            offsetPixels = Math.round((double) offsetPixels / (double) cnt);
            double var_L = 0.0d;
            double var_P = 0.0d;
            for (int i = 0; i < cnt; i++)
                var_L += Math.pow(sortResult.get(i, 1) - meanL, 2);
            for (int i = 0; i < cnt; i++)
                var_P += Math.pow(sortResult.get(i, 2) - meanP, 2);
            var_L /= (double) (cnt - 1);
            var_P /= (double) (cnt - 1);
            logger.info("Standard deviation offset L = " + Math.pow(var_L, 2));
            logger.info("Standard deviation offset P = " + Math.pow(var_P, 2));
            if (Math.sqrt(var_L) > 6.0 || Math.sqrt(var_P) > 6.0)
                logger.warn("Check estimated offset coarse corr: it seems unreliable.");

        }

        // ___ Warn if appropriate ___
        if (mean_coh < 0.2) {
            logger.warn("getoffset: mean coherence of estimates used < 0.2");
            logger.warn("(please check bottom of LOGFILE to see if offset is OK)");
        }
        if (nW < 6) {
            logger.info("getoffset: number of windows to estimate offset < 6");
            logger.info("(please check bottom of LOGFILE to see if offset is OK)");
        }

    } // END getoffset


    @Override
    public void getmodeoffset(float[][] Result, long offsetLines, long offsetPixels) {
    }

    @Override
    public void finecoreg(Input.FineCorr fineinput, SLCImage minfo, SLCImage sinfo) {
        logger.trace("finecoreg (PM 18-Oct-2011)");

        //final  int Mfilelines   = minfo.getCurrentWindow().lines();
        //final  int Sfilelines   = sinfo.getCurrentWindow().lines();
        final int Nwin = fineinput.Nwin; // n windows, from file or random
        int NwinNANrm = fineinput.Nwin; // [MA] number of windows w/o NaN
        final long initoffsetL = fineinput.initoffsetL; // initial offset
        final long initoffsetP = fineinput.initoffsetP; // initial offset
        int MasksizeL = fineinput.MasksizeL; // size of correlation Window
        int MasksizeP = fineinput.MasksizeP; // size of correlation Window
        int AccL = fineinput.AccL; // size of small chip
        int AccP = fineinput.AccP; // size of small chip
        final int OVS = fineinput.osfactor; // factor
        boolean pointsrandom = true;
        if (fineinput.ifpositions != null) {
            pointsrandom = false; // only use these points
        } // filename specified

        // ______Correct sizes if in space domain______
        if (fineinput.method.equals("fc_magspace") || fineinput.method.equals("fc_cmplxspace")) {
            logger.info("Adapting size of Window for space method");
            MasksizeL += 2 * fineinput.AccL;
            MasksizeP += 2 * fineinput.AccP;
        }

        // ______Corners of slave in master system______
        // ______offset = [A](slave system) - [A](master system)______
        final long sl0 = sinfo.getCurrentWindow().linelo - initoffsetL;
        final long slN = sinfo.getCurrentWindow().linehi - initoffsetL;
        final long sp0 = sinfo.getCurrentWindow().pixlo - initoffsetP;
        final long spN = sinfo.getCurrentWindow().pixhi - initoffsetP;

        // ______Corners of useful overlap master,slave in master system______
        final int BORDER = 20;// make slightly smaller
        final long l0 = max(minfo.getCurrentWindow().linelo, sl0) + BORDER;
        final long lN = Math.min(minfo.getCurrentWindow().linehi, slN) - MasksizeL - BORDER;
        final long p0 = max(minfo.getCurrentWindow().pixlo, sp0) + BORDER;
        final long pN = Math.min(minfo.getCurrentWindow().pixhi, spN) - MasksizeP - BORDER;
        final Window overlap = new Window(l0, lN, p0, pN);

        // ______ Distribute Nwin points over Window, or read from file ______
        // ______ Minlminp(i,0): line, (i,1): pixel, (i,2) flagfromdisk ______
        int[][] Minlminp = new int[Nwin][2];
        if (pointsrandom) // no filename specified
        {
            Minlminp = distributepoints(Nwin, overlap);
        }
        /*
                else // read in points (center of windows) from file
                {
                    Minlminp.resize(Nwin, 3);
                    ifstream ifpos(fineinput.ifpositions, ios::in);
                    bk_assert(ifpos, fineinput.ifpositions, __FILE__, __LINE__);
                    int ll, pp;
                    for (int i = 0; i < Nwin; ++i) {
                        ifpos >> ll >> pp;
                        Minlminp(i, 0) = int(ll - 0.5 * MasksizeL); // correct for lower left corner
                        Minlminp(i, 1) = int(pp - 0.5 * MasksizeP); // correct for lower left corner
                        Minlminp(i, 2) = int(1); // flag from file
                        ifpos.getline(dummyline, ONE27, '\n'); // goto next line.
                    }
                    ifpos.close();
                    // ______ Check last point for possible EOL after last position in file ______
                    if (Minlminp(Nwin - 1, 0) == Minlminp(Nwin - 2, 0) && Minlminp(
                            Nwin - 1, 1) == Minlminp(Nwin - 2, 1)) {
                        Minlminp(Nwin - 1, 0) = int(0.5 * (lN + l0) + 27); // random
                        Minlminp(Nwin - 1, 1) = int(0.5 * (pN + p0) + 37); // random
                    }
                    // ______ Check if points are in overlap ______
                    // ______ no check for uniqueness of points ______
                    boolean troubleoverlap = false;
                    for (int i = 0; i < Nwin; ++i) {
                        if (Minlminp(i, 0) < l0) {
                            troubleoverlap = true;
                            logger.warn("FINE: point from file: " << i + 1 << " "
                                    << Minlminp(i, 0) + 0.5 * MasksizeL << " " << Minlminp(
                                    i, 1) + 0.5 * MasksizeP
                                    << " outside overlap master, slave. New position: ";
                            Minlminp(i, 0) = l0 + l0 - Minlminp(i, 0);
                            logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                            WARNING.print();
                        }
                        if (Minlminp(i, 0) > lN) {
                            troubleoverlap = true;
                            logger.warn("FINE: point from file: " << i + 1 << " "
                                    << Minlminp(i, 0) + 0.5 * MasksizeL << " " << Minlminp(
                                    i, 1) + 0.5 * MasksizeP
                                    << " outside overlap master, slave. New position: ";
                            Minlminp(i, 0) = lN + lN - Minlminp(i, 0);
                            logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                            WARNING.print();
                        }
                        if (Minlminp(i, 1) < p0) {
                            troubleoverlap = true;
                            logger.warn("FINE: point from file: " << i + 1 << " "
                                    << Minlminp(i, 0) + 0.5 * MasksizeL << " " << Minlminp(
                                    i, 1) + 0.5 * MasksizeP
                                    << " outside overlap master, slave. New position: ";
                            Minlminp(i, 1) = p0 + p0 - Minlminp(i, 1);
                            logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                            WARNING.print();
                        }
                        if (Minlminp(i, 1) > pN) {
                            troubleoverlap = true;
                            logger.warn("FINE: point from file: " << i + 1 << " "
                                    << Minlminp(i, 0) + 0.5 * MasksizeL << " " << Minlminp(
                                    i, 1) + 0.5 * MasksizeP
                                    << " outside overlap master, slave. New position: ";
                            Minlminp(i, 1) = pN + pN - Minlminp(i, 1);
                            logger.warn(Minlminp(i, 0) << " " << Minlminp(i, 1);
                            WARNING.print();
                        }
                    }
                    if (troubleoverlap) // give some additional info
                    {
                        WARNING
                                << "FINE: there were points from file outside overlap (l0,lN,p0,pN): "
                                << l0 << " " << lN << " " << p0 << " " << pN;
                        WARNING.print();
                    }
                }
        */

        // ______Compute coherence of these points______
        ComplexDoubleMatrix Master;
        ComplexDoubleMatrix Mask;
        DoubleMatrix Result = new DoubleMatrix(Nwin, 3); // R(i,0):delta l; R(i,1):delta p; R(i,2):correl

        // ====== Compute for all locations ======
        for (int i = 0; i < Nwin; i++) {

            // ______Minlminp (lower left corners) of Window in master system______
            final int minMwinL = Minlminp[i][0];
            final int minMwinP = Minlminp[i][1];
            logger.debug("Window: " + i + " [" + minMwinL + ", " + minMwinP + "]");
            Window master = new Window(minMwinL, minMwinL + MasksizeL - 1, minMwinP, minMwinP + MasksizeP - 1);// size=masksize
            // ______Same points in slave system (disk)______
            Window mask = new Window(minMwinL + initoffsetL, minMwinL + initoffsetL + MasksizeL - 1, minMwinP + initoffsetP, minMwinP + initoffsetP + MasksizeP - 1);// size=masksize

            // TODO: get the data!!!
            // ______Read windows from files______
            Master = readdata(master);
            Mask = readdata(mask);

            // ______Coherence______
            // ______update offsetL/P______
            double offsetL = 0;
            double offsetP = 0;
            double coheren;

            if (fineinput.method.equals("fc_magfft")) {
                if (AccL > MasksizeL / 2) {
                    AccL = MasksizeL / 2;
                    logger.warn("FINE: AccL for magfft can be half of the Window size at max, changing to " + AccL);
                } else if (AccP > MasksizeP / 2) {
                    AccP = MasksizeP / 2;
                    logger.warn("FINE: AccP for magfft can be half of the Window size at max, changing to " + AccP);
                }
                coheren = crosscorrelate(Master, Mask, OVS, AccL, AccP, offsetL, offsetP);// returned
                break;
            } else if (fineinput.method.equals("fc_oversample")) {
                if (AccL > MasksizeL / 2) {
                    AccL = MasksizeL / 2;
                    logger.warn("FINE: AccL for magfft can be half of the Window size at max, changing to " + AccL);
                } else if (AccP > MasksizeP / 2) {
                    AccP = MasksizeP / 2;
                    logger.warn("FINE: AccP for magfft can be half of the Window size at max, changing to " + AccP);
                }

                // Oversample complex chips by factor two
                // neg.shift input shifts to -> 0
                logger.debug("Centering azimuth spectrum patches around 0");
                final double m_pixlo = (double) (master.pixlo);// neg.shift -> 0
                final double s_pixlo = (double) (mask.pixlo);// neg.shift -> 0
                double mPrf = minfo.getPRF();
                double sPrf = sinfo.getPRF();
                double mRsr2x = minfo.getRsr2x();
                double sRsr2x = sinfo.getRsr2x();

                double[] mFdc = new double[3];
                mFdc[0] = minfo.doppler.getF_DC_a0();
                mFdc[1] = minfo.doppler.getF_DC_a1();
                mFdc[2] = minfo.doppler.getF_DC_a2();

                double[] sFdc = new double[3];
                sFdc[0] = sinfo.doppler.getF_DC_a0();
                sFdc[1] = sinfo.doppler.getF_DC_a1();
                sFdc[2] = sinfo.doppler.getF_DC_a2();

                shiftazispectrum(Master, mPrf, mRsr2x, mFdc, -m_pixlo);// shift from fDC to zero
                shiftazispectrum(Mask, sPrf, sRsr2x, sFdc, -s_pixlo);// shift from fDC to zero
                logger.info("Oversampling patches with factor two using zero padding");
                final ComplexDoubleMatrix m_ovs_chip = SarUtils.oversample(Master, 2, 2);
                final ComplexDoubleMatrix s_ovs_chip = SarUtils.oversample(Mask, 2, 2);
                // ______ Peak in cross-corr of magnitude of ovs data ______
                logger.debug("Cross-correlating magnitude of ovs patches");
                logger.debug("(no need to shift spectrum back)");// (else account for ovs..)
                //coheren = coherencefft(m_ovs_chip, s_ovs_chip,
                //                       OVS/2, 2*AccL, 2*AccP,
                //                       offsetL,offsetP);
                coheren = crosscorrelate(m_ovs_chip, s_ovs_chip, OVS / 2, 2 * AccL, 2 * AccP, offsetL, offsetP);
                offsetL /= 2.0;// orig data oversampled by factor 2
                offsetP /= 2.0;// orig data oversampled by factor 2
                break;


            } else if (fineinput.method.equals("fc_magspace")) {

//                coheren = coherencespace(fineinput, Master, Mask, offsetL, offsetP);
                int accL = 0;
                int accP = 0;
                int osFactor = 1;
                coheren = coherencespace(accL, accP, osFactor, Master, Mask, offsetL, offsetP);


            } else {
                logger.error("unknown method for fine coregistration.");
                throw new IllegalArgumentException("unknown method for fine coregistration.");
            }

            Result.put(i, 0, initoffsetL + offsetL);
            Result.put(i, 1, initoffsetP + offsetP);
            Result.put(i, 2, coheren);

            logger.info("Fine offset between small patches:   " + Result.get(i, 0) + ", "
                    + Result.get(i, 1) + " (coh=" + coheren + ")");


        } // for nwin

        // ______ Position approx. with respect to center of Window ______
        // ______ correct position array for center instead of lower left ______
        for (int i = 0; i < Nwin; i++) {
            Minlminp[i][0] += (int) (0.5 * MasksizeL);
            Minlminp[i][1] += (int) (0.5 * MasksizeP);
        }

        logger.info("\n\n*******************************************************************");
        logger.info("\n* FINE_COREGISTRATION");
        logger.info("\n*******************************************************************");
        logger.info("\nNumber of correlation windows: \t" + Nwin);
        logger.info("\nWindow size (l,p):             \t" + MasksizeL + ", " + MasksizeP);
        logger.info("\nInitial offsets:               \t" + initoffsetL + ", " + initoffsetP);
        logger.info("\nOversampling factor:           \t" + OVS);
        logger.info("\n\nNumber \tposl \tposp \toffsetl offsetp\tcorrelation\n");
        for (int i = 0; i < Nwin; i++) {
            if (Result.get(i, 2) == Double.NaN) {
                logger.warn("NaN value!!!");
            }
            logger.info(i + " " + Minlminp[i][0] + " " + Minlminp[i][1] + " " + Result.get(i, 0) + " " + Result.get(i, 1) + " " + Result.get(i, 2));
        }
        logger.info("\n*******************************************************************\n");
        logger.info("\n* End_FINE_COREGISTRATION_NORMAL");
        logger.info("\n*******************************************************************\n");
    } // END finecoreg


    /**
     * *************************************************************
     * coherencefft                                                 *
     * *
     * coherence in spectral domain by fft's based on magnitude     *
     * uses extension with zeros.  returns relative shift between  *
     * two patched and the estimated correlation.                  *
     * *
     * input:                                                       *
     * - Master                                                    *
     * - Mask (size Master)                                        *
     * output:                                                      *
     * - coherence value [-1 1]                                    *
     * - updated offsetL, P                                        *
     * positive offsetL: Mask is shifted up                      *
     * positive offsetP: Mask is shifted left                    *
     * *
     * 1) should find max at pixel level, then oversample sub-pixel *
     * but it seems to be implemented strangely                     *
     * 2) oversampling should be performed on complex images with   *
     * factor 2, so to avoid aliasing of spectrum (shift azi).      *
     * **************************************************************
     */
    @Override
    public double coherencefft(ComplexDoubleMatrix Master, ComplexDoubleMatrix Mask, int ovsfactor, int AccL,
                               int AccP, double offsetL, double offsetP) {

        logger.trace("coherencefft (PM 02-Mar-2012)");
        // ______ Internal variables ______
        final int L = Master.rows;
        final int P = Master.columns;
        final int twoL = 2 * L;
        final int twoP = 2 * P;
        final int halfL = L / 2;
        final int halfP = P / 2;

        // Check input
        if (!MathUtils.isPower2(ovsfactor)) {
            logger.error("coherencefft factor not power of 2");
            throw new IllegalArgumentException("coherencefft factor not power of 2");
        }
        if (Master.rows != Mask.rows || Master.columns != Mask.columns) {
            logger.error("Mask, Master not same size.");
            throw new IllegalArgumentException("Mask, Master not same size.");
        }
        if (!(MathUtils.isPower2(L) || MathUtils.isPower2(P))) {
            logger.error("Mask, Master size not power of 2.");
            throw new IllegalArgumentException("Mask, Master size not power of 2.");
        }

        // Zero mean magnitude images ______
        logger.debug("Using de-meaned magnitude patches for incoherent cross-correlation");
        DoubleMatrix magMaster = SarUtils.magnitude(Master);
        DoubleMatrix magMask = SarUtils.magnitude(Mask);
        magMaster.subi(magMaster.mean());
        magMask.subi(magMask.mean());

        // FFT's of master/mask
        // Pad with N zeros to prevent periodical convolution
        ComplexDoubleMatrix Master2 = new ComplexDoubleMatrix(twoL, twoP);
        ComplexDoubleMatrix Mask2 = new ComplexDoubleMatrix(twoL, twoP);

        Window windef = new Window(0, 0, 0, 0); // defaults to total matrix
        Window win1 = new Window(0, L - 1, 0, P - 1);
        Window win2 = new Window(halfL, halfL + L - 1, halfP, halfP + P - 1);

        LinearAlgebraUtils.setdata(Master2, win1, new ComplexDoubleMatrix(magMaster, null), windef);
        LinearAlgebraUtils.setdata(Mask2, win2, new ComplexDoubleMatrix(magMask, null), windef);

        // Crossproducts in spectral/space domain
        // Use Mask2 to store cross products temporarly
        SpectralUtils.fft2D_inplace(Master2);
        SpectralUtils.fft2D_inplace(Mask2);

        Master2.conji();
        Mask2.mul(Master2); // corr = conj(M).*S
        SpectralUtils.invfft2D_inplace(Mask2); // cross prod. in space

        // keep cross-products for shifts [-AccL,+AccL)
        Window wintmp = new Window(halfL - AccL, halfL + AccL - 1, halfP - AccP, halfP + AccP - 1);

        //        ComplexDoubleMatrix TMP(wintmp, Mask2);
        // TODO: DODGY, possible bug over here!
        ComplexDoubleMatrix TMP = new ComplexDoubleMatrix((int) wintmp.lines(), (int) wintmp.pixels());
        LinearAlgebraUtils.setdata(TMP, Mask2, wintmp);

        //        matrix<real4> Covar = real(TMP); // imag==0
        DoubleMatrix Covar = TMP.getReal();

        // Compute norms, zero padded matrices
        ComplexDoubleMatrix blok = ComplexDoubleMatrix.ones(L, P);

        Master2 = ComplexDoubleMatrix.zeros(Master.rows, Master.columns);   // reset to zeros
        Mask2 = ComplexDoubleMatrix.zeros(Mask2.rows, Mask2.columns);       // reset to zeros

        LinearAlgebraUtils.setdata(Master, win1, new ComplexDoubleMatrix(magMaster, null), windef); // use Master2 for intensity
        LinearAlgebraUtils.setdata(Mask2, win2, blok, windef);

        SpectralUtils.fft2D_inplace(Master2); // (intensity of master)
        SpectralUtils.fft2D_inplace(Mask2); // (block)

        Mask2.conji();      // conj(block)
        Master2.mul(Mask2); // pointwise!!!
        Master2.conji();    // Master2 == conj(Master)*block

        SpectralUtils.invfft2D_inplace(Master2);

        // Master2 now contains norms of master image in space domain
        // Resize to shifts [-AccL,+AccL)
        LinearAlgebraUtils.setdata(TMP, Master2, wintmp); // fill TMP
        DoubleMatrix pmaster = TMP.getReal(); // norms in pmaster

        // Now compute norms for slave image
        Master2 = ComplexDoubleMatrix.zeros(Master2.rows, Master2.columns); // reset to zeros
        Window win5 = new Window(L, twoL - 1, P, twoP - 1);

        //        Master2.setdata(win5, mat2cr4(sqr(magMask)), windef);
        LinearAlgebraUtils.setdata(Master2, win5, new ComplexDoubleMatrix(MatrixFunctions.pow(magMask, 2)), windef);

        SpectralUtils.fft2D_inplace(Master2); // (intensity of slave)

        //        Master2*=Mask2; // Master2 == conj(block)*Slave
        Master2.mul(Mask2); // Master2 == conj(block)*Slave

        SpectralUtils.invfft2D_inplace(Master2);

        // Master2 now contains norms of slave image in space domain
        // Resize to shifts [-AccL,+AccL)
        LinearAlgebraUtils.setdata(TMP, Master2, wintmp); // fill TMP
        final DoubleMatrix pmask = TMP.getReal(); // norms in pmask
        pmaster.mul(pmask);
        Covar.div(MatrixFunctions.sqrt(pmaster));

        // Estimate shift by oversampling estimated correlation
        int offL;
        int offP;
        double maxcorr;

        // final double maxcorr = (ovsfactor == 1) ? max(Covar, offL, offP) : max(oversample(Covar, ovsfactor, ovsfactor), offL, offP);
        if (ovsfactor == 1) {
            int corrIndex = Covar.argmax();
            offL = Covar.indexRows(corrIndex);
            offP = Covar.indexColumns(corrIndex);
            maxcorr = Covar.get(corrIndex);
        } else {
            DoubleMatrix CovarOversampled = SarUtils.oversample(new ComplexDoubleMatrix(Covar, null), ovsfactor, ovsfactor).getReal();
            int corrIndex = CovarOversampled.argmax();
            offL = CovarOversampled.indexRows(corrIndex);
            offP = CovarOversampled.indexColumns(corrIndex);
            maxcorr = CovarOversampled.get(corrIndex);
        }

        offsetL = -(double) AccL + (double) offL / (double) (ovsfactor); // update by reference
        offsetP = -(double) AccP + (double) offP / (double) (ovsfactor); // update by reference
        return maxcorr;

    } // END coherencefft

    @Override
    public double crosscorrelate(ComplexDoubleMatrix Master, ComplexDoubleMatrix Mask,
                                 int ovsfactor,
                                 int AccL, int AccP,
                                 double offsetL, double offsetP) {

        logger.trace("crosscorrelate (PM 15-Apr-2012)");

        // ______ Internal variables ______
        final int L = Master.rows;
        final int P = Master.columns;
        final int twoL = 2 * L;
        final int twoP = 2 * P;
        final int halfL = L / 2;
        final int halfP = P / 2;

        // ______ Check input ______
        if (Master.rows != Mask.rows || Master.columns != Mask.columns) {
            logger.error("Mask, Master not same size.");
            throw new IllegalArgumentException("Mask, Master not same size.");
        }

        if (!(MathUtils.isPower2(L) || MathUtils.isPower2(P))) {
            logger.error("Mask, Master size not power of 2.");
            throw new IllegalArgumentException("Mask, Master size not power of 2.");
        }

        if (!MathUtils.isPower2(ovsfactor)) {
            logger.error("coherencefft factor not power of 2");
            throw new IllegalArgumentException("coherencefft factor not power of 2");
        }

        // ______ Zero mean magnitude images ______
        logger.debug("Using de-meaned magnitude patches for incoherent cross-correlation");
        DoubleMatrix magMaster = SarUtils.magnitude(Master);
        DoubleMatrix magMask = SarUtils.magnitude(Mask);
        magMaster.subi(magMaster.mean());
        magMask.subi(magMask.mean());

        // ====== (1) Compute cross-products of Master/Mask ======
        // ______ Pad with N zeros to prevent periodical convolution ______
        ComplexDoubleMatrix Master2 = ComplexDoubleMatrix.zeros(twoL, twoP); // initial 0
        ComplexDoubleMatrix Mask2 = ComplexDoubleMatrix.zeros(twoL, twoP); // initial 0

        Window windef = new Window(); // defaults to total matrix
        Window win1 = new Window(0, L - 1, 0, P - 1);
        Window win2 = new Window(halfL, halfL + L - 1, halfP, halfP + P - 1);

        // TODO: something is going wrong with declarations of data here
        LinearAlgebraUtils.setdata(Master2, win1, new ComplexDoubleMatrix(magMaster), windef); // zero-mean magnitude
        LinearAlgebraUtils.setdata(Mask2, win2, new ComplexDoubleMatrix(magMask), windef); // zero-mean magnitude

        // ______ Crossproducts in spectral/space domain ______
        // ______ Use Mask2 to store cross products temporarly ______
        SpectralUtils.fft2D_inplace(Master2);
        SpectralUtils.fft2D_inplace(Mask2);

        Master2.conji();
        Mask2.muli(Master2); // corr = conj(M).*S

//        Mask2.mmuli(Master2);

        SpectralUtils.invfft2D_inplace(Mask2); // real(Mask2): cross prod. in space

        // ====== (2) compute norms for all shifts ======
        // ______ use tricks to do this efficient ______
        // ______ real(Mask2) contains cross-products ______
        // ______ Mask2(0,0):Mask2(N,N) for shifts = -N/2:N/2 ______
        // ______ rest of this matrix should not be used ______
        // ______ Use Master2 to store intensity here in re,im ______
        Master2 = ComplexDoubleMatrix.zeros(twoL, twoP); // reset to zeros
        int l, p;
        // --- flipud(fliplr(master^2) in real ---
        // --- mask^2 in imag part; this saves a fft ---
        // --- automatically the real/imag parts contain the norms ---
        for (l = L; l < twoL; ++l) {
            for (p = P; p < twoP; ++p) {
                double realPart = magMaster.get(twoL - 1 - l, twoP - 1 - p);
                double imagPart = magMask.get(l - L, p - P);
                ComplexDouble value = new ComplexDouble(Math.pow(realPart, 2), Math.pow(imagPart, 2));
                Master2.put(l, p, value);
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

        // _____ Compute the cross-products, i.e., the norms for each shift ---
        // ______ Master2(0,0):Master2(N,N) for shifts = -N/2:N/2 ______
        SpectralUtils.fft2D_inplace(Master2);

        Master2.muli(BLOCK);

        SpectralUtils.invfft2D_inplace(Master2);// real(Master2): powers of Master; imag(Master2): Mask

        // ====== (3) find maximum correlation at pixel level ======
        DoubleMatrix Covar = new DoubleMatrix(L + 1, P + 1);// correlation for each shift

        double maxcorr = -999.0f;
        long maxcorrL = 0;// local index in Covar of maxcorr
        long maxcorrP = 0;// local index in Covar of maxcorr

        ComplexDouble maskValueTemp;
        ComplexDouble master2ValueTemp;

        for (l = 0; l <= L; ++l) { // all shifts
            for (p = 0; p <= P; ++p) {// all shifts
                maskValueTemp = Mask2.get(l, p);
                master2ValueTemp = Master2.get(l, p);

                Covar.put(l, p, maskValueTemp.real() / Math.sqrt(master2ValueTemp.real() * master2ValueTemp.imag()));
                if (Covar.get(l, p) > maxcorr) {
                    maxcorr = Covar.get(l, p);
                    maxcorrL = l;// local index in Covar of maxcorr
                    maxcorrP = p;// local index in Covar of maxcorr
                }
            }
        }

        offsetL = -halfL + maxcorrL; // update by reference
        offsetP = -halfP + maxcorrP; // update by reference
        logger.debug("Pixel level offset:     " + offsetL + ", " + offsetP + " (corr=" + maxcorr + ")");

        // ====== (4) oversample to find peak sub-pixel ======
        // ====== Estimate shift by oversampling estimated correlation ======
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
            maxcorr = chipOversampled.get(corrIndex);

            offsetL = -halfL + maxcorrL - AccL + (double) offL / (double) ovsfactor;
            offsetP = -halfP + maxcorrP - AccP + (double) offP / (double) ovsfactor;

            System.out.println("Oversampling factor: " + ovsfactor);
            System.out.println("Sub-pixel level offset: " + offsetL + ", " + offsetP + " (corr=" + maxcorr + ")");

            logger.debug("Sub-pixel level offset: " + offsetL + ", " + offsetP + " (corr=" + maxcorr + ")");
        }
        return maxcorr;
    }

    @Override
    public double coherencespace(final int AccL, final int AccP, final int osfactor,
                                 ComplexDoubleMatrix Master, ComplexDoubleMatrix Mask,
                                 double offsetL, double offsetP) {

        logger.trace("coherencespace (PM 14-Feb-2012)");

        // Internal variables
        final int L = Master.rows;
        final int P = Master.columns;
//        final int AccL = fineinput.AccL;
//        final int AccP = fineinput.AccP;
        final int factor = osfactor;

        // Select parts of Master/slave
        final int MasksizeL = L - 2 * AccL;
        final int MasksizeP = P - 2 * AccP;

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

        DoubleMatrix magMask = SarUtils.magnitude(Mask); // magnitude
        magMask.subi(magMask.mean()); // subtract mean
        DoubleMatrix Mask2 = new DoubleMatrix((int) winmask.lines(), (int) winmask.pixels());
        LinearAlgebraUtils.setdata(Mask2, magMask, winmask); // construct as part
        double normmask = Math.pow(Mask2.norm2(), 2);
        DoubleMatrix Master2 = new DoubleMatrix(MasksizeL, MasksizeP);
        DoubleMatrix magMaster = SarUtils.magnitude(Master);
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
        final double maxcor = coher8.get(coher8MaxIndex);
        offsetL = AccL - offL / (double) (factor); // update by reference - this does not work in JAVA
        offsetP = AccP - offP / (double) (factor); // update by reference - this does not work in JAVA

        System.out.println("Oversampling factor: " + factor);
        System.out.println("Sub-pixel level offset: " + offsetL + ", " + offsetP + " (corr=" + maxcor + ")");
        return maxcor;
    }

    private DoubleMatrix oversample(final DoubleMatrix data, final int factor, final int factor1) {
        return SarUtils.oversample(new ComplexDoubleMatrix(data), factor, factor).getReal();
    }

    @Override
    public void coregpm(SLCImage master, SLCImage slave, String i_resfile, Input.CoregPM coregpminput,
                        int demassist) {

        // see javaschool.estimation.TestEstimation_EJML for implementation stub
        // reporting is still to be refactored from C++ code

    }

    private DoubleMatrix matxmatT(DoubleMatrix a, DoubleMatrix b) {
        return a.mmul(b.transpose());
    }

    @Override
    public DoubleMatrix getofffile(String file, float threshold) {
        return new DoubleMatrix();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void resample(Input.Resample resampleinput,
                         SLCImage master, SLCImage slave,
                         double[] cpmL, double[] cpmP,
                         int demassist) {

        logger.trace("resample (BK 16-Mar-1999; BK 09-Nov-2000)");
        if (resampleinput.shiftAziSpectra == true)
            logger.debug("shifting kernelL to data fDC BK 26-Oct-2002");

        // ___ Handle input ___
//     	    const uint BUFFERMEMSIZE = generalinput.memory; // Bytes  500MB --> 500 000 000 bytes
        int Npoints = 16; //resampleinput.method % 100; // #pnts interpolator
        if (MathUtils.isOdd(Npoints)) {
            logger.error("resample only even point interpolators.");
            throw new IllegalArgumentException("resmple only even point interpolators");
        }
        final int Npointsd2 = Npoints / 2;
        final int Npointsd2m1 = Npointsd2 - 1;
        //const uint  Sfilelines   = slave.currentwindow.lines();
//     	    final uint sizeofci16 = sizeof(compli16);
//     	    final uint sizeofcr4 = sizeof(complr4);

        // normalize data for polynomial
//            final double minL = master. originalwindow.linelo;
        final double minL = master.getCurrentWindow().linelo;
        final double maxL = master.getCurrentWindow().linehi;
        final double minP = master.getCurrentWindow().pixlo;
        final double maxP = master.getCurrentWindow().pixhi;
        logger.info("resample: polynomial normalized by factors: " + minL + " " + maxL + " " + minP + " " + maxP + " to [-2,2]");

        /** Create lookup table */
        // ........ e.g. four point interpolator
        // ........ interpolating point: p=6.4925
        // ........ required points: 5, 6, 7, 8
        // ........ kernel number from lookup table: floor(.4925*interval+.5)
        // ........  table[0]= 0 1 0 0 ;table[interval]= 0 0 1 0
        // ........ intervals in lookup table: dx
        // ........ for high doppler 100 is OK (fdc=3prf; 6pi --> 10deg error?)
        final int INTERVAL = 127; // precision: 1./interval [pixel]
        final int Ninterval = INTERVAL + 1; // size of lookup table
        final double dx = 1.0d / INTERVAL; // interval look up table
        logger.info("resample: lookup table size: " + Ninterval);

        /** Notes:
         *  ...Lookup table complex because of multiplication with complex
         *  ...Loopkup table for azimuth and range and
         *  ...shift spectrum of azi kernel with doppler centroid
         *  ...kernel in azimuth should be sampled higher
         *  ...and may be different from range due to different oversampling ratio and spectral shift (const)
         */
        LUT lut = new LUT(LUT.RECT, Npoints);
        lut.constructLUT();
        lut.overviewLUT();

        final ComplexDoubleMatrix pntKernelAz = new ComplexDoubleMatrix(lut.getKernel());
        final ComplexDoubleMatrix pntKernelRg = new ComplexDoubleMatrix(lut.getKernel());
        final DoubleMatrix pntAxis = lut.getAxis().dup();

        // Save some time by computing degree here
        final int degree_cpmL = PolyUtils.degreeFromCoefficients(cpmL.length);
        final int degree_cpmP = PolyUtils.degreeFromCoefficients(cpmP.length);

        // ______Corners of overlap in master system______
        Window overlap = getOverlap(master, slave, cpmL, cpmP);

        // TODO: Test adjusting of overlap RS_DBOW card implemented
        /*
            int write0lines1 = 0; // DBOW card, 0's at start
            int write0linesN = 0;
            int write0pixels1 = 0;
            int write0pixelsN = 0;
            if (!(resampleinput.dbow.linelo == 0 && // as such initialized by readinput
                    resampleinput.dbow.linehi == 0 && resampleinput.dbow.pixlo == 0
                    && resampleinput.dbow.pixhi == 0)) {
                // ______ Check if overlap is large enough to contain DBOW ______
                if (resampleinput.dbow.linelo > overlap.linehi) {
                    logger.error("RS_DBOW: specified min. line larger than max. line of overlap.");
                }

                if (resampleinput.dbow.linehi < overlap.linelo) {
                    logger.error("RS_DBOW: specified max. line smaller than min. line of overlap.");
                }
                if (resampleinput.dbow.pixlo > overlap.pixhi) {
                    logger.error("RS_DBOW: specified min. pixel larger than max. pixel of overlap.");
                }
                if (resampleinput.dbow.pixhi < overlap.pixlo) {
                    logger.error("RS_DBOW: specified max. pixel smaller than min. pixel of overlap.")
                }

                write0lines1 = (int) (overlap.linelo - resampleinput.dbow.linelo);

                if (write0lines1 < 0)
                    write0lines1 = 0; // smaller window selected
                write0linesN = (int) (-overlap.linehi + resampleinput.dbow.linehi);

                if (write0linesN < 0)
                    write0linesN = 0; // smaller window selected
                write0pixels1 = (int) (overlap.pixlo - resampleinput.dbow.pixlo);

                if (write0pixels1 < 0)
                    write0pixels1 = 0; // smaller window selected
                write0pixelsN = (int) (-overlap.pixhi + resampleinput.dbow.pixhi);

                if (write0pixelsN < 0)
                    write0pixelsN = 0; // smaller window selected

                if (resampleinput.dbow.linelo < overlap.linelo) {
                    logger.warn("RS_DBOW: min. line < overlap (writing: " + write0lines1 + " lines with zeros before first resampled line).");
                } else
                    overlap.linelo = resampleinput.dbow.linelo; // correct it
                if (resampleinput.dbow.linehi > overlap.linehi) {
                    logger.warn("RS_DBOW: max. line > overlap (writing: " + write0linesN + " lines with zeros after last resampled line).");
                } else
                    overlap.linehi = resampleinput.dbow.linehi; // correct it

                if (resampleinput.dbow.pixlo < overlap.pixlo) {
                    logger.warn("RS_DBOW: min. pixel < overlap (writing: " + write0pixels1 + " columns with zeros before first resampled column).");
                } else
                    overlap.pixlo = resampleinput.dbow.pixlo; // correct it

                if (resampleinput.dbow.pixhi > overlap.pixhi) {
                    logger.warn("RS_DBOW: max. pixel > overlap (writing: " + write0pixelsN + " columns with zeros after last resampled column).");
                } else
                    overlap.pixhi = resampleinput.dbow.pixhi; // correct it

            } // adjust overlap
        */

        /** Buffersize output matrix */
        // these parameters <---- should be dependent on the size of buffer, i am pulling the whole tile
        final int nPixels = (int) slave.getCurrentWindow().pixels(); // this is actually tile size
        final int bufferLines = (int) (slave.getCurrentWindow().lines()); // buffer bufferLines


        /** Declare/allocate matrices */
        ComplexDoubleMatrix BUFFER = null; // load after output is written --> THIS IS TILE!

        // RESULT ---> not the same size as master, depends which tile is being resampled!
        // ComplexDoubleMatrix RESULT = new ComplexDoubleMatrix(bufferLines, (int) (overlap.pixhi - overlap.pixlo + 1));
        ComplexDoubleMatrix RESULT = ComplexDoubleMatrix.zeros(bufferLines, (int) (overlap.pixhi - overlap.pixlo + 1)); // set to ZERO
        ComplexDoubleMatrix PART = new ComplexDoubleMatrix(Npoints, Npoints);

        logger.info("Overlap window: " + overlap.linelo + ":" + overlap.linehi + ", " + overlap.pixlo + ":" + overlap.pixhi);

        /** Resample all lines that are requested */
        boolean newbufferrequired = true; // read initial slave buffer
        int linecnt = -1; // indicate output buffer full
        int firstBufferLine = 0; // slave system
        int lastBufferLine = 0;

        for (int line = (int) overlap.linelo; line <= overlap.linehi; line++) {

            double normPixLo = normalize2(overlap.pixlo, minP, maxP);
            double normPixHi = normalize2(overlap.pixhi, minP, maxP);

            firstBufferLine = (int) ((ceil(min(line + polyval(normalize2(line, minL, maxL), normPixLo, cpmL),
                    line + polyval(normalize2(line, minL, maxL), normPixHi, cpmL))))
                    - Npoints);

            int line2 = line + bufferLines - 1;

            lastBufferLine = (int) ((ceil(min(line2 + polyval(normalize2((line2), minL, maxL),
                    normPixLo, cpmL), line2 + polyval(normalize2(line2, minL, maxL), normPixHi, cpmL))))
                    + Npoints);

            //const int FORSURE = 5;                // buffer larger 2*FORSURE start/end
            int FORSURE = 25; // buffer larger 2*FORSURE start/end
            firstBufferLine -= FORSURE;
            lastBufferLine += FORSURE;

            // Don't compare apples with pears, uint<->int!
            if (firstBufferLine < (int) (slave.getCurrentWindow().linelo))
                firstBufferLine = (int) slave.getCurrentWindow().linelo;

            if (lastBufferLine > (int) (slave.getCurrentWindow().linehi))
                lastBufferLine = (int) slave.getCurrentWindow().linehi;
            // ______ Fill slave BUFFER from disk ______

            Window winSlaveFile = new Window(firstBufferLine, lastBufferLine, // part of slave loaded
                    slave.getCurrentWindow().pixlo, // from file in BUFFER.
                    slave.getCurrentWindow().pixhi);

            logger.debug("Reading slave: [" + winSlaveFile.linelo + ":"
                    + winSlaveFile.linehi + ", " + winSlaveFile.pixlo + ":"
                    + winSlaveFile.pixhi + "]");

            /** Evaluate coregistration polynomial */
            double interpL = 0;
            double interpP = 0;
            for (int pixel = (int) overlap.pixlo; pixel <= (int) (overlap.pixhi); pixel++) {

                // interpL = line  + polyval(line,pixel,cpmL,degree_cpmL); // e.g. 255.35432
                // interpP = pixel + polyval(line,pixel,cpmP,degree_cpmP); // e.g. 2.5232
                interpL = line + polyval(normalize2(line, minL, maxL),
                        normalize2(pixel, minP, maxP), cpmL);

                interpP = pixel + polyval(normalize2(line, minL, maxL),
                        normalize2(pixel, minP, maxP), cpmP);


                /** Get correct lines for interpolation */
                int fl_interpL = (int) (interpL);
                int fl_interpP = (int) (interpP);
                int firstL = fl_interpL - Npointsd2m1; // e.g. 254 (5 6 7)
                int firstP = fl_interpP - Npointsd2m1; // e.g. 1   (2 3 4)
                double interpLdec = interpL - fl_interpL; // e.g. .35432
                double interpPdec = interpP - fl_interpP; // e.g. .5232

                // ______ Copy kernels here, change kernelL if required _ // BK 26-Oct-2002
                // ______ Faster to have two kernel lookup tables ! _____
                // ______ I have that now, but still make copy (slow) ______
                int kernelnoL = (int) (interpLdec * INTERVAL + 0.5); // lookup table index
                int kernelnoP = (int) (interpPdec * INTERVAL + 0.5); // lookup table index

                // TODO: this will crash for sure!!!
                ComplexDoubleMatrix kernelL = new ComplexDoubleMatrix(kernelnoL);
                ComplexDoubleMatrix kernelP = new ComplexDoubleMatrix(kernelnoP);
//        const matrix<complr4> kernelP = ( * pntKernelRg[kernelnoP]);// local copy

                // ______ Shift azimuth kernel with fDC before interpolation ______
                if (resampleinput.shiftAziSpectra == true) {
                    // ___ Doppler centroid is function of range only ____
                    double tmp = 2.0 * Constants.PI * slave.doppler.pix2fdc(interpP) / slave.getPRF();
                    // ___ to shift spectrum of convolution kernel to fDC of data, multiply
                    // ___ in the space domain with a phase trend of -2pi*t*fdc/prf
                    // ___ (to shift back (no need) you would use +fdc), see manual;
                    for (int i = 0; i < Npoints; ++i) {
                        // Modify kernel, shift spectrum to fDC

                        double t = pntAxis.get(i) * tmp;
                        //kernelL(i,0)  *= complr4(cos(t),-sin(t));// note '-' (see manual)
                        kernelL.put(i, 0, new ComplexDouble(Math.cos(t), Math.sin(t)));// note '-' (see manual)
                    }
                }

                // TODO: this has to be fixed
/*
                // For speed: define setdata internally (memcpy)
                int firstLine;
                for (int i = 0; i < Npoints; i++){
                    PART.put(BUFFER.get(i + firstL-firstLine))
                    memcpy(PART[i], BUFFER[i + firstL - firstline] + firstP
                            - slave.currentwindow.pixlo, Npointsxsize);

                }
*/


                // TODO: this was quick fix perhaps it will not work
                RESULT.put(linecnt, (int) (pixel - overlap.pixlo), LinearAlgebraUtils.matTxmat(PART.mul(kernelP), kernelL).get(0, 0));

            }


        }


//
//                    // ______ For speed: define setdata internally (memcpy) ______
//                    for (i = 0; i < Npoints; i++)
//                        memcpy(PART[i], BUFFER[i + firstL - firstBufferLine] + firstP
//                                - slave.currentwindow.pixlo, Npointsxsize);
//
//                    // ====== Some speed considerations ======
//         #ifdef __USE_VECLIB_LIBRARY__
//                    // ______Compute PART * kernelP______
//                    cgemv("T",&Np,&Np,&c4alpha, PART[0],&Np,
//                            kernelP[0],&ONEint,
//                            &c4beta,TMPRES[0],&ONEint,1);
//                    // ______Compute Result * kernelL; put in matrix RESULT______
//                    ANS = cdotu(&Np,TMPRES[0],&ONEint, kernelL[0],&ONEint);
//                    RESULT(linecnt,pixel-overlap.pixlo) = complr4(ANS.re,ANS.im);
//         #else // do not use VECLIB
//                    // ______ NO VECLIB: slower, but works ______
//                    RESULT(linecnt, pixel - overlap.pixlo) = ((matTxmat(PART * kernelP,
//                            kernelL))(0, 0));
//         #endif // VECLIB y/n
//                } // for all pixels in overlap
//            } // for all lines in overlap
//
//
//            // ______ Write last lines of Result to disk (filled upto linecnt) ______
//            DEBUG + "Writing slave: [" + overlap.linehi - linecnt + ":"
//                    + overlap.linehi + ", " + overlap.pixlo + ":" + overlap.pixhi
//                    + "] (master coord. system)";
//            DEBUG.print();
//
//            // ______ Actually write ______
//            switch (resampleinput.oformatflag) {
//            case FORMATCR4: {
//                const complr4 zerocr4(0, 0);
//                for (int32 thisline = 0; thisline <= linecnt; thisline++) {
//                    // ______ Write zero pixels at start ______
//                    for (int32 thispixel = 0; thispixel < write0pixels1; ++thispixel) {
//                        ofile.write((char*) &zerocr4, sizeofcr4);
//                    }
//                    // ______ WRITE the interpolated data per row ______
//                    ofile.write((char*) &RESULT[thisline][0], RESULT.pixels()
//                            * sizeofcr4);
//                    // ______ Write zero pixels at end ______
//                    for (int32 thispixel = 0; thispixel < write0pixelsN; ++thispixel) {
//                        ofile.write((char*) &zerocr4, sizeofcr4);
//                    }
//                }
//                break;
//            }
//            case FORMATCI2: {
//                const compli16 zeroci16(0, 0);
//                compli16 castedresult;
//                for (int32 thisline = 0; thisline <= linecnt; thisline++) {
//                    // ______ Write zero pixels at start ______
//                    for (int32 thispixel = 0; thispixel < write0pixels1; ++thispixel) {
//                        ofile.write((char*) &zeroci16, sizeofci16);
//                    }
//                    // ______ Write the interpolated data per row ______
//                    for (int32 thispixel = 0; thispixel < int32(RESULT.pixels()); thispixel++) {
//                        castedresult = cr4toci2(RESULT(thisline, thispixel));
//                        ofile.write((char*) &castedresult, sizeofci16);
//                    }
//                    // ______ Write zero pixels at end ______
//                    for (int32 thispixel = 0; thispixel < write0pixelsN; ++thispixel) {
//                        ofile.write((char*) &zeroci16, sizeofci16);
//                    }
//                }
//                break;
//            }
//            default:
//                PRINT_ERROR("impossible format")
//                throw(unhandled_case_error);
//            }
//
//            // ====== Write last zero lines if appropriate (DBOW card) ======
//            switch (resampleinput.oformatflag) {
//            case FORMATCR4: {
//                complr4 zerocr4(0, 0);
//                for (int32 thisline = 0; thisline < write0linesN; ++thisline)
//                    for (int32 thispixel = 0; thispixel < int32(RESULT.pixels())
//                            + write0pixels1 + write0pixelsN; ++thispixel)
//                        ofile.write((char*) &zerocr4, sizeofcr4);
//                break;
//            }
//            case FORMATCI2: {
//                compli16 zeroci16(0, 0);
//                for (int32 thisline = 0; thisline < write0linesN; ++thisline)
//                    for (int32 thispixel = 0; thispixel < int32(RESULT.pixels())
//                            + write0pixels1 + write0pixelsN; ++thispixel)
//                        ofile.write((char*) &zeroci16, sizeofci16);
//                break;
//            }
//            default:
//                PRINT_ERROR("impossible format")
//                throw(unhandled_case_error);
//            }
//            ofile.close();
//
//            // ====== Write results to slave resfile ======
//            char rsmethod[EIGHTY];
//            switch (resampleinput.method) {
//            case rs_rect:
//                strcpy(rsmethod, "nearest neighbour");
//                break;
//            case rs_tri:
//                strcpy(rsmethod, "piecewise linear");
//                break;
//            case rs_cc4p:
//                strcpy(rsmethod, "4 point cubic convolution");
//                break;
//            case rs_cc6p:
//                strcpy(rsmethod, "6 point cubic convolution");
//                break;
//            case rs_ts6p:
//                strcpy(rsmethod, "6 point truncated sinc");
//                break;
//            case rs_ts8p:
//                strcpy(rsmethod, "8 point truncated sinc");
//                break;
//            case rs_ts16p:
//                strcpy(rsmethod, "16 point truncated sinc");
//                break;
//            case rs_knab4p:
//                strcpy(rsmethod, "4 point knab kernel");
//                break;
//            case rs_knab6p:
//                strcpy(rsmethod, "6 point knab kernel");
//                break;
//            case rs_knab8p:
//                strcpy(rsmethod, "8 point knab kernel");
//                break;
//            case rs_knab10p:
//                strcpy(rsmethod, "10 point knab kernel");
//                break;
//            case rs_knab16p:
//                strcpy(rsmethod, "16 point knab kernel");
//                break;
//            case rs_rc6p:
//                strcpy(rsmethod, "6 point raised cosine kernel");
//                break;
//            case rs_rc12p:
//                strcpy(rsmethod, "12 point raised cosine kernel");
//                break;
//            default:
//                PRINT_ERROR("impossible.")
//                throw(unhandled_case_error);
//            }
//
//            char rsoformat[EIGHTY];
//            switch (resampleinput.oformatflag) {
//            case FORMATCR4:
//                strcpy(rsoformat, "complex_real4");
//                break;
//            case FORMATCI2:
//                strcpy(rsoformat, "complex_short");
//                break;
//            default:
//                PRINT_ERROR("impossible.")
//                throw(unhandled_case_error);
//            }
//
//            // --- Write result file ---
//            ofstream scratchlogfile("scratchlogresample", ios::out | ios::trunc);
//            bk_assert(scratchlogfile, "resample: scratchlogresample", __FILE__,
//                    __LINE__);
//            scratchlogfile
//                    + "\n\n*******************************************************************"
//                    + "\n* RESAMPLE:"
//                    + "\n*******************************************************************"
//                    + "\nData_output_file: \t\t\t" + resampleinput.fileout
//                    + "\nData_output_format: \t\t\t" + rsoformat
//                    + "\nInterpolation kernel: \t\t\t" + rsmethod
//                    + "\nResampled slave size in master system: \t" + overlap.linelo
//                    - write0lines1 + ", " + overlap.linehi + write0linesN + ", "
//                    + overlap.pixlo - write0pixels1 + ", " + overlap.pixhi
//                    + write0pixelsN
//                    + "\n*******************************************************************\n";
//            scratchlogfile.close();
//
//            ofstream scratchresfile("scratchresresample", ios::out | ios::trunc);
//            bk_assert(scratchresfile, "resample: scratchresresample", __FILE__,
//                    __LINE__);
//            scratchresfile
//                    + "\n\n*******************************************************************"
//                    + "\n*_Start_" + processcontrol[pr_s_resample]
//                    + "\n*******************************************************************"
//                    + "\nShifted azimuth spectrum:             \t\t"
//                    + resampleinput.shiftazi
//                    + "\nData_output_file:                     \t\t"
//                    + resampleinput.fileout
//                    + "\nData_output_format:                   \t\t" + rsoformat
//                    + "\nInterpolation kernel:                 \t\t" + rsmethod
//                    + "\nFirst_line (w.r.t. original_master):  \t\t" + overlap.linelo
//                    - write0lines1 + "\nLast_line (w.r.t. original_master):   \t\t"
//                    + overlap.linehi + write0linesN
//                    + "\nFirst_pixel (w.r.t. original_master): \t\t" + overlap.pixlo
//                    - write0pixels1 + "\nLast_pixel (w.r.t. original_master):  \t\t"
//                    + overlap.pixhi + write0pixelsN
//                    + "\n*******************************************************************"
//                    + "\n* End_" + processcontrol[pr_s_resample] + "_NORMAL"
//                    + "\n*******************************************************************\n";
//            scratchresfile.close();
//
//            // ______Tidy up______
//            DEBUG.print("deleting new matrix, memory errors could be caused by this");
//            for (i = 0; i < nInterval; i++)// like this ???
//            {
//                delete pntKernelAz[i];
//                delete pntKernelRg[i];
//                delete pntAxis[i];
//                //    delete [] pntKernelAz[i];
//            }
//            DEBUG.print("Exiting resample.");

    }

    @Override
    public void ms_timing_error(SLCImage master, String i_resfile, Input.RelTiming timinginput,
                                int coarse_orbit_offsetL, int coarse_orbit_offsetP) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public DoubleMatrix correlate(DoubleMatrix A, DoubleMatrix Mask) {

        double varM = 0.; // variance of Mask
        Mask.subi(Mask.mean());

        for (int ii = 0; ii < Mask.length; ii++) {
            varM += Math.pow(Mask.get(ii), 2); // 1/N later
        }

        // ______Compute correlation at these points______
        int beginl = (Mask.rows - 1) / 2; // floor
        int beginp = (Mask.columns - 1) / 2; // floor

        DoubleMatrix Result = DoubleMatrix.zeros(A.rows, A.columns); // init to 0
        DoubleMatrix Am = new DoubleMatrix(Mask.rows, Mask.columns);

// ______First Window of A, updated at end of loop______
        Window winA = new Window(0, Mask.rows - 1, 0, Mask.columns - 1);
//        Window windef = new Window(0, Am.rows - 1, 0, Am.columns - 1);// defaults to total Am
        Window windef = new Window();// defaults to total Am

// ______Correlate part of Result______
        for (int i = beginl; i < A.rows - beginl; i++) {
            for (int j = beginp; j < A.columns - beginp; j++) {

                // Am.setdata(windef, A, winA); // Am no allocs.
                LinearAlgebraUtils.setdata(Am, windef, A, winA);

                Am.subi(Am.mean()); // center around mean
                double covAM = 0.; // covariance A,Mask
                double varA = 0.; // variance of A(part)

/*
                Type * pntM = Mask[0];
                Type * pntAm = Am[0];
                for (int l = 0; l < Mask.length; l++) {
                    covAM += (( * pntM++) * ( * pntAm)); // wait for move pnt
                    varA += sqr( * pntAm++); // pnt ++
                }
*/

                for (int l = 0; l < Mask.length; l++) {
                    covAM += (Mask.get(l) * Am.get(l));
                    varA += Math.pow(Am.get(l), 2);
                }

                Result.put(i, j, covAM / Math.sqrt(varM * varA)); // [BO]
                winA.pixlo++;
                winA.pixhi++;
            }
            winA.linelo++;
            winA.linehi++;
            winA.pixlo = 0;
            winA.pixhi = winA.pixlo + Mask.columns - 1;
        }
        return Result;

    }


    /**
     * shiftazispectrum                                          *
     * Shift spectrum of input matrix data either from fDC to zero, *
     * or from zero to fDC (Doppler centroid frequency).            *
     * Slcimage gives required polynomial fDC(column).              *
     * Abs(shift) gives the first range column for this polynomial. *
     * First column is 1, so use this, not 0!                      *
     * (shift<0) indicates to shift towards zero, and               *
     * (shift>0) indicates to shift spectrum to fDC (back)          *
     * Spectrum is shifted by multiplication by a trend e^{iphase}  *
     * in the space domain.                                         *
     * *
     * If fDC is more or less equal for all columns, then an        *
     * approximation is used. If fDC is smaller then 2 percent of   *
     * PRF then no shifting is performed.                           *
     * memory intensive cause matrix is used.                       *
     */
    public void shiftazispectrum(final ComplexDoubleMatrix data, // slcData in space domain
                                 final double prf,
                                 final double rsr2x,
                                 final double[] doppler,   // polynomial fDC
                                 final double shift)       // abs(shift) == firstpix in slave system)
    {

        // Evaluate fdc for all columns
        if (doppler[0] == 0 && doppler[1] == 0 && doppler[2] == 0) // no shift at all
            return;

        // Some constants
        final int NLINES = data.rows; // compute e^ix
        final int NCOLS = data.columns; // width
        final int SIGN = (shift < 0) ? -1 : 1; // e^{SIGN*i*x}
// -1
        final double PIXLO = Math.abs(shift) - 1; // first column for fDC polynomial

        // Compute fDC for all columns
        // Create axis to evaluate fDC polynomial
        // fDC(column) = fdc_a0 + fDC_a1*(col/RSR) + fDC_a2*(col/RSR)^2
        // offset slave,master defined as: cols=colm+offsetP
        DoubleMatrix FDC = new DoubleMatrix(1, NCOLS);
        FDC.fill(doppler[0]); // constant term
        if (doppler[1] != 0 || doppler[2] != 0) // check to save time
        {
            // doppler progression over range axis
            DoubleMatrix xaxis = DoubleMatrix.linspace((int) PIXLO, (int) (PIXLO + NCOLS - 1), NCOLS);
            xaxis.divi(rsr2x / 2);
            FDC.addi(xaxis.mul(doppler[1])); // linear term
            FDC.addi(MatrixFunctions.pow(xaxis, 2).mul(doppler[2])); // cubic term
        }

        logger.debug("fDC of first pixel: " + FDC.get(0, 0));
        if (SIGN == 1)
            logger.debug("Shifting from zero to fDC.");
        else
            logger.debug("Shifting from fDC to zero.");

        // Actually shift the azimuth spectrum
        // TODO: check indexing for yAxis vector : this doesn't look righ, however, it is consistent with DORIS.core
        // .... spectra of the first range line should be also shifted!
//        DoubleMatrix trend = DoubleMatrix.linspace(1, NLINES, NLINES).transpose();
        DoubleMatrix trend = DoubleMatrix.linspace(0, NLINES - 1, NLINES);
        trend.muli(2 * Constants.PI / prf);

        trend.assertMultipliesWith(FDC); // check on orientation
        DoubleMatrix P = trend.mmul(FDC);

//        for (int ii = 0; ii < NLINES; ++ii) {
//            trend.put(ii, 0, (2 * ii) * Constants.PI / doppler.getPRF());
//        }
//
//        matrix<real8> P = trend * FDC;

        ComplexDoubleMatrix cplxTrend = (SIGN == -1) ?
                new ComplexDoubleMatrix(MatrixFunctions.cos(P), MatrixFunctions.sin(P).neg()) :
                new ComplexDoubleMatrix(MatrixFunctions.cos(P), MatrixFunctions.sin(P));

        data.muli(cplxTrend);

    } // END shiftazispectrum

    /**
     * getoverlap : overlap of 2 windows in same coord. system
     */
    @Deprecated
    public static Window getOverlap(final Window master, final Window slave) {
        Window overlap = new Window(slave);
        if (master.linelo > overlap.linelo)
            overlap.linelo = master.linelo;
        if (master.linehi < overlap.linehi)
            overlap.linehi = master.linehi;
        if (master.pixlo > overlap.pixlo)
            overlap.pixlo = master.pixlo;
        if (master.pixhi < overlap.pixhi)
            overlap.pixhi = master.pixhi;
        return overlap;
    } // getoverlap


    /**
     * getoverlap : compute approx. rectangular overlap (master coord.)
     * between master/slave with help of transformation polynomial
     */
    public static Window getOverlap(final SLCImage master, final SLCImage slave, final double[] cpmL, final double[] cpmP) {

        // ______ Normalize data for polynomial ______
        double minL = master.getOriginalWindow().linelo;
        double maxL = master.getOriginalWindow().linehi;
        double minP = master.getOriginalWindow().pixlo;
        double maxP = master.getOriginalWindow().pixhi;

        logger.info("getoverlap: polynomial normalized by factors: " + minL + " " + maxL + " " + minP + " " + maxP + " to [-2,2]");

        // offset = A(slave system) - A(master system)
        // ....corners of slave in master system
        // ....offsets for slave corners (approx.)
        // ....approx: defined as offset = f(l,p)_M in master system not slave.
        double approxOffL = cpmL[0]; // zero order term;
        double approxOffP = cpmP[0]; // zero order term;

        final double sL00 = slave.getCurrentWindow().linelo -
                polyval(normalize2((double) slave.getCurrentWindow().linelo - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixlo - approxOffP, minP, maxP), cpmL);

        final double sP00 = slave.getCurrentWindow().pixlo -
                polyval(normalize2((double) slave.getCurrentWindow().linelo - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixlo - approxOffP, minP, maxP), cpmP);

        final double sL0N = slave.getCurrentWindow().linelo -
                polyval(normalize2((double) slave.getCurrentWindow().linelo - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixhi - approxOffP, minP, maxP), cpmL);

        final double sP0N = slave.getCurrentWindow().pixhi -
                polyval(normalize2((double) slave.getCurrentWindow().linelo - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixhi - approxOffP, minP, maxP), cpmP);

        final double sLN0 = slave.getCurrentWindow().linehi -
                polyval(normalize2((double) slave.getCurrentWindow().linehi - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixlo - approxOffP, minP, maxP), cpmL);

        final double sPN0 = slave.getCurrentWindow().pixlo -
                polyval(normalize2((double) slave.getCurrentWindow().linehi - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixlo - approxOffP, minP, maxP), cpmP);

        final double sLNN = slave.getCurrentWindow().linehi -
                polyval(normalize2((double) slave.getCurrentWindow().linehi - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixhi - approxOffP, minP, maxP), cpmL);

        final double sPNN = slave.getCurrentWindow().pixhi -
                polyval(normalize2((double) slave.getCurrentWindow().linehi - approxOffL, minL, maxL),
                        normalize2((double) slave.getCurrentWindow().pixhi - approxOffP, minP, maxP), cpmP);


        // Corners of overlap master,slave in master system
        Window win = new Window();
        win.linelo = max((int) (master.getCurrentWindow().linelo), (int) (Math.ceil(max(sL00, sL0N))));
        win.linehi = min((int) (master.getCurrentWindow().linehi), (int) (Math.ceil(max(sLN0, sLNN))));
        win.pixlo = max((int) (master.getCurrentWindow().pixlo), (int) (Math.ceil(max(sP00, sPN0))));
        win.pixhi = min((int) (master.getCurrentWindow().pixhi), (int) (Math.ceil(max(sP0N, sPNN))));
        return win;
    } // END getoverlap


    /**
     * getoverlap: compute rectangular overlap between master and slave
     * (master coordinate system)
     */
    public static Window getOverlap(final SLCImage master, final SLCImage slave,
                                    final double Npointsd2, final double timing_L, final double timing_P) {

        double ml0 = master.getCurrentWindow().linelo;
        double mlN = master.getCurrentWindow().linehi;
        double mp0 = master.getCurrentWindow().pixlo;
        double mpN = master.getCurrentWindow().pixhi;

        double sl00 = slave.getCurrentWindow().linelo + slave.getSlaveMaterOffset().l00 + Npointsd2 - timing_L;
        double sp00 = slave.getCurrentWindow().pixlo + slave.getSlaveMaterOffset().p00 + Npointsd2 - timing_P;
        double sl0N = slave.getCurrentWindow().linelo + slave.getSlaveMaterOffset().l0N + Npointsd2 - timing_L;
        double sp0N = slave.getCurrentWindow().pixhi + slave.getSlaveMaterOffset().p0N - Npointsd2 - timing_P;
        double slN0 = slave.getCurrentWindow().linehi + slave.getSlaveMaterOffset().lN0 - Npointsd2 - timing_L;
        double spN0 = slave.getCurrentWindow().pixlo + slave.getSlaveMaterOffset().pN0 + Npointsd2 - timing_P;
        double slNN = slave.getCurrentWindow().linehi + slave.getSlaveMaterOffset().lNN - Npointsd2 - timing_L;
        double spNN = slave.getCurrentWindow().pixhi + slave.getSlaveMaterOffset().pNN - Npointsd2 - timing_P;

        double[] mh1sv1 = lineIntersect(ml0, mp0, ml0, mpN, sl00, sp00, slN0, spN0);
        double[] mh1sv2 = lineIntersect(ml0, mp0, ml0, mpN, sl0N, sp0N, slNN, spNN);
        double[] mh2sv1 = lineIntersect(mlN, mp0, mlN, mpN, sl00, sp00, slN0, spN0);
        double[] mh2sv2 = lineIntersect(mlN, mp0, mlN, mpN, sl0N, sp0N, slNN, spNN);
        double[] mv1sh1 = lineIntersect(ml0, mp0, mlN, mp0, sl00, sp00, sl0N, sp0N);
        double[] mv1sh2 = lineIntersect(ml0, mp0, mlN, mp0, slN0, spN0, slNN, spNN);
        double[] mv2sh1 = lineIntersect(ml0, mpN, mlN, mpN, sl00, sp00, sl0N, sp0N);
        double[] mv2sh2 = lineIntersect(ml0, mpN, mlN, mpN, slN0, spN0, slNN, spNN);

        double overlap_l0 = max(max(max(max(max(max(ml0, sl00), sl0N), mh1sv1[0]), mh1sv2[0]), mv1sh1[0]), mv2sh1[0]);
        double overlap_p0 = max(max(max(max(max(max(mp0, sp00), spN0), mh1sv1[1]), mh2sv1[1]), mv1sh1[1]), mv1sh2[1]);
        double overlap_lN = min(min(min(min(min(min(mlN, slN0), slNN), mh2sv1[0]), mh2sv2[0]), mv1sh2[0]), mv2sh2[0]);
        double overlap_pN = min(min(min(min(min(min(mpN, sp0N), spNN), mh1sv2[1]), mh2sv2[1]), mv2sh1[1]), mv2sh2[1]);

        // ______Corners of overlap master,slave in master system______
        Window overlap = new Window();
        overlap.linelo = (long) Math.ceil(overlap_l0);
        overlap.linehi = (long) Math.floor(overlap_lN);
        overlap.pixlo = (long) Math.ceil(overlap_p0);
        overlap.pixhi = (long) Math.floor(overlap_pN);

        return overlap;
    } // END getoverlap


    /**
     * lineintersect : compute intersection point of two line segments
     * (master coordinate system)
     */
    private static double[] lineIntersect(final double ax, final double ay, final double bx,
                                          final double by, final double cx, final double cy, final double dx,
                                          final double dy) {

        double[] exy = new double[2]; // x,y coordinate

        double u1 = bx - ax;
        double u2 = by - ay;
        double v1 = dx - cx;
        double v2 = dy - cy;
        double w1 = ax - cx;
        double w2 = ay - cy;

        double s = (v2 * w1 - v1 * w2) / (v1 * u2 - v2 * u1);
        exy[0] = ax + s * u1;
        exy[1] = ay + s * u2;

        return exy;

    } // END lineintersect


}




