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

import java.io.IOException;
import java.util.zip.ZipException;

/**
 * The abstract class <code>AbstractLandsatImageSources</code> is used as a template for clases stores
 * the data of the sources of the satellite images
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public abstract class AbstractLandsatImageSources {

    protected Object[] imageSources;

    protected final LandsatTMFile file;

    /**
     * @param file
     */
    public AbstractLandsatImageSources(final LandsatTMFile file) {
        this.file = file;
    }

    protected final void setImageLocations() throws
                                             ZipException,
                                             IOException {

        if (file.isZipped()) {
            setImageZipEntries();
        } else {
            setImageFiles();
        }
    }

    protected abstract void setImageFiles();

    protected abstract void setImageZipEntries() throws IOException;

    public final int getSize() {
        if (imageSources != null) {
            return imageSources.length;
        } else {
            return 0;
        }
    }

    /**
     * @param index
     *
     * @return the source of the images at a given index
     */
    public final Object getLandsatImageSourceAt(final int index) {
        return imageSources[index];
    }
}
