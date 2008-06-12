/*
 * $Id: ReaderLoadedAsServiceTest.java,v 1.2 2006/11/02 13:20:03 norman Exp $
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
package org.esa.beam;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.dataio.netcdf.NetcdfReaderPlugIn;

import java.util.Iterator;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ReaderLoadedAsServiceTest extends TestCase {

    public void testReaderIsLoaded() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns("NetCDF");

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(NetcdfReaderPlugIn.class, plugIn.getClass());
        } else {
            fail("Where is NetcdfReaderPlugIn?");
        }

    }

}
