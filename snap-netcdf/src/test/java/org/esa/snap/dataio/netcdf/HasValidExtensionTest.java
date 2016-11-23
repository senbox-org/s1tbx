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
package org.esa.snap.dataio.netcdf;

import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class HasValidExtensionTest {

    @Test
    public void testNumberOfExtensions() {
        assertEquals(5, Constants.FILE_EXTENSIONS.length);
    }

    @Test
    public void testInvalidExtension() {
        assertFalse(ReaderUtils.hasValidExtension("AnyPathname.invalid"));
    }

    @Test
    public void testValid_Netcdf_Extensions() {
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.nc"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.nC"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.Nc"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.Nc.gz"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.Nc"));
    }

    @Test
    public void testValid_HDF_Extensions() {
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.hdf"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.HDF"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.HDF.gz"));
        assertTrue(ReaderUtils.hasValidExtension("AnyPathname.hdf.bz2"));
    }
}
