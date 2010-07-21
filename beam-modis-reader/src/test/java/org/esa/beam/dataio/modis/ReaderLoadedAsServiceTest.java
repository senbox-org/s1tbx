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
package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.Iterator;

public class ReaderLoadedAsServiceTest extends TestCase {

    public void testReaderIsLoaded() {

        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("MODIS");

        // Note: iterator may be empty since MODIS uses native HDF libs which may not be available
        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(ModisProductReaderPlugIn.class, plugIn.getClass());
        }
    }
}
