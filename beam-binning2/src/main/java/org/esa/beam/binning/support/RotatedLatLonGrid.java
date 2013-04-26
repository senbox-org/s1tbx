package org.esa.beam.binning.support;

import org.esa.beam.binning.PlanetaryGrid;

/**
 * A {@link PlanetaryGrid} implementation representing a rotated Lat/Lon grid.
 */
public class RotatedLatLonGrid implements PlanetaryGrid {

    @Override
    public long getBinIndex(double lat, double lon) {
        return 0;
    }

    @Override
    public int getRowIndex(long bin) {
        return 0;
    }

    @Override
    public long getNumBins() {
        return 0;
    }

    @Override
    public int getNumRows() {
        return 0;
    }

    @Override
    public int getNumCols(int row) {
        return 0;
    }

    @Override
    public long getFirstBinIndex(int row) {
        return 0;
    }

    @Override
    public double getCenterLat(int row) {
        return 0;
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        return new double[0];
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
