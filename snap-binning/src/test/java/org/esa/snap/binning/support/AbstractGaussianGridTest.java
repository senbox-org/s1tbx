package org.esa.snap.binning.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ralf Quast
 */
public class AbstractGaussianGridTest {

    @Test
    public void testFindNearest() throws Exception {
        final double[] values = {175, 50, -50, -175};
        assertEquals(0, AbstractGaussianGrid.findNearest(values, 178));
        assertEquals(0, AbstractGaussianGrid.findNearest(values, 173));
        assertEquals(1, AbstractGaussianGrid.findNearest(values, 60));
        assertEquals(2, AbstractGaussianGrid.findNearest(values, -30));
        assertEquals(3, AbstractGaussianGrid.findNearest(values, -160));
        assertEquals(3, AbstractGaussianGrid.findNearest(values, -179));
    }

}
