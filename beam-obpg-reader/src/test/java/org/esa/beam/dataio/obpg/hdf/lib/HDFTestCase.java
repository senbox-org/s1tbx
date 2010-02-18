/*
 * $Id$
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
package org.esa.beam.dataio.obpg.hdf.lib;

import junit.framework.TestCase;
import org.esa.beam.dataio.obpg.hdf.IHDF;

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
            ihdf[0] = HDF.getInstance();
            super.runBare();
        } finally {
            HDF.setInstance(ihdf[0]);
        }
    }

    /**
     * Use this Method to set the HDF-Mock in a JUnit-Test
     * Use this class to inherit your HDF test.
     *
     * @param mock
     */
    protected void setHdfMock(IHDF mock) {
        HDF.setInstance(mock);
    }
}