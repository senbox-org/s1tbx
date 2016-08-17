package org.esa.snap.python.gpf;


import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.python.PyBridge;
import org.esa.snap.runtime.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * @author Norman Fomferra
 */
public class PyOperatorSpiTest {

    @BeforeClass
    public static void init() {
        File file = PyOperatorTest.getResourceFile("/");
        assertTrue(file.isDirectory());
        Config.instance().preferences().put(PyBridge.PYTHON_EXTRA_PATHS_PROPERTY, file.getPath());
    }

    /*
     * Note that this test will not execute any Python code. It ensures, that the XML descriptor is
     * correctly looked up.
     */
    @Test
    public void testOperatorSpiIsLoaded() throws Exception {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi ndviOpSpi = registry.getOperatorSpi("py_ndvi_op");
        assertNotNull(ndviOpSpi);
        assertEquals("py_ndvi_op", ndviOpSpi.getOperatorAlias());
        assertSame(PyOperator.class, ndviOpSpi.getOperatorClass());
        assertNotNull(ndviOpSpi.getOperatorDescriptor());
        assertEquals("org.esa.snap.python.NdviOp", ndviOpSpi.getOperatorDescriptor().getName());
        assertSame(PyOperator.class, ndviOpSpi.getOperatorDescriptor().getOperatorClass());
    }

    @Test
    public void testPathUriWithSpaces() throws Exception {

        assumeTrue(System.getProperty("os.name").contains("Win"));

        File file1 = new File("C:\\Program Files (x86)");
        URI uri1 = file1.toURI();
        assertEquals("file:/C:/Program%20Files%20(x86)/", uri1.toString());

        Path file2 = Paths.get("C:\\Program Files (x86)");
        URI uri2 = file2.toUri();
        assertEquals("file:///C:/Program%20Files%20(x86)/", uri2.toString());

        // What the heck is this???
        assertEquals(uri1, uri2);
        assertNotEquals(uri1.toString(), uri2.toString());

        Path path1 = Paths.get(uri1);
        assertEquals("C:\\Program Files (x86)", path1.toString());

        Path path2 = Paths.get(URI.create(file2.toUri().toString()));
        assertEquals("C:\\Program Files (x86)", path2.toString());
        assertEquals(path1, path2);
    }

}
