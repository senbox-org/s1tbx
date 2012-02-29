/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.csv.dataio.reader;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReaderPlugInTest {


    private CsvProductReaderPlugIn csvProductReaderPlugIn;

    @Before
    public void setUp() throws Exception {
        csvProductReaderPlugIn = new CsvProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws Exception {
        final CsvProductReaderPlugIn csvProductReaderPlugIn = new CsvProductReaderPlugIn();
        File validFile = new File("test1.csv");
        assertEquals(DecodeQualification.INTENDED, csvProductReaderPlugIn.getDecodeQualification(validFile));
    }

    @Test
    public void testGetFormatNames() throws Exception {
        assertEquals("CSV", csvProductReaderPlugIn.getFormatNames()[0]);
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("CSV products", csvProductReaderPlugIn.getDescription(null));
    }
}
