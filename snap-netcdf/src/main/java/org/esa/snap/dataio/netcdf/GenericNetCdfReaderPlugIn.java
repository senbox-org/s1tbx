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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;


/**
 * @author Norman Fomferra
 */
public class GenericNetCdfReaderPlugIn implements ProductReaderPlugIn {

    private static AbstractNetCdfReaderPlugIn[] netCdfReaderPlugIns;
    private static final Logger LOGGER = Logger.getLogger(GenericNetCdfReaderPlugIn.class.getName());


    // needed for creation by SPI

    public GenericNetCdfReaderPlugIn() {
    }

    /**
     * Returns true only if all of the following conditions are true:
     * <ul>
     * <li>the given input object's string representation has the ".nc" extension,</li>
     * <li>the given input object's string representation points to a valid netCDF file,</li>
     * <li>the netCDF file has variables which can be interpreted as bands.</li>
     * </ul>
     */
    @Override
    public DecodeQualification getDecodeQualification(final Object input) {
        if (input == null) {
            return DecodeQualification.UNABLE;
        }
        NetcdfFile netcdfFile = null;
        try {
            final String inputPath = input.toString();
            final List<String> extensionList = Arrays.asList(getDefaultFileExtensions());
            for (String extension : extensionList) {
                if (inputPath.endsWith(extension)) {
                    netcdfFile = NetcdfFileOpener.open(inputPath);
                    LOGGER.warning("GenericNetCdfReaderPlugIn.getDecodeQualification(" + inputPath + ") extension=" + extension + " netcdfFile=" + netcdfFile);
                    break;
                }
            }
            if (netcdfFile == null) {
                return DecodeQualification.UNABLE;
            }

            final AbstractNetCdfReaderPlugIn[] plugIns = getAllNetCdfReaderPlugIns();
            return getDecodeQualification(plugIns, netcdfFile);
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        } finally {
            try {
                if (netcdfFile != null) {
                    netcdfFile.close();
                }
            } catch (IOException ignored) {
                // OK, ignored
            }
        }
        return DecodeQualification.UNABLE;
    }

    private DecodeQualification getDecodeQualification(AbstractNetCdfReaderPlugIn[] plugIns, NetcdfFile netcdfFile) {
        for (AbstractNetCdfReaderPlugIn plugIn : plugIns) {
            try {
                final DecodeQualification decodeQualification = plugIn.getDecodeQualification(netcdfFile);
                if (DecodeQualification.INTENDED.equals(decodeQualification) || DecodeQualification.SUITABLE.equals(decodeQualification)) {
                    return DecodeQualification.SUITABLE;
                }
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p> Intances of the classes returned in this array are valid objects for the {@code setInput} method of the
     * {@code ProductReader} interface (the method will not throw an {@code InvalidArgumentException} in this
     * case).
     *
     * @return an array containing valid input types, never {@code null}
     */
    @Override
    public Class[] getInputTypes() {
        return Constants.READER_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return {@code null}.
     *
     * @return a new reader instance, never {@code null}
     */
    @Override
    public ProductReader createReaderInstance() {
        return new GenericNetCdfReader(this);
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never {@code null}
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{Constants.FORMAT_NAME};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. {@code ".hdf"}
     *
     * @return the default file extensions for this product I/O plug-in, never {@code null}
     */
    @Override
    public String[] getDefaultFileExtensions() {
        Set<String> extensionSet = new HashSet<>();
        AbstractNetCdfReaderPlugIn[] abstractNetCdfReaderPlugIns = getAllNetCdfReaderPlugIns();
        for (AbstractNetCdfReaderPlugIn plugIn : abstractNetCdfReaderPlugIns) {
            String[] fileExtensions = plugIn.getDefaultFileExtensions();
            extensionSet.addAll(Arrays.asList(fileExtensions));
        }
        return extensionSet.toArray(new String[extensionSet.size()]);
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to {@code null} the default locale is
     * used.
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given description string, if {@code null} the default locale is used
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(final Locale locale) {
        return "Generic NetCDF Data Product";
    }

    static AbstractNetCdfReaderPlugIn[] getAllNetCdfReaderPlugIns() {
        if (netCdfReaderPlugIns == null) {
            final ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
            final Iterator<ProductReaderPlugIn> allReaderPlugIns = plugInManager.getAllReaderPlugIns();
            List<AbstractNetCdfReaderPlugIn> netCdfReaderPlugInList = new ArrayList<>();
            while (allReaderPlugIns.hasNext()) {
                ProductReaderPlugIn readerPlugIn = allReaderPlugIns.next();
                if (readerPlugIn instanceof AbstractNetCdfReaderPlugIn) {
                    AbstractNetCdfReaderPlugIn netCdfReaderPlugIn = (AbstractNetCdfReaderPlugIn) readerPlugIn;
                    netCdfReaderPlugInList.add(netCdfReaderPlugIn);
                }
            }
            netCdfReaderPlugIns = netCdfReaderPlugInList.toArray(
                    new AbstractNetCdfReaderPlugIn[netCdfReaderPlugInList.size()]);
        }
        return netCdfReaderPlugIns;
    }
}
