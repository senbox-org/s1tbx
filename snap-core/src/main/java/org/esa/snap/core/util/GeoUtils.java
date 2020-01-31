package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.*;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import static org.esa.snap.core.util.ProductUtils.assemblePathList;

public class GeoUtils {

    /**
     * Creates the geographical boundary of the given product and returns it as a list of geographical coordinates.
     *
     * @param product the input product, must not be null
     * @param step    the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(Product product, int step) {
        final Rectangle rect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        return createGeoBoundary(product, rect, step, true);
    }

    /**
     * Creates the geographical boundary of the given region within the given product and returns it as a list of
     * geographical coordinates.
     *
     * @param product        the input product, must not be null
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the boundary
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(Product product, Rectangle region, int step,
                                             final boolean usePixelCenter) {
        final GeoCoding gc = product.getSceneGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }

        if (region == null) {
            region = new Rectangle(0,
                    0,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight());
        }

        return getGeoBoundary(gc, region, step, usePixelCenter);
    }

    /**
     * Creates the geographical boundary of the given region within the given raster and returns it as a list of
     * geographical coordinates.
     *
     * @param raster the input raster, must not be null
     * @param region the region rectangle in raster pixel coordinates, can be null for entire raster
     * @param step   the step given in pixels
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if raster is null or if the raster has no {@link GeoCoding} is null
     * @see #createPixelBoundary(RasterDataNode, java.awt.Rectangle, int)
     */
    public static GeoPos[] createGeoBoundary(RasterDataNode raster, Rectangle region, int step) {
        return createGeoBoundary(raster, region, step, true);
    }

    /**
     * Creates the geographical boundary of the given region within the given RasterDataNode and returns it as a list of
     * geographical coordinates.
     *
     * @param rasterDataNode the input rasterDataNode, must not be null
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the boundary
     * @return an array of geographical coordinates
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     */
    public static GeoPos[] createGeoBoundary(RasterDataNode rasterDataNode, Rectangle region, int step,
                                             final boolean usePixelCenter) {
        final GeoCoding gc = rasterDataNode.getGeoCoding();
        if (gc == null) {
            throw new IllegalArgumentException(UtilConstants.MSG_NO_GEO_CODING);
        }

        if (region == null) {
            region = new Rectangle(0,
                    0,
                    rasterDataNode.getRasterWidth(),
                    rasterDataNode.getRasterHeight());
        }

        return getGeoBoundary(gc, region, step, usePixelCenter);
    }

    // @todo 1 tb/tb implement this! 2020-01-31
    public static ArrayList<GeoPos[]> createGeoBoundaries(Product product, Rectangle region, int step,
                                                          final boolean usePixelCenter) {
        throw new RuntimeException("not implemented");
    }

    /**
     * Converts the geographic boundary of the region within the given product into one, two or three shape objects. If
     * the product does not intersect the 180 degree meridian, a single general path is returned. Otherwise two or three
     * shapes are created and returned in the order from west to east.
     * <p>
     * The geographic boundary of the given product are returned as shapes comprising (longitude,latitude) pairs.
     *
     * @param product        the input product
     * @param region         the region rectangle in product pixel coordinates, can be null for entire product
     * @param step           the step given in pixels
     * @param usePixelCenter {@code true} if the pixel center should be used to create the pathes
     * @return an array of shape objects
     * @throws IllegalArgumentException if product is null or if the product's {@link GeoCoding} is null
     * @see #createGeoBoundary(Product, java.awt.Rectangle, int, boolean)
     */
    public static GeneralPath[] createGeoBoundaryPaths(Product product, Rectangle region, int step,
                                                       final boolean usePixelCenter) {
        final GeoPos[] geoPoints = createGeoBoundary(product, region, step, usePixelCenter);
        ProductUtils.normalizeGeoPolygon(geoPoints);

        final ArrayList<GeneralPath> pathList = assemblePathList(geoPoints);

        return pathList.toArray(new GeneralPath[0]);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a raster boundary expressed in geographical
     * co-ordinates.
     *
     * @param raster the raster
     * @param rect   the source rectangle
     * @param step   the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(RasterDataNode raster, Rectangle rect, int step) {
        final int width = raster.getRasterWidth();
        final int height = raster.getRasterHeight();
        return createPixelBoundary(width, height, rect, step);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a raster boundary expressed in geographical
     * co-ordinates.
     *
     * @param rasterWidth  the raster width in pixels
     * @param rasterHeight the raster height in pixels
     * @param rect         the source rectangle
     * @param step         the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createPixelBoundary(int rasterWidth, int rasterHeight, Rectangle rect, int step) {
        if (rect == null) {
            rect = new Rectangle(0,
                    0,
                    rasterWidth,
                    rasterHeight);
        }
        return createRectBoundary(rect, step);
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p> This method delegates to {@link #createRectBoundary(java.awt.Rectangle, int, boolean) createRectBoundary(Rectangle, int, boolean)}
     * and the additional boolean parameter {@code usePixelCenter} is {@code true}.
     *
     * @param rect the source rectangle
     * @param step the mean distance from one pixel position to the other in the returned array
     * @return the rectangular boundary
     */
    public static PixelPos[] createRectBoundary(Rectangle rect, int step) {
        final boolean usePixelCenter = true;
        return createRectBoundary(rect, step, usePixelCenter);
    }

    private static GeoPos[] getGeoBoundary(GeoCoding gc, Rectangle region, int step, boolean usePixelCenter) {
        final PixelPos[] points = createRectBoundary(region, step, usePixelCenter);
        ArrayList<GeoPos> geoPoints = new ArrayList<>(points.length);
        boolean calculateInsets = false;
        for (final PixelPos pixelPos : points) {
            final GeoPos gcGeoPos = gc.getGeoPos(pixelPos, null);
            if (!gcGeoPos.isValid()) {
                calculateInsets = true;
                break;
            }
            geoPoints.add(gcGeoPos);
        }

        if (calculateInsets) {
            geoPoints.clear();
            geoPoints = createInsetsGeoBoundary(gc, step, region, usePixelCenter);
        }

        return geoPoints.toArray(new GeoPos[0]);
    }

    private static ArrayList<GeoPos> createInsetsGeoBoundary(GeoCoding gc, int step, Rectangle rect, boolean usePixelCenter) {
        final double insetDistance = usePixelCenter ? 0.5 : 0.0;
        int xStart = rect.x;
        int yStart = rect.y;
        final int w = usePixelCenter ? rect.width - 1 : rect.width;
        final int h = usePixelCenter ? rect.height - 1 : rect.height;
        int xEnd = xStart + w;
        int yEnd = yStart + h;

        final ArrayList<GeoPos> geoPosList = new ArrayList<>();

        final int validYMin = getFirstValid_Y(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validYMin >= yEnd) {
            return geoPosList;  // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }
        final int validYMax = getLastValid_Y(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validYMax <= validYMin) {
            return geoPosList;  // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }

        yStart = validYMin;
        yEnd = validYMax;

        final int validXMin = getFirstValid_X(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validXMin >= xEnd) {
            return geoPosList; // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }
        final int validXMax = getLastValid_X(gc, insetDistance, xStart, yStart, xEnd, yEnd);
        if (validXMax <= validXMin) {
            return geoPosList; // empty list, we do not have valid geo positions within the rectangle tb 2020-01-31
        }

        xStart = validXMin;
        xEnd = validXMax;

        PixelPos pixelPos;
        GeoPos geoPos;

        int lastX = 0;
        for (int x = xStart; x < xEnd; x += step) {
            final double xPos = x + insetDistance;
            pixelPos = new PixelPos(xPos, yStart + insetDistance);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // increase y until we have a valid pixel.
                for (int y = yStart + 1; y < yEnd; y++) {
                    pixelPos = new PixelPos(xPos, y + insetDistance);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
            lastX = x;
        }

        int lastY = 0;
        for (int y = yStart; y < yEnd; y += step) {
            final double yPos = y + insetDistance;
            pixelPos = new PixelPos(xEnd, yPos);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // decrease x until we have a valid pixel
                for (int x = xEnd; x >= xStart; x--) {
                    pixelPos = new PixelPos(x, yPos);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
            lastY = y;
        }

        // add corner pixel, if it is a valid one
        pixelPos = new PixelPos(xEnd, yEnd);
        geoPos = gc.getGeoPos(pixelPos, null);
        if (geoPos.isValid()) {
            geoPosList.add(geoPos);
        }

        for (int x = lastX; x > xStart; x -= step) {
            final double xPos = x + insetDistance;
            pixelPos = new PixelPos(xPos, yEnd + insetDistance);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // decrease y until we have a valid pixel.
                for (int y = yEnd - 1; y >= yStart; y--) {
                    pixelPos = new PixelPos(xPos, y + insetDistance);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
        }

        // add corner pixel, if it is a valid one
        pixelPos = new PixelPos(xStart, yEnd);
        geoPos = gc.getGeoPos(pixelPos, null);
        if (geoPos.isValid()) {
            geoPosList.add(geoPos);
        }

        for (int y = lastY; y > yStart; y -= step) {
            final double yPos = y + insetDistance;
            pixelPos = new PixelPos(xStart + insetDistance, yPos);
            geoPos = gc.getGeoPos(pixelPos, null);
            if (geoPos.isValid()) {
                geoPosList.add(geoPos);
            } else {
                // increase x until we have a valid pixel
                for (int x = xStart; x < xEnd; x++) {
                    pixelPos = new PixelPos(x + insetDistance, yPos);
                    geoPos = gc.getGeoPos(pixelPos, null);
                    if (geoPos.isValid()) {
                        geoPosList.add(geoPos);
                        break;
                    }
                }
            }
        }

        return geoPosList;
    }

    private static int getFirstValid_Y(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return y;
                }
            }
        }
        return yEnd;
    }

    private static int getLastValid_Y(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int y = yEnd; y >= yStart; y--) {
            for (int x = xStart; x < xEnd; x++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return y;
                }
            }
        }
        return yStart;
    }

    private static int getFirstValid_X(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int x = xStart; x < xEnd; x++) {
            for (int y = yStart; y < yEnd; y++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return x;
                }
            }
        }

        return xEnd;
    }

    private static int getLastValid_X(GeoCoding gc, double insetDistance, int xStart, int yStart, int xEnd, int yEnd) {
        for (int x = xEnd; x >= xStart; x--) {
            for (int y = yStart; y < yEnd; y++) {
                final PixelPos pixelPos = new PixelPos(x + insetDistance, y + insetDistance);
                final GeoPos geoPos = gc.getGeoPos(pixelPos, null);
                if (geoPos.isValid()) {
                    return x;
                }
            }
        }

        return xEnd;
    }

    /**
     * Creates a rectangular boundary expressed in pixel positions for the given source rectangle. If the source
     * {@code rect} is 100 x 50 pixels and {@code step} is 10 the returned array will countain exactly 2 * 10
     * + 2 * (5 - 2) = 26 pixel positions.
     * <p>
     * This method is used for an intermediate step when determining a product boundary expressed in geographical
     * co-ordinates.
     * <p>
     *
     * @param rect           the source rectangle
     * @param step           the mean distance from one pixel position to the other in the returned array
     * @param usePixelCenter {@code true} if the pixel center should be used
     * @return the rectangular boundary
     */
    static PixelPos[] createRectBoundary(final Rectangle rect, int step, final boolean usePixelCenter) {
        // package access for testing only tb 2020-01-30
        final double insetDistance = usePixelCenter ? 0.5 : 0.0;
        final int x1 = rect.x;
        final int y1 = rect.y;
        final int w = usePixelCenter ? rect.width - 1 : rect.width;
        final int h = usePixelCenter ? rect.height - 1 : rect.height;
        final int x2 = x1 + w;
        final int y2 = y1 + h;

        if (step <= 0) {
            step = 2 * Math.max(rect.width, rect.height); // don't step!
        }

        final ArrayList<PixelPos> pixelPosList = new ArrayList<>(2 * (rect.width + rect.height) / step + 10);

        int lastX = 0;
        for (int x = x1; x < x2; x += step) {
            pixelPosList.add(new PixelPos(x + insetDistance, y1 + insetDistance));
            lastX = x;
        }

        int lastY = 0;
        for (int y = y1; y < y2; y += step) {
            pixelPosList.add(new PixelPos(x2 + insetDistance, y + insetDistance));
            lastY = y;
        }

        pixelPosList.add(new PixelPos(x2 + insetDistance, y2 + insetDistance));

        for (int x = lastX; x > x1; x -= step) {
            pixelPosList.add(new PixelPos(x + insetDistance, y2 + insetDistance));
        }

        pixelPosList.add(new PixelPos(x1 + insetDistance, y2 + insetDistance));

        for (int y = lastY; y > y1; y -= step) {
            pixelPosList.add(new PixelPos(x1 + insetDistance, y + insetDistance));
        }

        return pixelPosList.toArray(new PixelPos[0]);
    }
}
