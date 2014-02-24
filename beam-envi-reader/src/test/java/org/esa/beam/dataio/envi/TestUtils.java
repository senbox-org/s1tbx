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
package org.esa.beam.dataio.envi;

import junit.framework.Assert;
import org.junit.Ignore;

import java.io.File;

@Ignore
class TestUtils {

    private TestUtils() {
    }

    static void deleteFileTree(File treeRoot) {
        final File[] files = treeRoot.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFileTree(file);
                } else if (!file.delete()) {
                    Assert.fail("unable to remove file/directory: " + file.getAbsolutePath());
                }
            }
        }
        treeRoot.delete();
    }
}
