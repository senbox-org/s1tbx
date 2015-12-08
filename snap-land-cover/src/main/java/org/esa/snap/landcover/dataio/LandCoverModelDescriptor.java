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

import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.dataop.resamp.Resampling;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * A <code>LandCoverModel</code> is used to obtain a land cover class above a
 * specified geographical datum for a given geographical position.
 */
public interface LandCoverModelDescriptor {

    /**
     * The name of this land cover source
     *
     * @return a name
     */
    String getName();

    /**
     * Gets the no-data value for this land cover map.
     *
     * @return the no-data value, e.g. -99999
     */
    double getNoDataValue();

    /**
     * Gets the unit value for this land cover map.
     *
     * @return the unit value, e.g. class or percent
     */
    String getUnit();

    /**
     * Gets the resolution (in degrees) of this land cover map.
     *
     * @return the  (degree) value, e.g.
     */
    int getDegreeRes();

    /**
     * Gets the resolution (in pixels) of this land cover map.
     *
     * @return the  (pixels) value, e.g.
     */
    int getPixelRes();

    /**
     * Gets the number of tiles in x direction
     *
     * @return number of rows
     */
    int getNumXTiles();

    /**
     * Gets the number of tiles in y direction
     *
     * @return number of columns
     */
    int getNumYTiles();

    /**
     * Creates the land cover model instance.
     *
     * @param resampling The resampling method to be used.
     * @return a LandCoverModel instance, can be null e.g. if related files are not installed
     * @throws IOException
     * @since BEAM 4.6
     */
    LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException;

    /**
     * Returns the directory (if any) where the files shall be located.
     *
     * @return the file directory
     */
    File getInstallDir();

    /**
     * Gets the URL of the (ZIP) archive to be used for on-line installation.
     *
     * @return the URL of the (ZIP) archive
     */
    URL getArchiveUrl();

    /**
     * Tests whether or not the LandCover and associated files are installed.
     *
     * @return true, if so
     */
    boolean isInstalled();

    /**
     * Tests whether or not the LandCover is currently being installed.
     *
     * @return true, if so
     */
    boolean isInstalling();

    /**
     * Asynchronously installs the files required to use the LandCover if not already done or in progress.
     *
     * @return true, if the DEM is already installed, is being installed or will be installed. False, if an error occurred
     * or the user canceled the installation
     */
    boolean installFiles();

    /**
     * create the file name of a tile based on the position
     *
     * @param tileX x position
     * @param tileY y position
     * @return tile name
     */
    String createTileFilename(final int tileX, final int tileY);

    IndexCoding getIndexCoding();

    ImageInfo getImageInfo();

    int getDataType();
}
