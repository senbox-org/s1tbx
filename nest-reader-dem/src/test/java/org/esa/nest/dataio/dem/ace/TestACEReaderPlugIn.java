/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem.ace;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


public class TestACEReaderPlugIn extends TestCase {

    private ACEReaderPlugIn _plugIn;

    @Override
    protected void setUp() throws Exception {
        _plugIn = new ACEReaderPlugIn();
    }

    @Override
    protected void tearDown() throws Exception {
        _plugIn = null;
    }

    public void testValidInputs() {
        testValidInput("./ACE/00N015W.ACE");
        testValidInput("./ACE/00N015W.ACE");
        testValidInput("./ACE/00N015W.ACE");
    }

    private void testValidInput(final String s) {
        assertTrue(_plugIn.getDecodeQualification(s) == DecodeQualification.INTENDED);
        assertTrue(_plugIn.getDecodeQualification(new File(s)) == DecodeQualification.INTENDED);
    }

    public void testInvalidInputs() {
        testInvalidInput("10n143w.ACE.zip");
        testInvalidInput("./ACE/00N015W.ACE.zip");
        testInvalidInput("./ACE/00N015W.ACE.zip");
        testInvalidInput("./ACE/readme.txt");
        testInvalidInput("./ACE/readme.txt.zip");
        testInvalidInput("./ACE/readme");
        testInvalidInput("./ACE/");
        testInvalidInput("./");
        testInvalidInput(".");
        testInvalidInput("");
        testInvalidInput("./ACE/.hgt");
        testInvalidInput("./ACE/.hgt.zip");
        testInvalidInput("./ACE/.hgt");
        testInvalidInput("./ACE/.hgt.zip");
    }

    private void testInvalidInput(final String s) {
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(s));
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(new File(s)));
    }

    public void testThatOtherTypesCannotBeDecoded() throws MalformedURLException {
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(null));
        final URL url = new File("./ACE/readme.txt").toURI().toURL();
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(url));
        final Object object = new Object();
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(object));
    }

    public void testCreateReaderInstance() {
        final ProductReader reader = _plugIn.createReaderInstance();
        assertTrue(reader instanceof ACEReader);
    }

    public void testGetInputTypes() {
        final Class[] inputTypes = _plugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertTrue(inputTypes.length == 2);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    public void testGetFormatNames() {
        final String[] formatNames = _plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertTrue(formatNames.length == 1);
        assertEquals("ACE", formatNames[0]);
    }

    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = _plugIn.getDefaultFileExtensions();
        assertNotNull(defaultFileExtensions);
        assertTrue(defaultFileExtensions.length == 1);
        assertEquals(".ACE", defaultFileExtensions[0]);
    }

}
