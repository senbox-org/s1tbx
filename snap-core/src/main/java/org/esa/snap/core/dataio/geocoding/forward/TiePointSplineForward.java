package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.util.SplineInterpolator;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

public class TiePointSplineForward extends TiePointForward {

    private GeoRaster geoRaster;
    private ThreadLocal<InterpolationContext> contextTL;

    private int gridWidth;
    private int gridHeight;

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        checkGeoRaster(geoRaster);

        this.geoRaster = geoRaster;

        contextTL = ThreadLocal.withInitial(() -> {
            final InterpolationContext context = new InterpolationContext();
            if (containsAntiMeridian) {
                context.lonInterpolator = new AntiMeridianLonInterpolator();
            } else {
                context.lonInterpolator = new StandardLonInterpolator();
            }

            context.latSubset = new double[3][3];
            context.lonSubset = new double[3][3];
            context.xSpline = Integer.MIN_VALUE;
            context.ySpline = Integer.MIN_VALUE;
            return context;
        });

        gridWidth = geoRaster.getRasterWidth();
        gridHeight = geoRaster.getRasterHeight();
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }

        final double x = pixelPos.x - geoRaster.getOffsetX();
        final double y = pixelPos.y - geoRaster.getOffsetY();

        if (x < 0 || x > geoRaster.getSceneWidth()
                || y < 0 || y > geoRaster.getSceneHeight()) {
            geoPos.setInvalid();
        } else {
            final InterpolationContext ic = contextTL.get();

            // pixel position in tie-point coordinate system
            double fi = x / geoRaster.getSubsamplingX();
            double fj = y / geoRaster.getSubsamplingY();

            // tie-point start indices for 3x3 Spline interpolator
            int i = (int) (fi / 2) * 2;
            if (i + 2 >= gridWidth) {
                i = gridWidth - 3;
            }
            int j = (int) (fj / 2) * 2;
            if (j + 2 >= gridHeight) {
                j = gridHeight - 3;
            }

            // pixel position relative to Spline interpolator
            fi = fi - i;
            fj = fj - j;

            // if we're in the same 3x3 spline area, we do not need to copy the data again.
            if (i != ic.xSpline || j != ic.ySpline) {
                copyInterpolationSubset(gridWidth, i, j);
                ic.xSpline = i;
                ic.ySpline = j;
            }

            geoPos.lon = ic.lonInterpolator.interpolate(fi, fj);
            geoPos.lat = SplineInterpolator.interpolate2d(ic.latSubset, fi, fj);
        }

        return geoPos;
    }

    @Override
    public void dispose() {
        geoRaster = null;
        if (contextTL != null) {
            contextTL.remove();
            contextTL = null;
        }
    }

    private void copyInterpolationSubset(int gridWidth, int i, int j) {
        final InterpolationContext ic = contextTL.get();
        final double[] lonTiePoints = geoRaster.getLongitudes();
        System.arraycopy(lonTiePoints, j * gridWidth + i, ic.lonSubset[0], 0, 3);
        System.arraycopy(lonTiePoints, (j + 1) * gridWidth + i, ic.lonSubset[1], 0, 3);
        System.arraycopy(lonTiePoints, (j + 2) * gridWidth + i, ic.lonSubset[2], 0, 3);
        ic.lonInterpolator.setData(ic.lonSubset);

        final double[] latTiePoints = geoRaster.getLatitudes();
        System.arraycopy(latTiePoints, j * gridWidth + i, ic.latSubset[0], 0, 3);
        System.arraycopy(latTiePoints, (j + 1) * gridWidth + i, ic.latSubset[1], 0, 3);
        System.arraycopy(latTiePoints, (j + 2) * gridWidth + i, ic.latSubset[2], 0, 3);
    }

    private interface LonInterpolator {
        void setData(double[][] longitudes);

        double interpolate(double fi, double fj);
    }

    static class StandardLonInterpolator implements LonInterpolator {
        private double[][] longitudes;

        public void setData(double[][] longitudes) {
            this.longitudes = longitudes;
        }

        public double interpolate(double fi, double fj) {
            return SplineInterpolator.interpolate2d(this.longitudes, fi, fj);
        }
    }

    static class AntiMeridianLonInterpolator implements LonInterpolator {
        private static final double OFFSET = 360.0;
        private double[][] longitudes;

        public void setData(double[][] longitudes) {
            this.longitudes = longitudes;

            boolean containsAntiMeridian = false;
            for (int k = 0; k < 3; k++) {
                for (int l = 1; l < 3; l++) {
                    final double delta = Math.abs(this.longitudes[k][l] - this.longitudes[k][l - 1]);
                    if (delta > 180.0) {
                        containsAntiMeridian = true;
                        break;
                    }
                }
                if (containsAntiMeridian) {
                    break;
                }
            }

            if (containsAntiMeridian) {
                for (int k = 0; k < 3; k++) {
                    for (int l = 0; l < 3; l++) {
                        if (this.longitudes[k][l] < 0.0) {
                            this.longitudes[k][l] += OFFSET;
                        }
                    }
                }
            }
        }

        public double interpolate(double fi, double fj) {
            final double lon = SplineInterpolator.interpolate2d(this.longitudes, fi, fj);

            return lon > 180.0 ? lon - OFFSET : lon;
        }
    }

    private static class InterpolationContext {
        double[][] latSubset;
        double[][] lonSubset;
        LonInterpolator lonInterpolator;
        int xSpline;
        int ySpline;
    }
}


