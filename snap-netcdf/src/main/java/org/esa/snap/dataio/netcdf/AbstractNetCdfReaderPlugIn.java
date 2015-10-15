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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.snap.dataio.netcdf.util.RasterDigest;
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
            netcdfFile = NetcdfFileOpener.open(input.toString());
            if (netcdfFile == null) {
                return DecodeQualification.UNABLE;
            }
            return getDecodeQualification(netcdfFile);
        } catch (Throwable ignored) {
            // ok -- just clean up and return UNABLE
            if (input != null) {
                String pathname = input.toString();
                if (pathname.toLowerCase().trim().endsWith(".zip")) {
                    final String trimmed = pathname.trim();
                    pathname = trimmed.substring(0, trimmed.length() - 4);
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
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    ///////////////////////////////////////////////
    // NetCdfReadProfile related methods

    /**
     * Initialises the {@link ProfileReadContext} for the following read operation.
     * When overriding this method at least the {@link RasterDigest} must be set to the context.
     *
     * @param ctx the context
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
     * {@link Product#getMetadataRoot() metadata}.
     *
     * @return the {@link ProfilePartReader} for metadata
     */
    public ProfilePartReader createMetadataPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link Band bands}.
     *
     * @return the {@link ProfilePartReader} for bands
     */
    public ProfilePartReader createBandPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link FlagCoding flag coding}.
     *
     * @return the {@link ProfilePartReader} for flag coding
     */
    public ProfilePartReader createFlagCodingPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link GeoCoding geo-coding}.
     *
     * @return the {@link ProfilePartReader} for geo-coding
     */
    public ProfilePartReader createGeoCodingPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link ImageInfo image info}.
     *
     * @return the {@link ProfilePartReader} for image-info
     */
    public ProfilePartReader createImageInfoPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link IndexCoding index coding}.
     *
     * @return the {@link ProfilePartReader} for index coding
     */
    public ProfilePartReader createIndexCodingPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link Mask masks}.
     *
     * @return the {@link ProfilePartReader} for masks
     */
    public ProfilePartReader createMaskPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link Stx statistics}.
     *
     * @return the {@link ProfilePartReader} for statistics
     */
    public ProfilePartReader createStxPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link TiePointGrid tie-point grids}.
     *
     * @return the {@link ProfilePartReader} for tie-point grids
     */
    public ProfilePartReader createTiePointGridPartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link Product#setStartTime(ProductData.UTC)} and
     * {@link Product#setEndTime(ProductData.UTC)}.
     *
     * @return the {@link ProfilePartReader} for time information
     */
    public ProfilePartReader createTimePartReader() {
        return new NullProfilePartReader();
    }

    /**
     * Creates an instance of {@link ProfilePartReader} responsible for reading
     * {@link Product#setDescription(String)}.
     *
     * @return the {@link ProfilePartReader} for description
     */
    public ProfilePartReader createDescriptionPartReader() {
        return new NullProfilePartReader();
    }

}
