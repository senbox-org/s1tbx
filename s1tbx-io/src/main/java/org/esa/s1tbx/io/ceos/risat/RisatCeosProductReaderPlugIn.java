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
package org.esa.s1tbx.io.ceos.risat;

import org.esa.s1tbx.io.ceos.CEOSProductReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;

import java.nio.file.Path;

/**
 * The ReaderPlugIn for CEOS products.
 */
public class RisatCeosProductReaderPlugIn extends CEOSProductReaderPlugIn {

    public RisatCeosProductReaderPlugIn() {
        constants = new RisatCeosConstants();
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new RisatCeosProductReader(this);
    }

    @Override
    protected DecodeQualification checkProductQualification(final Path path) {
        final String name = path.getFileName().toString().toUpperCase();
        if (isVolumeFile(name)) {
            final RisatCeosProductReader reader = new RisatCeosProductReader(this);
            return reader.checkProductQualification(path);
        }
        return DecodeQualification.UNABLE;
    }
}
