/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.Maths;

import java.awt.Rectangle;

/**
 * Fill hole pixels in source product with linear interpolations in both x and y directions.
 */

@OperatorMetadata(alias = "Fill-DEM-Hole",
        category = "Raster/DEM Tools",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Fill holes in given DEM product file.")
public final class FillDEMHoleOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBands;

    @Parameter(label = "No Data Value", defaultValue = "0.0")
    private Double NoDataValue = 0.0;

    private int sourceImageWidth;
    private int sourceImageHeight;

    private enum Direction {UP, DOWN, LEFT, RIGHT}

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        ensureSingleRasterSize(sourceProduct);

        try {
            getSourceImageDimension();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add user select bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] srcBands = OperatorUtils.getSourceBands(sourceProduct, sourceBands, false);
        if (!ProductUtils.areRastersEqualInSize(srcBands)) {
            throw new OperatorException("Source bands must all be the same size");
        }
        for (Band srcBand : srcBands) {
            if (srcBand instanceof VirtualBand) {
                ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
            } else {
                ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
            }
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final Rectangle sourceTileRectangle = getSourceRectangle(x0, y0, w, h);
            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final ProductData srcData = sourceTile.getDataBuffer();
            final ProductData trgData = targetTile.getDataBuffer();

            final int maxY = y0 + h;
            final int maxX = x0 + w;
            double v;
            for (int y = y0; y < maxY; ++y) {
                for (int x = x0; x < maxX; ++x) {
                    v = srcData.getElemDoubleAt(sourceTile.getDataBufferIndex(x, y));
                    if (NoDataValue.equals(v)) {
                        v = getPixelValueByInterpolation(x, y, srcData, sourceTile);
                    }
                    trgData.setElemDoubleAt(targetTile.getDataBufferIndex(x, y), v);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw  The target tile width.
     * @param th  The target tile height.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        // extend target rectangle by 20% to all directions
        final int x0 = Math.max(0, tx0 - tw / 5);
        final int y0 = Math.max(0, ty0 - th / 5);
        final int xMax = Math.min(tx0 + tw - 1 + tw / 5, sourceImageWidth);
        final int yMax = Math.min(ty0 + th - 1 + th / 5, sourceImageHeight);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    /**
     * Compute pixel value using linear interpolations in both x and y direction.
     *
     * @param x       The X coordinate of the given pixel.
     * @param y       The Y coordinate of the given pixel.
     * @param srcData The source data.
     * @param srcTile The source tile.
     * @return The interpolated pixel value.
     */
    private double getPixelValueByInterpolation(
            final int x, final int y, final ProductData srcData, final Tile srcTile) {

        final PixelPos pixelUp = new PixelPos(x, y);
        final PixelPos pixelDown = new PixelPos(x, y);
        final PixelPos pixelLeft = new PixelPos(x, y);
        final PixelPos pixelRight = new PixelPos(x, y);

        final double vUp = getNearestNonHolePixelPosition(x, y, srcData, srcTile, pixelUp, Direction.UP);
        final double vDown = getNearestNonHolePixelPosition(x, y, srcData, srcTile, pixelDown, Direction.DOWN);
        final double vLeft = getNearestNonHolePixelPosition(x, y, srcData, srcTile, pixelLeft, Direction.LEFT);
        final double vRight = getNearestNonHolePixelPosition(x, y, srcData, srcTile, pixelRight, Direction.RIGHT);

        double v1 = NoDataValue;
        if (vUp != NoDataValue && vDown != NoDataValue) {
            final double mu = (y - pixelUp.y) / (pixelDown.y - pixelUp.y);
            v1 = Maths.interpolationLinear(vUp, vDown, mu);
        }

        double v2 = NoDataValue;
        if (vLeft != NoDataValue && vRight != NoDataValue) {
            final double mu = (x - pixelLeft.x) / (pixelRight.x - pixelLeft.x);
            v2 = Maths.interpolationLinear(vLeft, vRight, mu);
        }

        if (v1 != NoDataValue && v2 != NoDataValue) {
            return (v1 + v2) / 2.0;
        } else if (v1 != NoDataValue) {
            return v1;
        } else if (v2 != NoDataValue) {
            return v2;
        } else {
            return NoDataValue;
        }
    }

    /**
     * Get the position and value for the nearest non-hole pixel in a given direction.
     *
     * @param x       The X coordinate of the given pixel.
     * @param y       The Y coordinate of the given pixel.
     * @param srcData The source data.
     * @param srcTile The source tile.
     * @param pixel   The pixel position.
     * @param dir     The direction enum which can be "up", "down", "left" and "right".
     * @return The pixel value.
     */
    private double getNearestNonHolePixelPosition(final int x, final int y, final ProductData srcData,
                                                  final Tile srcTile, final PixelPos pixel, final Direction dir) {

        final Rectangle srcTileRectangle = srcTile.getRectangle();
        final int x0 = srcTileRectangle.x;
        final int y0 = srcTileRectangle.y;
        final int w = srcTileRectangle.width;
        final int h = srcTileRectangle.height;

        double v = 0.0;
        if (dir == Direction.UP) {

            for (int yy = y; yy >= y0; yy--) {
                v = srcData.getElemDoubleAt(srcTile.getDataBufferIndex(x, yy));
                if (v != NoDataValue) {
                    pixel.y = yy;
                    return v;
                }
            }

        } else if (dir == Direction.DOWN) {

            for (int yy = y; yy < y0 + h; yy++) {
                v = srcData.getElemDoubleAt(srcTile.getDataBufferIndex(x, yy));
                if (v != NoDataValue) {
                    pixel.y = yy;
                    return v;
                }
            }

        } else if (dir == Direction.LEFT) {

            for (int xx = x; xx >= x0; xx--) {
                v = srcData.getElemDoubleAt(srcTile.getDataBufferIndex(xx, y));
                if (v != NoDataValue) {
                    pixel.x = xx;
                    return v;
                }
            }

        } else if (dir == Direction.RIGHT) {

            for (int xx = x; xx < x0 + w; xx++) {
                v = srcData.getElemDoubleAt(srcTile.getDataBufferIndex(xx, y));
                if (v != NoDataValue) {
                    pixel.x = xx;
                    return v;
                }
            }

        } else {
            throw new OperatorException("Invalid direction");
        }

        return NoDataValue;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FillDEMHoleOp.class);
        }
    }
}
