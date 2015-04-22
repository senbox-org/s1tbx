package org.esa.snap.gpf.python;


import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.OperatorSpiRegistry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.esa.snap.gpf.python.PyOperatorSpi.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * @author Norman Fomferra
 */
public class PyOperatorSpiTest {

    @BeforeClass
    public static void init() {
        File file = PyOperatorTest.getResourceFile("/");
        assertTrue(file.isDirectory());
        System.setProperty(EXT_PROPERTY_NAME, file.getPath());
        //System.out.printf("%s = %s%n", EXT_PROPERTY_NAME, System.getProperty(EXT_PROPERTY_NAME));
    }

    /*
     * Note that this test will not execute any Python code. It ensures, that the XML descriptor is
     * correctly looked up.
     */
    @Test
    public void testOperatorSpiIsLoaded() throws Exception {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
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

    @Test
    public void testGetPythonModulePath() throws Exception {

        URI fileUri = PyOperatorSpiTest.class.getResource("test.zip").toURI();
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + fileUri), Collections.emptyMap());
        Path zipFsPath = fs.getPath("/");
        Assert.assertEquals(new File(fileUri), PyOperatorSpi.getPythonModuleRootFile(zipFsPath));

        Path dirPath = Paths.get(".").toAbsolutePath().normalize();
        assertEquals(dirPath.toFile(), PyOperatorSpi.getPythonModuleRootFile(dirPath));
    }

}
