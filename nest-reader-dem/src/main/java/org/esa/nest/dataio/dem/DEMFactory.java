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
package org.esa.nest.dataio.dem;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.gpf.TileGeoreferencing;

import java.util.Arrays;

/**
 * DEM Handling
 */
public class DEMFactory {

    private static final String AUTODEM = " (Auto Download)";
    static final String DELAUNAY_INTERPOLATION = "DELAUNAY_INTERPOLATION";

    private static final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
    private static final String[] demNameList = new String[descriptors.length];
    private static final String[] demResamplingList = new String[ResamplingFactory.resamplingNames.length +1];

    static {
        for (int i = 0; i < descriptors.length; i++) {
            demNameList[i] = DEMFactory.appendAutoDEM(descriptors[i].getName());
        }

        int i=0;
        for(String resampleName : ResamplingFactory.resamplingNames) {
            demResamplingList[i++] = resampleName;
        }
        demResamplingList[i] = DELAUNAY_INTERPOLATION;
    }

    public static String[] getDEMNameList() {
        return  demNameList;
    }

    public static String[] getDEMResamplingMethods() {
        return demResamplingList;
    }

    public static String getProperDEMName(String name) {
        return name.replace(DEMFactory.AUTODEM, "");
    }

    public static ElevationModel createElevationModel(final String demName, String demResamplingMethod) {

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        if (demDescriptor.isInstallingDem()) {
            throw new OperatorException("The DEM '" + demName + "' is currently being installed.");
        }

        Resampling resampleMethod = null;
        if(!demResamplingMethod.equals(DELAUNAY_INTERPOLATION))               // resampling not actual used for Delaunay
            resampleMethod = ResamplingFactory.createResampling(demResamplingMethod);

        final ElevationModel dem = demDescriptor.createDem(resampleMethod);
        if(dem == null) {
            throw new OperatorException("The DEM '" + demName + "' has not been installed.");
        }
        return dem;
    }

    public static void checkIfDEMInstalled(final String demName) {

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        if (!demDescriptor.isInstallingDem() && !demDescriptor.isDemInstalled()) {
            if(!demDescriptor.installDemFiles(VisatApp.getApp())) {
                throw new OperatorException("DEM "+ demName +" must be installed first");
            }
        }
    }

    public static void validateDEM(final String demName, final Product srcProduct) {
        // check if outside dem area
        if(demName.contains("SRTM")) {
            final GeoCoding geocoding = srcProduct.getGeoCoding();
            final int w = srcProduct.getSceneRasterWidth();
            final int h = srcProduct.getSceneRasterHeight();
            final GeoPos geo1 = geocoding.getGeoPos(new PixelPos(0,0), null);
            final GeoPos geo2 = geocoding.getGeoPos(new PixelPos(w,0), null);
            final GeoPos geo3 = geocoding.getGeoPos(new PixelPos(w,h), null);
            final GeoPos geo4 = geocoding.getGeoPos(new PixelPos(0, h), null);

            if((geo1.getLat() > 60 && geo2.getLat() > 60 && geo3.getLat() > 60 && geo4.getLat() > 60) ||
                    (geo1.getLat() < -60 && geo2.getLat() < -60 && geo3.getLat() < -60 && geo4.getLat() < -60)) {
                throw new OperatorException("Entire image is outside of SRTM valid area.\nPlease use another DEM.");
            }
        }
    }

    public static void fillDEM(final double[][] localDEM, final float alt) {
        for (double[] row : localDEM) {
            Arrays.fill(row, alt);
        }
    }

    public static String appendAutoDEM(String demName) {
        if(demName.equals("GETASSE30") || demName.equals("SRTM 3Sec") || demName.equals("ACE2_5Min")
           || demName.equals("ACE30"))
            demName += AUTODEM;
        return demName;
    }

    /**
     * Read DEM for current tile.
     * @param dem the model
     * @param demNoDataValue the no data value of the dem
     * @param tileGeoRef the georeferencing of the target product
     * @param x0 The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0 The y coordinate of the pixel at the upper left corner of current tile.
     * @param tileHeight The tile height.
     * @param tileWidth The tile width.
     * @param localDEM The DEM for the tile.
     * @return true if all dem values are valid
     * @throws Exception from DEM
     */
    public static boolean getLocalDEM(final ElevationModel dem, final float demNoDataValue,
                                       final TileGeoreferencing tileGeoRef,
                                       final int x0, final int y0,
                                       final int tileWidth, final int tileHeight,
                                       final double[][] localDEM) throws Exception {
        if(dem.getResampling() == null) {
            return getLocalDEMUsingDelaunayInterpolation(dem, demNoDataValue, tileGeoRef, x0, y0, tileWidth, tileHeight, localDEM);
        }

        // Note: the localDEM covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
        //       the left and 1 extra column to the right of the tile.

        final int maxY = y0 + tileHeight + 1;
        final int maxX = x0 + tileWidth + 1;
        final GeoPos geoPos = new GeoPos();

        double alt;
        boolean valid = false;
        for (int y = y0 - 1; y < maxY; y++) {
            final int yy = y - y0 + 1;

            for (int x = x0 - 1; x < maxX; x++) {
                tileGeoRef.getGeoPos(x, y, geoPos);
                /*if(!geoPos.isValid()) {
                    localDEM[yy][x - x0 + 1] = demNoDataValue;
                    continue;
                }
                if (geoPos.lon > 180) {
                    geoPos.lon -= 360;
                } else if (geoPos.lon < -180) {
                    geoPos.lon += 360;
                }    */

                alt = dem.getElevation(geoPos);
                if(!valid && alt != demNoDataValue)
                    valid = true;
                localDEM[yy][x - x0 + 1] = alt;
            }
        }
        return valid;
    }

    public synchronized static boolean getLocalDEMUsingDelaunayInterpolation(
            final ElevationModel dem, final float demNoDataValue, final TileGeoreferencing tileGeoRef, final int x0,
            final int y0, final int tileWidth, final int tileHeight, final double[][] localDEM) throws Exception {

        // Note: the localDEM covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
        //       the left and 1 extra column to the right of the tile.

        final int maxY = y0 + tileHeight + 1;
        final int maxX = x0 + tileWidth + 1;
        final PixelPos pixelPos = new PixelPos();
        final org.jdoris.core.Window tileWindow = new org.jdoris.core.Window(y0-1, y0 + tileHeight, x0-1, x0 + tileWidth);

        final GeoPos tgtUL = new GeoPos();
        final GeoPos tgtUR = new GeoPos();
        final GeoPos tgtLL = new GeoPos();
        final GeoPos tgtLR = new GeoPos();

        tileGeoRef.getGeoPos(x0-1, y0-1, tgtUL);
        tileGeoRef.getGeoPos(x0+tileWidth, y0-1, tgtUR);
        tileGeoRef.getGeoPos(x0-1, y0+tileHeight, tgtLL);
        tileGeoRef.getGeoPos(x0+tileWidth, y0+tileHeight, tgtLR);

        final double latMin = Math.min(Math.min(Math.min(tgtUL.lat, tgtUR.lat), tgtLL.lat), tgtLR.lat);
        final double latMax = Math.max(Math.max(Math.max(tgtUL.lat, tgtUR.lat), tgtLL.lat), tgtLR.lat);
        final double lonMin = Math.min(Math.min(Math.min(tgtUL.lon, tgtUR.lon), tgtLL.lon), tgtLR.lon);
        final double lonMax = Math.max(Math.max(Math.max(tgtUL.lon, tgtUR.lon), tgtLL.lon), tgtLR.lon);

        final GeoPos upperLeftCorner = new GeoPos((float)latMax, (float)lonMin);
        final GeoPos lowerRightCorner = new GeoPos((float)latMin, (float)lonMax);

        GeoPos[] geoCorners = {upperLeftCorner, lowerRightCorner};
        final GeoPos geoExtent = new GeoPos((float)(0.25*(latMax - latMin)), (float)(0.25*(lonMax - lonMin)));
        geoCorners = org.jdoris.core.utils.GeoUtils.extendCorners(geoExtent, geoCorners);

        if (geoCorners[0].lon > 180) {
            geoCorners[0].lon -= 360;
        }
        if (geoCorners[1].lon > 180) {
            geoCorners[1].lon -= 360;
        }

        boolean crossMeridian = false;
        if (geoCorners[0].lon > 0 && geoCorners[1].lon < 0) {
            crossMeridian = true;
        }

        PixelPos upperLeftCornerPos = dem.getIndex(geoCorners[0]);
        PixelPos lowerRightCornerPos = dem.getIndex(geoCorners[1]);

        upperLeftCornerPos = new PixelPos((float) Math.floor(upperLeftCornerPos.x), (float) Math.floor(upperLeftCornerPos.y));
        lowerRightCornerPos = new PixelPos((float) Math.ceil(lowerRightCornerPos.x), (float) Math.ceil(lowerRightCornerPos.y));

        double[][] x_in = null;
        double[][] y_in = null;
        double[][] z_in = null;
        final int nLatPixels = (int) Math.abs(lowerRightCornerPos.y - upperLeftCornerPos.y);
        final PixelPos pos = new PixelPos();
        if (!crossMeridian) {

            final int nLonPixels = (int) Math.abs(lowerRightCornerPos.x - upperLeftCornerPos.x);
            x_in = new double[nLatPixels][nLonPixels];
            y_in = new double[nLatPixels][nLonPixels];
            z_in = new double[nLatPixels][nLonPixels];
            final int startX = (int) upperLeftCornerPos.x;
            final int endX = startX + nLonPixels;
            final int startY = (int) upperLeftCornerPos.y;
            final int endY = startY + nLatPixels;
            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = startX, j = 0; x < endX; x++, j++) {
                    pos.setLocation(x,y);
                    tileGeoRef.getPixelPos(dem.getGeoPos(pos), pixelPos);
                    x_in[i][j] = pixelPos.x; // lon
                    y_in[i][j] = pixelPos.y; // lat
                    try {
                        float elev = dem.getSample(x, y);
                        if (Float.isNaN(elev))
                            elev = demNoDataValue;
                        z_in[i][j] = elev;
                    } catch (Exception e) {
                        z_in[i][j] = demNoDataValue;
                    }
                }
            }

        } else {

            final PixelPos endPixelPos = dem.getIndex(new GeoPos(geoCorners[0].lat, 180));
            final int nLonPixels = (int) (Math.abs(upperLeftCornerPos.x - endPixelPos.x) + lowerRightCornerPos.x);
            x_in = new double[nLatPixels][nLonPixels];
            y_in = new double[nLatPixels][nLonPixels];
            z_in = new double[nLatPixels][nLonPixels];
            final int startX = (int) upperLeftCornerPos.x;
            final int endX = (int)endPixelPos.x;
            final int startY = (int) upperLeftCornerPos.y;
            final int endY = startY + nLatPixels;
            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = startX, j = 0; x < endX; x++, j++) {
                    pos.setLocation(x,y);
                    tileGeoRef.getPixelPos(dem.getGeoPos(pos), pixelPos);
                    x_in[i][j] = pixelPos.x; // lon
                    y_in[i][j] = pixelPos.y; // lat
                    try {
                        float elev = dem.getSample(x, y);
                        if (Float.isNaN(elev))
                            elev = demNoDataValue;
                        z_in[i][j] = elev;
                    } catch (Exception e) {
                        z_in[i][j] = demNoDataValue;
                    }
                }
            }

            for (int y = startY, i = 0; y < endY; y++, i++) {
                for (int x = 0, j = endX - startX; x < (int)lowerRightCornerPos.x; x++, j++) {
                    pos.setLocation(x,y);
                    tileGeoRef.getPixelPos(dem.getGeoPos(pos), pixelPos);
                    x_in[i][j] = pixelPos.x; // lon
                    y_in[i][j] = pixelPos.y; // lat
                    try {
                        float elev = dem.getSample(x, y);
                        if (Float.isNaN(elev))
                            elev = demNoDataValue;
                        z_in[i][j] = elev;
                    } catch (Exception e) {
                        z_in[i][j] = demNoDataValue;
                    }
                }
            }
        }

        final double[][] elevation = org.jdoris.core.utils.TriangleUtils.gridDataLinear(y_in, x_in, z_in, tileWindow, 1, 1, 1, demNoDataValue, 0);

        float alt;
        boolean valid = false;
        for (int y = y0 - 1; y < maxY; y++) {
            final int yy = y - y0 + 1;
            for (int x = x0 - 1; x < maxX; x++) {
                alt = (float)elevation[yy][x - x0 + 1];
                if(!valid && alt != demNoDataValue)
                    valid = true;
                localDEM[yy][x - x0 + 1] = alt;
            }
        }
        return valid;
    }
}
