package org.jlinda.core.unwrapping.mcf;

import com.winvector.lp.LPException;
import org.apache.commons.math3.util.FastMath;
import org.esa.beam.util.SystemUtils;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Constants;
import org.jlinda.core.unwrapping.mcf.utils.JblasUtils;
import org.jlinda.core.unwrapping.mcf.utils.SimulateData;
import org.jlinda.core.unwrapping.mcf.utils.UnwrapUtils;
import org.perf4j.StopWatch;
import scpsolver.constraints.LinearEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jblas.DoubleMatrix.concatHorizontally;
import static org.jlinda.core.unwrapping.mcf.utils.JblasUtils.grid2D;
import static org.jlinda.core.unwrapping.mcf.utils.JblasUtils.intRangeDoubleMatrix;

/**
 * Description: Implementation of Linear Programming Unwrapping. Heavily based on Matlab package
 * for Costantini Unwrapping - implementation of costantiniUnwrap() function code.
 */
public class UnwrapperGLPK {

    private static final Logger logger = SystemUtils.LOG;

    private DoubleMatrix wrappedPhase;
    private DoubleMatrix unwrappedPhase;

    private boolean roundK = true;

    public void setWrappedPhase(DoubleMatrix wrappedPhase) {
        this.wrappedPhase = wrappedPhase;
    }

    public DoubleMatrix getUnwrappedPhase() {
        return unwrappedPhase;
    }

    public UnwrapperGLPK(DoubleMatrix wrappedPhase) {
        logger.setLevel(Level.INFO);
        this.wrappedPhase = wrappedPhase;
    }

    // for now only one method - this should be facade like call
    public void unwrap() {
        try {
            costantiniUnwrap();
        } catch (LPException ex) {
            ex.printStackTrace();
        }
    }

    private void costantiniUnwrap() throws LPException {

        final int ny = wrappedPhase.rows - 1; // start from Zero!
        final int nx = wrappedPhase.columns - 1; // start from Zero!

        if (wrappedPhase.isVector()) throw new IllegalArgumentException("Input must be 2D array");
        if (wrappedPhase.rows < 2 || wrappedPhase.columns < 2)
            throw new IllegalArgumentException("Size of input must be larger than 2");

        // Default weight
        DoubleMatrix w1 = DoubleMatrix.ones(ny + 1, 1);
        w1.put(0, 0.5);
        w1.put(w1.length - 1, 0.5);
        DoubleMatrix w2 = DoubleMatrix.ones(1, nx + 1);
        w2.put(0, 0.5);
        w2.put(w2.length - 1, 0.5);
        DoubleMatrix weight = w1.mmul(w2);

        DoubleMatrix i, j, I_J, IP1_J, I_JP1;
        DoubleMatrix Psi1, Psi2;
        DoubleMatrix[] ROWS;

        // Compute partial derivative Psi1, eqt (1,3)
        i = intRangeDoubleMatrix(0, ny - 1);
        j = intRangeDoubleMatrix(0, nx);
        ROWS = grid2D(i, j);
        I_J = JblasUtils.sub2ind(wrappedPhase.rows, ROWS[0], ROWS[1]);
        IP1_J = JblasUtils.sub2ind(wrappedPhase.rows, ROWS[0].add(1), ROWS[1]);
        Psi1 = JblasUtils.getMatrixFromIdx(wrappedPhase, IP1_J).sub(JblasUtils.getMatrixFromIdx(wrappedPhase, I_J));
        Psi1 = UnwrapUtils.wrapDoubleMatrix(Psi1);

        // Compute partial derivative Psi2, eqt (2,4)
        i = intRangeDoubleMatrix(0, ny);
        j = intRangeDoubleMatrix(0, nx - 1);
        ROWS = grid2D(i, j);
        I_J = JblasUtils.sub2ind(wrappedPhase.rows, ROWS[0], ROWS[1]);
        I_JP1 = JblasUtils.sub2ind(wrappedPhase.rows, ROWS[0], ROWS[1].add(1));
        Psi2 = JblasUtils.getMatrixFromIdx(wrappedPhase, I_JP1).sub(JblasUtils.getMatrixFromIdx(wrappedPhase, I_J));
        Psi2 = UnwrapUtils.wrapDoubleMatrix(Psi2);

        // Compute beq
        DoubleMatrix beq = DoubleMatrix.zeros(ny, nx);
        i = intRangeDoubleMatrix(0, ny - 1);
        j = intRangeDoubleMatrix(0, nx - 1);
        ROWS = grid2D(i, j);
        I_J = JblasUtils.sub2ind(Psi1.rows, ROWS[0], ROWS[1]);
        I_JP1 = JblasUtils.sub2ind(Psi1.rows, ROWS[0], ROWS[1].add(1));
        beq.addi(JblasUtils.getMatrixFromIdx(Psi1, I_JP1).sub(JblasUtils.getMatrixFromIdx(Psi1, I_J)));
        I_J = JblasUtils.sub2ind(Psi2.rows, ROWS[0], ROWS[1]);
        I_JP1 = JblasUtils.sub2ind(Psi2.rows, ROWS[0].add(1), ROWS[1]);
        beq.subi(JblasUtils.getMatrixFromIdx(Psi2, I_JP1).sub(JblasUtils.getMatrixFromIdx(Psi2, I_J)));
        beq.muli(-1 / (2 * Constants._PI));
        for (int k = 0; k < beq.length; k++) {
            beq.put(k, Math.round(beq.get(k)));
        }
        beq.reshape(beq.length, 1);

        logger.info("Constraint matrix");
        i = intRangeDoubleMatrix(0, ny - 1);
        j = intRangeDoubleMatrix(0, nx - 1);
        ROWS = grid2D(i, j);
        DoubleMatrix ROW_I_J = JblasUtils.sub2ind(i.length, ROWS[0], ROWS[1]);
        int nS0 = nx * ny;

        // Use by S1p, S1m
        DoubleMatrix[] COLS;
        COLS = grid2D(i, j);
        DoubleMatrix COL_IJ_1 = JblasUtils.sub2ind(i.length, COLS[0], COLS[1]);
        COLS = grid2D(i, j.add(1));
        DoubleMatrix COL_I_JP1 = JblasUtils.sub2ind(i.length, COLS[0], COLS[1]);
        int nS1 = (nx + 1) * (ny);

        // SOAPBinding.Use by S2p, S2m
        COLS = grid2D(i, j);
        DoubleMatrix COL_IJ_2 = JblasUtils.sub2ind(i.length + 1, COLS[0], COLS[1]);
        COLS = grid2D(i.add(1), j);
        DoubleMatrix COL_IP1_J = JblasUtils.sub2ind(i.length + 1, COLS[0], COLS[1]);
        int nS2 = nx * (ny + 1);

        // Equality constraint matrix (Aeq)
        /*
            S1p = + sparse(ROW_I_J, COL_I_JP1,1,nS0,nS1) ...
                  - sparse(ROW_I_J, COL_IJ_1,1,nS0,nS1);
            S1m = -S1p;

            S2p = - sparse(ROW_I_J, COL_IP1_J,1,nS0,nS2) ...
                  + sparse(ROW_I_J, COL_IJ_2,1,nS0,nS2);
            S2m = -S2p;
        */

        // ToDo: Aeq matrix should be sparse from it's initialization, look into JblasMatrix factory for howto
        // ...otherwise even a data set of eg 40x40 pixels will exhaust heap:
        // ...    dimension of Aeq (equality constraints) matrix for 30x30 input is 1521x6240 matrix
        // ...    dimension of Aeq (                    ) matrix for 50x50 input is 2401x9800
        // ...    dimension of Aeq (                    ) matrix for 512x512 input is 261121x1046528
        DoubleMatrix S1p = JblasUtils.setUpMatrixFromIdx(nS0, nS1, ROW_I_J, COL_I_JP1).sub(JblasUtils.setUpMatrixFromIdx(nS0, nS1, ROW_I_J, COL_IJ_1));
        DoubleMatrix S1m = S1p.neg();

        DoubleMatrix S2p = JblasUtils.setUpMatrixFromIdx(nS0, nS2, ROW_I_J, COL_IP1_J).neg().add(JblasUtils.setUpMatrixFromIdx(nS0, nS2, ROW_I_J, COL_IJ_2));
        DoubleMatrix S2m = S2p.neg();

        DoubleMatrix Aeq = concatHorizontally(concatHorizontally(S1p, S1m), concatHorizontally(S2p, S2m));

        final int nObs = Aeq.columns;
        final int nUnkn = Aeq.rows;

        DoubleMatrix c1 = JblasUtils.getMatrixFromRange(0, ny, 0, weight.columns, weight);
        DoubleMatrix c2 = JblasUtils.getMatrixFromRange(0, weight.rows, 0, nx, weight);

        c1.reshape(c1.length, 1);
        c2.reshape(c2.length, 1);

        DoubleMatrix cost = DoubleMatrix.concatVertically(DoubleMatrix.concatVertically(c1, c1), DoubleMatrix.concatVertically(c2, c2));

        logger.info("Minimum network flow resolution");

        StopWatch clockLP = new StopWatch();
        LinearProgram lp = new LinearProgram(cost.data);
        lp.setMinProblem(true);

        boolean[] integerBool = new boolean[nObs];
        double[] lowerBound = new double[nObs];
        double[] upperBound = new double[nObs];

        for (int k = 0; k < nUnkn; k++) {
            lp.addConstraint(new LinearEqualsConstraint(Aeq.getRow(k).toArray(), beq.get(k), "cost"));
        }

        for (int k = 0; k < nObs; k++) {
            integerBool[k] = true;
            lowerBound[k] = 0;
            upperBound[k] = 99999;
        }

        // setup bounds and integer nature
        lp.setIsinteger(integerBool);
        lp.setUpperbound(upperBound);
        lp.setLowerbound(lowerBound);
        LinearProgramSolver solver = SolverFactory.newDefault();

//        double[] solution;
//        solution = solver.solve(lp);
        DoubleMatrix solution = new DoubleMatrix(solver.solve(lp));

        clockLP.stop();
        logger.info("Total GLPK time: {} [sec]"+ (double) (clockLP.getElapsedTime()) / 1000);

        // Displatch the LP solution
        int offset;

        int[] idx1p = JblasUtils.intRangeIntArray(0, nS1 - 1);
        DoubleMatrix x1p = solution.get(idx1p);
        x1p.reshape(ny, nx + 1);
        offset = idx1p[nS1 - 1] + 1;

        int[] idx1m = JblasUtils.intRangeIntArray(offset, offset + nS1 - 1);
        DoubleMatrix x1m = solution.get(idx1m);
        x1m.reshape(ny, nx + 1);
        offset = idx1m[idx1m.length - 1] + 1;

        int[] idx2p = JblasUtils.intRangeIntArray(offset, offset + nS2 - 1);
        DoubleMatrix x2p = solution.get(idx2p);
        x2p.reshape(ny + 1, nx);
        offset = idx2p[idx2p.length - 1] + 1;

        int[] idx2m = JblasUtils.intRangeIntArray(offset, offset + nS2 - 1);
        DoubleMatrix x2m = solution.get(idx2m);
        x2m.reshape(ny + 1, nx);

        // Compute the derivative jumps, eqt (20,21)
        DoubleMatrix k1 = x1p.sub(x1m);
        DoubleMatrix k2 = x2p.sub(x2m);

        // (?) Round to integer solution
        if (roundK == true) {
            for (int idx = 0; idx < k1.length; idx++) {
                k1.put(idx, FastMath.floor(k1.get(idx)));
            }
            for (int idx = 0; idx < k2.length; idx++) {
                k2.put(idx, FastMath.floor(k2.get(idx)));
            }
        }

        // Sum the jumps with the wrapped partial derivatives, eqt (10,11)
        k1.reshape(ny, nx + 1);
        k2.reshape(ny + 1, nx);
        k1.addi(Psi1.div(Constants._TWO_PI));
        k2.addi(Psi2.div(Constants._TWO_PI));

        // Integrate the partial derivatives, eqt (6)
        // cumsum() method in JblasTester -> see cumsum_demo() in JblasTester.cumsum_demo()
        DoubleMatrix k2_temp = DoubleMatrix.concatHorizontally(DoubleMatrix.zeros(1), k2.getRow(0));
        k2_temp = JblasUtils.cumsum(k2_temp, 1);
        DoubleMatrix k = DoubleMatrix.concatVertically(k2_temp, k1);
        k = JblasUtils.cumsum(k, 1);

        // Unwrap - final solution
        unwrappedPhase = k.mul(Constants._TWO_PI);

    }

    public static void main(String[] args) throws LPException {

        final int rows = 40;
        final int cols = rows;

        logger.info("Start Unwrapping");
        logger.info("Simulate Data");
        SimulateData simulateData = new SimulateData(rows, cols);
        simulateData.peaks();

        DoubleMatrix Phi = simulateData.getSimulatedData();
        DoubleMatrix Psi = UnwrapUtils.wrapDoubleMatrix(Phi);

        StopWatch clockFull = new StopWatch();
        clockFull.start();

        Unwrapper unwrapper = new Unwrapper(Psi);
        unwrapper.unwrap();

        clockFull.stop();
        logger.info("Total processing time {} [sec]"+ (double) (clockFull.getElapsedTime()) / 1000);
    }

}
