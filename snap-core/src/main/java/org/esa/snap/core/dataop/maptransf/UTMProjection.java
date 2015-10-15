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

/**
 * This class represents the UTM map projection.
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.TransverseMercator} instead.
 */
@Deprecated
public class UTMProjection extends MapProjection {

    private final int _zoneIndex;
    private final boolean _south;

    /**
     * Creates a new UTM projection instance.
     *
     * @param zoneIndex the zne as zero-based index, e.g. 31 for zone 32
     * @param south     false, if the zone is on the northern hemisphere
     *
     * @return the new projection
     */
    public static UTMProjection create(int zoneIndex, boolean south) {
        return create(UTM.getProjectionName(zoneIndex, south), zoneIndex, south);
    }

    /**
     * Creates a new UTM projection instance.
     *
     * @param name      the projection name, e.g. "UTM Zone 32"
     * @param zoneIndex the zne as zero-based index, e.g. 31 for zone 32
     * @param south     false, if the zone is on the northern hemisphere
     *
     * @return the new projection
     */
    public static UTMProjection create(final String name, int zoneIndex, boolean south) {
        final double[] params = UTM.getProjectionParams(zoneIndex, south);
        final MapTransform mapTransform = MapTransformFactory.createTransform("Transverse_Mercator", params);
        final UTMProjection mapProjection = new UTMProjection(name, mapTransform, zoneIndex, south);
        return mapProjection;
    }

    /**
     * Gets the UTM zone as zero-based index: zoneIndex = zone - 1
     *
     * @return the UTM zone as zero-based index
     */
    public int getZoneIndex() {
        return _zoneIndex;
    }

    /**
     * Gets the UTM zone.
     *
     * @return <code>{@link #getZoneIndex()} + 1</code>
     */
    public int getZone() {
        return getZoneIndex() + 1;
    }

    /**
     * This mehtod allways returns <code>false</code>, because UTM projections does not allow editing the
     * transformation parameters.
     *
     * @return <code>false</code> because UTM projections does not allow editing the transformation parameters.
     */
    @Override
    public boolean hasMapTransformUI() {
        return false;
    }

    /**
     * Tests if this UTM zone is on the northern hemisphere.
     *
     * @return true, if so
     */
    public boolean isNorth() {
        return !isSouth();
    }

    /**
     * Tests if this UTM zone is on the southern hemisphere.
     *
     * @return true, if so
     */
    public boolean isSouth() {
        return _south;
    }

    private UTMProjection(String name, MapTransform mapTransform, int zoneIndex, boolean south) {
        super(name, mapTransform, true);
        _zoneIndex = zoneIndex;
        _south = south;
    }
}
