package org.esa.snap.core.util.io;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.*;

public class TreeCopierTest {

    private static Path tempDirectory;
    private static Path testZip;

    @BeforeClass
    public static void setUpClass() throws Exception {
        tempDirectory = Files.createTempDirectory(TreeCopierTest.class.getName());
        URI uri = TreeCopierTest.class.getResource("testZip.zip").toURI();
        URI zipUri = URI.create("jar:file:" + uri.getPath() + "!/");
        FileSystems.newFileSystem(zipUri, Collections.emptyMap());
        testZip = Paths.get(zipUri);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        FileUtils.deleteTree(tempDirectory.toFile());
    }

    @Test
    public void testCopy() throws Exception {
        Path dir2 = tempDirectory.resolve("dir2");
        Files.createDirectory(dir2);
        TreeCopier.copy(testZip.resolve("dir2"), dir2);
        assertTrue(Files.exists(dir2.resolve("dir2_1")));
        assertTrue(Files.exists(dir2.resolve("text1.txt")));
        assertTrue(Files.exists(dir2.resolve("text2.txt")));
        assertTrue(Files.exists(dir2.resolve("dir2_1/text1.txt")));
    }

    @Test
    public void testCopyDir() throws Exception {
        Path dir2 = TreeCopier.copyDir(testZip.resolve("dir2"), tempDirectory);
        assertEquals(tempDirectory.resolve("dir2"), dir2);
        assertTrue(Files.exists(dir2.resolve("dir2_1")));
        assertTrue(Files.exists(dir2.resolve("text1.txt")));
        assertTrue(Files.exists(dir2.resolve("text2.txt")));
        assertTrue(Files.exists(dir2.resolve("dir2_1/text1.txt")));
    }
}
