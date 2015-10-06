/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.reader;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class BinnedProductReaderPlugin implements ProductReaderPlugIn {

    static final String FORMAT_NAME = "Binned_data_product";
    static final String FORMAT_DESCRIPTION = "SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data";
    static final String FILE_EXTENSION = ".nc";

    public DecodeQualification getDecodeQualification(Object input) {
        if (input == null) {
            return DecodeQualification.UNABLE;
        }
        final String path = input.toString();
        if (BinnedProductReaderPlugin.FILE_EXTENSION.equalsIgnoreCase(FileUtils.getExtension(path))) {
            try {
                NetcdfFile netcdfFile = null;
                try {
                    netcdfFile = NetcdfFileOpener.open(path);
                    if (netcdfFile == null) {
                        return DecodeQualification.UNABLE;
                    }
                    for (Variable variable : netcdfFile.getVariables()) {
                        Attribute gridMappingName = variable.findAttribute("grid_mapping_name");
                        if (gridMappingName != null) {
                            if ("1D binned sinusoidal".equalsIgnoreCase(gridMappingName.getStringValue())) {
                                return DecodeQualification.INTENDED;
                            }
                        }
                        Attribute gridMapping = variable.findAttribute("grid_mapping");
                        if (gridMapping != null) {
                            if ("sinusoidal".equalsIgnoreCase(gridMapping.getStringValue())) {
                                return DecodeQualification.INTENDED;
                            }
                        }
                    }
                    if (netcdfFile.findDimension("bin_index") != null &&
                            (netcdfFile.findDimension("sin_grid") != null ||
                                    netcdfFile.findDimension("bin_list") != null)) {
                        return DecodeQualification.INTENDED;
                    }
                } finally {
                    if (netcdfFile != null) {
                        netcdfFile.close();
                    }
                }
            } catch (IOException e) {
                return DecodeQualification.UNABLE;
            }
        }
        return DecodeQualification.UNABLE;
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    public ProductReader createReaderInstance() {
        return new BinnedProductReader(this);
    }

    public SnapFileFilter getProductFileFilter() {
        return new BinnedFileFilter();
    }

    /**
     * Returns a string array containing the single entry <code>&quot;Binned data product&quot;</code>.
     */
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
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
        return new String[]{FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return "Reader for SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data";
    }
}
