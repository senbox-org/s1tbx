package org.jlinda.core.coregistration.legacy;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.*;
import org.jlinda.core.todo_classes.Input;
import org.jlinda.core.todo_classes.todo_classes;

public interface ICoregistration {

    // ______ Compute coarse ICoregistration ______
    void coarseporbit(final Ellipsoid ell, final SLCImage master, final SLCImage slave,
                      final Orbit masterorbit, final Orbit slaveorbit,
                      final Baseline baseline) throws Exception;

    // ______ Coarse ICoregistration ______
    void coarsecorrel(Input.CoarseCorr input, final SLCImage minfo, final SLCImage sinfo);

    // ______ Corr by fft ______
    void coarsecorrelfft(Input.CoarseCorr input, final SLCImage minfo, final SLCImage sinfo);

    // ______ Sim. Amplitude ICoregistration (magspace) ______
    void mtiming_correl(Input.MasterTiming input, final SLCImage minfo, todo_classes.productinfo sinfo);

    // ______ Sim. Amplitude ICoregistration (magfft) ______
    void mtiming_correlfft(Input.MasterTiming input, final SLCImage minfo, todo_classes.productinfo sinfo);

    // ______ Distribute nW windows over win ______
    int[][] distributepoints(float numberofpoints, final Window win);

    // ______ Estimate offset based on consistency ______
    void getoffset(DoubleMatrix Result, long offsetLines, long offsetPixels);

    // ______ Estimate offset based on consistency ______
    void getmodeoffset(float[][] Result, long offsetLines, long offsetPixels);

    // ______ Fine ICoregistration ______
    void finecoreg(Input.FineCorr fineinput, final SLCImage minfo, final SLCImage sinfo);

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
                 final Input.CoregPM coregpminput,
                 final int demassist);


    // ______ Read observations from file ______
    DoubleMatrix getofffile(String file, float threshold);

    // ______ Resample slave ______
    void resample(final Input.Resample resampleinput,
                  final SLCImage master, final SLCImage slave,
                  final double[] cpmL, final double[] cpmP,
                  final int demassist);

    // ______ Compute master-slave timing error ______
    void ms_timing_error(final SLCImage master, final String i_resfile,
                         final Input.RelTiming timinginput, int coarse_orbit_offsetL,
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

}
