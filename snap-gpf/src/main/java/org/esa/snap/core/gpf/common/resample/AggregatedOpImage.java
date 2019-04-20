package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;
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
    private Downsampling downsampling;
    private final int dataBufferType;
    private RasterDataNode rasterDataNode;

    AggregatedOpImage(RasterDataNode rasterDataNode, RenderedImage sourceImage, ImageLayout layout, double noDataValue, Downsampling downsampling, int dataBufferType,
                      AffineTransform sourceImageToModelTransform, AffineTransform referenceImageToModelTransform) throws NoninvertibleTransformException {
        super(vectorize(sourceImage), layout, null, true, createBorderExtender(noDataValue), null,
                createBackground(noDataValue));
        this.rasterDataNode = rasterDataNode;
        this.noDataValue = noDataValue;
        final AffineTransform transform = new AffineTransform(referenceImageToModelTransform);
        transform.concatenate(sourceImageToModelTransform.createInverse());
        scaleX = transform.getScaleX();
        scaleY = transform.getScaleY();
        offsetX = (float) (referenceImageToModelTransform.getTranslateX() / sourceImageToModelTransform.getScaleX()) -
                (float) (sourceImageToModelTransform.getTranslateX() / sourceImageToModelTransform.getScaleX());
        offsetY = (float) (referenceImageToModelTransform.getTranslateY() / sourceImageToModelTransform.getScaleY()) -
                (float) (sourceImageToModelTransform.getTranslateY() / sourceImageToModelTransform.getScaleY());
        this.downsampling = downsampling;
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
        //final Aggregator aggregator = AggregatorFactory.createAggregator(downsampling, dataBufferType);
        Aggregator aggregator = downsampling.createDownsampler(rasterDataNode, dataBufferType);
        aggregator.init(rasterDataNode, srcAccessor, dstAccessor, noDataValue);

        for (int dstY = 0; dstY < dstH; dstY++) {
            double srcYFO0 = offsetY + scaleY * (dstY + destRect.y);
            double srcYFO1 = srcYFO0 + scaleY;
            int srcYO0 = (int) srcYFO0;
            int srcYO1 = (int) srcYFO1;
            double wy0 = 1.0 - (srcYFO0 - srcYO0);
            double wy1 = srcYFO1 - srcYO1;
            int srcY0 = srcYO0 - srcRect.y;
            int srcY1 = srcYO1 - srcRect.y;
            if (wy1 < EPS) {
                wy1 = 1.0;
                if (srcY1 > srcY0) {
                    srcY1--;
                }
            }
            final int dstYIndexOffset = dstAccessor.getBandOffset(0) + dstY * dstAccessor.getScanlineStride();
            for (int dstX = 0; dstX < dstW; dstX++) {
                double srcXFO0 = offsetX + scaleX * (dstX + destRect.x);
                double srcXFO1 = srcXFO0 + scaleX;
                int srcXO0 = (int) srcXFO0;
                int srcXO1 = (int) srcXFO1;
                double wx0 = 1.0 - (srcXFO0 - srcXO0);
                double wx1 = srcXFO1 - srcXO1;
                int srcX0 = srcXO0 - srcRect.x;
                int srcX1 = srcXO1 - srcRect.x;
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
        final int x = (int) Math.floor(rectangle.getX() * (1 / scaleX) - offsetX);
        final int y = (int) Math.floor(rectangle.getY() * (1 / scaleY) - offsetY);
        final int width = (int) Math.ceil((rectangle.getX() + rectangle.getWidth()) * (1 / scaleX) - offsetX) - x;
        final int height = (int) Math.ceil((rectangle.getY() + rectangle.getHeight()) * (1 / scaleY) - offsetY) - y;
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
        final int x = (int) Math.floor(offsetX + rectangle.getX() * scaleX);
        final int y = (int) Math.floor(offsetY + rectangle.getY() * scaleY);
        final int width = (int) Math.ceil(offsetX + (rectangle.getX() + rectangle.getWidth()) * scaleX) - x;
        final int height = (int) Math.ceil(offsetY + (rectangle.getY() + rectangle.getHeight()) * scaleY) - y;
        return new Rectangle(x, y, width, height);
    }

}
