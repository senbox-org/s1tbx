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

import java.text.MessageFormat;

public class ObpgProductReaderPlugIn_Test extends TestCase {

    private ObpgProductReaderPlugIn plugIn;

    @Override
    protected void setUp() throws Exception {
        if (!TestUtil.isHdfLibraryAvailable()) {
            return;
        }

        plugIn = new ObpgProductReaderPlugIn();
    }

    public void testDefaultFileExtensions() {
        if (!TestUtil.isHdfLibraryAvailable()) {
            System.out.println(MessageFormat.format(
                    "Skipping test in class ''{0}'' since HDF library is not available", getClass().getName()));
            return;
        }
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();

        assertNotNull(fileExtensions);
        assertEquals(3, fileExtensions.length);
        assertEquals(".hdf", fileExtensions[0]);
        assertEquals(".L2_LAC", fileExtensions[1]);
        assertEquals(".L2_MLAC", fileExtensions[2]);
    }

    public void testCreateReaderInstance() {
        if (!TestUtil.isHdfLibraryAvailable()) {
            System.out.println(MessageFormat.format(
                    "Skipping test in class ''{0}'' since HDF library is not available", getClass().getName()));
            return;
        }
        final ProductReader productReader = plugIn.createReaderInstance();

        assertNotNull(productReader);
        assertTrue(productReader instanceof AbstractProductReader);
        assertTrue(productReader instanceof ObpgProductReader);
    }

    public void testGetFormatNames() {
        if (!TestUtil.isHdfLibraryAvailable()) {
            System.out.println(MessageFormat.format(
                    "Skipping test in class ''{0}'' since HDF library is not available", getClass().getName()));
            return;
        }
        final String[] formatNames = plugIn.getFormatNames();

        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("NASA-OBPG", formatNames[0]);
    }

    public void testGetInputTypes() {
        if (!TestUtil.isHdfLibraryAvailable()) {
            System.out.println(MessageFormat.format(
                    "Skipping test in class ''{0}'' since HDF library is not available", getClass().getName()));
            return;
        }
        final Class[] classes = plugIn.getInputTypes();

        assertNotNull(classes);
        assertEquals(2, classes.length);
        assertEquals("java.lang.String", classes[0].getName());
        assertEquals("java.io.File", classes[1].getName());
    }

    public void test() {
        if (!TestUtil.isHdfLibraryAvailable()) {
            System.out.println(MessageFormat.format(
                    "Skipping test in class ''{0}'' since HDF library is not available", getClass().getName()));
            return;
        }
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        final String[] extensions = beamFileFilter.getExtensions();
        assertNotNull(extensions);
        assertEquals(3, extensions.length);
        assertEquals(".hdf", extensions[0]);
        assertEquals(".L2_LAC", extensions[1]);
        assertEquals(".L2_MLAC", extensions[2]);
        assertEquals("NASA-OBPG", beamFileFilter.getFormatName());
    }

}