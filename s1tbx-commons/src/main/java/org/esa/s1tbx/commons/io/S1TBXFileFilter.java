/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.io;

import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;

public class S1TBXFileFilter extends SnapFileFilter {

    private final S1TBXProductReaderPlugIn plugIn;

    public S1TBXFileFilter(final S1TBXProductReaderPlugIn plugIn) {
        super();
        this.plugIn = plugIn;

        setFormatName(plugIn.getFormatNames()[0]);
        setExtensions(plugIn.getDefaultFileExtensions());
        setDescription(plugIn.getDescription(null));
    }

    /**
     * Tests whether or not the given file is accepted by this filter. The default implementation returns
     * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
     * if no extension are defined, the method always returns <code>true</code>
     *
     * @param file the file to be or not be accepted.
     * @return <code>true</code> if given file is accepted by this filter
     */
    public boolean accept(final File file) {
        if (super.accept(file)) {
            return file.isDirectory() || plugIn.findMetadataFile(file.toPath()) != null;
        }
        return false;
    }
}
