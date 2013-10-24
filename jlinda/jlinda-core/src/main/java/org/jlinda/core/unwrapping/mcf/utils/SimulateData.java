package org.jlinda.core.unwrapping.mcf.utils;

import org.jblas.DoubleMatrix;

import static org.jblas.MatrixFunctions.exp;
import static org.jblas.MatrixFunctions.pow;

/*
* Description: Simulate simulatedData for input for unwrapping. Based on Matlab's peaks function.
*/
public class SimulateData {

    int xNumElems;
    int xMin; // min value in x-direction
    int xMax; // max value in y-direction

    int yNumElems;
    int yMin; // max value in y-direction
    int yMax; // min value in y-direction

    private DoubleMatrix grid[] = new DoubleMatrix[2];
    public DoubleMatrix xGrid;
    public DoubleMatrix yGrid;
    private DoubleMatrix simulatedData;

    public SimulateData(int xNumElems, int yNumElems) {
        this.yNumElems = yNumElems;
        this.xNumElems = xNumElems;

        this.xMin = -3;
        this.xMax = 3;
        this.yMin = xMin;
        this.yMax = xMax;

        this.simulatedData = new DoubleMatrix(xNumElems, yNumElems);

        // initially I was using ndgrid, but inconsistent with matlab prototype, therefore meshgrid
        this.grid = UnwrapUtils.meshgrid(DoubleMatrix.linspace(xMin, xMax, xNumElems),
                DoubleMatrix.linspace(yMin, yMax, yNumElems));

        this.xGrid = grid[0];
        this.yGrid = grid[1];
    }

    public DoubleMatrix getSimulatedData() {
        return simulatedData;
    }

    public void peaks() {

        simulatedData = peaks(xGrid, yGrid);

    }

    // tested as public method, and test method commented out
    private double peaks(double x, double y) {
        double z;
        z = 3 * Math.pow(1 - x, 2) * Math.exp(-Math.pow(x, 2) - Math.pow(y + 1, 2))
                - 10 * (x / 5 - Math.pow(x, 3) - Math.pow(y, 5)) * Math.exp(-Math.pow(x, 2) - Math.pow(y, 2))
                - 1 / 3 * Math.exp(-Math.pow(x + 1, 2) - Math.pow(y, 2));
        return z;
    }

    public static DoubleMatrix peaks(DoubleMatrix x, DoubleMatrix y) {

        // z =  3*(1-x).^2.*exp(-(x.^2) - (y+1).^2) - 10*(x/5 - x.^3 - y.^5).*exp(-x.^2-y.^2) - 1/3*exp(-(x+1).^2 - y.^2)
        return pow(x.rsub(1), 2).mul(3).mul(exp(pow(x, 2).neg().sub(pow(y.add(1), 2)))).
                sub((x.div(5).sub(pow(x, 3)).sub(pow(y, 5))).mul(10).mul(exp(pow(x, 2).neg().sub(pow(y, 2))))).
                sub(exp(pow(x.add(1), 2).neg().sub(pow(y, 2))).div(3));
    }

}
