/*
 * $Id: ElevationModelDescriptor.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
     * @return a DEM instance, can be null e.g. if related DEM files are not installed
     * @since BEAM 4.6 (resampling parameter)
     * @deprecated since BEAM 4.6, use {@link #createDem(org.esa.beam.framework.dataop.resamp.Resampling)} instead
     */
    @Deprecated
    ElevationModel createDem();

    /**
     * Creates the elevation model instance.
     *
     * @param resampling The resampling method to be used.
     * @return a DEM instance, can be null e.g. if related DEM files are not installed
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
     * @return true, if the DEM is already installed, is being installed or will be installed. False, if an error occured
     *         or the user canceled the installation
     */
    boolean installDemFiles(Object uiComponent);

    /**
     * Currently not used.
     */
    int getInstallationStatus();
}
