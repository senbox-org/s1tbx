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

package org.esa.snap.dataio.geotiff.internal;

import junit.framework.TestCase;

/**
 * TiffDouble Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/21/2005</pre>
 */

public class TiffDoubleTest extends TestCase {

    public void testCreation() {
        new TiffDouble(123497d);
    }

    public void testGetValue() throws Exception {
        final TiffDouble tiffDouble = new TiffDouble(123497d);
        assertEquals(123497d, tiffDouble.getValue(), 1e-10);
    }

    public void testGetSizeInBytes() {
        final TiffDouble tiffDouble = new TiffDouble(932846d);
        assertEquals(8, tiffDouble.getSizeInBytes());
    }
}
