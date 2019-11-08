/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.paz;

import org.esa.s1tbx.io.terrasarx.TerraSarXProductDirectory;
import org.esa.s1tbx.io.terrasarx.TerraSarXProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;

import java.io.File;

/**
 * The product reader for PAZ products.
 */
public class PazProductReader extends TerraSarXProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public PazProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }


    @Override
    protected TerraSarXProductDirectory createProductDirectory(final File fileFromInput) {
        return new PazProductDirectory(fileFromInput);
    }
}
