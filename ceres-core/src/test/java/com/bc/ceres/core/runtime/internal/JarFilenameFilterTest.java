/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
