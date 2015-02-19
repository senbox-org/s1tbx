package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class PyOperatorSpiTest {

    static {
        System.setProperty("snap.snappy.ext", "C:\\Users\\Norman\\JavaProjects\\senbox\\snap-engine\\snap-python\\target\\classes\\beampy-examples");
    }

    @Test
    public void testIt() throws Exception {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
        OperatorSpi ndviOp = registry.getOperatorSpi("py_ndvi_op");
        assertNotNull(ndviOp);
    }
}
