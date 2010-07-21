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
package org.esa.beam.dataio.modis.hdf.lib;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.hdf.IHDF;

/**
 * This class should be inherited by your test to ensure that the
 * static member {@link HDF#wrap} is set back to its default
 * value after finishing the test.
 */
public abstract class HDFTestCase extends TestCase {

    @Override
    public void runBare() throws Throwable {
        final IHDF[] ihdf = new IHDF[1];
        try {
            ihdf[0] = HDF.getWrap();
            super.runBare();
        } finally {
            HDF.setWrap(ihdf[0]);
        }
    }

    /**
     * Use this Method to set the HDF-Mock in a JUnit-Test
     * Use this class to inherit your HDF test.
     * @param mock
     */
    protected void setHdfMock(IHDF mock) {
        HDF.setWrap(mock);
    }
}
