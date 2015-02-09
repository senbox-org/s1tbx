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
package org.esa.beam.framework.dataio;

import org.esa.beam.util.io.BeamFileFilter;

import java.util.Locale;


/**
 * The <code>ProductIOPlugIn</code> interface is the base for all data product reader or writer plug-ins.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface ProductIOPlugIn {

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    String[] getFormatNames();

    /**
     * Gets the default file extensions associated with each of the format names returned
     * by the {@link #getFormatNames} method. <p>The string array returned
     * shall always have the same length as the array returned by the
     * {@link #getFormatNames} method. <p>The extensions returned in the
     * string array also shall always include a leading colon ('.') character,
     * e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    String[] getDefaultFileExtensions();

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    String getDescription(Locale locale);

    /**
     * Gets an instance of {@link org.esa.beam.util.io.BeamFileFilter} for use in a {@link javax.swing.JFileChooser JFileChooser}.
     *
     * @return a file filter
     */
    BeamFileFilter getProductFileFilter();
}
