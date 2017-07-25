package org.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ResourceInstallerTest {

    private Path sourceDir;
    private Path targetDir;

    @Before
    public void setUp() throws Exception {
        sourceDir = ResourceInstaller.findModuleCodeBasePath(ResourceInstallerTest.class);

        FileSystem targetFs = Jimfs.newFileSystem();
        targetDir = targetFs.getPath("test");
        Files.createDirectories(targetDir);
    }

    @After
    public void tearDown() throws Exception {
        targetDir.getFileSystem().close();
    }

    @Test
    public void testInstall() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir.resolve("org/esa/snap/core/dataio/dimap"), targetDir);
        resourceInstaller.install(".*xml", ProgressMonitor.NULL);
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("spi"));
        assertEquals(4, targetFileList.toArray().length);
    }

    @Test
    public void testInstall_withGlob() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir, targetDir);
        resourceInstaller.install("glob:**/*xml", ProgressMonitor.NULL);
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("org/esa/snap/core/dataio/dimap/spi"));
        assertEquals(4, targetFileList.toArray().length);
    }

    /*
    @Test
    public void testResourcesFromDir() throws Exception {
        URL resource = ResourceInstallerTest.class.getResource("/resource-testdata");
        assertNotNull(resource);
        testResourcesFromClassLoader(new URLClassLoader(new URL[]{resource}));
    }

    @Test
    public void testResourcesFromJar() throws Exception {
        URL resource = ResourceInstallerTest.class.getResource("/resource-testdata.jar");
        assertNotNull(resource);
        testResourcesFromClassLoader(new URLClassLoader(new URL[]{resource}));
    }

    private void testResourcesFromClassLoader(ClassLoader cl) throws URISyntaxException, IOException {
        Enumeration<URL> auxdata = cl.getResources("/auxdata");
        while (auxdata.hasMoreElements()) {
            URL url = auxdata.nextElement();
            System.out.println("url = " + url);
        }

        URL auxdataDirUrl = cl.getResource("auxdata");
        assertNotNull(auxdataDirUrl);
        Path auxdataDir = Paths.get(auxdataDirUrl.toURI());
        assertTrue(Files.isDirectory(auxdataDir));

        List<Path> auxdataDirList = Files.list(auxdataDir).collect(Collectors.toList());
        Collections.sort(auxdataDirList);
        assertEquals(2, auxdataDirList.size());
        assertEquals("file-1.txt", auxdataDirList.get(0).getFileName().toString());
        assertEquals("file-2.txt", auxdataDirList.get(1).getFileName().toString());
        assertEquals("file-3.txt", auxdataDirList.get(2).getFileName().toString());
        assertEquals("subdir", auxdataDirList.get(3).getFileName().toString());

        URL auxdataSubdirUrl = cl.getResource("/auxdata/subdir");
        assertNotNull(auxdataSubdirUrl);
        Path auxdataSubdir = Paths.get(auxdataSubdirUrl.toURI());
        assertTrue(Files.isDirectory(auxdataSubdir));

        List<Path> auxdataSubdirList = Files.list(auxdataSubdir).collect(Collectors.toList());
        Collections.sort(auxdataSubdirList);
        assertEquals(3, auxdataSubdirList.size());
        assertEquals("file-A.txt", auxdataSubdirList.get(0).getFileName().toString());
        assertEquals("file-B.txt", auxdataSubdirList.get(1).getFileName().toString());
        assertEquals("file-C.txt", auxdataSubdirList.get(2).getFileName().toString());
    }
*/
}
