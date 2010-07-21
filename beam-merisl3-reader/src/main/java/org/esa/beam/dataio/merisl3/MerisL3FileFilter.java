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

import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;

public class MerisL3FileFilter extends BeamFileFilter {

    public MerisL3FileFilter() {
        super(MerisL3ProductReaderPlugIn.FORMAT_NAME,
              MerisL3ProductReaderPlugIn.FILE_EXTENSION,
              MerisL3ProductReaderPlugIn.FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        if (super.accept(file)) {
            return isMerisBinnedL3Name(file.getName());
        }
        return false;
    }

    /**
     * Checks if the given file name is valid.
     *
     * @param name the file name
     *
     * @return true, if so.
     */
    public static boolean isMerisBinnedL3Name(String name) {
        return name.startsWith("L3_ENV_MER_")
               && name.indexOf("_GLOB_SI_") != -1
               && name.endsWith(MerisL3ProductReaderPlugIn.FILE_EXTENSION);
    }

}
