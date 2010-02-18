/*
 * $Id: WriterTest.java,v 1.2 2006/10/05 13:22:21 marcop Exp $
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
package org.esa.beam.dataio.hdf5;

import junit.framework.TestCase;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
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
