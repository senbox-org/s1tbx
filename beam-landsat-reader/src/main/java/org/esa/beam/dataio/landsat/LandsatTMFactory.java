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


package org.esa.beam.dataio.landsat;

import org.esa.beam.dataio.landsat.ceos.Landsat5CEOS;
import org.esa.beam.dataio.landsat.fast.Landsat5FAST;
import org.esa.beam.framework.dataio.IllegalFileFormatException;

import java.io.File;
import java.io.IOException;

/**
 * The class <code>LandsatFormatManager</code> is used to accomblish all Landsat formats in one common java object
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */

final class LandsatTMFactory {


    private final LandsatTMFile inputFile;


    /**
     * create a landsatTM Object
     *
     * @param inputFile
     *
     * @throws IOException
     */
    LandsatTMFactory(final File inputFile) throws
                                           IOException {
        this.inputFile = new LandsatTMFile(inputFile);
    }

    /**
     * Landsat factory method. Creates an appropiate format object
     *
     * @return data format object
     */
    public final LandsatTMData createLandsatTMObject() throws IOException {
        if (inputFile.getFormat() == LandsatConstants.FAST_L5) {
            return new Landsat5FAST(inputFile);
        } else if (inputFile.getFormat() == LandsatConstants.CEOS) {
            return new Landsat5CEOS(inputFile);
        } else {
            throw new IllegalFileFormatException("Unknown data format.");
        }
    }

}
