package org.esa.snap.core.datamodel;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.snap.core.util.math.DistanceMeasure;
import org.esa.snap.core.util.math.SinusoidalDistance;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;

/**
 * @author Ralf Quast
 * @author Tonio Fincke
 */
class PixelFinder {

    private static final int MAX_SEARCH_CYCLE_COUNT = 30; // enough for MERIS FSG where we have duplicated pixels

    private final PlanarImage lonImage;
    private final PlanarImage latImage;
    private final PlanarImage maskImage;
    private final double halfPixelDiagonal;
    private final int imageW;
    private final int imageH;
    private final PixelPosCreator pixelFindingStrategy;
    private final int searchRadius;

    PixelFinder(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage, double halfPixelDiagonal,
                boolean fractionAccuracy, int searchRadius) {
        this.lonImage = lonImage;
        this.latImage = latImage;
        this.maskImage = maskImage;
        this.halfPixelDiagonal = halfPixelDiagonal;
        this.searchRadius = searchRadius;
        if (fractionAccuracy) {
            pixelFindingStrategy = new FractionPixelPosCreator();
        } else {
            pixelFindingStrategy = new DefaultPixelPosCreator();
        }

        imageW = lonImage.getWidth();
        imageH = lonImage.getHeight();
    }

    /**
     * Returns the pixel position for a given geographic position.
     *
     * @param geoPos   the geographic position.
     * @param pixelPos the pixel position.
     */
    void findPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        final int searchableExtent = 2 * MAX_SEARCH_CYCLE_COUNT;

        int x0 = (int) Math.floor(pixelPos.x);
        int y0 = (int) Math.floor(pixelPos.y);

        if (x0 + searchableExtent >= 0 && x0 - searchableExtent < imageW && y0 +
                searchableExtent >= 0 && y0 - searchableExtent < imageH) {
            if (x0 < 0) {
                x0 = 0;
            } else if (x0 >= imageW) {
                x0 = imageW - 1;
            }
            if (y0 < 0) {
                y0 = 0;
            } else if (y0 >= imageH) {
                y0 = imageH - 1;
            }

            int x1 = Math.max(x0 - searchableExtent, 0);
            int y1 = Math.max(y0 - searchableExtent, 0);
            int x2 = Math.min(x0 + searchableExtent, imageW - 1);
            int y2 = Math.min(y0 + searchableExtent, imageH - 1);

            final int rasterMinX = x1;
            final int rasterMinY = y1;
            @SuppressWarnings("UnnecessaryLocalVariable")
            final int rasterMaxX = x2;
            @SuppressWarnings("UnnecessaryLocalVariable")
            final int rasterMaxY = y2;

            final double lon0 = GeoApproximation.normalizeLon(geoPos.lon);
            final double lat0 = geoPos.lat;
            final DistanceMeasure dc = new SinusoidalDistance(lon0, lat0);

            double minDistance;
            GeoPos bestGeoPos;
            if (maskImage == null || getSample(x0, y0, maskImage) != 0) {
                final double lon = GeoApproximation.normalizeLon(getSampleDouble(x0, y0, lonImage));
                final double lat = getSampleDouble(x0, y0, latImage);
                bestGeoPos = new GeoPos(lat, lon);
                minDistance = dc.distance(lon, lat);
            } else {
                bestGeoPos = null;
                minDistance = Double.POSITIVE_INFINITY;
            }

            Rectangle knownRectangle = new Rectangle(x0, y0, 1, 1);
            for (int i = 0; i < MAX_SEARCH_CYCLE_COUNT; i++) {
                x1 = x0;
                y1 = y0;

                int minX = Math.max(x1 - searchRadius, rasterMinX);
                int minY = Math.max(y1 - searchRadius, rasterMinY);
                int maxX = Math.min(x1 + searchRadius, rasterMaxX);
                int maxY = Math.min(y1 + searchRadius, rasterMaxY);

                if (maskImage != null) {
                    // enlarge the search region in across-track direction (useful for e.g. MERIS FSG where we have duplicated pixels)
                    while (minX > rasterMinX) {
                        if (getSample(minX, y1, maskImage) != 0) {
                            break;
                        }
                        if (minX > rasterMinX) {
                            minX--;
                        }
                    }
                    while (maxX < rasterMaxX) {
                        if (getSample(maxX, y1, maskImage) != 0) {
                            break;
                        }
                        if (maxX < rasterMaxX) {
                            maxX++;
                        }
                    }
                }
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        if (!knownRectangle.contains(x, y)) {
                            if (maskImage == null || getSample(x, y, maskImage) != 0) {
                                final double lon = GeoApproximation.normalizeLon(getSampleDouble(x, y, lonImage));
                                final double lat = getSampleDouble(x, y, latImage);
                                final double d = dc.distance(lon, lat);
                                if (d < minDistance) {
                                    bestGeoPos = new GeoPos(lat, lon);
                                    minDistance = d;
                                    x1 = x;
                                    y1 = y;
                                }
                            }
                        }
                    }
                }
                if (x1 == x0 && y1 == y0) {
                    break;
                }
                x0 = x1;
                y0 = y1;
                knownRectangle = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
            }
            pixelPos.setLocation(pixelFindingStrategy.createPixelPos(lat0, lon0, minDistance, x0, y0, bestGeoPos));
        } else {
            pixelPos.setInvalid();
        }
    }

    private static double getSampleDouble(int pixelX, int pixelY, PlanarImage image) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);

        return data.getSampleDouble(x, y, 0);
    }

    private static int getSample(int pixelX, int pixelY, PlanarImage image) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);

        return data.getSample(x, y, 0);
    }

    interface PixelPosCreator {

        PixelPos createPixelPos(double lat0, double lon0, double minDistance, int bestX, int bestY, GeoPos bestGeoPos);

    }

    private class DefaultPixelPosCreator implements PixelPosCreator {

        @Override
        public PixelPos createPixelPos(double lat0, double lon0, double minDistance, int bestX, int bestY, GeoPos bestGeoPos) {
            final PixelPos pixelPos = new PixelPos();
            if (minDistance < halfPixelDiagonal) {
                pixelPos.setLocation(bestX + 0.5, bestY + 0.5);
            } else {
                pixelPos.setInvalid();
            }
            return pixelPos;
        }
    }

    private class FractionPixelPosCreator implements PixelPosCreator {

        private final static int upper_left_quadrant = 0;
        private final static int upper_right_quadrant = 1;
        private final static int lower_left_quadrant = 2;
        private final static int lower_right_quadrant = 3;

        private int centerX;
        private int centerY;
        private GeoPos centerGeoPos;
        private GeoPos leftGeoPos;
        private GeoPos upperGeoPos;
        private GeoPos rightGeoPos;
        private GeoPos lowerGeoPos;

        @Override
        public PixelPos createPixelPos(double lat0, double lon0, double minDistance, int x, int y, GeoPos geoPos) {
            this.centerX = x;
            this.centerY = y;
            this.centerGeoPos = geoPos;
            leftGeoPos = new GeoPos();
            upperGeoPos = new GeoPos();
            rightGeoPos = new GeoPos();
            lowerGeoPos = new GeoPos();
            boolean onLeftBorder = x <= 0;
            boolean onUpperBorder = y <= 0;
            boolean onRightBorder = x >= imageW - 1;
            boolean onLowerBorder = y >= imageH - 1;
            if (!onLeftBorder) {
                leftGeoPos = initGeoPos(x - 1, y);
            }
            if (!onUpperBorder) {
                upperGeoPos = initGeoPos(x, y - 1);
            }
            if (!onRightBorder) {
                rightGeoPos = initGeoPos(x + 1, y);
            }
            if (!onLowerBorder) {
                lowerGeoPos = initGeoPos(x, y + 1);
            }

            if (onLeftBorder && onUpperBorder) {
                return getPixelPosFromLowerRight(lat0, lon0);
            } else if (onLeftBorder && onLowerBorder) {
                return getPixelPosFromUpperRight(lat0, lon0);
            } else if (onRightBorder && onUpperBorder) {
                return getPixelPosFromLowerLeft(lat0, lon0);
            } else if (onRightBorder && onLowerBorder) {
                return getPixelPosFromUpperLeft(lat0, lon0);
            }

            simulateGeoPosIfNecessary(onLeftBorder, leftGeoPos, rightGeoPos);
            simulateGeoPosIfNecessary(onUpperBorder, upperGeoPos, lowerGeoPos);
            simulateGeoPosIfNecessary(onRightBorder, rightGeoPos, leftGeoPos);
            simulateGeoPosIfNecessary(onLowerBorder, lowerGeoPos, upperGeoPos);

            final int quadrant = determineQuadrant(lat0, lon0);

            if (quadrant == upper_left_quadrant) {
                if (onLeftBorder) {
                    return getPixelPosFromUpperRight(lat0, lon0);
                } else if (onUpperBorder) {
                    return getPixelPosFromLowerLeft(lat0, lon0);
                } else {
                    return getPixelPosFromUpperLeft(lat0, lon0);
                }
            } else if (quadrant == upper_right_quadrant) {
                if (onRightBorder) {
                    return getPixelPosFromUpperLeft(lat0, lon0);
                } else if (onUpperBorder) {
                    return getPixelPosFromLowerRight(lat0, lon0);
                } else {
                    return getPixelPosFromUpperRight(lat0, lon0);
                }
            } else if (quadrant == lower_left_quadrant) {
                if (onLeftBorder) {
                    return getPixelPosFromLowerRight(lat0, lon0);
                } else if (onLowerBorder) {
                    return getPixelPosFromUpperLeft(lat0, lon0);
                } else {
                    return getPixelPosFromLowerLeft(lat0, lon0);
                }
            } else {
                if (onRightBorder) {
                    return getPixelPosFromLowerLeft(lat0, lon0);
                } else if (onLowerBorder) {
                    return getPixelPosFromUpperRight(lat0, lon0);
                } else {
                    return getPixelPosFromLowerRight(lat0, lon0);
                }
            }
        }

        private GeoPos initGeoPos(int x, int y) {
            final double lon = GeoApproximation.normalizeLon(getSampleDouble(x, y, lonImage));
            final double lat = getSampleDouble(x, y, latImage);
            return new GeoPos(lat, lon);
        }

        private int determineQuadrant(final double lat, final double lon) {
            boolean isAboveLeftCenterLine = determineWhetherIsAbove(leftGeoPos, lat, lon);
            boolean isLeftToUpperCenterLine = determineWhetherIsLeftTo(upperGeoPos, lat, lon);
            if (isAboveLeftCenterLine && isLeftToUpperCenterLine) {
                return upper_left_quadrant;
            }
            boolean isAboveRightCenterLine = determineWhetherIsAbove(rightGeoPos, lat, lon);
            if (isAboveRightCenterLine && !isLeftToUpperCenterLine) {
                return upper_right_quadrant;
            }
            boolean isLeftToLowerCenterLine = determineWhetherIsLeftTo(lowerGeoPos, lat, lon);
            if (isLeftToLowerCenterLine && !isAboveLeftCenterLine) {
                return lower_left_quadrant;
            }
            return lower_right_quadrant;
        }

        private boolean determineWhetherIsAbove(GeoPos geoPos, double lat, double lon) {
            if (!(Math.abs(centerGeoPos.getLat() - geoPos.getLat()) < 1e-8)) {
                double mToLeft = (centerGeoPos.getLon() - geoPos.getLon()) /
                        (centerGeoPos.getLat() - geoPos.getLat());
                double calculatedLon = mToLeft * (lat - geoPos.getLat()) + geoPos.getLon();
                double calculatedUpperLon = mToLeft * (upperGeoPos.getLat() - geoPos.getLat()) + geoPos.getLon();
                double diff = calculatedLon - lon;
                double upperDiff = calculatedUpperLon - upperGeoPos.getLon();
                return (diff >= 0 && upperDiff >= 0) || (diff < 0 && upperDiff < 0);
            } else {
                return (lat >= centerGeoPos.getLat() && upperGeoPos.getLat() >= centerGeoPos.getLat()) ||
                        (lat < centerGeoPos.getLat() && upperGeoPos.getLat() < centerGeoPos.getLat());
            }
        }

        private boolean determineWhetherIsLeftTo(GeoPos geoPos, double lat, double lon) {
            if (!(Math.abs(centerGeoPos.getLat() - geoPos.getLat()) < 1e-8)) {
                double mToUpper = (centerGeoPos.getLon() - geoPos.getLon()) /
                        (centerGeoPos.getLat() - geoPos.getLat());
                double calculatedLon = mToUpper * (lat - geoPos.getLat()) + geoPos.getLon();
                double calculatedLeftLon = mToUpper * (leftGeoPos.getLat() - geoPos.getLat()) + geoPos.getLon();
                double diff = calculatedLon - lon;
                double leftDiff = calculatedLeftLon - leftGeoPos.getLon();
                return (diff >= 0 && leftDiff >= 0) || (diff < 0 && leftDiff < 0);
            } else {
                return (lon >= centerGeoPos.getLon() && leftGeoPos.getLon() >= centerGeoPos.getLon()) ||
                        (lon < centerGeoPos.getLon() && leftGeoPos.getLon() < centerGeoPos.getLon());
            }
        }

        private GeoPos simulateGeoPosIfNecessary(boolean simulate, GeoPos geoPos, GeoPos counterGeoPos) {
            if (simulate) {
                double lonDiff = centerGeoPos.getLon() - counterGeoPos.getLon();
                double latDiff = centerGeoPos.getLat() - counterGeoPos.getLat();
                final double simulatedLat = centerGeoPos.getLat() + latDiff;
                final double simulatedLon = centerGeoPos.getLon() + lonDiff;
                geoPos.setLocation(simulatedLat, simulatedLon);
            }
            return geoPos;
        }

        private PixelPos getPixelPosFromUpperLeft(final double lat, final double lon) {
            GeoPos[] geoPoses = new GeoPos[]{centerGeoPos, upperGeoPos, leftGeoPos};
            int[] xPositions = new int[]{centerX, centerX, centerX - 1};
            int[] yPositions = new int[]{centerY, centerY - 1, centerY};
            return getPixelPosWithFractionAccuracy(lat, lon, geoPoses, xPositions, yPositions);
        }

        private PixelPos getPixelPosFromUpperRight(final double lat, final double lon) {
            GeoPos[] geoPoses = new GeoPos[]{centerGeoPos, upperGeoPos, rightGeoPos};
            int[] xPositions = new int[]{centerX, centerX, centerX + 1};
            int[] yPositions = new int[]{centerY, centerY - 1, centerY};
            return getPixelPosWithFractionAccuracy(lat, lon, geoPoses, xPositions, yPositions);
        }

        private PixelPos getPixelPosFromLowerLeft(final double lat, final double lon) {
            GeoPos[] geoPoses = new GeoPos[]{centerGeoPos, lowerGeoPos, leftGeoPos};
            int[] xPositions = new int[]{centerX, centerX, centerX - 1};
            int[] yPositions = new int[]{centerY, centerY + 1, centerY};
            return getPixelPosWithFractionAccuracy(lat, lon, geoPoses, xPositions, yPositions);
        }

        private PixelPos getPixelPosFromLowerRight(final double lat, final double lon) {
            GeoPos[] geoPoses = new GeoPos[]{centerGeoPos, lowerGeoPos, rightGeoPos};
            int[] xPositions = new int[]{centerX, centerX, centerX + 1};
            int[] yPositions = new int[]{centerY, centerY + 1, centerY};
            return getPixelPosWithFractionAccuracy(lat, lon, geoPoses, xPositions, yPositions);
        }

        private PixelPos getPixelPosWithFractionAccuracy(final double lat, final double lon, GeoPos[] geoPositions,
                                                         int[] xPositions, int[] yPositions) {
            //todo make this the exception: Only use when we know we are not within the pixel
            final PixelPos pixelPos = new PixelPos();
            if (!geoPositions[1].isValid() || !geoPositions[2].isValid()) {
                pixelPos.setInvalid();
                return pixelPos;
            }
            final Matrix mA = new Matrix(3, 3);
            mA.set(0, 0, 1.0);
            mA.set(1, 0, 1.0);
            mA.set(2, 0, 1.0);
            mA.set(0, 1, geoPositions[0].getLat());
            mA.set(1, 1, geoPositions[1].getLat());
            mA.set(2, 1, geoPositions[2].getLat());
            mA.set(0, 2, geoPositions[0].getLon());
            mA.set(1, 2, geoPositions[1].getLon());
            mA.set(2, 2, geoPositions[2].getLon());
            final LUDecomposition decomp = new LUDecomposition(mA);

            final Matrix mB = new Matrix(3, 1);
            mB.set(0, 0, yPositions[0] + 0.5);
            mB.set(1, 0, yPositions[1] + 0.5);
            mB.set(2, 0, yPositions[2] + 0.5);

            Matrix mY;
            try {
                mY = decomp.solve(mB);
            } catch (Exception e) {
                pixelPos.setInvalid();
                return pixelPos;
            }

            mB.set(0, 0, xPositions[0] + 0.5);
            mB.set(1, 0, xPositions[1] + 0.5);
            mB.set(2, 0, xPositions[2] + 0.5);
            Matrix mX;
            try {
                mX = decomp.solve(mB);
            } catch (Exception e) {
                pixelPos.setInvalid();
                return pixelPos;
            }

            final double fx = mX.get(0, 0) + mX.get(1, 0) * lat + mX.get(2, 0) * lon;
            final double fy = mY.get(0, 0) + mY.get(1, 0) * lat + mY.get(2, 0) * lon;

            pixelPos.setLocation(fx, fy);
            return pixelPos;
        }

    }

}
