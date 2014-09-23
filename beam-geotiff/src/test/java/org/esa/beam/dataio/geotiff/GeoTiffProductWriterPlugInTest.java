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

package org.esa.beam.dataio.geotiff;

import org.esa.beam.util.io.BeamFileFilter;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class GeoTiffProductWriterPlugInTest {

    private GeoTiffProductWriterPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new GeoTiffProductWriterPlugIn();
    }

    @Test
    public void testFileExtensions() {
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();

        assertNotNull(fileExtensions);
        final List<String> extensionList = Arrays.asList(fileExtensions);
        assertEquals(2, extensionList.size());
        assertEquals(true, extensionList.contains(".tif"));
        assertEquals(true, extensionList.contains(".tiff"));
    }

    @Test
    public void testFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("GeoTIFF", formatNames[0]);
    }

    @Test
    public void testOutputTypes() {
        final Class[] classes = plugIn.getOutputTypes();

        assertNotNull(classes);
        assertEquals(2, classes.length);
        final List<Class> list = Arrays.asList(classes);
        assertEquals(true, list.contains(File.class));
        assertEquals(true, list.contains(String.class));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(),  beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }
}
