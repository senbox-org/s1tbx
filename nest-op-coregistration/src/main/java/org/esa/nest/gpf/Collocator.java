package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ProductUtils;

import java.awt.*;

/**
 * Collocates two products based on their geocoding
 */
public class Collocator {

    private final PixelPos[] sourcePixelPositions;
    private final Rectangle sourceRectangle;
    private final Operator operator;

    public Collocator(final Operator op, final Product srcProduct, final Product trgProduct, final Rectangle trgRectangle) {
        this.operator = op;
        sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                srcProduct.getGeoCoding(),
                srcProduct.getSceneRasterWidth(),
                srcProduct.getSceneRasterHeight(),
                trgProduct.getGeoCoding(),
                trgRectangle);

        sourceRectangle = getBoundingBox(
                sourcePixelPositions,
                srcProduct.getSceneRasterWidth(),
                srcProduct.getSceneRasterHeight());
    }

    public void collocateSourceBand(final RasterDataNode sourceBand,
                                    final Tile targetTile, final Resampling selectedResampling) throws OperatorException {

        final RasterDataNode targetBand = targetTile.getRasterDataNode();
        final Rectangle targetRectangle = targetTile.getRectangle();
        final ProductData trgBuffer = targetTile.getDataBuffer();

        final float noDataValue = (float) targetBand.getGeophysicalNoDataValue();
        final int maxX = targetRectangle.x + targetRectangle.width;
        final int maxY = targetRectangle.y + targetRectangle.height;

        Tile sourceTile = null;
        if(sourceRectangle!=null)
            sourceTile = operator.getSourceTile(sourceBand, sourceRectangle);

        if (sourceTile != null) {
            final Product srcProduct = sourceBand.getProduct();
            final int sourceRasterHeight = srcProduct.getSceneRasterHeight();
            final int sourceRasterWidth = srcProduct.getSceneRasterWidth();

            final Resampling resampling;
            if (isFlagBand(sourceBand) || isValidPixelExpressionUsed(sourceBand)) {
                resampling = Resampling.NEAREST_NEIGHBOUR;
            } else {
                resampling = selectedResampling;
            }

            final Resampling.Index resamplingIndex = resampling.createIndex();
            final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

            for (int y = targetRectangle.y, index = 0; y < maxY; ++y) {
                for (int x = targetRectangle.x; x < maxX; ++x, ++index) {
                    final PixelPos sourcePixelPos = sourcePixelPositions[index];

                    final int trgIndex = targetTile.getDataBufferIndex(x, y);
                    if (sourcePixelPos != null) {
                        resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                                sourceRasterWidth, sourceRasterHeight, resamplingIndex);
                        try {
                            double sample = resampling.resample(resamplingRaster, resamplingIndex);
                            if (Double.isNaN(sample)) {
                                sample = noDataValue;
                            }
                            trgBuffer.setElemDoubleAt(trgIndex, sample);
                        } catch (Exception e) {
                            throw new OperatorException(e.getMessage());
                        }
                    } else {
                        trgBuffer.setElemDoubleAt(trgIndex, noDataValue);
                    }
                }
            }
            sourceTile.getDataBuffer().dispose();
        } else {
            final TileIndex trgIndex = new TileIndex(targetTile);
            for (int y = targetRectangle.y, index = 0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = targetRectangle.x; x < maxX; ++x, ++index) {
                    trgBuffer.setElemDoubleAt(trgIndex.getIndex(x), noDataValue);
                }
            }
        }
    }

    private static boolean isFlagBand(final RasterDataNode sourceRaster) {
        return (sourceRaster instanceof Band && ((Band) sourceRaster).isFlagBand());
    }

    private static boolean isValidPixelExpressionUsed(final RasterDataNode sourceRaster) {
        final String validPixelExpression = sourceRaster.getValidPixelExpression();
        return validPixelExpression != null && !validPixelExpression.trim().isEmpty();
    }

    private static Rectangle getBoundingBox(final PixelPos[] pixelPositions, final int maxWidth, final int maxHeight) {
        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (final PixelPos pixelsPos : pixelPositions) {
            if (pixelsPos != null) {
                final int x = (int) Math.floor(pixelsPos.getX());
                final int y = (int) Math.floor(pixelsPos.getY());

                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 4, 0);
        maxX = Math.min(maxX + 4, maxWidth - 1);
        minY = Math.max(minY - 4, 0);
        maxY = Math.min(maxY + 4, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }


    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;
        private final ProductData dataBuffer;

        public ResamplingRaster(final Tile tile) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            this.usesNoData = rasterDataNode.isNoDataValueUsed();
            this.noDataValue = rasterDataNode.getNoDataValue();
            this.geophysicalNoDataValue = rasterDataNode.getGeophysicalNoDataValue();
            this.scalingApplied = rasterDataNode.isScalingApplied();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;
            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {

                    samples[i][j] = dataBuffer.getElemDoubleAt(tile.getDataBufferIndex(x[j], y[i]));

                    if (usesNoData) {
                        if(scalingApplied && geophysicalNoDataValue == samples[i][j] || noDataValue == samples[i][j]) {
                            samples[i][j] = Double.NaN;
                            allValid = false;
                        }
                    }
                }
            }
            return allValid;
        }
    }
}
