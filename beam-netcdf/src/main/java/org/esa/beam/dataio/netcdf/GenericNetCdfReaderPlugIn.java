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

package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * @author Norman Fomferra
 */
public class GenericNetCdfReaderPlugIn implements ProductReaderPlugIn {

    private static AbstractNetCdfReaderPlugIn[] netCdfReaderPlugIns;


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
            String fileExtension = FileUtils.getExtension(inputPath);
            if (fileExtension != null && extensionList.contains(fileExtension)) {
                netcdfFile = NetcdfFile.open(inputPath);
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

    private DecodeQualification getDecodeQualification(AbstractNetCdfReaderPlugIn[] plugIns, NetcdfFile netcdfFile) throws IOException {
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
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    @Override
    public Class[] getInputTypes() {
        return Constants.READER_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new GenericNetCdfReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{Constants.FORMAT_NAME};
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
        Set<String> extensionSet = new HashSet<String>();
        AbstractNetCdfReaderPlugIn[] abstractNetCdfReaderPlugIns = getAllNetCdfReaderPlugIns();
        for (AbstractNetCdfReaderPlugIn plugIn : abstractNetCdfReaderPlugIns) {
            String[] fileExtensions = plugIn.getDefaultFileExtensions();
            extensionSet.addAll(Arrays.asList(fileExtensions));
        }
        return extensionSet.toArray(new String[extensionSet.size()]);
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
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
            List<AbstractNetCdfReaderPlugIn> netCdfReaderPlugInList = new ArrayList<AbstractNetCdfReaderPlugIn>();
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
