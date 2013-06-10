package org.jlinda.core.geocode;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jlinda.core.Baseline;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.PolyUtils;

/**
 * DInSAR core class
 * <p/>
 * Differential insar with an unwrapped topo interferogram (hgt or real4 format) and a wrapped(!) defo interf.
 * <p/>
 * The topography is removed from the deformation interferogram by the formula (prime ' denotes defo pair):
 * <p/>
 * dr = lambda\4pi * [phi' - phi(Bperp'/Bperp)]
 * phi_diff = phi(Bperp'/Bperp) - phi'
 * <p/>
 * where Bperp is the perpendicular baseline for points on the ellipsoid (and not the true one)!
 * <p/>
 * Implementation details: First the baseline for a number of points for topo and defo interferograms computed,
 * and then the ratio between baselines is modeled by a 2D polynomial of degree 1. Then this polynomial is
 * evaluated to compute the new (defo only) phase according to the equation above.
 * It is assumed that the interferogram's are coregistered on each other and have the same dimensions.
 * <p/>
 * Input:
 * -input parameters
 * -orbits
 * -info on input files
 * <p/>
 * Output:
 * -complex float file with differential phase.
 * (set to (0,0) for not ok unwrapped parts)
<<<<<<< HEAD
 *
 * Known issues:
 *  - polyfit will trigger warnings on maxerror deviating more then expected, it's because of scaling of error is
 *    not being performed. See code example how to resolve for this.
 *
 *  - potential issues with threading safety, volatile on 'data' fields not being properly tested.
 *
 * Note: optimization was done in how the unwrapped_phase is subtracted from DEFO pair. I am working
 *       with tiles now, and not removing TOPO phase line by line as initally implemented. This resulted
 *       ~10x improvement. See unit tests for some rough benchmarks.
 *
 * Performance issues & TODO: input of TOPO (unwrapped phase coming from SNAPHU) has to be looped
 *    for a NaN values check. Perhaps it is more efficient to do this check while parsing the dat
 *    into NEST.
=======
 * <p/>
 * Known issues:
 * - polyfit will trigger warnings on maxerror deviating more then expected, it's because of scaling of error is
 * not being performed. See code example how to resolve for this.
 * <p/>
 * - potential issues with threading safety, volatile on 'data' fields not being properly tested.
 * <p/>
 * Note: optimization was done in how the unwrapped_phase is subtracted from DEFO pair. I am working
 * with tiles now, and not removing TOPO phase line by line as initally implemented. This resulted
 * ~10x improvement. See unit tests for some rough benchmarks.
 * <p/>
 * Performance issues & TODO: input of TOPO (unwrapped phase coming from SNAPHU) has to be looped
 * for a NaN values check. Perhaps it is more efficient to do this check while parsing the dat
 * into NEST.
>>>>>>> dinsar_operator
 */

public class DInSAR {

    final private SLCImage masterMeta;
    final private SLCImage slaveDefoMeta;
    final private SLCImage topoSlaveMeta;
    final private Orbit masterOrbit;
    final private Orbit slaveDefoOrbit;
    final private Orbit slaveTopoOrbit;

    private volatile Window dataWindow;
    private Window tileWindow;

    private DoubleMatrix topoData;
    private ComplexDoubleMatrix defoData;

    private DoubleMatrix rhs;

    public DInSAR(SLCImage masterMeta, Orbit masterOrbit,
                  SLCImage slaveDefoMeta, Orbit slaveDefoOrbit,
                  SLCImage topoSlaveMeta, Orbit slaveTopoOrbit) {

        this.masterMeta = masterMeta;
        this.masterOrbit = masterOrbit;

        this.slaveDefoMeta = slaveDefoMeta;
        this.slaveDefoOrbit = slaveDefoOrbit;

        this.topoSlaveMeta = topoSlaveMeta;
        this.slaveTopoOrbit = slaveTopoOrbit;

        dataWindow = masterMeta.getCurrentWindow();
    }

    public void setDataWindow(Window dataWindow) {
        this.dataWindow = dataWindow;
    }

    public void setTileWindow(Window tileWindow) {
        this.tileWindow = tileWindow;
    }

    public void setTopoData(DoubleMatrix topoData) {
        this.topoData = topoData;
    }

    public void setDefoData(ComplexDoubleMatrix defoData) {
        this.defoData = defoData;
    }

    public ComplexDoubleMatrix getDefoData() {
        return defoData;
    }

    @Deprecated
    public void dinsar() throws Exception {

        // Normalization factors for polynomial
        double minL = dataWindow.linelo;
        double maxL = dataWindow.linehi;
        double minP = dataWindow.pixlo;
        double maxP = dataWindow.pixhi;

        // Model perpendicular baseline for master and defo
        // .....compute B on grid every 500 lines, 100 pixels
        // .....in window for topoData/defoData
        final int nPoints = 200;
        final int[][] positionArray = MathUtils.distributePoints(nPoints, masterMeta.getCurrentWindow());
        DoubleMatrix position = new DoubleMatrix(nPoints, 2);
        for (int i = 0; i < nPoints; i++) {
            position.put(i, 0, positionArray[i][0]);
            position.put(i, 1, positionArray[i][1]);
        }

        // model baselines
        Baseline topoBaseline = new Baseline();
        topoBaseline.model(masterMeta, topoSlaveMeta, masterOrbit, slaveTopoOrbit);

        Baseline defoBaseline = new Baseline();
        defoBaseline.model(masterMeta, slaveDefoMeta, masterOrbit, slaveDefoOrbit);

        DoubleMatrix bPerpTopo = new DoubleMatrix(nPoints);
        DoubleMatrix bPerpDefo = new DoubleMatrix(nPoints);

        for (int i = 0; i < nPoints; ++i) {
            bPerpTopo.put(i, topoBaseline.getBperp(position.get(i, 0), position.get(i, 1), 0));
            bPerpDefo.put(i, defoBaseline.getBperp(position.get(i, 0), position.get(i, 1), 0));
        }

        // Now model ratio bPerpDefo/bPerpTopo as linear ______
        //   ...r(l,p) = a00 + a10*l + a01*p ______
        //   ...give stats on max. error ______

        DoubleMatrix baselineRatio = bPerpDefo.div(bPerpTopo);

        DoubleMatrix rhs = new DoubleMatrix(PolyUtils.polyFit2D(normalize(position.getColumn(0), minL, maxL),
                normalize(position.getColumn(1), minP, maxP), baselineRatio, 1));

        // TODO: polyfit will trigger a warning on e_hat larger than expected, for DInSAR e_hat has to be scaled!
/*
        ....for DInSAR baseline ratio estimation this error should be scaled
        ....eg using jblas notation

        double maxErrorRatio = e_hat.normmax() // max error
        int maxErrorRatioIdx = e_hat.abs().argmax() // index of maximum error
        double maxRelativeErrorRatio = 100.0 * maxErrorRatio / ratio.get(maxErrorRatioIdx)
        logger.INFO("maximum error for l,p : {}, {}", x.get(maxErrorRatioIdx), y.get(maxErrorRatioIdx));
        logger.INFO("Ratio = {}; estimate = {}; rel.error = ", ratio(maxErrorRatioIdx), y_hat(maxErrorRatioIdx), maxRelativeErrorRatio);


        if (maxRelativeErrorRatio < 5.0) {
            logger.INFO("max (relative) error: OK!");
        } else {
            logger.WARN("max error quite large");
            logger.WARN("Error in deformation vector larger than 5% due to mismodeling baseline!");
        }
*/

        DoubleMatrix azimuthAxisNormalize = DoubleMatrix.linspace((int) tileWindow.linelo, (int) tileWindow.linehi, defoData.rows);
        normalize_inplace(azimuthAxisNormalize, minL, maxL);

        DoubleMatrix rangeAxisNormalize = DoubleMatrix.linspace((int) tileWindow.pixlo, (int) tileWindow.pixhi, defoData.columns);
        normalize_inplace(rangeAxisNormalize, minP, maxP);

        DoubleMatrix ratio = PolyUtils.polyval(azimuthAxisNormalize, rangeAxisNormalize, rhs, PolyUtils.degreeFromCoefficients(rhs.length));

        DoubleMatrix scaledTopo = topoData.mul(ratio);
        ComplexDoubleMatrix ratioBaselinesCplx = new ComplexDoubleMatrix(MatrixFunctions.cos(scaledTopo), MatrixFunctions.sin(scaledTopo).neg());

        // check whether any NaNs are coming from unwrapped data
        for (int i = 0; i < defoData.length; i++) {
            if (defoData.data[i] == Double.NaN) {
                defoData.data[i] = 0.0d;
            }
        }
        defoData.muli(ratioBaselinesCplx);
    }

    public void computeBperpRatios() throws Exception {

        // Normalization factors for polynomial
        double minL = dataWindow.linelo;
        double maxL = dataWindow.linehi;
        double minP = dataWindow.pixlo;
        double maxP = dataWindow.pixhi;

        // Model perpendicular baseline for master and defo
        // .....compute B on grid every 500 lines, 100 pixels
        // .....in window for topoData/defoData
        final int nPoints = 200;
        final int[][] positionArray = MathUtils.distributePoints(nPoints, masterMeta.getCurrentWindow());
        DoubleMatrix position = new DoubleMatrix(nPoints, 2);
        for (int i = 0; i < nPoints; i++) {
            position.put(i, 0, positionArray[i][0]);
            position.put(i, 1, positionArray[i][1]);
        }

        // model baselines
        Baseline topoBaseline = new Baseline();
        topoBaseline.model(masterMeta, topoSlaveMeta, masterOrbit, slaveTopoOrbit);

        Baseline defoBaseline = new Baseline();
        defoBaseline.model(masterMeta, slaveDefoMeta, masterOrbit, slaveDefoOrbit);

        DoubleMatrix bPerpTopo = new DoubleMatrix(nPoints);
        DoubleMatrix bPerpDefo = new DoubleMatrix(nPoints);

        for (int i = 0; i < nPoints; ++i) {
            bPerpTopo.put(i, topoBaseline.getBperp(position.get(i, 0), position.get(i, 1), 0));
            bPerpDefo.put(i, defoBaseline.getBperp(position.get(i, 0), position.get(i, 1), 0));
        }

        // Now model ratio bPerpDefo/bPerpTopo as linear ______
        //   ...r(l,p) = a00 + a10*l + a01*p ______
        //   ...give stats on max. error ______

        DoubleMatrix baselineRatio = bPerpDefo.div(bPerpTopo);

        rhs = new DoubleMatrix(PolyUtils.polyFit2D(normalize(position.getColumn(0), minL, maxL),
                normalize(position.getColumn(1), minP, maxP), baselineRatio, 1));

        // TODO: polyfit will trigger a warning on e_hat larger than expected, for DInSAR e_hat has to be scaled!
/*
        ....for DInSAR baseline ratio estimation this error should be scaled
        ....eg using jblas notation

        double maxErrorRatio = e_hat.normmax() // max error
        int maxErrorRatioIdx = e_hat.abs().argmax() // index of maximum error
        double maxRelativeErrorRatio = 100.0 * maxErrorRatio / ratio.get(maxErrorRatioIdx)
        logger.INFO("maximum error for l,p : {}, {}", x.get(maxErrorRatioIdx), y.get(maxErrorRatioIdx));
        logger.INFO("Ratio = {}; estimate = {}; rel.error = ", ratio(maxErrorRatioIdx), y_hat(maxErrorRatioIdx), maxRelativeErrorRatio);


        if (maxRelativeErrorRatio < 5.0) {
            logger.INFO("max (relative) error: OK!");
        } else {
            logger.WARN("max error quite large");
            logger.WARN("Error in deformation vector larger than 5% due to mismodeling baseline!");
        }
*/

    }

    public void applyDInSAR(final Window tileWindow, final ComplexDoubleMatrix defoData, final DoubleMatrix topoData) {

        // Normalization factors for polynomial
        double minL = dataWindow.linelo;
        double maxL = dataWindow.linehi;
        double minP = dataWindow.pixlo;
        double maxP = dataWindow.pixhi;

        DoubleMatrix azimuthAxisNormalize = DoubleMatrix.linspace((int) tileWindow.linelo, (int) tileWindow.linehi, defoData.rows);
        normalize_inplace(azimuthAxisNormalize, minL, maxL);

        DoubleMatrix rangeAxisNormalize = DoubleMatrix.linspace((int) tileWindow.pixlo, (int) tileWindow.pixhi, defoData.columns);
        normalize_inplace(rangeAxisNormalize, minP, maxP);

        DoubleMatrix ratio = PolyUtils.polyval(azimuthAxisNormalize, rangeAxisNormalize, rhs, PolyUtils.degreeFromCoefficients(rhs.length));

        DoubleMatrix scaledTopo = topoData.mul(ratio);
        ComplexDoubleMatrix ratioBaselinesCplx = new ComplexDoubleMatrix(MatrixFunctions.cos(scaledTopo), MatrixFunctions.sin(scaledTopo).neg());

        // check whether any NaNs are coming from unwrapped data
        for (int i = 0; i < defoData.length; i++) {
            if (defoData.data[i] == Double.NaN) {
                defoData.data[i] = 0.0d;
            }
        }
        defoData.muli(ratioBaselinesCplx);
    }

    public static double[] linspace(final int lower, final int upper, final int size) {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            double t = (double) i / (size - 1);
            result[i] = lower * (1 - t) + t * upper;
        }
        return result;
    }

    public static double[] linspace(final int lower, final int upper) {
        return linspace(lower, upper, 100);
    }

    private void normalize_inplace(DoubleMatrix data, double min, double max) {
        data.subi(.5 * (min + max));
        data.divi(.25 * (max - min));
    }

    private DoubleMatrix normalize(DoubleMatrix in, double min, double max) {
        DoubleMatrix out = in.dup();
        out.subi(.5 * (min + max));
        out.divi(.25 * (max - min));
        return out;
    }

}
