package com.bc.ceres.fs;

import junit.framework.TestCase;

import java.io.File;
import java.util.List;
import java.util.Arrays;


public class DefaultFileNodeFactoryTest extends TestCase {
    public void testFsFolder() {
        DefaultFileNodeFactory fs = new DefaultFileNodeFactory();

        FileNode fn = fs.createFileNode(".");
        assertEquals(true, fn.exists());
        assertEquals(true, fn.isDirectory());
        assertEquals(false, fn.isFile());
        assertEquals(".", fn.getPath());
        assertEquals(new File(".").getParent(), fn.getParent());
        List<String> children = Arrays.asList(fn.list(FileNodeFilter.ALL));
        assertNotNull(children);
        assertTrue(children.contains("pom.xml"));
    }

    public void testFsFile() {
        DefaultFileNodeFactory fs = new DefaultFileNodeFactory();

        FileNode fn = fs.createFileNode("pom.xml");
        assertEquals(true, fn.exists());
        assertEquals(false, fn.isDirectory());
        assertEquals(true, fn.isFile());
        assertEquals("pom.xml", fn.getPath());
        assertEquals(new File("pom.xml").getParent(), fn.getParent());
        String[] children = fn.list(FileNodeFilter.ALL);
        assertNotNull(children);
        assertEquals(0, children.length);
    }

    public void testFsImaginaryFile() {
        DefaultFileNodeFactory fs = new DefaultFileNodeFactory();

        FileNode fn = fs.createFileNode("bogus/foo.txt");
        assertEquals(false, fn.exists());
        assertEquals(false, fn.isDirectory());
        assertEquals(false, fn.isFile());
        assertEquals("bogus" + File.separator + "foo.txt", fn.getPath());
        assertEquals("bogus", fn.getParent());
        String[] children = fn.list(FileNodeFilter.ALL);
        assertNotNull(children);
        assertEquals(0, children.length);
    }
}
