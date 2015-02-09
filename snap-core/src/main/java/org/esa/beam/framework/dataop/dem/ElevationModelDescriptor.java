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
package org.esa.beam.framework.dataop.dem;

import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.resamp.Resampling;

import java.io.File;
import java.net.URL;

/**
 * An <code>ElevationModel</code> is used to obtain an elevation above a
 * specified geographical datum for a given geographical position.
 *
 * @author Norman Fomferra
 * @version $Revision$
 */
public interface ElevationModelDescriptor {

    int DEM_INSTALLED = 1;
    int DEM_INSTALLATION_IN_PROGRESS = 2;
    int DEM_INSTALLATION_CANCELED = 3;
    int DEM_INSTALLATION_ERROR = 4;

    /**
     * The name of this elevation source, e.g. "GTOPO30"
     *
     * @return a name
     */
    String getName();

    /**
     * Gets the datum for geographical coordinates interpreted by this elevation source, e.g. WGS-84.
     *
     * @return the datum, e.g. {@link org.esa.beam.framework.dataop.maptransf.Datum#WGS_84}
     */
    Datum getDatum();

    /**
     * Gets the no-data value for this elevation map.
     *
     * @return the  no-data value, e.g. -99999
     */
    float getNoDataValue();

    /**
     * Creates the elevation model instance.
     *
     * @param resampling The resampling method to be used.
     *
     * @return a DEM instance, can be null e.g. if related DEM files are not installed
     *
     * @since BEAM 4.6
     */
    ElevationModel createDem(Resampling resampling);

    /**
     * Returns the directory (if any) where the DEM files shall be located.
     *
     * @return the DEM file directory
     */
    File getDemInstallDir();

    /**
     * Gets the URL of the DEM (ZIP) archive to be used for on-line installaion.
     *
     * @return the URL of the DEM (ZIP) archive
     */
    URL getDemArchiveUrl();

    /**
     * Tests whether or not the DEM and associated files are installed.
     *
     * @return true, if so
     */
    boolean isDemInstalled();

    /**
     * Tests whether or not the DEM is currently being installed.
     *
     * @return true, if so
     */
    boolean isInstallingDem();

    /**
     * Asynchronously installs the files required to use the DEM if not already done or in progress.
     *
     * @param uiComponent an optional UI component which serves as parent for progress monitoring
     *
     * @return true, if the DEM is already installed, is being installed or will be installed. False, if an error occurred
     *         or the user canceled the installation
     */
    boolean installDemFiles(Object uiComponent);

    /**
     * Currently not used.
     */
    int getInstallationStatus();
}
