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

package com.bc.ceres.launcher.internal;

import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import junit.framework.TestCase;

import java.io.File;

public class BruteForceClasspathFactoryTest extends TestCase {

    public void testGetFiles() throws Exception {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        BruteForceClasspathFactory bfcf = new BruteForceClasspathFactory(config);
        final File modulesDir = new File("modules");
        modulesDir.mkdir();
        final File beamHelpDir = new File(modulesDir, "beam-help-4.0");
        beamHelpDir.mkdir();
        try {
            bfcf.processClasspathFile(new File("lib/xpp3-1.3.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(new File("lib/xstream-1.2.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(new File("lib/jdom-1.0.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(new File("lib/jhall-2.0.4.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(new File("lib/ceres-launcher.jar"), BruteForceClasspathFactory.LibType.LIBRARY, 0);
            bfcf.processClasspathFile(new File("modules/ceres-core-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(new File("modules/ceres-ui-1.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(new File("modules/beam-core-4.0.jar"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(new File("modules/beam-help-4.0"), BruteForceClasspathFactory.LibType.MODULE, 0);
            bfcf.processClasspathFile(new File("modules/beam-help-4.0/lib/lib.jar"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(new File("modules/beam-help-4.0/lib/tile.zip"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(new File("modules/beam-help-4.0/foo/foo.jar"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(new File("modules/beam-help-4.0/dgg/tile.zip"), BruteForceClasspathFactory.LibType.MODULE, 2);
            bfcf.processClasspathFile(new File("modules/beam-help-4.0/first-level.jar"), BruteForceClasspathFactory.LibType.MODULE, 1);
            File[] files = bfcf.getClasspathFiles();
            assertEquals(10, files.length);
            assertEquals(new File("lib/xpp3-1.3.4.jar"), files[0]);
            assertEquals(new File("lib/xstream-1.2.jar"), files[1]);
            assertEquals(new File("lib/jdom-1.0.jar"), files[2]);
            assertEquals(new File("lib/jhall-2.0.4.jar"), files[3]);
            assertEquals(new File("modules/ceres-core-1.0.jar"), files[4]);
            assertEquals(new File("modules/ceres-ui-1.0.jar"), files[5]);
            assertEquals(new File("modules/beam-core-4.0.jar"), files[6]);
            assertEquals(new File("modules/beam-help-4.0"), files[7]);
            assertEquals(new File("modules/beam-help-4.0/lib/lib.jar"), files[8]);
            assertEquals(new File("modules/beam-help-4.0/lib/tile.zip"), files[9]);
        } finally {
            beamHelpDir.delete();
            modulesDir.delete();
        }
    }
}
