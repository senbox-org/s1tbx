package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class PyOperatorSpiTest {

    @BeforeClass
    public static void init() {
        URL resource = PyOperatorSpi.class.getResource("/beampy-examples");
        assertNotNull(resource);
        File file = new File(URI.create(resource.toString()));
        assertNotNull(file.isDirectory());
        System.setProperty("snap.snappy.ext", file.getPath());
        System.out.println("snap.snappy.ext = " + System.getProperty("snap.snappy.ext"));
    }

    @Test
    public void testIt() throws Exception {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
        OperatorSpi ndviOp = registry.getOperatorSpi("py_ndvi_op");
        assertNotNull(ndviOp);
    }
}
