package org.jlinda.core.unwrapping.mcf.utils;

import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

public class UnwrapUtilsTest {

    @Test
    public void testGrid2D() throws Exception {

        DoubleMatrix xGrid = DoubleMatrix.linspace(1, 2, 2);
        DoubleMatrix yGrid = DoubleMatrix.linspace(1, 3, 3);

        DoubleMatrix grid[] = UnwrapUtils.grid2D(xGrid, yGrid);

        DoubleMatrix xGridMatrix_ACTUALS = grid[0];
        DoubleMatrix xGridMatrix_EXPECTED = new DoubleMatrix(xGrid.length, yGrid.length);
        xGridMatrix_EXPECTED.putRow(0, new DoubleMatrix(new double[]{1, 1, 1}));
        xGridMatrix_EXPECTED.putRow(1, new DoubleMatrix(new double[]{2, 2, 2}));

        Assert.assertArrayEquals(xGridMatrix_EXPECTED.data, xGridMatrix_ACTUALS.data, 1e-03);

        DoubleMatrix yGridMatrix_ACTUALS = grid[1];
        DoubleMatrix yGridMatrix_EXPECTED = new DoubleMatrix(xGrid.length, yGrid.length);
        yGridMatrix_EXPECTED.putRow(0, new DoubleMatrix(new double[]{1, 2, 3}));
        yGridMatrix_EXPECTED.putRow(1, new DoubleMatrix(new double[]{1, 2, 3}));

        Assert.assertArrayEquals(yGridMatrix_EXPECTED.data, yGridMatrix_ACTUALS.data, 1e-03);

        System.out.println("xGridMatrix_ACTUALS = " + xGridMatrix_ACTUALS.toString());
        System.out.println("yGridMatrix_ACTUALS = " + yGridMatrix_ACTUALS.toString());

    }

    @Test
    public void testMeshGrid() {

        DoubleMatrix x = DoubleMatrix.linspace(1, 2, 2);
        DoubleMatrix y = DoubleMatrix.linspace(1, 3, 3);

        DoubleMatrix meshGrid[] = UnwrapUtils.meshgrid(x, y);

        DoubleMatrix xGrid_ACTUAL = meshGrid[0];
        DoubleMatrix xGrid_EXPECTED = new DoubleMatrix(y.length, x.length); // first rows, then columns
        xGrid_EXPECTED.putRow(0, new DoubleMatrix(new double[]{1, 2}));
        xGrid_EXPECTED.putRow(1, new DoubleMatrix(new double[]{1, 2}));
        xGrid_EXPECTED.putRow(2, new DoubleMatrix(new double[]{1, 2}));

        Assert.assertArrayEquals(xGrid_EXPECTED.data, xGrid_ACTUAL.data, 1e-03);

        DoubleMatrix yGrid_ACTUAL = meshGrid[1];
        DoubleMatrix yGrid_EXPECTED = new DoubleMatrix(y.length, x.length);
        yGrid_EXPECTED.putRow(0, new DoubleMatrix(new double[]{1, 1}));
        yGrid_EXPECTED.putRow(1, new DoubleMatrix(new double[]{2, 2}));
        yGrid_EXPECTED.putRow(2, new DoubleMatrix(new double[]{3, 3}));

        Assert.assertArrayEquals(yGrid_EXPECTED.data, yGrid_ACTUAL.data, 1e-03);

        System.out.println("yGrid_ACTUAL = " + yGrid_ACTUAL.toString());
        System.out.println("xGrid_ACTUAL = " + xGrid_ACTUAL.toString());

    }


}
