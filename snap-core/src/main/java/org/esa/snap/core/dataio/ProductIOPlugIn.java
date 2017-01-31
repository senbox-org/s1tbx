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
package org.esa.snap.core.dataio;

import org.esa.snap.core.util.io.SnapFileFilter;

import java.util.Locale;


/**
 * The {@code ProductIOPlugIn} interface is the base for all data product reader or writer plug-ins.
 *
 * @author Norman Fomferra
 */
public interface ProductIOPlugIn {

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never {@code null}
     */
    String[] getFormatNames();

    /**
     * Gets the default file extensions associated with each of the format names returned
     * by the {@link #getFormatNames} method. <p>The string array returned
     * shall have the same length as the array returned by the
     * {@link #getFormatNames} method. <p>The extensions returned in the
     * string array also shall always include a leading colon ('.') character,
     * e.g. {@code ".hdf"}. If there is no default files extensions an empty array can
     * be returned.
     *
     * @return the default file extensions for this product I/O plug-in, never {@code null}
     */
    String[] getDefaultFileExtensions();

    /**
     * Gets a short description of this plug-in. If the given locale is set to {@code null} the default locale is
     * used.
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given description string, if {@code null} the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    String getDescription(Locale locale);

    /**
     * Gets an instance of {@link SnapFileFilter} for use in a {@link javax.swing.JFileChooser JFileChooser}.
     *
     * @return a file filter or {@code null} if this plugin doesn't support file filter
     */
    SnapFileFilter getProductFileFilter();
}
