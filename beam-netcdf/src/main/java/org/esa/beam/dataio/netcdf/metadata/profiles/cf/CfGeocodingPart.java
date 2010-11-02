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

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.Dimension;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class CfGeocodingPart extends ProfilePart {

    private boolean usePixelGeoCoding;
    private boolean latLonAlreadyPresent;

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        GeoCoding geoCoding = readConventionBasedMapGeoCoding(ctx, p);
        if (geoCoding == null) {
            geoCoding = readPixelGeoCoding(ctx, p);
        }
        if (geoCoding != null) {
            p.setGeoCoding(geoCoding);
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product product) throws IOException {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            // we don't need to write a geo coding if there is none present
            return;
        }
        usePixelGeoCoding = !isGeographicLatLon(geoCoding);
        final NetcdfFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        if (usePixelGeoCoding) {
            addXYCoordVariables(ncFile);
            Group rootGroup = ncFile.getRootGroup();
            latLonAlreadyPresent = rootGroup.findVariable("lat") != null && rootGroup.findVariable("lon") != null;
            if (!latLonAlreadyPresent) {
                addLatLonBands(ncFile);
            }
        } else {
            GeoPos ul = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
            GeoPos br = geoCoding.getGeoPos(
                    new PixelPos(product.getSceneRasterWidth() - 0.5f, product.getSceneRasterHeight() - 0.5f), null);
            addLatLonCoordVariables(ncFile, ul, br);
        }
        ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, true);
    }

    @Override
    public void write(ProfileWriteContext ctx, Product product) throws IOException {
        if (!usePixelGeoCoding && !latLonAlreadyPresent) {
            return;
        }
        try {
            final int h = product.getSceneRasterHeight();
            final int w = product.getSceneRasterWidth();
            final float[] lat = new float[w];
            final float[] lon = new float[w];
            PixelPos pixelPos = new PixelPos();
            GeoPos geoPos = new GeoPos();
            final Boolean isYFlipped = (Boolean) ctx.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
            for (int y = 0; y < h; y++) {
                pixelPos.y = y + 0.5f;
                for (int x = 0; x < w; x++) {
                    pixelPos.x = x + 0.5f;
                    product.getGeoCoding().getGeoPos(pixelPos, geoPos);
                    lat[x] = geoPos.getLat();
                    lon[x] = geoPos.getLon();
                }
                int flippedY = isYFlipped ? (h - 1) - y : y;
                final int[] shape = new int[]{1, w};
                final int[] origin = new int[]{flippedY, 0};
                ctx.getNetcdfFileWriteable().write("lat", origin, Array.factory(DataType.FLOAT, shape, lat));
                ctx.getNetcdfFileWriteable().write("lon", origin, Array.factory(DataType.FLOAT, shape, lon));
            }
        } catch (InvalidRangeException e) {
            throw new ProductIOException("Data not in the expected range", e);
        }
    }

    static boolean isGeographicLatLon(final GeoCoding geoCoding) {
        if (geoCoding instanceof CrsGeoCoding || geoCoding instanceof MapGeoCoding) {
            return CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }
        return false;
    }

    private void addLatLonCoordVariables(NetcdfFileWriteable ncFile, GeoPos ul, GeoPos br) {
        final Variable latVar = ncFile.addVariable(null, "lat", DataType.FLOAT, "lat");
        latVar.addAttribute(new Attribute("units", "degrees_north"));
        latVar.addAttribute(new Attribute("long_name", "latitude coordinate"));
        latVar.addAttribute(new Attribute("standard_name", "latitude"));
        latVar.addAttribute(new Attribute(Constants.VALID_MIN_ATT_NAME, br.getLat()));
        latVar.addAttribute(new Attribute(Constants.VALID_MAX_ATT_NAME, ul.getLat()));

        final Variable lonVar = ncFile.addVariable(null, "lon", DataType.FLOAT, "lon");
        lonVar.addAttribute(new Attribute("units", "degrees_east"));
        lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
        lonVar.addAttribute(new Attribute("standard_name", "longitude"));
        lonVar.addAttribute(new Attribute(Constants.VALID_MIN_ATT_NAME, ul.getLon()));
        lonVar.addAttribute(new Attribute(Constants.VALID_MAX_ATT_NAME, br.getLon()));
    }

    private void addXYCoordVariables(NetcdfFileWriteable ncFile) {
        final Variable yVar = ncFile.addVariable(null, "y", DataType.FLOAT, "y");
        yVar.addAttribute(new Attribute("axis", "y"));
        yVar.addAttribute(new Attribute("long_name", "y-coordinate in Cartesian system"));
        yVar.addAttribute(new Attribute("units", "m"));             // todo check

        final Variable xVar = ncFile.addVariable(null, "x", DataType.FLOAT, "x");
        xVar.addAttribute(new Attribute("axis", "x"));
        xVar.addAttribute(new Attribute("long_name", "x-coordinate in Cartesian system"));
        xVar.addAttribute(new Attribute("units", "m"));             // todo check
    }

    private void addLatLonBands(final NetcdfFileWriteable ncFile) {
        final Variable latVar = ncFile.addVariable(null, "lat", DataType.FLOAT, "y x");
        latVar.addAttribute(new Attribute("units", "degrees_north"));
        latVar.addAttribute(new Attribute("long_name", "latitude coordinate"));
        latVar.addAttribute(new Attribute("standard_name", "latitude"));

        final Variable lonVar = ncFile.addVariable(null, "lon", DataType.FLOAT, "y x");
        lonVar.addAttribute(new Attribute("units", "degrees_east"));
        lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
        lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    }

    private static GeoCoding readConventionBasedMapGeoCoding(ProfileReadContext ctx, Product product) {
        final String[] cfConvention_lonLatNames = new String[]{
                Constants.LON_VAR_NAME,
                Constants.LAT_VAR_NAME
        };
        final String[] coardsConvention_lonLatNames = new String[]{
                Constants.LONGITUDE_VAR_NAME,
                Constants.LATITUDE_VAR_NAME
        };

        Variable[] lonLat;
        List<Variable> variableList = ctx.getNetcdfFile().getVariables();
        lonLat = ReaderUtils.getVariables(variableList, cfConvention_lonLatNames);
        if (lonLat == null) {
            lonLat = ReaderUtils.getVariables(variableList, coardsConvention_lonLatNames);
        }

        if (lonLat != null) {
            final Variable lonVariable = lonLat[0];
            final Variable latVariable = lonLat[1];
            final Dimension rasterDim = ctx.getRasterDigest().getRasterDim();
            if (rasterDim.fitsTo(lonVariable, latVariable)) {
                try {
                    return createConventionBasedMapGeoCoding(lonVariable, latVariable,
                                                             product.getSceneRasterWidth(),
                                                             product.getSceneRasterHeight(), ctx);
                } catch (Exception e) {
                    BeamLogManager.getSystemLogger().warning("Failed to create NetCDF geo-coding");
                }
            }
        }
        return null;
    }

    private static GeoCoding createConventionBasedMapGeoCoding(Variable lonVar,
                                                               Variable latVar,
                                                               int sceneRasterWidth,
                                                               int sceneRasterHeight,
                                                               ProfileReadContext ctx) throws Exception {
        double pixelX;
        double pixelY;
        double easting;
        double northing;
        double pixelSizeX;
        double pixelSizeY;

        final Attribute lonValidMinAttr = lonVar.findAttribute(Constants.VALID_MIN_ATT_NAME);
        final Attribute lonValidMaxAttr = lonVar.findAttribute(Constants.VALID_MAX_ATT_NAME);

        final Attribute latValidMinAttr = latVar.findAttribute(Constants.VALID_MIN_ATT_NAME);
        final Attribute latValidMaxAttr = latVar.findAttribute(Constants.VALID_MAX_ATT_NAME);

        boolean yFlipped;
        if (lonValidMinAttr != null && lonValidMaxAttr != null && latValidMinAttr != null && latValidMaxAttr != null) {
            // COARDS convention uses 'valid_min' and 'valid_max' attributes

            double lonValidMin = lonValidMinAttr.getNumericValue().doubleValue();
            double latValidMin = latValidMinAttr.getNumericValue().doubleValue();
            double lonValidMax = lonValidMaxAttr.getNumericValue().doubleValue();
            double latValidMAx = latValidMaxAttr.getNumericValue().doubleValue();

            pixelX = 0.5;
            pixelY = (sceneRasterHeight - 1.0) + 0.5;
            easting = lonValidMin;
            northing = latValidMin;
            pixelSizeX = (lonValidMax - lonValidMin) / sceneRasterWidth;
            pixelSizeY = (latValidMAx - latValidMin) / sceneRasterHeight;
            // must flip
            yFlipped = true; // todo - check
        } else {
            // CF convention
            final Array lonData = lonVar.read();
            final Array latData = latVar.read();

            final Index i0 = lonData.getIndex().set(0);
            final Index i1 = lonData.getIndex().set(1);
            pixelSizeX = lonData.getDouble(i1) - lonData.getDouble(i0);
            easting = lonData.getDouble(i0);

            int latSize = (int) latVar.getSize();
            final Index j0 = latData.getIndex().set(0);
            final Index j1 = latData.getIndex().set(1);
            pixelSizeY = latData.getDouble(j1) - latData.getDouble(j0);

            pixelX = 0.5f;
            pixelY = 0.5f;

            // this should be the 'normal' case
            if (pixelSizeY < 0) {
                pixelSizeY = -pixelSizeY;
                yFlipped = false;
                northing = latData.getDouble(latData.getIndex().set(0));
            } else {
                yFlipped = true;
                northing = latData.getDouble(latData.getIndex().set(latSize - 1));
            }
        }

        if (pixelSizeX <= 0 || pixelSizeY <= 0) {
            return null;
        }
        ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, yFlipped);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                sceneRasterWidth, sceneRasterHeight,
                                easting, northing,
                                pixelSizeX, pixelSizeY,
                                pixelX, pixelY);
    }

    private static GeoCoding readPixelGeoCoding(ProfileReadContext ctx, Product product) throws IOException {
        Band lonBand = product.getBand(Constants.LON_VAR_NAME);
        if (lonBand == null) {
            lonBand = product.getBand(Constants.LONGITUDE_VAR_NAME);
        }
        Band latBand = product.getBand(Constants.LAT_VAR_NAME);
        if (latBand == null) {
            latBand = product.getBand(Constants.LATITUDE_VAR_NAME);
        }
        if (latBand != null && lonBand != null) {
            final NetcdfFile netcdfFile = ctx.getNetcdfFile();
            ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME,
                            detectFlipping(netcdfFile.getRootGroup().findVariable(latBand.getName())));
            return new PixelGeoCoding(latBand, lonBand, latBand.getValidMaskExpression(), 5);
        }
        return null;
    }

    private static boolean detectFlipping(Variable latVar) throws IOException {
        final Array latData = latVar.read();
        final Index j0 = latData.getIndex().set(0);
        final Index j1 = latData.getIndex().set(1);
        double pixelSizeY = latData.getDouble(j1) - latData.getDouble(j0);

        // this should be the 'normal' case
        return pixelSizeY >= 0;
    }
}
