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

package org.esa.snap.core.util.io;

import junit.framework.TestCase;

import java.io.File;

public class SnapFileFilterTest extends TestCase {
    public void testSingleExt() {
        testSingleExt(new SnapFileFilter("X", "xml", "X files"));
        testSingleExt(new SnapFileFilter("X", new String[]{"xml"}, "X files"));
    }

    public void testMultiExt() {
        testMultiExt(new SnapFileFilter("X", "xml,zip", "X files"));
        testMultiExt(new SnapFileFilter("X", new String[]{".xml",".zip"}, "X files"));
    }

    public void testNoExt() {
        testNoExt(new SnapFileFilter("X", "", "X files"));
        testNoExt(new SnapFileFilter("X", new String[0], "X files"));
    }

    private void testSingleExt(SnapFileFilter fileFilter) {
        assertEquals("X", fileFilter.getFormatName());
        assertNotNull(fileFilter.getExtensions());
        assertEquals(1, fileFilter.getExtensions().length);
        assertEquals(".xml", fileFilter.getExtensions()[0]);
        assertEquals(".xml", fileFilter.getDefaultExtension());
        assertEquals("X files (*.xml)", fileFilter.getDescription());
        assertEquals(true, fileFilter.accept(new File(".")));
        assertEquals(true, fileFilter.accept(new File("./a.xml")));
        assertEquals(false, fileFilter.accept(new File("./a.zip")));
        assertEquals(false, fileFilter.accept(new File("./a.txt")));
    }

    private void testMultiExt(SnapFileFilter fileFilter) {
        assertEquals("X", fileFilter.getFormatName());
        assertNotNull(fileFilter.getExtensions());
        assertEquals(2, fileFilter.getExtensions().length);
        assertEquals(".xml", fileFilter.getExtensions()[0]);
        assertEquals(".zip", fileFilter.getExtensions()[1]);
        assertEquals(".xml", fileFilter.getDefaultExtension());
        assertEquals("X files (*.xml,*.zip)", fileFilter.getDescription());
        assertEquals(true, fileFilter.accept(new File(".")));
        assertEquals(true, fileFilter.accept(new File("./a.xml")));
        assertEquals(true, fileFilter.accept(new File("./a.zip")));
        assertEquals(false, fileFilter.accept(new File("./a.txt")));
    }

    private void testNoExt(SnapFileFilter fileFilter) {
        assertEquals("X", fileFilter.getFormatName());
        assertNotNull(fileFilter.getExtensions());
        assertEquals(0, fileFilter.getExtensions().length);
        assertEquals(null, fileFilter.getDefaultExtension());
        assertEquals("X files", fileFilter.getDescription());
        assertEquals(true, fileFilter.accept(new File(".")));
        assertEquals(true, fileFilter.accept(new File("./a.xml")));
        assertEquals(true, fileFilter.accept(new File("./a.zip")));
        assertEquals(true, fileFilter.accept(new File("./a.txt")));
    }


    public void testDefaultConstructor() {
        final SnapFileFilter f = new SnapFileFilter();
        assertNull(f.getFormatName());
        assertNull(f.getDescription());
        assertNull(f.getDefaultExtension());
        assertNull(f.getExtensions());
        assertFalse(f.hasExtensions());
    }

    public void testSingleExtConstructor() {
        final SnapFileFilter f = new SnapFileFilter("RALLA", ".ral", "RALLA Files");
        assertEquals("RALLA", f.getFormatName());
        assertEquals("RALLA Files (*.ral)", f.getDescription());
        assertTrue(f.hasExtensions());
        assertEquals(".ral", f.getDefaultExtension());
        assertEquals(1, f.getExtensions().length);
        assertEquals(".ral", f.getExtensions()[0]);
    }

    public void testMultipleExtConstructor() {
        final SnapFileFilter f = new SnapFileFilter("Holla", new String[]{".hol", ".ho", ".holla"}, "Holla Files");
        assertEquals("Holla", f.getFormatName());
        assertEquals("Holla Files (*.hol,*.ho,*.holla)", f.getDescription());
        assertTrue(f.hasExtensions());
        assertEquals(".hol", f.getDefaultExtension());
        assertEquals(3, f.getExtensions().length);
        assertEquals(".hol", f.getExtensions()[0]);
        assertEquals(".ho", f.getExtensions()[1]);
        assertEquals(".holla", f.getExtensions()[2]);
    }

    public void testConstructorsBehaveEqualWithEmptyExtension() {
        SnapFileFilter fileFilter = new SnapFileFilter("All", "", "No Extension");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(null, fileFilter.getDefaultExtension());
        assertEquals("No Extension", fileFilter.getDescription());
        assertEquals(0, fileFilter.getExtensions().length);

        fileFilter = new SnapFileFilter("All", new String[]{""}, "No Extension");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(null, fileFilter.getDefaultExtension());
        assertEquals("No Extension", fileFilter.getDescription());
        assertEquals(0, fileFilter.getExtensions().length);

        fileFilter = new SnapFileFilter("All", ".42, ,uni", "One Empty");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(".42", fileFilter.getDefaultExtension());
        assertEquals("One Empty (*.42,*.uni)", fileFilter.getDescription());
        assertEquals(2, fileFilter.getExtensions().length);

        fileFilter = new SnapFileFilter("All", new String[]{".42", "", "uni"}, "One Empty");
        assertEquals("All", fileFilter.getFormatName());
        assertEquals(".42", fileFilter.getDefaultExtension());
        assertEquals("One Empty (*.42,*.uni)", fileFilter.getDescription());
        assertEquals(2, fileFilter.getExtensions().length);
    }

    public void testSetters() {
        final SnapFileFilter f = new SnapFileFilter();
        f.setFormatName("Zappo");
        f.setDescription("Zappo File Format");
        f.setExtensions(new String[]{".zap", ".ZAPPO"});

        assertEquals("Zappo", f.getFormatName());
        assertEquals("Zappo File Format", f.getDescription());
        assertTrue(f.hasExtensions());
        assertEquals(".zap", f.getDefaultExtension());
        assertEquals(2, f.getExtensions().length);
        assertEquals(".zap", f.getExtensions()[0]);
        assertEquals(".ZAPPO", f.getExtensions()[1]);
    }

    public void testSingleExtIgnoreCase() {
        final SnapFileFilter f = new SnapFileFilter("RALLA", ".ral", "RALLA Files");

        assertTrue(f.accept(new File("my_ralla.ral")));
        assertTrue(f.accept(new File("my_ralla.RAL")));
        assertTrue(f.accept(new File("my_ralla.Ral")));
    }

    public void testMultipleExtIgnoreCase() {
        final SnapFileFilter f = new SnapFileFilter("RALLA", new String[]{".ral", ".lar"}, "RALLA Files");

        assertTrue(f.accept(new File("my_ralla.ral")));
        assertTrue(f.accept(new File("my_ralla.RAL")));
        assertTrue(f.accept(new File("my_ralla.Ral")));
        assertTrue(f.accept(new File("my_ralla.lar")));
        assertTrue(f.accept(new File("my_ralla.LAR")));
        assertTrue(f.accept(new File("my_ralla.Lar")));
    }

    public void testThatExtensionsIgnoreCase() {
        final SnapFileFilter f = new SnapFileFilter("RALLA", new String[]{".ral", ".ral.zip"}, "RALLA Files");

        assertTrue(f.accept(new File("my_ralla.ral")));
        assertTrue(f.accept(new File("my_ralla.RAL")));
        assertTrue(f.accept(new File("my_ralla.Ral")));
        assertTrue(f.accept(new File("my_ralla.ral.zip")));
        assertTrue(f.accept(new File("my_ralla.ral.ZIP")));
        assertTrue(f.accept(new File("my_ralla.RAL.Zip")));
    }

}
