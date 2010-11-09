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
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
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
        final NetcdfFile netcdfFile;
        try {
            netcdfFile = NetcdfFile.open(input.toString());
        } catch (Throwable ignored) {
            return DecodeQualification.UNABLE;
        }

        return getDecodeQualification(netcdfFile);
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

    protected void initReadContext(ProfileReadContext ctx) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        ctx.setRasterDigest(rasterDigest);

    }

    protected abstract DecodeQualification getDecodeQualification(NetcdfFile netcdfFile);

    public abstract ProfileInitPartReader createInitialisationPartReader();

    public ProfilePartReader createMetadataPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createBandPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createFlagCodingPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createGeoCodingPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createImageInfoPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createIndexCodingPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createMaskPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createStxPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createTiePointGridPartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createTimePartReader() {
        return new NullProfilePartReader();
    }

    public ProfilePartReader createDescriptionPartReader() {
        return new NullProfilePartReader();
    }

}
