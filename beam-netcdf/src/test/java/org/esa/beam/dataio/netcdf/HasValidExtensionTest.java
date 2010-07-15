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
package org.esa.beam.dataio.netcdf;

import junit.framework.TestCase;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;

public class HasValidExtensionTest extends TestCase {

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testNumberOfExtensions() {
        assertEquals(2, Constants.FILE_EXTENSIONS.length);
    }

    public void testInvalidExtension() {
        assertFalse(ReaderUtils.hasValidExtension("AnyPathname.invalid"));
    }

    public void testValid_Netcdf_Extensions() {
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.nc"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.nC"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.Nc"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.NC"));
    }

    public void testValid_HDF_Extensions() {
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.hdf"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.HDF"));
    }
}
