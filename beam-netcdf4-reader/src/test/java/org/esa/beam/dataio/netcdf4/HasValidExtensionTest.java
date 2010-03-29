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
package org.esa.beam.dataio.netcdf4;

import junit.framework.TestCase;

public class HasValidExtensionTest extends TestCase {

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testNumberOfExtensions() {
        assertEquals(2, Nc4Constants.FILE_EXTENSIONS.length);
    }

    public void testInvalidExtension() {
        assertFalse(Nc4ReaderUtils.hasValidExtension("AnyPathname.invalid"));
    }

    public void testValid_Netcdf_Extensions() {
        assertTrue(Nc4ReaderUtils.hasValidExtension("AnyPathname.nc"));
        assertTrue(Nc4ReaderUtils.hasValidExtension("AnyPathname.nC"));
        assertTrue(Nc4ReaderUtils.hasValidExtension("AnyPathname.Nc"));
        assertTrue(Nc4ReaderUtils.hasValidExtension("AnyPathname.NC"));
    }

    public void testValid_HDF_Extensions() {
        assertTrue(Nc4ReaderUtils.hasValidExtension("AnyPathname.hdf"));
        assertTrue(Nc4ReaderUtils.hasValidExtension("AnyPathname.HDF"));
    }
}
