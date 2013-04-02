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

package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

public abstract class AbstractNetCdfReaderPlugIn implements ProductReaderPlugIn {

    ///////////////////////////////////////////////
    // ProductReaderPlugIn related methods

    @Override
    public final Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public final DecodeQualification getDecodeQualification(Object input) {
        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFile.open(input.toString());
            return getDecodeQualification(netcdfFile);
        } catch (Throwable ignored) {
            // ok -- just clean up and return UNABLE
            if (input != null) {
                String pathname = input.toString();
                if (pathname.toLowerCase().trim().endsWith(".zip")) {
                    final String trimmed = pathname.trim();
                    pathname = trimmed.substring(0, trimmed.length()-4);
                    final File file = new File(pathname);
                    if (file.isFile() && file.length() == 0) {
                        file.deleteOnExit();
                        file.delete();
                    }
                }
            }
        } finally {
            try {
                if (netcdfFile != null) {
                    netcdfFile.close();
                }
            } catch (IOException ignore) {
                // OK, ignored
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new DefaultNetCdfReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    ///////////////////////////////////////////////
    // NetCdfReadProfile related methods

    /**
     * Initialises the {@link ProfileReadContext} for the following read operation.
     * When overriding this method at least the {@link RasterDigest} must be set to the context.
     *
     * @param ctx the context
     *
     * @throws IOException if an IO-Error occurs
     */
    protected void initReadContext(ProfileReadContext ctx) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest == null) {
            throw new IOException("File does not contain any bands.");
        }
        ctx.setRasterDigest(rasterDigest);
    }

    /**
     * Gets the qualification of the product reader to decode the given {@link NetcdfFile NetCDF file}.
     *
     * @param netcdfFile the NetCDF file
     *
     * @return the decode qualification
     */
    protected abstract DecodeQualification getDecodeQualification(NetcdfFile netcdfFile);

    /**
     * Creates an instance of {@link ProfileInitPartReader}.
     *
     * @return the {@link ProfileInitPartReader}
     */
    public abstract ProfileInitPartReader createInitialisationPartReader();

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.Product#getMetadataRoot() metadata}.
     *
     * @return the {@link ProfilePartReader} for metadata
     */
    public ProfilePartReader createMetadataPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.Band bands}.
     *
     * @return the {@link ProfilePartReader} for bands
     */
    public ProfilePartReader createBandPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.FlagCoding flag coding}.
     *
     * @return the {@link ProfilePartReader} for flag coding
     */
    public ProfilePartReader createFlagCodingPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.GeoCoding geo-coding}.
     *
     * @return the {@link ProfilePartReader} for geo-coding
     */
    public ProfilePartReader createGeoCodingPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.ImageInfo image info}.
     *
     * @return the {@link ProfilePartReader} for image-info
     */
    public ProfilePartReader createImageInfoPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.IndexCoding index coding}.
     *
     * @return the {@link ProfilePartReader} for index coding
     */
    public ProfilePartReader createIndexCodingPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.Mask masks}.
     *
     * @return the {@link ProfilePartReader} for masks
     */
    public ProfilePartReader createMaskPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.Stx statistics}.
     *
     * @return the {@link ProfilePartReader} for statistics
     */
    public ProfilePartReader createStxPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.TiePointGrid tie-point grids}.
     *
     * @return the {@link ProfilePartReader} for tie-point grids
     */
    public ProfilePartReader createTiePointGridPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.Product#setStartTime(org.esa.beam.framework.datamodel.ProductData.UTC)} and
     * {@link org.esa.beam.framework.datamodel.Product#setEndTime(org.esa.beam.framework.datamodel.ProductData.UTC)}.
     *
     * @return the {@link ProfilePartReader} for time information
     */
    public ProfilePartReader createTimePartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link org.esa.beam.framework.datamodel.Product#setDescription(String)}.
     *
     * @return the {@link ProfilePartReader} for description
     */
    public ProfilePartReader createDescriptionPartReader() {
        return new NullProfilePartReader();
    }

}
