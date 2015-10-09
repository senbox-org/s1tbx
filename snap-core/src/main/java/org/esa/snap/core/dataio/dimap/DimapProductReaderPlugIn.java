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
package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * The <code>DimapProductReaderPlugIn</code> class is an implementation of the <code>ProductReaderPlugIn</code>
 * interface exclusively for data products having the BEAM-DIMAP product format.
 * <p>XMLDecoder plug-ins are used to provide meta-information about a particular data format and to create instances of
 * the actual reader objects.
 * <p>
 * The BEAM-DIMAP version history is provided in the API doc of the {@link DimapProductWriterPlugIn}.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see DimapProductReader
 */
public class DimapProductReaderPlugIn implements ProductReaderPlugIn {

    private final SnapFileFilter dimapFileFilter = (SnapFileFilter) DimapProductHelpers.createDimapFileFilter();
    private ArrayList<DimapProductReader.ReaderExtender> readerExtenders;

    /**
     * Constructs a new BEAM-DIMAP product reader plug-in instance.
     */
    public DimapProductReaderPlugIn() {
    }

    /**
     * Returns a string array containing the single entry <code>&quot;BEAM-DIMAP&quot;</code>.
     */
    public String[] getFormatNames() {
        return new String[]{DimapProductConstants.DIMAP_FORMAT_NAME};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same length as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return new String[]{DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param name the local for the given description string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale name) {
        return "DIMAP (BEAM profile) product reader"; /*I18N*/
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if
     * it's content has the BEAM-DIMAP format.
     * <p> BEAM-DIMAP product readers accept <code>java.lang.String</code> - a file path or a <code>java.io.File</code>
     * - an abstract file path.
     *
     * @param object the input object
     * @return <code>true</code> if the given input is an object referencing a physical BEAM-DIMAP data source.
     */
    public DecodeQualification getDecodeQualification(Object object) {
        File file = null;
        if (object instanceof String) {
            file = new File((String) object);
        } else if (object instanceof File) {
            file = (File) object;
        }
        if (file != null
                && file.getPath().toLowerCase().endsWith(DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION)
                && file.exists()
                && file.isFile()) {
            FileReader fr = null;
            try {
                // todo - URGENT: check this code!!! 80 charters are not enough, instead read until "<Dimap_Document" is found or EOF is reached or illegal text characters are detected
                fr = new FileReader(file);
                final char[] cbuf = new char[80];
                if (fr.read(cbuf) != -1) {
                    final String s = new String(cbuf);
                    fr.close();
                    if (s.contains("<Dimap_Document")) {
                        return DecodeQualification.INTENDED;
                    }
                }
            } catch (IOException e) {
                // ignore
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for an BEAM-DIMAP product reader.
     * <p> Instances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>DimapProductReader</code> class (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual BEAM-DIMAP product reader class.
     *
     * @return a new instance of the <code>DimapProductReader</code> class
     */
    public ProductReader createReaderInstance() {
        final DimapProductReader dimapProductReader = new DimapProductReader(this);
        if (readerExtenders != null) {
            for (DimapProductReader.ReaderExtender readerExtender : readerExtenders) {
                dimapProductReader.addExtender(readerExtender);
            }
        }
        return dimapProductReader;
    }

    public SnapFileFilter getProductFileFilter() {
        return dimapFileFilter;
    }

    public void addReaderExtender(DimapProductReader.ReaderExtender extender) {
        if (extender == null) {
            return;
        }
        if (readerExtenders == null) {
            readerExtenders = new ArrayList<DimapProductReader.ReaderExtender>();
        }
        readerExtenders.add(extender);
    }

    public void removeReaderExtender(DimapProductReader.ReaderExtender extender) {
        if (extender == null || readerExtenders == null) {
            return;
        }
        readerExtenders.remove(extender);
    }

}
