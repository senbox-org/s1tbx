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
package org.esa.snap.dataio.geotiff.internal;

import org.esa.snap.core.dataio.ProductIOException;

public class GeoKeyHeader {

    private final int keyDirVersion;
    private final int revision;
    private final int minorRevision;
    private final int numKeys;

    public GeoKeyHeader(final int keyDirVersion,
                        final int revision,
                        final int minorRevision,
                        final int numKeys) throws ProductIOException {
        if (keyDirVersion != 1) {
            throw new ProductIOException("The GeoTIFFDirectoryHerader ist not defined.");
        }
        this.keyDirVersion = keyDirVersion;
        this.revision = revision;
        this.minorRevision = minorRevision;
        this.numKeys = numKeys;
    }

    public String getVersion() {
        return "" + revision + "." + minorRevision;
    }

    public int getNumKeys() {
        return numKeys;
    }
}
