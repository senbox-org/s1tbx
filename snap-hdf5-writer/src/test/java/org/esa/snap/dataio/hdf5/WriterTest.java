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
package org.esa.snap.dataio.hdf5;

import junit.framework.TestCase;

public class WriterTest extends TestCase {

    public void testWriterIsLoadedAsService() {
// todo - this test currently does not work - make it work 
//        int writerCount = 0;
//
//        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
//        Iterator writerPlugIns = plugInManager.getWriterPlugIns("HDF5");
//        while (writerPlugIns.hasNext()) {
//            writerCount++;
//            ProductWriterPlugIn plugIn = (ProductWriterPlugIn) writerPlugIns.next();
//            System.out.println("writerPlugIn.Class = " + plugIn.getClass());
//            System.out.println("writerPlugIn.Descr = " + plugIn.getDescription(null));
//        }
//
//        Assert.assertEquals(1, writerCount);

    }

    public void testHDF5LibraryIsAvailable() {
// todo - this test currently does not work - make it work
//        assertTrue("HDF5 Library not found", Hdf5ProductWriterPlugIn.isHdf5LibAvailable());
    }
}
