package org.esa.beam.framework.datamodel;

import org.esa.beam.util.math.DistanceMeasure;
import org.esa.beam.util.math.SinusoidalDistance;

import javax.media.jai.PlanarImage;
import java.awt.image.Raster;

/**
 * A strategy for finding a pixel position for a given geographic position.
 *
 * @author Ralf Quast
 */
class DefaultPixelFinder {

    private static final int MAX_SEARCH_CYCLE_COUNT = 30; // enough for MERIS FSG where we have duplicated pixels

    private final PlanarImage lonImage;
    private final PlanarImage latImage;
    private final PlanarImage maskImage;
    private final double pixelDiagonalSquared;
    private final int imageW;
    private final int imageH;

    DefaultPixelFinder(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage,
                       double pixelDiagonalSquared) {
        this.lonImage = lonImage;
        this.latImage = latImage;
        this.maskImage = maskImage;
        this.pixelDiagonalSquared = pixelDiagonalSquared;

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
        final int searchRadius = 2 * MAX_SEARCH_CYCLE_COUNT;

        int x0 = (int) Math.floor(pixelPos.x);
        int y0 = (int) Math.floor(pixelPos.y);

        if (x0 + searchRadius >= 0 && x0 - searchRadius < imageW && y0 + searchRadius >= 0 && y0 - searchRadius < imageH) {
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

            int x1 = Math.max(x0 - searchRadius, 0);
            int y1 = Math.max(y0 - searchRadius, 0);
            int x2 = Math.min(x0 + searchRadius, imageW - 1);
            int y2 = Math.min(y0 + searchRadius, imageH - 1);

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
            if (maskImage == null || getSample(x0, y0, maskImage) != 0) {
                final double lon = GeoApproximation.normalizeLon(getSampleDouble(x0, y0, lonImage));
                final double lat = getSampleDouble(x0, y0, latImage);
                minDistance = dc.distance(lon, lat);
            } else {
                minDistance = Double.POSITIVE_INFINITY;
            }

            for (int i = 0; i < MAX_SEARCH_CYCLE_COUNT; i++) {
                x1 = x0;
                y1 = y0;

                int minX = Math.max(x1 - 2, rasterMinX);
                int minY = Math.max(y1 - 2, rasterMinY);
                int maxX = Math.min(x1 + 2, rasterMaxX);
                int maxY = Math.min(y1 + 2, rasterMaxY);

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
                        if (y != y0 || x != x0) {
                            if (maskImage == null || getSample(x, y, maskImage) != 0) {
                                final double lon = GeoApproximation.normalizeLon(getSampleDouble(x, y, lonImage));
                                final double lat = getSampleDouble(x, y, latImage);
                                final double d = dc.distance(lon, lat);
                                if (d < minDistance) {
                                    x1 = x;
                                    y1 = y;
                                    minDistance = d;
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
            }
            if (minDistance < pixelDiagonalSquared) {
                pixelPos.setLocation(x0 + 0.5f, y0 + 0.5f);
            } else {
                pixelPos.setInvalid();
            }
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
}
