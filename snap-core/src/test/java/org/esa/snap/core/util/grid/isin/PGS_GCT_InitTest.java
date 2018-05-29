package org.esa.snap.core.util.grid.isin;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PGS_GCT_InitTest {

    @Test
    public void testInitForward() {
        final double[] projParam = new double[13];
        projParam[0] = 6371007.181;
        projParam[8] = 21600.0;
        projParam[10] = 1.0;

        final IsinForward isinFwd = PGS_GCT_Init.forward(projParam);
        assertNotNull(isinFwd);

    }

    @Test
    public void testInitReverse() {
        final double[] projParam = new double[13];
        projParam[0] = 6371007.181;
        projParam[8] = 43200.0;
        projParam[10] = 1.0;

        PGS_GCT_Init.reverse(projParam);
    }
}
