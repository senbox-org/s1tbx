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
package org.esa.snap.core.dataop.maptransf;

import org.esa.snap.core.datamodel.GeoPos;

/**
 * The <code>UTM</code> class provides useful, static methods for dealing with the UTM map-projection.
 * 
 * @deprecated since BEAM 4.7, use geotools instead.
 */
@Deprecated
public class UTM {

    /**
     * The name of the automatic zone projection.
     */
    public static final String AUTO_PROJECTION_NAME = "UTM Automatic";

    /**
     * The minimum zone index.
     */
    public static final int MIN_UTM_ZONE_INDEX = 0;

    /**
     * The maximum zone index.
     */
    public static final int MAX_UTM_ZONE_INDEX = 59;

    /**
     * The maximum UTM zone number.
     */
    public static final int MAX_UTM_ZONE = 60;

    private static boolean _projectionsRegistered;

    /**
     * Checks whether or not the given map projection is an automatic UTM projection.
     *
     * @param mapProjection the map projection, must not be <code>null</code>
     *
     * @return <code>true</code> if so
     */
    public static boolean isAutoZoneProjection(MapProjection mapProjection) {
        return AUTO_PROJECTION_NAME.equals(mapProjection.getName());
    }
                          
    /**
     * Creates an automatic-zone UTM map projection.
     *
     * @return an automatic-zone UTM map projection
     */
    public static UTMProjection createAutoZoneProjection() {
        return createProjection(AUTO_PROJECTION_NAME, 0, false);
    }


    /**
     * Creates an UTM map projection for the specified parameters.
     *
     * @param zoneIndex the zone index in the range 0 to {@link #MAX_UTM_ZONE} - 1.
     * @param south     whether or not the projection is defined for the southern hemispere
     *
     * @return an UTM map projection
     *
     * @see #getProjectionName
     */
    public static UTMProjection createProjection(int zoneIndex, boolean south) {
        return createProjection(getProjectionName(zoneIndex, south), zoneIndex, south);
    }

    /**
     * Creates a predefined UTM map projection for the specified parameters.
     *
     * @param projName  a name for the projection, e.g. <code>"UTM 33"</code>
     * @param zoneIndex the zone index in the range 0 to {@link #MAX_UTM_ZONE} - 1.
     * @param south     whether or not the projection is defined for the southern hemispere
     *
     * @return an UTM map projection
     */
    public static UTMProjection createProjection(final String projName, int zoneIndex, boolean south) {
        return UTMProjection.create(projName, zoneIndex, south);
    }

    /**
     * Gets a suitable name for the projection given by the specified UTM projection parameters.
     *
     * @param zoneIndex the zone index in the range 0 to {@link #MAX_UTM_ZONE} - 1.
     * @param south     whether or not the projection is defined for the southern hemispere
     *
     * @return a suitable projection name
     */
    public static String getProjectionName(int zoneIndex, boolean south) {
        return "UTM Zone " + (zoneIndex + 1) + (south ? ", South" : "");
    }

    /**
     * Gets the UTM projection parameters for the transverse mercator transformation.
     *
     * @param zoneIndex the zone index in the range 0 to {@link #MAX_UTM_ZONE} - 1.
     * @param south     whether or not the projection is defined for the southern hemispere
     *
     * @return the UTM projection parameters
     */
    public static double[] getProjectionParams(int zoneIndex, boolean south) {
        return new double[]{Ellipsoid.WGS_84.getSemiMajor(), // semi_major
                            Ellipsoid.WGS_84.getSemiMinor(), // semi_minor
                            0.0, // latitude_of_origin (not used)
                            getCentralMeridian(zoneIndex), // central_meridian
                            0.9996, // scale_factor
                            500000.0, // false_easting
                            south ? 10000000.0 : 0.0 // false_northing
        };
    }

    /**
     * Gets the UTM projection suitable for the given geodetic coordinate.
     *
     * @param geoPos a geodetic coordinate
     *
     * @return a suitable UTM projection
     */
    public static MapProjection getSuitableProjection(final GeoPos geoPos) {
        int zoneIndex = getZoneIndex(geoPos.getLon());
        final boolean south = geoPos.getLat() < 0.0;
        final String projName = getProjectionName(zoneIndex, south);
        MapProjection projection = MapProjectionRegistry.getProjection(projName);
        return projection;
    }

    /**
     * Computes the central meridian from the given UTM zone index.
     *
     * @param zoneIndex the zone index in the range 0 to {@link #MAX_UTM_ZONE} - 1.
     *
     * @return the central meridian in the range <code>-180</code> to <code>+180</code> degree.
     */
    public static double getCentralMeridian(int zoneIndex) {
        if (zoneIndex < 0) {
            zoneIndex = 0;
        }
        if (zoneIndex > MAX_UTM_ZONE - 1) {
            zoneIndex = MAX_UTM_ZONE - 1;
        }
        return (zoneIndex + 0.5) * 6.0 - 180.0;
    }

    /**
     * Computes the UTM zone index from the longitude value.
     *
     * @param longitude the longitude in the range <code>-180</code> to <code>+180</code> degrees.
     *
     * @return the zone index in the range 0 to {@link #MAX_UTM_ZONE} - 1.
     */
    public static int getZoneIndex(double longitude) {
        int zoneIndex = (int)Math.round((longitude + 180f) / 6f - 0.5f);
        if (zoneIndex < 0) {
            zoneIndex = 0;
        }
        if (zoneIndex > MAX_UTM_ZONE - 1) {
            zoneIndex = MAX_UTM_ZONE - 1;
        }
        return zoneIndex;
    }

    /**
     * Registers all possible UTM projections in the <code>{@link MapProjectionRegistry}</code>. The projection names
     * have the form "UTM Zone 1" to "UTM Zone 64" for the northern hemisphere and "UTM Zone 1, South" to "UTM Zone 64,
     * South" for the southern. Also contained is the special auto-UTM projection with the name <code>{@link
     * #AUTO_PROJECTION_NAME}</code>.
     */
    public static void registerProjections() {
        if (!_projectionsRegistered) {
            MapProjectionRegistry.registerProjection(createAutoZoneProjection());
            registerProjections(false);
            registerProjections(true);
        }
        _projectionsRegistered = true;
    }

    private UTM() {
    }

    private static void registerProjections(boolean south) {
        for (int zoneIndex = 0; zoneIndex < MAX_UTM_ZONE; zoneIndex++) {
            MapProjection mapProjection = createProjection(zoneIndex, south);
            MapProjectionRegistry.registerProjection(mapProjection);
        }
    }


}

