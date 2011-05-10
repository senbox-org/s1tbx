package org.esa.beam.framework.gpf.pointop;

import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Test;

public class PointOperatorInitialiseTest {
    @Test
    public void testInitialiseSequence() throws Exception {
        new SampleOperator() {
            @Override
            protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }
}
