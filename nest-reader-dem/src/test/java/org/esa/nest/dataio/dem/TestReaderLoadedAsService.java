/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;

import java.util.Iterator;

/**

 */
public class TestReaderLoadedAsService extends TestCase {

    public void testACEReaderIsLoaded() {
        testReaderIsLoaded("ACE");
    }

    public void testSRTM30ReaderIsLoaded() {
        //testReaderIsLoaded("SRTM30");
    }

    private static void testReaderIsLoaded(String name) {
        int readerCount = 0;
        final ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        final Iterator readerPlugIns = plugInManager.getReaderPlugIns(name);

        while (readerPlugIns.hasNext()) {
            readerCount++;
            final ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            //System.out.println("readerPlugIn.Class = " + plugIn.getClass());
            //System.out.println("readerPlugIn.Descr = " + plugIn.getDescription(null));
        }
        Assert.assertEquals(1, readerCount);
    }

}
