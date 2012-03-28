/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.obpg;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ObpgProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String DEFAULT_FILE_EXTENSION = ".hdf";
    private static final String DEFAULT_FILE_EXTENSION_L2 = ".L2";
    private static final String DEFAULT_FILE_EXTENSION_L2_LAC = DEFAULT_FILE_EXTENSION_L2 + "_LAC";
    private static final String DEFAULT_FILE_EXTENSION_L2_LAC_OC = DEFAULT_FILE_EXTENSION_L2_LAC + "_OC";
    private static final String DEFAULT_FILE_EXTENSION_L2_LAC_SST = DEFAULT_FILE_EXTENSION_L2_LAC + "_SST";
    private static final String DEFAULT_FILE_EXTENSION_L2_LAC_SST4 = DEFAULT_FILE_EXTENSION_L2_LAC + "_SST4";
    private static final String DEFAULT_FILE_EXTENSION_L2_MLAC = DEFAULT_FILE_EXTENSION_L2 + "_MLAC";
    private static final String DEFAULT_FILE_EXTENSION_L2_MLAC_OC = DEFAULT_FILE_EXTENSION_L2_MLAC + "_OC";

    public static final String READER_DESCRIPTION = "NASA Ocean Color (OBPG) Products";
    public static final String FORMAT_NAME = "NASA-OBPG";

    private static final String[] magicStrings = {
            "MERIS Level-2 Data",
            "MODISA Level-2 Data",
            "MODIST Level-2 Data",
            "SeaWiFS Level-2 Data",
            "CZCS Level-2 Data",
            "OCTS Level-2 Data"
    };

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = getInputFile(input);
        NetcdfFile ncfile = null;
        try {
            if (file == null || !file.isFile()) {
                return DecodeQualification.UNABLE;
            }
            String fileExtension = FileUtils.getExtension(file);
            if (fileExtension == null || !StringUtils.contains(getDefaultFileExtensions(), fileExtension)) {
                return DecodeQualification.UNABLE;
            }
            if (NetcdfFile.canOpen(file.getPath())) {
                ncfile = NetcdfFile.open(file.getPath());
                Attribute titleAttribute = ncfile.findGlobalAttribute("Title");
                if (titleAttribute != null) {
                    final String value = titleAttribute.getStringValue();
                    if (value != null) {
                        final String title = value.trim();
                        for (String magicString : magicStrings) {
                            if (title.contains(magicString)) {
                                return DecodeQualification.INTENDED;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        } finally {
            if (ncfile != null) {
                try {
                    ncfile.close();
                } catch (IOException ignore) {
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new ObpgProductReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }
        return new BeamFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same length as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{
                DEFAULT_FILE_EXTENSION,
                DEFAULT_FILE_EXTENSION_L2,
                DEFAULT_FILE_EXTENSION_L2_LAC,
                DEFAULT_FILE_EXTENSION_L2_LAC_OC,
                DEFAULT_FILE_EXTENSION_L2_LAC_SST,
                DEFAULT_FILE_EXTENSION_L2_LAC_SST4,
                DEFAULT_FILE_EXTENSION_L2_MLAC,
                DEFAULT_FILE_EXTENSION_L2_MLAC_OC
        };
    }

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
    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    private File getInputFile(Object input) {
        try {
            return ObpgUtils.getInputFile(input);
        } catch (IllegalArgumentException ignored) {
            //ignore
        }
        return null;
    }
}
