/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.ace;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class TestACEReaderPlugIn {

    private ACEReaderPlugIn _plugIn = new ACEReaderPlugIn();

    @Test
    public void testValidInputs() {
        checkValidInput("./ACE/00N015W.ACE");
        checkValidInput("./ACE/00N015W.ACE");
        checkValidInput("./ACE/00N015W.ACE");
    }

    private void checkValidInput(final String s) {
        assertTrue(_plugIn.getDecodeQualification(s) == DecodeQualification.INTENDED);
        assertTrue(_plugIn.getDecodeQualification(new File(s)) == DecodeQualification.INTENDED);
    }

    @Test
    public void testInvalidInputs() {
        checkInvalidInput("10n143w.ACE.zip");
        checkInvalidInput("./ACE/00N015W.ACE.zip");
        checkInvalidInput("./ACE/00N015W.ACE.zip");
        checkInvalidInput("./ACE/readme.txt");
        checkInvalidInput("./ACE/readme.txt.zip");
        checkInvalidInput("./ACE/readme");
        checkInvalidInput("./ACE/");
        checkInvalidInput("./");
        checkInvalidInput(".");
        checkInvalidInput("");
        checkInvalidInput("./ACE/.hgt");
        checkInvalidInput("./ACE/.hgt.zip");
        checkInvalidInput("./ACE/.hgt");
        checkInvalidInput("./ACE/.hgt.zip");
    }

    private void checkInvalidInput(final String s) {
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(s));
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(new File(s)));
    }

    @Test
    public void testThatOtherTypesCannotBeDecoded() throws MalformedURLException {
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(null));
        final URL url = new File("./ACE/readme.txt").toURI().toURL();
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(url));
        final Object object = new Object();
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(object));
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader reader = _plugIn.createReaderInstance();
        assertTrue(reader instanceof ACEReader);
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = _plugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertTrue(inputTypes.length == 2);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = _plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertTrue(formatNames.length == 1);
        assertEquals("ACE", formatNames[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = _plugIn.getDefaultFileExtensions();
        assertNotNull(defaultFileExtensions);
        assertTrue(defaultFileExtensions.length == 1);
        assertEquals(".ACE", defaultFileExtensions[0]);
    }

}
