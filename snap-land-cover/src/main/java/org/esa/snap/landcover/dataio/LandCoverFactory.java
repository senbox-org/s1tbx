/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.dataio;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;

import java.io.IOException;

/**
 * Land Cover Handling
 */
public class LandCoverFactory {

    private static final String AUTO = " (Auto Download)";

    private static final LandCoverModelDescriptor[] descriptors = LandCoverModelRegistry.getInstance().getAllDescriptors();
    private static final String[] nameList = new String[descriptors.length];

    static {
        for (int i = 0; i < descriptors.length; i++) {
            nameList[i] = appendAutoDownload(descriptors[i].getName());
        }
    }

    public static String[] getNameList() {
        return nameList;
    }

    public static String getProperName(String name) {
        return name.replace(LandCoverFactory.AUTO, "");
    }

    public static LandCoverModel createLandCoverModel(final String name, final String resamplingMethod) throws IOException {

        final LandCoverModelRegistry registry = LandCoverModelRegistry.getInstance();
        final LandCoverModelDescriptor descriptor = registry.getDescriptor(name);
        if (descriptor == null) {
            throw new OperatorException("The land cover '" + name + "' is not supported.");
        }

        Resampling resampling = ResamplingFactory.createResampling(resamplingMethod);
        if(resampling == null) {
            throw new OperatorException("Resampling method "+ resamplingMethod + " is invalid");
        }
        final LandCoverModel landcover = descriptor.createLandCoverModel(resampling);
        if (landcover == null) {
            throw new OperatorException("The land cover '" + name + "' has not been installed.");
        }
        return landcover;
    }

    public static void checkIfInstalled(final String name) {

        final LandCoverModelRegistry registry = LandCoverModelRegistry.getInstance();
        final LandCoverModelDescriptor descriptor = registry.getDescriptor(name);
        if (descriptor == null) {
            throw new OperatorException("The land cover '" + name + "' is not supported.");
        }

        if (!descriptor.isInstalling() && !descriptor.isInstalled()) {
            if (!descriptor.installFiles()) {
                throw new OperatorException("Land cover " + name + " must be installed first");
            }
        }
    }

    public static String appendAutoDownload(String name) {

        return name;
    }

    /**
     * Read land cover for current tile.
     *
     * @param landcover      the model
     * @param noDataValue    the no data value of the landcover
     * @param tileGeoRef     the georeferencing of the target product
     * @param x0             The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0             The y coordinate of the pixel at the upper left corner of current tile.
     * @param tileHeight     The tile height.
     * @param tileWidth      The tile width.
     * @param localLandCover The LandCover for the tile.
     * @return true if all values are valid
     * @throws Exception from LandCover
     */
    public static boolean getLocalLandCover(final LandCoverModel landcover, final float noDataValue,
                                            final TileGeoreferencing tileGeoRef,
                                            final int x0, final int y0,
                                            final int tileWidth, final int tileHeight,
                                            final double[][] localLandCover) throws Exception {

        // Note: the localLandCover covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
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

                alt = landcover.getLandCover(geoPos);

                if (!valid && alt != noDataValue) {
                    valid = true;
                }

                localLandCover[yy][x - x0 + 1] = alt;
            }
        }
        return valid;
    }
}
