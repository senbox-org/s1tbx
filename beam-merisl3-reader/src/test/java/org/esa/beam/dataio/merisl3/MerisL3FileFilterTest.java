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

package org.esa.beam.dataio.merisl3;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 26.06.2007
 * Time: 17:05:05
 * To change this template use File | Settings | File Templates.
 */
public class MerisL3FileFilterTest extends TestCase {

    public void testFilter() {
        assertTrue(MerisL3FileFilter.isMerisBinnedL3Name("L3_ENV_MER_ABSD_m__20050201_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.nc"));
        assertFalse(MerisL3FileFilter.isMerisBinnedL3Name("L2_ENV_MER_ABSD_m__20050201_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.nc"));
        assertFalse(MerisL3FileFilter.isMerisBinnedL3Name("L3_ENV_MER_ABSD_m__20050201_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.hdf"));
        assertFalse(MerisL3FileFilter.isMerisBinnedL3Name("L2_ENV_MER_ABSD_m__20050201_XXXX_SI_ACR_9277x9277_-90+90+-180+180_0000.nc"));
    }
}
