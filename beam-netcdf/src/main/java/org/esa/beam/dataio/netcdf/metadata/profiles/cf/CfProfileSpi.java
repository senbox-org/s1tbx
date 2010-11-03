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

package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

/**
 * A profile used for reading/writing general NetCDF/CF files.
 * <p/>
 * This reader tries to support the
 * <a href="http://ferret.wrc.noaa.gov/noaa_coop/coop_cdf_profile.html">COARDS</a> profile and
 * <a href="http://www.cgd.ucar.edu/cms/eaton/cf-metadata/CF-1.0.html">CF Conventions</a> to a maximum extend.
 * <p/>
 * The CF Conventions are supported for regular, lat/lon grids as follows.
 * If the dimensions are
 * <pre>
 *    lon = <i>integer</i>
 *    lat = <i>integer</i>
 *    time = <i>integer</i> (currently assumed to be 1)
 * </pre>
 * then the following variables are expected
 * <pre>
 *    lon(lon)
 *    lat(lat)
 *    time(time)
 *    <i>band-1</i>(time, lat, lon)
 *    <i>band-2</i>(time, lat, lon)
 *    ...
 * </pre>
 * <p/>
 * The CF Conventions are supported for non-regular, lat/lon grids as follows:
 * If the dimensions are
 * <pre>
 *    ni = <i>integer</i>
 *    nj = <i>integer</i>
 *    time = <i>integer</i> (currently assumed to be 1)
 *    ...
 * </pre>
 * then the following variables are expected
 * <pre>
 *    lat(nj, ni)
 *    lon(nj, ni)
 *    time(time)
 *    <i>band-1</i>(time, nj, ni)
 *    <i>band-2</i>(time, nj, ni)
 *    ...
 * </pre>
 * <p/>
 * The COARDS profile is supported as follows:
 * If the dimensions are
 * <pre>
 *    longitude = <i>integer</i>
 *    latitude = <i>integer</i>
 *    ...
 * </pre>
 * then the following variables are expected
 * <pre>
 *    longitude(longitude)
 *    latitude(latitude)
 *    <i>band-1</i>(latitude, longitude)
 *    <i>band-2</i>(latitude, longitude)
 *    ...
 * </pre>
 *
 * @author Thomas Storm
 * @author Norman Fomferra
 */
public class CfProfileSpi extends AbstractProfileSpi {

    @Override
    public ProfilePart createBandPart() {
        return new CfBandPart();
    }

    @Override
    public ProfilePart createDescriptionPart() {
        return new CfDescriptionPart();
    }

    @Override
    public ProfilePart createFlagCodingPart() {
        return new CfFlagCodingPart();
    }

    @Override
    public ProfilePart createGeocodingPart() {
        return new CfGeocodingPart();
    }

    @Override
    public ProfilePart createImageInfoPart() {
        return null;
    }

    @Override
    public ProfilePart createIndexCodingPart() {
        return new CfIndexCodingPart();
    }

    @Override
    public ProfileInitPart createInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePart createMaskPart() {
        return null;
    }

    @Override
    public ProfilePart createMetadataPart() {
        return new CfMetadataPart();
    }

    @Override
    public ProfilePart createTimePart() {
        return new CfTimePart();
    }

    @Override
    public ProfilePart createStxPart() {
        return null;
    }

    @Override
    public ProfilePart createTiePointGridPart() {
        return new CfTiePointGridPart();
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter("netCDF/CF", Constants.FILE_EXTENSIONS, "netCDF/CF compliant products");
    }

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest != null && rasterDigest.getRasterVariables().length > 0) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }
}
