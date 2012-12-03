package org.esa.nest.dat.layers.maptools;

/**
 * map tools options
 */
public class MapToolsOptions {

    private final boolean compassShown;
    private final boolean lookDirectionShown;
    private final boolean latLonGridShown;
    private final boolean mapOverviewShown;
    private boolean placeNamesShown = false;
    private final boolean nestLogoShown;

    public MapToolsOptions(final boolean compassShown,
                           final boolean latLonGridShown, final boolean lookDirectionShown,
                           final boolean mapOverviewShown, final boolean nestLogoShown) {
        this.compassShown = compassShown;
        this.latLonGridShown = latLonGridShown;
        this.lookDirectionShown = lookDirectionShown;
        this.mapOverviewShown = mapOverviewShown;
        this.nestLogoShown = nestLogoShown;
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

    public boolean showPlaceNames() {
        return placeNamesShown;
    }

    public boolean showNestLogo() {
        return nestLogoShown;
    }
}
