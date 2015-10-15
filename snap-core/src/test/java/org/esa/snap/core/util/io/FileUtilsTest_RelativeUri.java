package org.esa.snap.core.util.io;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.*;

public class FileUtilsTest_RelativeUri {

    private File root;
    private URI rootURI;

    @Before
    public void setUp() throws Exception {
        root = new File(".").getCanonicalFile();
        rootURI = root.toURI();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testRelativeInTheRootDir() {
        final File relativeFile = new File(root, "lsmf");

        final URI uri = FileUtils.getRelativeUri(rootURI, relativeFile);

        assertEquals("lsmf", uri.toString());
    }

    @Test
    public void testRelativeOneDirDeeper() {
        final File relativeDir = new File(root, "oneDirDeeper");
        final File relativeFile = new File(relativeDir, "lsmf");

        final URI uri = FileUtils.getRelativeUri(rootURI, relativeFile);

        assertEquals("oneDirDeeper/lsmf", uri.toString());
    }

    @Test
    public void testRelativeOneDirHigher() throws IOException {
        final File deeperRootDir = new File(root, "oneDirDeeper");
        final File twoDeeperRootDir = new File(deeperRootDir, "secondDirDeeper");
        final File relativeFileTwoDirsHigher = new File(twoDeeperRootDir, "../../lsmf").getCanonicalFile();

        final URI uri = FileUtils.getRelativeUri(twoDeeperRootDir.toURI(), relativeFileTwoDirsHigher);

        final String path = relativeFileTwoDirsHigher.getCanonicalPath().replace("\\", "/");
        final String expected = "file:" + (path.startsWith("/") ? path : "/" + path);
        assertEquals(expected, uri.toString());
    }
}
