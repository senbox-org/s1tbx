package org.jlinda.core.unwrapping.mcf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.winvector.linalg.DenseVec;
import com.winvector.linalg.Matrix;
import com.winvector.linalg.jblas.JBlasMatrix;
import com.winvector.lp.LPEQProb;
import com.winvector.lp.LPException;
import com.winvector.lp.LPSoln;
import com.winvector.lp.impl.RevisedSimplexSolver;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Constants;
import org.jlinda.core.unwrapping.mcf.utils.JblasUtils;
import org.jlinda.core.unwrapping.mcf.utils.SimulateData;
import org.jlinda.core.unwrapping.mcf.utils.UnwrapUtils;
import org.perf4j.StopWatch;
import org.slf4j.LoggerFactory;

import static org.jblas.DoubleMatrix.concatHorizontally;
import static org.jlinda.core.unwrapping.mcf.utils.UnwrapUtils.grid2D;
import static org.jlinda.core.unwrapping.mcf.utils.UnwrapUtils.sub2ind;

/**
 * Description: Implementation of MCF ~ Linear Programming Unwrapping. Based on work of Costantini.
 */
public class Unwrapper {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Unwrapper.class);

    // Parameters for Linear Programming estimation/solution
    final double tol = 1.0e-4;
    final int maxRounds = 10000;

    private DoubleMatrix wrappedPhase;
    private DoubleMatrix unwrappedPhase;

    public void setWrappedPhase(DoubleMatrix wrappedPhase) {
        this.wrappedPhase = wrappedPhase;
    }

    public DoubleMatrix getUnwrappedPhase() {
        return unwrappedPhase;
    }

    public Unwrapper(DoubleMatrix wrappedPhase) {
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

        final int ny = wrappedPhase.rows;
        final int nx = wrappedPhase.columns;

        if (wrappedPhase.isVector()) throw new IllegalArgumentException("Input must be 2D array");
        if (wrappedPhase.rows < 2 || wrappedPhase.columns < 2)
            throw new IllegalArgumentException("Size of input must be larger than 2");

        // Default weight
        DoubleMatrix w1 = DoubleMatrix.ones(ny, 1);
        w1.put(0, 0.5);
        w1.put(w1.length - 1, 0.5);
        DoubleMatrix w2 = DoubleMatrix.ones(1, nx);
        w2.put(0, 0.5);
        w2.put(w2.length - 1, 0.5);
        DoubleMatrix weight = w1.mmul(w2);

        // pre-allocate arrays for speed
        DoubleMatrix x1p = new DoubleMatrix(ny - 1, nx);
        DoubleMatrix x1m = new DoubleMatrix(ny - 1, nx);
        DoubleMatrix x2p = new DoubleMatrix(ny, nx - 1);
        DoubleMatrix x2m = new DoubleMatrix(ny, nx - 1);

        // start estimation

        // looping indices: work with full range, no overla
        DoubleMatrix iy = DoubleMatrix.linspace(0, ny - 1, ny);
        DoubleMatrix iy0 = iy.dup();
        DoubleMatrix iy1 = iy0.getRange(0, iy0.length - 1);
        DoubleMatrix iy2 = iy0.getRange(0, iy0.length);

        DoubleMatrix ix = DoubleMatrix.linspace(0, nx - 1, nx);
        DoubleMatrix ix1 = ix.getRange(0, ix.length);
        DoubleMatrix ix2 = ix.getRange(0, ix.length - 1);

        // Compute partial derivative Psi1, eqt (1,3)
        DoubleMatrix i, j, I_J, IP1_J, I_JP1;
        DoubleMatrix Psi1, Psi2;
        DoubleMatrix[] ROWS;

        i = DoubleMatrix.linspace(1, ny - 1, ny - 1);
        j = DoubleMatrix.linspace(1, nx, nx);
        ROWS = grid2D(i, j);
        I_J = sub2ind(wrappedPhase.rows, wrappedPhase.columns, ROWS[0], ROWS[1]);
        IP1_J = sub2ind(wrappedPhase.rows, wrappedPhase.columns, ROWS[0].add(1), ROWS[1]);
        Psi1 = JblasUtils.getMatrixFromIdx(wrappedPhase, IP1_J, 1).sub(JblasUtils.getMatrixFromIdx(wrappedPhase, I_J, 1));
        Psi1 = UnwrapUtils.wrapDoubleMatrix(Psi1);

        // Compute partial derivative Psi2, eqt (2,4)
        i = DoubleMatrix.linspace(1, ny, ny);
        j = DoubleMatrix.linspace(1, nx - 1, nx - 1);
        ROWS = grid2D(i, j);
        I_J = sub2ind(wrappedPhase.rows, wrappedPhase.columns, ROWS[0], ROWS[1]);
        I_JP1 = sub2ind(wrappedPhase.rows, wrappedPhase.columns, ROWS[0], ROWS[1].add(1));
        Psi2 = JblasUtils.getMatrixFromIdx(wrappedPhase, I_JP1, 1).sub(JblasUtils.getMatrixFromIdx(wrappedPhase, I_J, 1));
        Psi2 = UnwrapUtils.wrapDoubleMatrix(Psi2);

        // Compute beq
        i = DoubleMatrix.linspace(1, ny - 1, ny - 1);
        j = DoubleMatrix.linspace(1, nx - 1, nx - 1);
        ROWS = grid2D(i, j);
        I_J = sub2ind(Psi1.rows, Psi1.columns, ROWS[0], ROWS[1]);
        I_JP1 = sub2ind(Psi1.rows, Psi1.columns, ROWS[0], ROWS[1].add(1));
        DoubleMatrix beq = DoubleMatrix.zeros(I_JP1.rows, I_JP1.columns);
        beq.addi(JblasUtils.getMatrixFromIdx(wrappedPhase, I_JP1, 1).sub(JblasUtils.getMatrixFromIdx(wrappedPhase, I_J, 1)));
        beq.muli(-1 / (2 * Constants._PI));
        for (int k = 0; k < beq.length; k++) {
            beq.put(k, Math.round(beq.get(k)));
        }
        beq.reshape(beq.length, 1);

        /*
         The vector of LP is arranged as following:
           x := (x1p, x1m, x2p, x2m).'
           x1p, x1m: reshaping of [ny-1] x [nx]
           x2p, x2m: reshaping of [ny] x [nx-1]

         Row index, used by all foure blocks in Aeq, beq
        */
        logger.info("Constraint matrix");
        i = DoubleMatrix.linspace(1, ny - 1, ny - 1);
        j = DoubleMatrix.linspace(1, nx - 1, nx - 1);
        ROWS = grid2D(i, j);
        DoubleMatrix ROW_I_J = sub2ind(i.length, j.length, ROWS[0], ROWS[1]);

        double nS0 = (nx - 1) * (ny - 1);

        // Use by S1p, S1m
        DoubleMatrix[] COLS;
        COLS = grid2D(i, j);
        DoubleMatrix COL_IJ_1 = sub2ind(i.length, j.length + 1, COLS[0], COLS[1]);
        COLS = grid2D(i, j.add(1));
        DoubleMatrix COL_I_JP1 = sub2ind(i.length, j.length + 1, COLS[0], COLS[1]);
        double nS1 = (nx) * (ny - 1);

        // SOAPBinding.Use by S2p, S2m
        COLS = grid2D(i, j);
        DoubleMatrix COL_IJ_2 = sub2ind(i.length + 1, j.length, COLS[0], COLS[1]);
        COLS = grid2D(i.add(1), j);
        DoubleMatrix COL_IP1_J = sub2ind(i.length + 1, j.length, COLS[0], COLS[1]);

        double nS2 = (nx - 1) * (ny);

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
        DoubleMatrix S1p = JblasUtils.setMatrixFromIdx(nS0, nS1, ROW_I_J, COL_I_JP1, 1).sub(JblasUtils.setMatrixFromIdx(nS0, nS1, ROW_I_J, COL_IJ_1, 1));
        DoubleMatrix S1m = S1p.neg();

        DoubleMatrix S2p = JblasUtils.setMatrixFromIdx(nS0, nS2, ROW_I_J, COL_IP1_J, 1).neg().add(JblasUtils.setMatrixFromIdx(nS0, nS2, ROW_I_J, COL_IJ_2, 1));
        DoubleMatrix S2m = S2p.neg();

        DoubleMatrix Aeq = concatHorizontally(concatHorizontally(S1p, S1m), concatHorizontally(S2p, S2m));

        int nVars = Aeq.columns;


        DoubleMatrix c1 = JblasUtils.getMatrixFromRange(1, ny - 1, 1, weight.columns, weight, 1).add(JblasUtils.getMatrixFromRange(1, ny - 1, 1, weight.columns, weight, 1)).mul(0.5);
        DoubleMatrix c2 = JblasUtils.getMatrixFromRange(1, weight.rows, 1, nx - 1, weight, 1).add(JblasUtils.getMatrixFromRange(1, weight.rows, 1, nx - 1, weight, 1)).mul(0.5);

        c1.reshape(c1.length, 1);
        c2.reshape(c2.length, 1);

        DoubleMatrix cost = DoubleMatrix.concatVertically(DoubleMatrix.concatVertically(c1, c1), DoubleMatrix.concatVertically(c2, c2));

        logger.info("Minimum network flow resolution");

        final Matrix<?> m = JBlasMatrix.factory.newMatrix(Aeq.rows, Aeq.columns, true);
        for (int k = 0; k < Aeq.rows; k++) {
            m.setRow(k, Aeq.getRow(k).data);
        }
        final LPEQProb prob = new LPEQProb(m.columnMatrix(), beq.data, new DenseVec(cost.data));
//        prob.printCPLEX(System.out);
        final RevisedSimplexSolver solver = new RevisedSimplexSolver();

        final LPSoln soln = solver.solve(prob, null, tol, maxRounds, JBlasMatrix.factory);

        // ToDo: integrate LP solution - move code from closed source branch
    }

    public static void main(String[] args) throws LPException {

        final int rows = 40;
        final int cols = rows;

        logger.setLevel(Level.TRACE);
        logger.trace("Start Unwrapping");

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
        logger.info("Total processing time {} [sec]", (double) (clockFull.getElapsedTime()) / 1000);
    }

}
