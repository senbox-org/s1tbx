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
package org.esa.beam;

import junit.framework.TestCase;
import org.esa.beam.dataio.netcdf.GenericNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos.HdfEosNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.Iterator;

public class ReaderLoadedAsServiceTest extends TestCase {

    public void testGenericReaderIsLoaded() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns(Constants.FORMAT_NAME);

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(GenericNetCdfReaderPlugIn.class, plugIn.getClass());
        } else {
            fail("Where is GenericNetCdfReaderPlugIn?");
        }
    }

    public void testCfReaderIsLoaded() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("NetCDF-CF");

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(CfNetCdfReaderPlugIn.class, plugIn.getClass());
        } else {
            fail("Where is CfNetCdfReaderPlugIn?");
        }
    }

    public void testBeamReaderIsLoaded() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("NetCDF-BEAM");

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(BeamNetCdfReaderPlugIn.class, plugIn.getClass());
        } else {
            fail("Where is BeamNetCdfReaderPlugIn?");
        }
    }

    public void testHdfEosReaderIsLoaded() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("HDF-EOS");

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(HdfEosNetCdfReaderPlugIn.class, plugIn.getClass());
        } else {
            fail("Where is HdfEosNetCdfReaderPlugIn?");
        }
    }
}
