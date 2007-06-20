package com.bc.ceres.fs;

import junit.framework.TestCase;

public class VirtualFileSysTest extends TestCase {
    public void testMkdirAndTouchAndDelete() {
        VirtualFileSys fs = new VirtualFileSys();

        fs.mkdirs("/usr/home");
        fs.mkdir("/usr/home/ralf");
        fs.mkdir("/usr/home/norman");

        assertEquals(true, fs.exists("/usr/home"));
        assertEquals(true, fs.exists("/usr/home/ralf"));
        assertEquals(true, fs.exists("/usr/home/norman"));
        assertEquals(false, fs.exists("/usr/home/marco"));

        assertEquals(false, fs.exists("/usr/home/norman/private.txt"));
        fs.createNewFile("/usr/home/norman/private.txt");
        assertEquals(true, fs.exists("/usr/home/norman/private.txt"));

        fs.delete("/usr/home/norman/private.txt");
        assertEquals(false, fs.exists("/usr/home/norman/private.txt"));
        fs.delete("/usr/home/norman");
        assertEquals(false, fs.exists("/usr/home/norman"));
    }

    public void testPathNormalization() {
        VirtualFileSys fs = new VirtualFileSys();

        assertEquals("/usr/home/ralf", fs.createFileNode("/usr/home/ralf").getPath());
        assertEquals("/usr/home/ralf", fs.createFileNode("/usr/home/ralf/").getPath());
        assertEquals("/usr/home/ralf", fs.createFileNode("/usr/home//ralf/").getPath());
        assertEquals("/usr/home/ralf", fs.createFileNode("//usr/home////ralf/").getPath());
        assertEquals("/usr/home/ralf", fs.createFileNode("\\usr/home\\ralf").getPath());
        assertEquals("/usr/home/ralf", fs.createFileNode("\\usr/home////ralf\\\\").getPath());
    }

    public void testGetName() {
        VirtualFileSys fs = new VirtualFileSys();

        assertEquals("ralf", fs.createFileNode("/usr/home/ralf").getName());
        assertEquals("home", fs.createFileNode("/usr/home").getName());
        assertEquals("usr", fs.createFileNode("/usr").getName());
        assertEquals("/", fs.createFileNode("/").getName());
    }

    public void testGetParent() {
        VirtualFileSys fs = new VirtualFileSys();

        assertEquals("/usr/home", fs.createFileNode("/usr/home/ralf").getParent());
        assertEquals("/usr", fs.createFileNode("/usr/home").getParent());
        assertEquals("/", fs.createFileNode("/usr").getParent());
        assertEquals(null, fs.createFileNode("/").getParent());

        assertEquals(null, fs.createFileNode(".").getParent());
        assertEquals("/usr/..", fs.createFileNode("/usr/../ralf").getParent());
    }
}
