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
package org.esa.snap;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.dataio.netcdf.GenericNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos.HdfEosNetCdfReaderPlugIn;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ReaderLoadedAsServiceTest {

    @Test
    public void testNetCdfReaderPlugInsAreLoaded() {
        testReaderPlugInLoading("NetCDF", GenericNetCdfReaderPlugIn.class);
        testReaderPlugInLoading("NetCDF-CF", CfNetCdfReaderPlugIn.class);
        testReaderPlugInLoading("NetCDF-BEAM", BeamNetCdfReaderPlugIn.class);
        testReaderPlugInLoading("HDF-EOS", HdfEosNetCdfReaderPlugIn.class);
    }

    private void testReaderPlugInLoading(String formatName, Class<? extends ProductReaderPlugIn> readerPlugInClass) {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns(formatName);

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(readerPlugInClass, plugIn.getClass());
        } else {
            fail(String.format("Where is %s?", readerPlugInClass.getSimpleName()));
        }
    }
}
