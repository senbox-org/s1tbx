package org.esa.snap.python.gpf;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.snap.core.gpf.main.GPT;
import org.esa.snap.core.util.io.TreeDeleter;
import org.esa.snap.python.PyBridge;
import org.esa.snap.runtime.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * @author Norman Fomferra
 */
public class PyOperatorTest {

    @BeforeClass
    public static void init() {
        File moduleDir = PyOperatorTest.getResourceFile("/");
        assertTrue(moduleDir.isDirectory());

        // e.g. use -Dsnap.pythonExecutable=C:/Python34/python.exe
        String pythonExe = Config.instance().preferences().get(PyBridge.PYTHON_EXECUTABLE_PROPERTY, null);

        assumeTrue(String.format("Please set '%s' to execute this test", PyBridge.PYTHON_EXECUTABLE_PROPERTY),
                   pythonExe != null);

        Config.instance().preferences().put(PyBridge.FORCE_PYTHON_CONFIG_PROPERTY, "false");
        Config.instance().preferences().put(PyBridge.PYTHON_MODULE_DIR_PROPERTY, moduleDir.getPath());

        PyOperatorSpiTest.init();
    }

    @Test
    public void testPythonOperatorWithGPT() throws Exception {

        String targetName = "ndvi_" + Long.toHexString(System.currentTimeMillis());
        Path targetDim = Paths.get(targetName + ".dim");
        Path targetData = Paths.get(targetName + ".data");

        try {
            GPT.main("py_ndvi_op",
                     "-q", "4",
                     "-e",
                     "-t", targetDim.toString(),
                     "-PlowerName=radiance_13",
                     "-PupperName=radiance_7",
                     "-Ssource=" + getResourceFile("/snappy/testdata/MER_FRS_L1B_SUBSET.dim"));

            assertTrue(Files.isRegularFile(targetDim));
            assertTrue(Files.isDirectory(targetData));

            assertTrue(Files.isRegularFile(targetData.resolve("ndvi.hdr")));
            assertTrue(Files.isRegularFile(targetData.resolve("ndvi.img")));
            assertEquals(162656L, Files.size(targetData.resolve("ndvi.img")));

            assertTrue(Files.isRegularFile(targetData.resolve("ndvi_flags.hdr")));
            assertTrue(Files.isRegularFile(targetData.resolve("ndvi_flags.img")));
            assertEquals(40664L, Files.size(targetData.resolve("ndvi_flags.img")));
        } finally {
            Files.deleteIfExists(targetDim);
            if (Files.isDirectory(targetData)) {
                TreeDeleter.deleteDir(targetData);
            }
        }
    }

    @Test
    public void testPythonOperatorInstantiationAndInvocation() throws Exception {

        File moduleDir = PyOperatorTest.getResourceFile("/snappy_ndvi_op");
        assertTrue(moduleDir.isDirectory());

        Product source = new Product("N", "T", 100, 100);
        source.addBand("radiance_7", "120.0");  // upper wavelength
        source.addBand("radiance_13", "50.0"); // lower wavelength

        URL infoXmlFile = PyOperatorSpi.class.getResource("/snappy_ndvi_op/ndvi_op-info.xml");
        DefaultOperatorDescriptor descriptor = DefaultOperatorDescriptor.fromXml(infoXmlFile, getClass().getClassLoader());
        PyOperatorSpi spi = new PyOperatorSpi(descriptor);

        PyOperator operator = new PyOperator();
        operator.setSpi(spi);
        operator.setParameterDefaultValues();
        operator.setPythonModulePath(moduleDir.getPath());
        operator.setPythonModuleName("ndvi_op");
        operator.setPythonClassName("NdviOp");
        operator.setParameter("lowerName", "radiance_13");
        operator.setParameter("upperName", "radiance_7");
        operator.setSourceProduct("source", source);
        Product target = operator.getTargetProduct();

        assertNotNull(target);
        assertNotNull(target.getBand("ndvi"));
        assertNotNull(target.getBand("ndvi_flags"));

        assertEquals((120f - 50f) / (120f + 50f), target.getBand("ndvi").readPixels(10, 10, 1, 1, (float[]) null)[0], 1e-5f);
        assertEquals(2, target.getBand("ndvi_flags").readPixels(10, 10, 1, 1, (int[]) null)[0]);
    }

    public static File getResourceFile(String name) {
        URL resource = PyOperator.class.getResource(name);
        assertNotNull("missing resource '" + name + "'", resource);
        return new File(URI.create(resource.toString()));
    }
}
