package org.jlinda.core.coregistration.legacy;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Baseline;
import org.jlinda.core.Ellipsoid;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;

import java.io.File;

public interface ICoregistration {

    // ______ Compute coarse ICoregistration ______
    void coarseporbit(final Ellipsoid ell, final SLCImage master, final SLCImage slave,
                      final Orbit masterorbit, final Orbit slaveorbit,
                      final Baseline baseline) throws Exception;

    // ______ Coarse ICoregistration ______
    void coarsecorrel(CoarseCorr input, final SLCImage minfo, final SLCImage sinfo);

    // ______ Corr by fft ______
    void coarsecorrelfft(CoarseCorr input, final SLCImage minfo, final SLCImage sinfo);

    // ______ Sim. Amplitude ICoregistration (magspace) ______
    void mtiming_correl(MasterTiming input, final SLCImage minfo, productinfo sinfo);

    // ______ Sim. Amplitude ICoregistration (magfft) ______
    void mtiming_correlfft(MasterTiming input, final SLCImage minfo, productinfo sinfo);

    // ______ Distribute nW windows over win ______
    int[][] distributepoints(float numberofpoints, final Window win);

    // ______ Estimate offset based on consistency ______
    void getoffset(DoubleMatrix Result, long offsetLines, long offsetPixels);

    // ______ Estimate offset based on consistency ______
    void getmodeoffset(float[][] Result, long offsetLines, long offsetPixels);

    // ______ Fine ICoregistration ______
    void finecoreg(FineCorr fineinput, final SLCImage minfo, final SLCImage sinfo);

    // ______ Correlation with FFT ______
    double coherencefft(
            final ComplexDoubleMatrix Master, final ComplexDoubleMatrix Mask,
            final int ovsfactor, // ovs factor (1 for not)
            final int AccL, // search window to oversample
            final int AccP, // search window to oversample
            double offsetL,
            double offsetP);

    // ______ Correlation with FFT ______
    double crosscorrelate(
            final ComplexDoubleMatrix Master, final ComplexDoubleMatrix Mask,
            final int ovsfactor,
            final int AccL, // search window to oversample
            final int AccP, // search window to oversample
            double offsetL, double offsetP);

    // ______ Correlation in space domain ______
//    double coherencespace(Input.FineCorr fineinput,
//                          final ComplexDoubleMatrix Master, final ComplexDoubleMatrix Mask,
//                          double offsetL, double offsetP);

    double coherencespace(final int AccL, final int AccP, final int osfactor,
                          ComplexDoubleMatrix Master, ComplexDoubleMatrix Mask,
                          double offsetL, double offsetP);


    // ______ Compute ICoregistration parameters ______
    //      const window            &originalmaster,
    void coregpm(final SLCImage master,
                 final SLCImage slave,
                 final String i_resfile,
                 final CoregPM coregpminput,
                 final int demassist);


    // ______ Read observations from file ______
    DoubleMatrix getofffile(String file, float threshold);

    // ______ Resample slave ______
    void resample(final Resample resampleinput,
                  final SLCImage master, final SLCImage slave,
                  final double[] cpmL, final double[] cpmP,
                  final int demassist);

    // ______ Compute master-slave timing error ______
    void ms_timing_error(final SLCImage master, final String i_resfile,
                         final RelTiming timinginput, int coarse_orbit_offsetL,
                         int coarse_orbit_offsetP);

//    // ______Interpolation kernals______
//    float[][] cc4(final float[][] x);
//    float[][] cc6(final float[][] x);
//    float[][] ts6(final float[][] x);
//    float[][] ts8(final float[][] x);
//    float[][] ts16(final float[][] x);
//    float[][] rect(final float[][] x);
//    float[][] tri(final float[][] x);
//
//    // ___ knab: oversampling factor of signal CHI, number of points N ___
//    float[][] knab(final float[][] x, final float CHI, final long N);
//
//    // ___ raised cosine: oversampling factor of signal CHI, number of points N ___
//    float[][] rc_kernel(final float[][] x, final float CHI, final long N);

    public class productinfo {

        public File file;
        public int multilookL;
        public int multilookP;

        // file format flat
        public int formatflag;

        public productinfo() {
            formatflag = -1;// undefined
            multilookL = 1;
            multilookP = 1;
        }
    }

    public class Resample {

        // arguments for resampling slave
        public String method;                 // method selector (interpolator)
        public Window dbow_geo;               // cut out of original master.geo
        public Window dbow;                   // cut out of original master.radar
        public boolean shiftAziSpectra;        // [true] shift spectrum to 0
    }

    // Coregistration INPUT
    // ----------------------
    public class CoarseCorr // arguments for correlation
    {
        public String ifpositions; // input file name for positions
        public String method; // method selector
        public int Nwin; // #windows
        public int MasksizeL; // size of correlation window
        public int MasksizeP; // size of correlation window
        public int AccL; // #lines to be searched in 1 direction
        public int AccP; // #pixels to be searched in 1 direction
        public long initoffsetL; // initial offset lines
        public long initoffsetP; // initial offset pixels
    }

    public class FineCorr // arguments for fine coreg.
    {
        public String ifpositions; // input file name for positions
        public String method; // method selector
        public int Nwin; // #windows
        public int MasksizeL; // size of correlation window
        public int MasksizeP; // size of correlation window
        public int AccL; // #lines to be searched in l direction
        public int AccP; // #pixels to be searched in p direction
        public long initoffsetL; // initial offset lines
        public long initoffsetP; // initial offset pixels
        public int osfactor; // oversampling factor
        boolean plotoffsets; // call script
        boolean plotmagbg; // call script
        boolean plotthreshold; // call script
    }

    public class RelTiming // arguments for timing [FvL]
    {
        float threshold; // threshold for correlation
        long maxiter; // max. #pnts to remove (wtests)
        float k_alpha; // critical value for automated outlier removal
    }

    public class CoregPM // arguments for coregpm.
    {
        String idcoregpm;
        public float threshold; // threshold for correlation
        public int degree; // degree of polynomial
        public int weightflag; // 0: all same weight
        // 1: choice1: weigh with correlation ??
        public int maxiter; // max. #pnts to remove (wtests)
        public float k_alpha; // critical value for automated outlier removal
        boolean dumpmodel; // create float files with model
        boolean plot; // plot e_hat etc.
        boolean plotmagbg; // plot magnitude in background
    }

    public class MasterTiming {
        String ifpositions; // input file name for positions
        int method; // method selector, [MA] rm if not nec.
        int Nwin; // #windows
        int MasksizeL; // size of correlation window
        int MasksizeP; // size of correlation window
        int AccL; // #lines to be searched in 1 direction
        int AccP; // #pixels to be searched in 1 direction
        long initoffsetL; // initial offset lines
        long initoffsetP; // initial offset pixels
    }
}
