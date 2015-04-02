package org.esa.beam.util;

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
    public void testInstall_withRelativePath() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir, "org/esa/beam/dataio/dimap", targetDir);
        resourceInstaller.install(".*xml", ProgressMonitor.NULL);
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("spi"));
        assertEquals(4, targetFileList.toArray().length);
    }

   @Test
    public void testInstall_withoutRelativePath() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir.resolve("org/esa/beam/dataio/dimap"), "", targetDir);
        resourceInstaller.install(".*xml", ProgressMonitor.NULL);
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("spi"));
        assertEquals(4, targetFileList.toArray().length);
    }

   @Test
    public void testInstall_withRegex() throws Exception {
        ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDir, "", targetDir);
        resourceInstaller.install("glob:**/*xml", ProgressMonitor.NULL);
        assertEquals(1, Files.list(targetDir).toArray().length);
        Stream<Path> targetFileList = Files.list(targetDir.resolve("org/esa/beam/dataio/dimap/spi"));
        assertEquals(4, targetFileList.toArray().length);
   }
}