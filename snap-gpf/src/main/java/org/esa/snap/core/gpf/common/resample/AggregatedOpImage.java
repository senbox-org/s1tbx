package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.gpf.internal.OperatorContext;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

/**
 * @author Tonio Fincke
 */
class AggregatedOpImage extends GeometricOpImage {

    private static final double EPS = 1e-10;

    private final double scaleX;
    private final double scaleY;
    private final float offsetX;
    private final float offsetY;
    private final double noDataValue;
    private AggregationType aggregationType;
    private final int dataBufferType;

    AggregatedOpImage(RenderedImage sourceImage, ImageLayout layout, double noDataValue, AggregationType aggregationType, int dataBufferType,
                                 AffineTransform sourceImageToModelTransform, AffineTransform referenceImageToModelTransform) throws NoninvertibleTransformException {
        super(vectorize(sourceImage), layout, null, true, createBorderExtender(noDataValue), null,
              createBackground(noDataValue));
        this.noDataValue = noDataValue;
        final AffineTransform transform = new AffineTransform(referenceImageToModelTransform);
        transform.concatenate(sourceImageToModelTransform.createInverse());
        scaleX = transform.getScaleX();
        scaleY = transform.getScaleY();
        offsetX = (float) (referenceImageToModelTransform.getTranslateX() / sourceImageToModelTransform.getScaleX()) -
                (float) (sourceImageToModelTransform.getTranslateX() / sourceImageToModelTransform.getScaleX());
        offsetY = (float) (referenceImageToModelTransform.getTranslateY() / sourceImageToModelTransform.getScaleY()) -
                (float) (sourceImageToModelTransform.getTranslateY() / sourceImageToModelTransform.getScaleY());
        this.aggregationType = aggregationType;
        this.dataBufferType = dataBufferType;
        OperatorContext.setTileCache(this);
    }

    private static BorderExtender createBorderExtender(double value) {
        return new BorderExtenderConstant(new double[]{value});
    }

    private static double[] createBackground(double value) {
        return new double[]{value};
    }

    /**
     * The sources are cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        final Rectangle srcRect = mapDestRect(destRect, 0);
        int dstH = destRect.height;
        int dstW = destRect.width;

        RasterAccessor srcAccessor = new RasterAccessor(source, srcRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());
        final Aggregator aggregator = AggregatorFactory.createAggregator(aggregationType, dataBufferType);
        aggregator.init(srcAccessor, dstAccessor, noDataValue);

        for (int dstY = 0; dstY < dstH; dstY++) {
            double srcYF0 = scaleY * dstY;
            double srcYF1 = srcYF0 + scaleY;
            int srcY0 = (int) srcYF0;
            int srcY1 = (int) srcYF1;
            double wy0 = 1.0 - (srcYF0 - srcY0);
            double wy1 = srcYF1 - srcY1;
            if (wy1 < EPS) {
                wy1 = 1.0;
                if (srcY1 > srcY0) {
                    srcY1--;
                }
            }
            final int dstYIndexOffset = dstAccessor.getBandOffset(0) + dstY * dstAccessor.getScanlineStride();
            for (int dstX = 0; dstX < dstW; dstX++) {
                double srcXF0 = scaleX * dstX;
                double srcXF1 = srcXF0 + scaleX;
                int srcX0 = (int) srcXF0;
                int srcX1 = (int) srcXF1;
                double wx0 = 1.0 - (srcXF0 - srcX0);
                double wx1 = srcXF1 - srcX1;
                if (wx1 < EPS) {
                    wx1 = 1.0;
                    if (srcX1 > srcX0) {
                        srcX1--;
                    }
                }
                aggregator.aggregate(srcY0, srcY1, srcX0, srcX1, srcAccessor.getScanlineStride(), wx0, wx1, wy0, wy1,
                                     dstYIndexOffset + dstX);
            }
        }
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    @Override
    protected Rectangle forwardMapRect(Rectangle rectangle, int i) {
        //calculates the dest rectangle for a source rectangle
        final int x = (int) (rectangle.getX() * (1 / scaleX) - offsetX);
        final int y = (int) (rectangle.getY() * (1 / scaleY) - offsetY);
        final int width = (int) Math.ceil(rectangle.getWidth() * (1 / scaleX));
        final int height = (int) Math.ceil(rectangle.getHeight() * (1 / scaleY));
        return new Rectangle(x, y, width, height);
    }

    @Override
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {
        if(destRect == null) {
            throw new IllegalArgumentException("destRect must not be null");
        } else if(sourceIndex >= 0 && sourceIndex < this.getNumSources()) {
            return backwardMapRect(destRect, sourceIndex);
        } else {
            throw new IllegalArgumentException("Invalid source index");
        }
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle rectangle, int i) {
        //calculates the source rectangle for a dest rectangle
        final int x = (int) (offsetX + rectangle.getX() * scaleX);
        final int y = (int) (offsetY + rectangle.getY() * scaleY);
        final int width = (int) Math.ceil(rectangle.getWidth() * scaleX);
        final int height = (int) Math.ceil(rectangle.getHeight() * scaleY);
        return new Rectangle(x, y, width, height);
    }

}
