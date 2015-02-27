package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.beam.framework.gpf.main.GPT;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class PyOperatorTest {

    @BeforeClass
    public static void init() {
        PyOperatorSpiTest.init();
    }

    @Ignore
    public static void main(String[] args) throws Exception {
        GPT.main("py_ndvi_op",
                 "-q", "4",
                 "-e",
                 "-PlowerName=radiance_13",
                 "-PupperName=radiance_7",
                 "-Ssource=C:\\Users\\Norman\\EOData\\MER_FRS_1PNMAP20070709_111419_000001722059_00395_28004_0001.N1");
    }

    @Test
    public void testOp() throws Exception {
        Product source = new Product("X", "Y", 100, 100);
        source.addBand("radiance_7", "80.0");
        source.addBand("radiance_13", "120.0");

        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(PyOperatorSpi.class.getResource("/beampy-examples/beampy-ndvi-operator/ndvi_op-info.xml"), getClass().getClassLoader());
        PyOperatorSpi spi = new PyOperatorSpi(descriptor);

        PyOperator operator = new PyOperator();
        operator.setSpi(spi);
        operator.setPythonModulePath(new File(System.getProperty("snap.snappy.ext"), "beampy-ndvi-operator").getPath());
        operator.setPythonModuleName("ndvi_op");
        operator.setPythonClassName("NdviOp");
        operator.setParameter("lowerName", "radiance_13");
        operator.setParameter("upperName", "radiance_7");
        operator.setSourceProduct("source", source);
        Product target = operator.getTargetProduct();

        assertNotNull(target);
        assertNotNull(target.getBand("ndvi"));
        assertNotNull(target.getBand("ndvi_flags"));
    }
}
