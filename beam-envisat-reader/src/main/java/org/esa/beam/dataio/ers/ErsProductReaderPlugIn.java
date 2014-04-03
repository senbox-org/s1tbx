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
package org.esa.beam.dataio.ers;

import org.esa.beam.dataio.envisat.EnvisatProductReaderPlugIn;
import org.esa.beam.dataio.envisat.ProductFile;
import org.esa.beam.framework.dataio.DecodeQualification;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.util.Locale;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * The <code>ErsProductReaderPlugIn</code> class is an implementation of the <code>ProductReaderPlugIn</code>
 * interface exclusively for ERS1/2 data products having the standard ESA/ENVISAT raw format.
 * <p/>
 *
 * @author Norman Fomferra
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
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(Locale name) {
        return "ERS1/2 AATSR and SAR products";
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if
     * it's content has the ENVISAT format by checking if the first bytes in the file equals the ENVISAT magic file
     * string <code>PRODUCT=&quot;</code>.
     * <p/>
     * <p> ERS product readers accept <code>java.lang.String</code> - a file path, <code>java.io.File</code> - an
     * abstract file path or a <code>javax.imageio.stream.ImageInputStream</code> - an already opened image input
     * stream.
     *
     * @param input the input object
     * @return <code>true</code> if the given input is an object referencing a physical ERS in ENVISAT data source.
     */
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String fileName = null;
        if (input instanceof String) {
            fileName = ((String) input).toUpperCase();
        } else if (input instanceof File) {
            fileName = ((File) input).getName().toUpperCase();
        }
        if (fileName != null) {
            if (matchesExtension(fileName))
                return DecodeQualification.INTENDED;
            else if (!fileName.endsWith(".ZIP") && !fileName.endsWith(".GZ"))
                return DecodeQualification.UNABLE;
            return super.getDecodeQualification(input);
        }
        return DecodeQualification.UNABLE;
    }

    private boolean matchesExtension(String filename) {
        String[] extList = getDefaultFileExtensions();
        for (String ext : extList) {
            if (filename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
