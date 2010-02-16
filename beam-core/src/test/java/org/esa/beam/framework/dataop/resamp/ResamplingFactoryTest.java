package org.esa.beam.framework.dataop.resamp;


import junit.framework.TestCase;

public class ResamplingFactoryTest extends TestCase {

    public void testCreateResampling() {
        Resampling resampling;

        resampling = ResamplingFactory.createResampling(ResamplingFactory.CUBIC_CONVOLUTION_NAME);
        assertEquals(resampling.getName(), Resampling.CUBIC_CONVOLUTION.getName());

        resampling = ResamplingFactory.createResampling(ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        assertEquals(resampling.getName(), Resampling.NEAREST_NEIGHBOUR.getName());

        resampling = ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        assertEquals(resampling.getName(), Resampling.BILINEAR_INTERPOLATION.getName());

        resampling = ResamplingFactory.createResampling("Not known");
        assertTrue(resampling == null);

    }

}