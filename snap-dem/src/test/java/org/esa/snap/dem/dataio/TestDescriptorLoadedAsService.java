/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio;

import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.junit.Test;

import static org.junit.Assert.*;

/**

 */
public class TestDescriptorLoadedAsService {

    @Test
    public void testACEDescriptorIsLoaded() {
        checkDescriptorIsLoaded("ACE30");
    }

    @Test
    public void testSRTM3DescriptorIsLoaded() {
        checkDescriptorIsLoaded("SRTM 3Sec");
    }

    private static void checkDescriptorIsLoaded(String name) {
        final ElevationModelRegistry registry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor descriptor = registry.getDescriptor(name);
        assertNotNull(descriptor);
    }

}
