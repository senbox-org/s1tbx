package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.*;

import java.awt.*;
import java.util.ArrayList;

public class GeoUtils {

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

        final PixelPos[] points = createRectBoundary(region, step, usePixelCenter);
        final ArrayList<GeoPos> geoPoints = new ArrayList<>(points.length);
        for (final PixelPos pixelPos : points) {
            final GeoPos gcGeoPos = gc.getGeoPos(pixelPos, null);
            geoPoints.add(gcGeoPos);
        }
        return geoPoints.toArray(new GeoPos[0]);
    }

    public static ArrayList<GeoPos[]> createGeoBoundaries(Product product, Rectangle region, int step,
                                                          final boolean usePixelCenter) {
        throw new RuntimeException("not implemented");
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
