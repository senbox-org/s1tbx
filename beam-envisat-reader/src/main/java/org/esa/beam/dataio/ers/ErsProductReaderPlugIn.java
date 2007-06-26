/*
 * $Id: ErsProductReaderPlugIn.java,v 1.2 2007/02/09 09:54:49 marcop Exp $
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
package org.esa.beam.dataio.ers;

import org.esa.beam.dataio.envisat.EnvisatProductReaderPlugIn;

import java.util.Locale;

/**
 * The <code>ErsProductReaderPlugIn</code> class is an implementation of the <code>ProductReaderPlugIn</code>
 * interface exclusively for ERS1/2 data products having the standard ESA/ENVISAT raw format.
 * <p/>
 *
 * @author Norman Fomferra
 * @version $Revision: 1.2 $ $Date: 2007/02/09 09:54:49 $
 */
public class ErsProductReaderPlugIn extends EnvisatProductReaderPlugIn {


    /**
     * Constructs a new ERS1/2 product reader plug-in instance.
     */
    public ErsProductReaderPlugIn() {
    }

    /**
     * Returns a string array containing the single entry <code>&quot;ERS1/2&quot;</code>.
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{"ERS1/2"};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".E1", ".E2"};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param name the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(Locale name) {
        return "ERS1/2 AATSR and SAR products";
    }
}
