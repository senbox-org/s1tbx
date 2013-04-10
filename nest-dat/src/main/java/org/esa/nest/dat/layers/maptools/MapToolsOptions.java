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
package org.esa.nest.dat.layers.maptools;

/**
 * map tools options
 */
public class MapToolsOptions {

    private final boolean northArrowShown;
    private final boolean compassShown;
    private final boolean lookDirectionShown;
    private final boolean latLonGridShown;
    private final boolean mapOverviewShown;
    private final boolean scaleShown;
    private boolean placeNamesShown = false;
    private final boolean nestLogoShown;

    public MapToolsOptions(final boolean northArrowShown, final boolean compassShown,
                           final boolean latLonGridShown, final boolean lookDirectionShown,
                           final boolean mapOverviewShown, final boolean scaleShown, final boolean nestLogoShown) {
        this.northArrowShown = northArrowShown;
        this.compassShown = compassShown;
        this.latLonGridShown = latLonGridShown;
        this.lookDirectionShown = lookDirectionShown;
        this.mapOverviewShown = mapOverviewShown;
        this.scaleShown = scaleShown;
        this.nestLogoShown = nestLogoShown;
    }

    public boolean showNorthArrow() {
        return northArrowShown;
    }

    public boolean showCompass() {
        return compassShown;
    }

    public boolean showLookDirection() {
        return lookDirectionShown;
    }

    public boolean showLatLonGrid() {
        return latLonGridShown;
    }

    public boolean showMapOverview() {
        return mapOverviewShown;
    }

    public boolean showScale() {
        return scaleShown;
    }

    public boolean showPlaceNames() {
        return placeNamesShown;
    }

    public boolean showNestLogo() {
        return nestLogoShown;
    }
}
