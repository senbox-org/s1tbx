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
package org.esa.beam.dataio.obpg;

import junit.framework.Assert;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.io.IOException;

public class TestUtil {
    public static final String OUT_DIR = "testdata/out";
    public static final String HDF_FILE = OUT_DIR + "/beam-obpg-reader/product.hdf";

    public static void deleteFileTree() {
        final File treeRoot = new File(HDF_FILE).getParentFile();
        SystemUtils.deleteFileTree(treeRoot);
        if (treeRoot.isDirectory()) {
            Assert.fail("test directory could not be removed - check your tests");
        }
    }

    public static File createFile() throws IOException {
        File file = new File(HDF_FILE);
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }

    static boolean isHdfLibraryAvailable() {
        return ObpgProductReaderPlugIn.isHdfLibAvailable();
    }
}
