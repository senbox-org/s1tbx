/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.framework.dataio.ProductWriter;
import org.esa.snap.framework.dataio.ProductWriterPlugIn;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;

public abstract class AbstractNetCdfWriterPlugIn implements ProductWriterPlugIn {

    @Override
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new DefaultNetCdfWriter(this);
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    ///////////////////////////////////////////////
    // NetCdfWriteProfile related methods


    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfileInitPartReader}
     */
    public abstract ProfileInitPartWriter createInitialisationPartWriter();

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.nc.NFileWriteable} for the given
     * output path.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.nc.NFileWriteable}
     */
    public abstract NFileWriteable createWritable(String outputPath) throws IOException;


    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.Product#getMetadataRoot() metadata}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for metadata
     */
    public ProfilePartWriter createMetadataPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.Band bands}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for bands
     */
    public ProfilePartWriter createBandPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.FlagCoding flag coding}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for flag coding
     */
    public ProfilePartWriter createFlagCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.GeoCoding geo-coding}.
     *
     * @return the {@link ProfilePartWriter} for geo-coding
     */
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.ImageInfo image info}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for image-info
     */
    public ProfilePartWriter createImageInfoPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.IndexCoding index coding}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for index coding
     */
    public ProfilePartWriter createIndexCodingPartWriter() {
        return new NullProfilePartWriter();
    }
    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.Mask masks}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for masks
     */
    public ProfilePartWriter createMaskPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.Stx statistics}.
     *
     * @return the {@link ProfilePartWriter} for statistics
     */
    public ProfilePartWriter createStxPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.TiePointGrid tie-point grids}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for tie-point grids
     */
    public ProfilePartWriter createTiePointGridPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.Product#getStartTime()} and
     * {@link org.esa.snap.framework.datamodel.Product#getEndTime()}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for time information
     */
    public ProfilePartWriter createTimePartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.snap.framework.datamodel.Product#getDescription()}.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter} for description
     */
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }
}
