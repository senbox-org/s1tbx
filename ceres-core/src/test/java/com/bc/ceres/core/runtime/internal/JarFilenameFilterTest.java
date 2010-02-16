package com.bc.ceres.core.runtime.internal;

import junit.framework.TestCase;

import java.io.File;

public class JarFilenameFilterTest extends TestCase {

    public void testNPE(String name) {
        try {
            JarFilenameFilter.isJarName(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void testAccept() {
        JarFilenameFilter filter = new JarFilenameFilter();
        File dir = new File(".");
        assertTrue(filter.accept(dir, "xstream.jar"));
        assertTrue(filter.accept(dir, "xstream.JAR"));
        assertTrue(filter.accept(dir, "xstream.zip"));
        assertTrue(filter.accept(dir, "xstream.ZIP"));
        assertTrue(filter.accept(dir, "lib/xstream.jar"));
        assertTrue(filter.accept(dir, "lib/xstream.JAR"));
        assertTrue(filter.accept(dir, "lib/xstream.zip"));
        assertTrue(filter.accept(dir, "lib/xstream.ZIP"));
        assertFalse(filter.accept(dir, "xstream"));
        assertFalse(filter.accept(dir, "xstream.txt"));
        assertFalse(filter.accept(dir, "xstream.JaR"));
        assertFalse(filter.accept(dir, ".jar"));
        assertFalse(filter.accept(dir, ""));
    }

    public void testIsJarName() {
        assertTrue(JarFilenameFilter.isJarName("xstream.jar"));
        assertTrue(JarFilenameFilter.isJarName("xstream.JAR"));
        assertTrue(JarFilenameFilter.isJarName("xstream.zip"));
        assertTrue(JarFilenameFilter.isJarName("xstream.ZIP"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.jar"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.JAR"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.zip"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.ZIP"));
        assertFalse(JarFilenameFilter.isJarName("xstream"));
        assertFalse(JarFilenameFilter.isJarName("xstream.txt"));
        assertFalse(JarFilenameFilter.isJarName("xstream.JaR"));
        assertFalse(JarFilenameFilter.isJarName(".jar"));
        assertFalse(JarFilenameFilter.isJarName(""));
    }
}
