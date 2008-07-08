/*
 * $Id$
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.obpg;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;

import java.util.Locale;

public class ObpgProductReaderPlugIn_Test extends TestCase {

    private ObpgProductReaderPlugIn plugIn;

    protected void setUp() throws Exception {
        plugIn = new ObpgProductReaderPlugIn();
    }

    public void testDefaultFileExtensions() {
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();

        assertNotNull(fileExtensions);
        assertEquals(1, fileExtensions.length);
        assertEquals(".hdf", fileExtensions[0]);
    }

    public void testCreateReaderInstance() {
        final ProductReader productReader = plugIn.createReaderInstance();

        assertNotNull(productReader);
        assertTrue(productReader instanceof AbstractProductReader);
        assertTrue(productReader instanceof ObpgProductReader);
    }

    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("NASA-OBPG", formatNames[0]);
    }

    public void testGetInputTypes() {
        final Class[] classes = plugIn.getInputTypes();

        assertNotNull(classes);
        assertEquals(2, classes.length);
        assertEquals("java.lang.String", classes[0].getName());
        assertEquals("java.io.File", classes[1].getName());
    }

    public void test() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        final String[] extensions = beamFileFilter.getExtensions();
        assertNotNull(extensions);
        assertEquals(1, extensions.length);
        assertEquals(".hdf", extensions[0]);
        assertEquals("NASA-OBPG", beamFileFilter.getFormatName());
    }
}