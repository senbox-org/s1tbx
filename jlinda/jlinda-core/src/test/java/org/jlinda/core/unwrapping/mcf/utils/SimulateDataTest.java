package org.jlinda.core.unwrapping.mcf.utils;

import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

public class SimulateDataTest {

    @Test
    public void testPeaks_2D() throws Exception {

        SimulateData simulatedData = new SimulateData(3, 3);
        simulatedData.peaks();

        DoubleMatrix z_ACTUAL = SimulateData.peaks(simulatedData.xGrid, simulatedData.yGrid);


        DoubleMatrix z_EXPECTED = new DoubleMatrix(3, 3);
        // precomputed in matlab z_EXPECTED = peaks(3);
        z_EXPECTED.putRow(0, new DoubleMatrix(new double[]{6.67128029671744e-05, -0.24495404057435, -5.86418787258953e-06}));
        z_EXPECTED.putRow(1, new DoubleMatrix(new double[]{-0.0365062046131955, 0.981011843123846, 0.0331249499243083}));
        z_EXPECTED.putRow(2, new DoubleMatrix(new double[]{3.22353596126927e-05, 0.299871028226235, 4.10297274582676e-05}));

        Assert.assertArrayEquals(z_EXPECTED.data, z_ACTUAL.data, 1e-06);
        Assert.assertArrayEquals(z_EXPECTED.data, simulatedData.getSimulatedData().data, 1e-06);

    }

/*
    // only for testing of private method
    @Test
    public void testPeaks_1D() {

        double z_EXPECTED;

        z_EXPECTED = 2.4338;
        Assert.assertEquals(z_EXPECTED, SimulateData.peaks(1, 1), 1e-02);

        z_EXPECTED = 0.5805;
        Assert.assertEquals(z_EXPECTED, SimulateData.peaks(2, 1), 1e-02);
    }
*/

}
