package org.jlinda.core.simulation;

import org.jlinda.core.utils.MathUtils;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;

import static org.jblas.MatrixFunctions.cos;
import static org.jblas.MatrixFunctions.sin;

public class Simulation {

    public static ComplexDoubleMatrix simulateIFG(final int nRows, final int nCols, final int numFringes, boolean noiseFlag, final double noiseLevel) {

        DoubleMatrix phaseMatrix = MathUtils.ramp(nRows, nCols).mul(2 * Math.PI).mul(numFringes);
        DoubleMatrix magMatrix = DoubleMatrix.ones(nRows, nCols);

        if (noiseFlag) {
            phaseMatrix.addi(DoubleMatrix.randn(nRows, nCols).mmuli(noiseLevel * Math.PI));
            magMatrix.addi(DoubleMatrix.randn(nRows, nCols).mmuli(noiseLevel*2));
        }

        ComplexDoubleMatrix temp1 = new ComplexDoubleMatrix(magMatrix);
        ComplexDoubleMatrix temp2 = new ComplexDoubleMatrix(cos(phaseMatrix), sin(phaseMatrix));
        return temp1.mul(temp2);
    }
}
