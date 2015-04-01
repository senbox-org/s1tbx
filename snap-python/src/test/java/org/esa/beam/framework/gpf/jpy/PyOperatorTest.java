package org.esa.beam.framework.gpf.jpy;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.beam.framework.gpf.main.GPT;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.esa.beam.framework.gpf.jpy.PyOperatorSpi.EXT_PROPERTY_NAME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

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
        PyOperatorSpiTest.init();
        GPT.main("py_ndvi_op",
                 "-q", "4",
                 "-e",
                 "-PlowerName=radiance_13",
                 "-PupperName=radiance_7",
                 "-Ssource=C:\\Users\\Norman\\EOData\\MER_FRS_1PNMAP20070709_111419_000001722059_00395_28004_0001.N1");
    }

    @Ignore
    //@Test
    public void testOpInstantiationAndInvocation() throws Exception {
        // e.g. use -Dsnap.pythonExecutable=C:/Python34/python.exe
        assumeTrue(String.format("Please set '%s' to execute this test", PyBridge.PYTHON_EXECUTABLE_PROPERTY),
                   System.getProperty(PyBridge.PYTHON_EXECUTABLE_PROPERTY) != null);

        Product source = new Product("X", "Y", 100, 100);
        source.addBand("radiance_7", "80.0");
        source.addBand("radiance_13", "120.0");

        URL infoXmlFile = PyOperatorSpi.class.getResource("/beampy-ndvi-operator/ndvi_op-info.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(infoXmlFile, getClass().getClassLoader());
        PyOperatorSpi spi = new PyOperatorSpi(descriptor);

        PyOperator operator = new PyOperator();
        operator.setSpi(spi);
        operator.setPythonModulePath(new File(System.getProperty(EXT_PROPERTY_NAME), "beampy-ndvi-operator").getPath());
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
