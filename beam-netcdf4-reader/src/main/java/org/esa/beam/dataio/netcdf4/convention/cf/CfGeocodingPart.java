/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.netcdf4.convention.cf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf4.Nc4AttributeMap;
import org.esa.beam.dataio.netcdf4.Nc4Constants;
import org.esa.beam.dataio.netcdf4.Nc4Dim;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.Nc4ReaderUtils;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

public class CfGeocodingPart implements ModelPart {

    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        readGeocoding(p, rp);
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        //Todo change body of created method. Use File | Settings | File Templates to change
    }

    public static void readGeocoding(Product p, Nc4ReaderParameters rp) throws IOException {
        p.setGeoCoding(createConventionBasedMapGeoCoding(p, rp));
        if (p.getGeoCoding() == null) {
            setPixelGeoCoding(p);
        }
    }

    public static MapGeoCoding createConventionBasedMapGeoCoding(Product product, Nc4ReaderParameters rp) {
        final String[] coardsConvention_lonLatNames = new String[]{
                Nc4Constants.LONGITUDE_VAR_NAME,
                Nc4Constants.LATITUDE_VAR_NAME
        };
        final String[] cfConvention_lonLatNames = new String[]{
                Nc4Constants.LON_VAR_NAME,
                Nc4Constants.LAT_VAR_NAME
        };

        Variable[] lonLat;
        lonLat = Nc4ReaderUtils.getVariables(rp.getGlobalVariables(), cfConvention_lonLatNames);
        if (lonLat == null) {
            lonLat = Nc4ReaderUtils.getVariables(rp.getGlobalVariables(), coardsConvention_lonLatNames);
        }

        if (lonLat != null) {
            final Variable lonVariable = lonLat[0];
            final Variable latVariable = lonLat[1];
            final Nc4Dim rasterDim = rp.getRasterDigest().getRasterDim();
            if (rasterDim.fitsTo(lonVariable, latVariable)) {
                try {
                    final MapInfoX mapInfoX =
                            createMapInfoX(
                                    lonVariable, latVariable,
                                    product.getSceneRasterWidth(),
                                    product.getSceneRasterHeight());
                    if (mapInfoX != null) {
                        rp.setYFlipped(mapInfoX.isYFlipped());
                        return new MapGeoCoding(mapInfoX.getMapInfo());
                    }
                } catch (IOException e) {
                    BeamLogManager.getSystemLogger().warning("Failed to create NetCDF geo-coding");
                }
            }
        }
        return null;
    }

    private static MapInfoX createMapInfoX(final Variable lonVar,
                                           final Variable latVar,
                                           int sceneRasterWidth,
                                           final int sceneRasterHeight) throws IOException {
        double pixelX;
        double pixelY;
        double easting;
        double northing;
        double pixelSizeX;
        double pixelSizeY;

        final Nc4AttributeMap lonAttrMap = Nc4AttributeMap.create(lonVar);
        final Number lonValidMin = lonAttrMap.getNumericValue(Nc4Constants.VALID_MIN_ATT_NAME);
        final Number lonStep = lonAttrMap.getNumericValue(Nc4Constants.STEP_ATT_NAME);

        final Nc4AttributeMap latAttrMap = Nc4AttributeMap.create(latVar);
        final Number latValidMin = latAttrMap.getNumericValue(Nc4Constants.VALID_MIN_ATT_NAME);
        final Number latStep = latAttrMap.getNumericValue(Nc4Constants.STEP_ATT_NAME);

        boolean yFlipped;
        if (lonValidMin != null && lonStep != null && latValidMin != null && latStep != null) {
            // COARDS convention uses 'valid_min' and 'step' attributes
            pixelX = 0.5;
            pixelY = (sceneRasterHeight - 1.0) + 0.5;
            easting = lonValidMin.doubleValue();
            northing = latValidMin.doubleValue();
            pixelSizeX = lonStep.doubleValue();
            pixelSizeY = latStep.doubleValue();
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

        final MapProjection projection = MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME);
        final MapInfo mapInfo = new MapInfo(projection,
                                            (float) pixelX, (float) pixelY,
                                            (float) easting, (float) northing,
                                            (float) pixelSizeX, (float) pixelSizeY,
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(sceneRasterWidth);
        mapInfo.setSceneHeight(sceneRasterHeight);
        return new MapInfoX(mapInfo, yFlipped);
    }

    public static void setPixelGeoCoding(Product product) throws IOException {
        Band lonBand = product.getBand(Nc4Constants.LON_VAR_NAME);
        if (lonBand == null) {
            lonBand = product.getBand(Nc4Constants.LONGITUDE_VAR_NAME);
        }
        Band latBand = product.getBand(Nc4Constants.LAT_VAR_NAME);
        if (latBand == null) {
            latBand = product.getBand(Nc4Constants.LATITUDE_VAR_NAME);
        }
        if (latBand != null && lonBand != null) {
            product.setGeoCoding(new PixelGeoCoding(latBand,
                                                    lonBand,
                                                    latBand.getValidPixelExpression(),
                                                    5, ProgressMonitor.NULL));
        }
    }

    /**
     * Return type of the {@link CfGeocodingPart#createMapInfoX}()
     * method. Comprises a {@link MapInfo} and a boolean indicating that the reader
     * should flip data along the Y-axis.
     */
    private static class MapInfoX {

        final MapInfo _mapInfo;
        final boolean _yFlipped;

        public MapInfoX(MapInfo mapInfo, boolean yFlipped) {
            _mapInfo = mapInfo;
            _yFlipped = yFlipped;
        }

        public MapInfo getMapInfo() {
            return _mapInfo;
        }

        public boolean isYFlipped() {
            return _yFlipped;
        }
    }
}
