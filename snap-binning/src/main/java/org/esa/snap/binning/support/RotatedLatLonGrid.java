package org.esa.snap.binning.support;

import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.core.datamodel.Rotator;

/**
 * A {@link PlanetaryGrid} implementation representing a rotated Lat/Lon grid.
 */
public class RotatedLatLonGrid extends PlateCarreeGrid {

    private final Rotator rotator;

    public RotatedLatLonGrid(int numRows, double rotatedLat, double rotatedLon) {
        super(numRows);
        rotator = new Rotator(rotatedLon, rotatedLat);
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        final double[] lons = {lon};
        final double[] lats = {lat};
        rotator.transform(lons, lats);
        return super.getBinIndex(lats[0], lons[0]);
    }

//    @Override
//    public double getCenterLat(int row) {
//        throw new NotImplementedException();
//    }

    @Override
    public double[] getCenterLatLon(long bin) {
        final double[] centerLatLon = super.getCenterLatLon(bin);
        final double[] lons = {centerLatLon[1]};
        final double[] lats = {centerLatLon[0]};
        rotator.transformInversely(lons, lats);
        centerLatLon[0] = lats[0];
        centerLatLon[1] = lons[0];
        return centerLatLon;
    }

    public static double normalizeLat(double lat) {
        while (lat > 90) {
            lat -= 180;
        }
        while (lat < -90) {
            lat += 180;
        }
        return lat;
    }

    public static double normalizeLon(double lon) {
        while (lon > 180) {
            lon -= 360;
        }
        while (lon < -180) {
            lon += 360;
        }
        return lon;
    }

    // For later use when the PlanetaryGrid can be added as SPI
//    public static class Descriptor implements PlanetaryGridDescriptor {
//
//        @Parameter(label = "Number of Grid Rows", defaultValue = 1024 + "",
//                   description = "Number of rows of the global grid.")
//        private int numRows;
//
//        @Parameter(label = "Rotated Longitude", notNull = true,
//                   description = "The longitude value of the coordinate which will become the origin of the rotated grid.")
//        private Integer rotatedLon;
//
//        @Parameter(label = "Rotated Latitude", notNull = true,
//                   description = "The latitude value of the coordinate which will become the origin of the rotated grid.")
//        private Integer rotatedLat;
//
//        @Override
//        public String getName() {
//            return "Rotated Lat/Lon Grid";
//        }
//
//        @Override
//        public PropertySet createGridConfig() {
//            return PropertyContainer.createValueBacked(this.getClass(), new ParameterDescriptorFactory());
//        }
//
//        @Override
//        public PlanetaryGrid createGrid(PropertySet config) {
//            return new ReducedGaussianGrid((Integer) config.getProperty("numRows").getValue());
//        }
//    }
//
}
