/*
 * $Id: DimapProductWriterPlugIn.java,v 1.4 2006/10/11 10:36:41 marcop Exp $
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
package org.esa.beam.dataio.dimap;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The <code>DimapProductWriterPlugIn</code> class is the plug-in entry-point for the BEAM-DIMAP product writer.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class DimapProductWriterPlugIn implements ProductWriterPlugIn {

    public final static String DIMAP_FORMAT_NAME = DimapProductConstants.DIMAP_FORMAT_NAME;
    private final BeamFileFilter dimapFileFilter = (BeamFileFilter) DimapProductHelpers.createDimapFileFilter();

    /**
     * Constructs a new BEAM-DIMAP product writer plug-in instance.
     */
    public DimapProductWriterPlugIn() {
    }

    /**
     * Returns a string array containing the single entry <code>&quot;BEAM-DIMAP&quot;</code>.
     */
    public String[] getFormatNames() {
        return new String[]{DIMAP_FORMAT_NAME};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return new String[]{DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION};
    }

    /**
     * Returns an array containing the classes that represent valid output types for this BEAM-DIMAP product writer.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>writeProductNodes</code>
     * method of the <code>AbstractProductWriter</code> interface (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid output types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.AbstractProductWriter#writeProductNodes
     */
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the locale name for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return "BEAM-DIMAP product writer";
    }

    /**
     * Creates an instance of the actual BEAM-DIMAP product writer class.
     *
     * @return a new instance of the <code>DimapProductWriter</code> class
     */
    public ProductWriter createWriterInstance() {
        return new DimapProductWriter(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return dimapFileFilter;
    }
}
