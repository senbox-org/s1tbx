package org.esa.snap.binning.aggregators;

import org.junit.Assert;
import org.junit.Test;

public class PercentileTest {
    @Test
    public void testPercentileComputationFollowingNIST() {
        /*
            http://www.itl.nist.gov/div898/handbook/prc/section2/prc252.htm

             i  Measurements  Order stats   Ranks

             1     95.1772     95.0610       9
             2     95.1567     95.0925       6
             3     95.1937     95.1065       10
             4     95.1959     95.1195       11
             5     95.1442     95.1442        5
             6     95.0610     95.1567        1
             7     95.1591     95.1591        7
             8     95.1195     95.1682        4
             9     95.1065     95.1772        3
            10     95.0925     95.1937        2
            11     95.1990     95.1959       12
            12     95.1682     95.1990        8

            To find the 90% percentile, p(N+1) = 0.9(13) =11.7; k = 11, and d = 0.7.
            From condition (1) above, Y(0.90) is estimated to be 95.1981 ohm.cm. This
            percentile, although it is an estimate from a small sample of resistivities
            measurements, gives an indication of the percentile for a
            population of resistivity measurements.
         */

        Assert.assertEquals(95.0610F, computePercentile(0), 1E-4F);
        Assert.assertEquals(95.1579F, computePercentile(50), 1E-4F);
        Assert.assertEquals(95.1981F, computePercentile(90), 1E-4F);
        Assert.assertEquals(95.1990F, computePercentile(100), 1E-4F);
    }

    private float computePercentile(int p) {
        return AggregatorPercentile.computePercentile(p, new float[]{
                95.0610F,
                95.0925F,
                95.1065F,
                95.1195F,
                95.1442F,
                95.1567F,
                95.1591F,
                95.1682F,
                95.1772F,
                95.1937F,
                95.1959F,
                95.1990F,
        });
    }
}
