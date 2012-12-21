/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.netcdf;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;


public class NetCDFWriterPlugIn implements ProductWriterPlugIn {

    /**
     * Constructs a new product writer plug-in instance.
     */
    public NetCDFWriterPlugIn() {
    }

    public String[] getFormatNames() {
        return NetcdfConstants.NETCDF_FORMAT_NAMES;
    }

    public String[] getDefaultFileExtensions() {
        return NetcdfConstants.NETCDF_FORMAT_FILE_EXTENSIONS;
    }

    /**
     * Returns an array containing the classes that represent valid output types for this GDAL product writer.
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
        return new Class[]{
                String.class,
                File.class,
//            ImageOutputStream.class
        };
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
    public String getDescription(Locale name) {
        return "NetCDF product writer";
    }

    /**
     * Creates an instance of the actual GeoTIFF product writer class.
     *
     * @return a new instance of the <code>GDALWriter</code> class
     */
    public ProductWriter createWriterInstance() {
        return new NetCDFWriter(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}